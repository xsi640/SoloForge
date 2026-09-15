#Requires -Version 7.0
param(
  [string]$Jar = "$PSScriptRoot/../backend/target/board.jar",
  [int]$Port = 18080,
  [string]$ArtifactDir = "$PSScriptRoot/../test-artifacts/smoke",
  [switch]$KeepRunning
)

$ErrorActionPreference = 'Stop'
$BaseUrl = "http://localhost:$Port"

$script:Total = 0
$script:Failed = 0

$script:Results = @()

function Check {
  param([string]$Name, [bool]$Condition, [string]$Detail = '')
  $script:Total++
  if ($Condition) {
    $script:Results += [pscustomobject]@{ Name = $Name; Passed = $true; Detail = '' }
    Write-Host ("  PASS  " + $Name) -ForegroundColor Green
  } else {
    $script:Failed++
    $script:Results += [pscustomobject]@{ Name = $Name; Passed = $false; Detail = $Detail }
    Write-Host ("  FAIL  " + $Name + "  " + $Detail) -ForegroundColor Red
  }
}

function New-Session { New-Object Microsoft.PowerShell.Commands.WebRequestSession }

function Invoke-Api {
  param(
    [string]$Method,
    [string]$Path,
    $Body = $null,
    $Session
  )
  if (-not $Session) { $Session = New-Session }
  $params = @{
    Uri                 = "$BaseUrl$Path"
    Method              = $Method
    SkipHttpErrorCheck  = $true
    WebSession          = $Session
  }
  if ($null -ne $Body) {
    $params.ContentType = 'application/json; charset=utf-8'
    $params.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
  }
  $resp = Invoke-WebRequest @params
  $json = $null
  if ($resp.Content) {
    try { $json = $resp.Content | ConvertFrom-Json } catch { $json = $null }
  }
  [pscustomobject]@{
    Status  = [int]$resp.StatusCode
    Code    = if ($json) { $json.code } else { $null }
    Message = if ($json) { $json.message } else { $null }
    Data    = if ($json) { $json.data } else { $null }
  }
}

function FieldErrorOf {
  param($Response, [string]$Field)
  if (-not $Response.Data) { return '' }
  $items = @($Response.Data)
  $hit = $items | Where-Object { $_.field -eq $Field } | Select-Object -First 1
  if ($hit) { return $hit.reason } else { return '' }
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runDir = Join-Path $ArtifactDir "run-$stamp"
$workDir = Join-Path $runDir 'runtime'
New-Item -ItemType Directory -Force -Path $workDir | Out-Null
$logFile = Join-Path $workDir 'app.log'

Write-Host ""
Write-Host "SoloForge 团队任务看板 —— 接口冒烟测试" -ForegroundColor Cyan
Write-Host "测试产物目录：$ArtifactDir"
Write-Host "本次运行目录：$runDir"
Write-Host ""

if (-not (Test-Path $Jar)) {
  Write-Host "找不到 jar：$Jar" -ForegroundColor Red
  exit 2
}

$env:BOARD_DB_URL = "jdbc:h2:file:./data/board;DB_CLOSE_DELAY=-1"
$env:BOARD_ADMIN_USERNAME = 'admin'
$env:BOARD_ADMIN_DISPLAY_NAME = '管理员'
$env:BOARD_ADMIN_PASSWORD = 'admin123'
$env:BOARD_PORT = "$Port"

$proc = Start-Process -FilePath 'java' `
  -ArgumentList @('-jar', (Resolve-Path $Jar).Path) `
  -WorkingDirectory $workDir `
  -PassThru -WindowStyle Hidden `
  -RedirectStandardOutput (Join-Path $workDir 'out.log') `
  -RedirectStandardError $logFile

try {
  $ready = $false
  for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Milliseconds 1000
    if ($proc.HasExited) { break }
    try {
      $probe = Invoke-WebRequest -Uri "$BaseUrl/api/auth/me" -SkipHttpErrorCheck -TimeoutSec 3
      if ($probe.StatusCode -eq 401) { $ready = $true; break }
    } catch { }
  }
  if (-not $ready) {
    Write-Host "服务未能启动，日志尾部：" -ForegroundColor Red
    Get-Content $logFile -Tail 40 -ErrorAction SilentlyContinue | Write-Host
    exit 2
  }

  $admin = New-Session

  Write-Host "[API-001 登录]" -ForegroundColor Yellow
  $r = Invoke-Api POST '/api/auth/login' @{ username = 'admin'; password = 'wrong-password' } $admin
  Check "错误密码返回 400 / code 1004" ($r.Status -eq 400 -and $r.Code -eq 1004) "实际 status=$($r.Status) code=$($r.Code)"

  $r = Invoke-Api POST '/api/auth/login' @{ username = 'admin'; password = 'admin123' } $admin
  Check "管理员登录成功 code 0" ($r.Status -eq 200 -and $r.Code -eq 0) "实际 status=$($r.Status) code=$($r.Code)"
  Check "返回登录名与管理员标记" ($r.Data.username -eq 'admin' -and $r.Data.isAdmin -eq $true) "实际 data=$($r.Data | ConvertTo-Json -Compress)"

  Write-Host "[API-003 当前用户]" -ForegroundColor Yellow
  $r = Invoke-Api GET '/api/auth/me' $null $admin
  Check "GET /api/auth/me 返回当前用户" ($r.Code -eq 0 -and $r.Data.username -eq 'admin') "实际 code=$($r.Code)"

  Write-Host "[鉴权：未登录]" -ForegroundColor Yellow
  $anon = New-Session
  $r = Invoke-Api GET '/api/board' $null $anon
  Check "未登录访问看板返回 401 / code 1002" ($r.Status -eq 401 -and $r.Code -eq 1002) "实际 status=$($r.Status) code=$($r.Code)"

  Write-Host "[API-008 / API-009 看板与状态列]" -ForegroundColor Yellow
  $r = Invoke-Api GET '/api/board' $null $admin
  Check "GET /api/board 返回唯一看板" ($r.Code -eq 0 -and $r.Data.name -eq '团队任务看板') "实际 code=$($r.Code)"

  $r = Invoke-Api GET '/api/board/columns' $null $admin
  $cols = @($r.Data)
  Check "GET /api/board/columns 返回三列" ($r.Code -eq 0 -and $cols.Count -eq 3) "实际 count=$($cols.Count)"
  Check "列顺序为 待办/进行中/完成" ($cols[0].code -eq 'TODO' -and $cols[1].code -eq 'DOING' -and $cols[2].code -eq 'DONE') "实际 $($cols.name -join ',')"
  $todoId = $cols[0].id; $doingId = $cols[1].id

  Write-Host "[API-005 / API-004 成员管理]" -ForegroundColor Yellow
  $r = Invoke-Api POST '/api/members' @{ username = 'tester1'; displayName = '测试成员'; password = 'test123456' } $admin
  Check "新增成员成功" ($r.Code -eq 0 -and $r.Data.username -eq 'tester1') "实际 code=$($r.Code) msg=$($r.Message)"
  $memberId = $r.Data.id

  $r = Invoke-Api POST '/api/members' @{ username = 'tester1'; displayName = '重复'; password = 'test123456' } $admin
  Check "登录名重复返回 400 / code 1005" ($r.Status -eq 400 -and $r.Code -eq 1005) "实际 status=$($r.Status) code=$($r.Code)"
  Check "重复登录名带字段级提示 username" ((FieldErrorOf $r 'username') -ne '') "实际 data=$($r.Data | ConvertTo-Json -Compress)"

  $r = Invoke-Api GET '/api/members' $null $admin
  Check "成员列表包含停用与新增成员" ($r.Code -eq 0 -and @($r.Data).Count -ge 2) "实际 count=$(@($r.Data).Count)"

  Write-Host "[API-002 越权访问]" -ForegroundColor Yellow
  $nonAdmin = New-Session
  $r = Invoke-Api POST '/api/auth/login' @{ username = 'tester1'; password = 'test123456' } $nonAdmin
  Check "普通成员可登录" ($r.Code -eq 0 -and $r.Data.isAdmin -eq $false) "实际 code=$($r.Code)"
  $testerUserId = $r.Data.id
  $r = Invoke-Api GET '/api/members' $null $nonAdmin
  Check "非管理员访问成员接口返回 403 / code 1008" ($r.Status -eq 403 -and $r.Code -eq 1008) "实际 status=$($r.Status) code=$($r.Code)"

  Write-Host "[API-010 新建任务与校验]" -ForegroundColor Yellow
  $r = Invoke-Api POST '/api/tasks' @{ title = '   '; assigneeId = $testerUserId; columnId = $todoId } $admin
  Check "空标题返回 400 / code 1001" ($r.Status -eq 400 -and $r.Code -eq 1001) "实际 status=$($r.Status) code=$($r.Code)"
  Check "空标题带字段级提示 title" ((FieldErrorOf $r 'title') -ne '') "实际 data=$($r.Data | ConvertTo-Json -Compress)"

  $r = Invoke-Api POST '/api/tasks' @{ title = '不存在的负责人'; assigneeId = 999999; columnId = $todoId } $admin
  Check "负责人不存在返回 400 / code 1001" ($r.Status -eq 400 -and $r.Code -eq 1001) "实际 status=$($r.Status) code=$($r.Code)"

  $r = Invoke-Api POST '/api/tasks' @{ title = '优先级非法'; assigneeId = $testerUserId; columnId = $todoId; priority = 'URGENT' } $admin
  Check "优先级非法返回 400" ($r.Status -eq 400 -and $r.Code -eq 1001) "实际 status=$($r.Status) code=$($r.Code)"

  $today = Get-Date
  $overdueDate = $today.AddDays(-1).ToString('yyyy-MM-dd')
  $soonDate = $today.ToString('yyyy-MM-dd')
  $normalDate = $today.AddDays(5).ToString('yyyy-MM-dd')

  $r = Invoke-Api POST '/api/tasks' @{ title = '冒烟任务-超期'; assigneeId = $testerUserId; columnId = $todoId; priority = 'HIGH'; dueDate = $overdueDate } $admin
  Check "新建任务成功且优先级为 HIGH" ($r.Code -eq 0 -and $r.Data.priority -eq 'HIGH') "实际 code=$($r.Code) msg=$($r.Message)"
  Check "截止日期为昨天判定 OVERDUE" ($r.Data.dueState -eq 'OVERDUE') "实际 dueState=$($r.Data.dueState)"
  $overdueTaskId = $r.Data.id

  $r = Invoke-Api POST '/api/tasks' @{ title = '冒烟任务-临期'; assigneeId = $testerUserId; columnId = $todoId; dueDate = $soonDate } $admin
  Check "缺省优先级为 MEDIUM" ($r.Data.priority -eq 'MEDIUM') "实际 priority=$($r.Data.priority)"
  Check "截止日期为当天判定 DUE_SOON" ($r.Data.dueState -eq 'DUE_SOON') "实际 dueState=$($r.Data.dueState)"

  $r = Invoke-Api POST '/api/tasks' @{ title = '冒烟任务-正常'; assigneeId = $testerUserId; columnId = $doingId; dueDate = $normalDate } $admin
  Check "截止日期较远判定 NORMAL" ($r.Data.dueState -eq 'NORMAL') "实际 dueState=$($r.Data.dueState)"
  $normalTaskId = $r.Data.id

  $r = Invoke-Api POST '/api/tasks' @{ title = '冒烟任务-无日期'; assigneeId = $testerUserId; columnId = $todoId } $admin
  Check "无截止日期判定 NONE" ($r.Data.dueState -eq 'NONE') "实际 dueState=$($r.Data.dueState)"
  $noDateTaskId = $r.Data.id

  Write-Host "[API-013 / API-011 任务详情与编辑]" -ForegroundColor Yellow
  $r = Invoke-Api GET "/api/tasks/$normalTaskId" $null $admin
  Check "任务详情返回负责人显示名" ($r.Code -eq 0 -and $r.Data.assigneeName -eq '测试成员') "实际 code=$($r.Code) name=$($r.Data.assigneeName)"
  Check "任务详情返回列编码 DOING" ($r.Data.columnCode -eq 'DOING') "实际 columnCode=$($r.Data.columnCode)"

  $r = Invoke-Api GET '/api/tasks/999999' $null $admin
  Check "任务不存在返回 404 / code 1006" ($r.Status -eq 404 -and $r.Code -eq 1006) "实际 status=$($r.Status) code=$($r.Code)"

  $r = Invoke-Api PUT "/api/tasks/$normalTaskId" @{ title = '冒烟任务-已编辑'; assigneeId = $testerUserId; columnId = $doingId; priority = 'LOW'; description = '编辑后的描述' } $admin
  Check "编辑任务成功" ($r.Code -eq 0 -and $r.Data.title -eq '冒烟任务-已编辑') "实际 code=$($r.Code) msg=$($r.Message)"
  Check "编辑后优先级与描述已更新" ($r.Data.priority -eq 'LOW' -and $r.Data.description -eq '编辑后的描述') "实际 priority=$($r.Data.priority)"

  Write-Host "[API-014 状态更新]" -ForegroundColor Yellow
  $r = Invoke-Api PATCH "/api/tasks/$normalTaskId/status" @{ columnId = $todoId } $admin
  Check "拖拽换列成功，状态变更为 TODO" ($r.Code -eq 0 -and $r.Data.columnCode -eq 'TODO') "实际 code=$($r.Code) columnCode=$($r.Data.columnCode)"

  $r = Invoke-Api PATCH "/api/tasks/$normalTaskId/status" @{ columnId = $todoId } $admin
  Check "切换到同一列不报错" ($r.Code -eq 0) "实际 code=$($r.Code)"

  $r = Invoke-Api PATCH "/api/tasks/$normalTaskId/status" @{ columnId = 999999 } $admin
  Check "目标列不存在返回 400 / code 1001" ($r.Status -eq 400 -and $r.Code -eq 1001) "实际 status=$($r.Status) code=$($r.Code)"

  Write-Host "[API-015 条件查询]" -ForegroundColor Yellow
  $r = Invoke-Api GET '/api/tasks' $null $admin
  Check "无条件下返回全部任务" ($r.Code -eq 0 -and @($r.Data).Count -eq 4) "实际 count=$(@($r.Data).Count)"

  $r = Invoke-Api GET '/api/tasks?keyword=已编辑' $null $admin
  Check "关键词匹配标题" ($r.Code -eq 0 -and @($r.Data).Count -eq 1) "实际 count=$(@($r.Data).Count)"

  $r = Invoke-Api GET '/api/tasks?keyword=编辑后的描述' $null $admin
  Check "关键词匹配描述" ($r.Code -eq 0 -and @($r.Data).Count -eq 1) "实际 count=$(@($r.Data).Count)"

  $r = Invoke-Api GET "/api/tasks?assigneeId=$testerUserId" $null $admin
  Check "按负责人筛选" ($r.Code -eq 0 -and @($r.Data).Count -eq 4) "实际 count=$(@($r.Data).Count)"

  $r = Invoke-Api GET '/api/tasks?dueFilter=OVERDUE' $null $admin
  Check "按已超期筛选命中 1 条" ($r.Code -eq 0 -and @($r.Data).Count -eq 1) "实际 count=$(@($r.Data).Count)"

  $r = Invoke-Api GET '/api/tasks?dueFilter=DUE_SOON' $null $admin
  Check "按临期筛选命中 1 条" ($r.Code -eq 0 -and @($r.Data).Count -eq 1) "实际 count=$(@($r.Data).Count)"

  $r = Invoke-Api GET '/api/tasks?priority=HIGH&dueFilter=OVERDUE' $null $admin
  Check "条件组合为与关系" ($r.Code -eq 0 -and @($r.Data).Count -eq 1) "实际 count=$(@($r.Data).Count)"

  $r = Invoke-Api GET '/api/tasks?dueFilter=LAST_WEEK' $null $admin
  Check "非法 dueFilter 返回 400" ($r.Status -eq 400 -and $r.Code -eq 1001) "实际 status=$($r.Status) code=$($r.Code)"

  Write-Host "[API-016 我的提醒]" -ForegroundColor Yellow
  $r = Invoke-Api GET '/api/reminders/mine' $null $admin
  Check "管理员名下无临期超期任务" ($r.Code -eq 0 -and @($r.Data.items).Count -eq 0 -and $r.Data.overdueCount -eq 0) "实际 items=$(@($r.Data.items).Count) overdue=$($r.Data.overdueCount)"

  $r = Invoke-Api GET '/api/reminders/mine' $null $nonAdmin
  Check "普通成员提醒含 1 超期 1 临期" ($r.Code -eq 0 -and $r.Data.overdueCount -eq 1 -and $r.Data.dueSoonCount -eq 1) "实际 overdue=$($r.Data.overdueCount) soon=$($r.Data.dueSoonCount)"
  Check "提醒条目超期在前" (@($r.Data.items)[0].dueState -eq 'OVERDUE') "实际 首条=$(@($r.Data.items)[0].dueState)"

  Write-Host "[API-006 / API-007 密码与状态]" -ForegroundColor Yellow
  $r = Invoke-Api PUT "/api/members/$memberId/password" @{ password = 'newpass123' } $admin
  Check "重置密码成功" ($r.Code -eq 0) "实际 code=$($r.Code)"

  $relogin = New-Session
  $r = Invoke-Api POST '/api/auth/login' @{ username = 'tester1'; password = 'newpass123' } $relogin
  Check "被重置成员可用新密码登录" ($r.Code -eq 0) "实际 code=$($r.Code)"

  $r = Invoke-Api POST '/api/auth/login' @{ username = 'tester1'; password = 'test123456' } (New-Session)
  Check "旧密码已失效" ($r.Status -eq 400 -and $r.Code -eq 1004) "实际 status=$($r.Status) code=$($r.Code)"

  $r = Invoke-Api PUT "/api/members/$memberId/status" @{ status = 'DISABLED' } $admin
  Check "停用成员成功" ($r.Code -eq 0 -and $r.Data.status -eq 'DISABLED') "实际 code=$($r.Code) status=$($r.Data.status)"

  $r = Invoke-Api GET '/api/board' $null $relogin
  Check "被停用成员的会话下一次请求即被拒绝" ($r.Status -eq 401 -and $r.Code -eq 1003) "实际 status=$($r.Status) code=$($r.Code)"

  $r = Invoke-Api POST '/api/auth/login' @{ username = 'tester1'; password = 'newpass123' } (New-Session)
  Check "被停用账号登录返回 401 / code 1003" ($r.Status -eq 401 -and $r.Code -eq 1003) "实际 status=$($r.Status) code=$($r.Code)"

  $r = Invoke-Api PUT "/api/members/$memberId/status" @{ status = 'ENABLED' } $admin
  Check "启用成员成功" ($r.Code -eq 0 -and $r.Data.status -eq 'ENABLED') "实际 code=$($r.Code)"

  $adminId = (Invoke-Api GET '/api/auth/me' $null $admin).Data.id
  $r = Invoke-Api PUT "/api/members/$adminId/status" @{ status = 'DISABLED' } $admin
  Check "停用当前登录账号返回 400 / code 1007" ($r.Status -eq 400 -and $r.Code -eq 1007) "实际 status=$($r.Status) code=$($r.Code)"

  $r = Invoke-Api PUT '/api/members/999999/status' @{ status = 'DISABLED' } $admin
  Check "成员不存在返回 404 / code 1006" ($r.Status -eq 404 -and $r.Code -eq 1006) "实际 status=$($r.Status) code=$($r.Code)"

  Write-Host "[API-012 删除任务]" -ForegroundColor Yellow
  $r = Invoke-Api DELETE "/api/tasks/$overdueTaskId" $null $admin
  Check "删除任务成功" ($r.Code -eq 0) "实际 code=$($r.Code)"
  $r = Invoke-Api GET "/api/tasks/$overdueTaskId" $null $admin
  Check "删除后再查询返回 404" ($r.Status -eq 404 -and $r.Code -eq 1006) "实际 status=$($r.Status) code=$($r.Code)"
  $r = Invoke-Api DELETE "/api/tasks/$overdueTaskId" $null $admin
  Check "重复删除返回 404" ($r.Status -eq 404 -and $r.Code -eq 1006) "实际 status=$($r.Status) code=$($r.Code)"

  Write-Host "[API-002 退出登录]" -ForegroundColor Yellow
  $r = Invoke-Api POST '/api/auth/logout' $null $admin
  Check "退出登录成功" ($r.Code -eq 0) "实际 code=$($r.Code)"
  $r = Invoke-Api POST '/api/auth/logout' $null $admin
  Check "重复退出不报错" ($r.Status -eq 200) "实际 status=$($r.Status)"
  $r = Invoke-Api GET '/api/board' $null $admin
  Check "退出后会话失效返回 401" ($r.Status -eq 401 -and $r.Code -eq 1002) "实际 status=$($r.Status) code=$($r.Code)"

  Write-Host ""
  Write-Host "数据目录：$workDir" -ForegroundColor Cyan
}
finally {
  if (-not $KeepRunning) {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue }
  }
}

Write-Host ""
Write-Host "测试产物目录：$ArtifactDir" -ForegroundColor Cyan
Write-Host "本次运行目录：$runDir" -ForegroundColor Cyan

$passed = $script:Total - $script:Failed
$verdict = if ($script:Failed -eq 0) { '通过' } else { '未通过' }
$rows = @()
$index = 0
foreach ($item in $script:Results) {
  $index++
  $mark = if ($item.Passed) { '通过' } else { '未通过' }
  $detail = if ($item.Detail) { $item.Detail } else { '' }
  $rows += "| $index | $($item.Name) | $mark | $detail |"
}

$report = @(
  '# 接口冒烟测试记录',
  '',
  '本文件由 `code/scripts/smoke-test.ps1` 自动生成，记录最近一次接口冒烟测试的结果。',
  '',
  '| 项 | 值 |',
  '|---|---|',
  "| 运行时间 | $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') |",
  "| 被测产物 | $Jar |",
  "| 服务端口 | $Port |",
  "| 断言总数 | $script:Total |",
  "| 通过 | $passed |",
  "| 失败 | $script:Failed |",
  "| 结论 | $verdict |",
  "| 运行目录 | $runDir |",
  '',
  '运行目录内含本次使用的 H2 数据库（`runtime/data/`）与服务日志（`runtime/out.log`、`runtime/app.log`），仅用于排查，可随时删除。',
  '',
  '## 断言明细',
  '',
  '| 编号 | 断言 | 结果 | 说明 |',
  '|---|---|---|---|',
  ($rows -join "`n"),
  ''
)
[System.IO.File]::WriteAllLines((Join-Path $ArtifactDir 'report.md'), $report, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "测试记录：$(Join-Path $ArtifactDir 'report.md')" -ForegroundColor Cyan

if ($script:Failed -eq 0) {
  Write-Host ("全部通过：" + $script:Total + " 项断言") -ForegroundColor Green
  exit 0
} else {
  Write-Host ("失败 " + $script:Failed + " / " + $script:Total + " 项断言") -ForegroundColor Red
  Write-Host "服务日志：$logFile"
  exit 1
}
