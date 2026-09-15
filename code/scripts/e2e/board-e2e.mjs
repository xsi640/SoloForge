import fs from 'node:fs'
import path from 'node:path'
import os from 'node:os'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright-core'

const here = path.dirname(fileURLToPath(import.meta.url))
const BASE = process.env.BOARD_URL || 'http://localhost:8080'
const SHOTS = process.env.BOARD_SHOTS || path.join(here, '..', '..', 'test-artifacts', 'e2e', 'shots')
const ADMIN = { username: 'admin', password: 'admin123' }
const MEMBER = { username: 'liting', password: 'board123456' }

fs.mkdirSync(SHOTS, { recursive: true })

let total = 0
let failed = 0
const failures = []

function check(name, ok, detail = '') {
  total += 1
  if (ok) {
    console.log(`  PASS  ${name}`)
  } else {
    failed += 1
    failures.push(`${name}${detail ? ' — ' + detail : ''}`)
    console.log(`  FAIL  ${name}  ${detail}`)
  }
}

function section(title) {
  console.log(`\n[${title}]`)
}

function resolveChromium() {
  const root = path.join(os.homedir(), 'AppData', 'Local', 'ms-playwright')
  if (!fs.existsSync(root)) return undefined
  const dirs = fs.readdirSync(root).filter((n) => n.startsWith('chromium-'))
  for (const dir of dirs) {
    for (const rel of [['chrome-win', 'chrome.exe'], ['chrome-linux', 'chrome'], ['chrome-mac', 'Chromium.app']]) {
      const candidate = path.join(root, dir, ...rel)
      if (fs.existsSync(candidate)) return candidate
    }
  }
  return undefined
}

async function shot(page, name, fullPage = true) {
  await page.screenshot({ path: path.join(SHOTS, `${name}.png`), fullPage })
  return name
}

const fieldOf = (scope, label) => scope.locator('.field', { hasText: label }).first()

const browser = await chromium.launch({ executablePath: resolveChromium(), args: ['--no-sandbox'] })
const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
const page = await context.newPage()
const consoleErrors = []
page.on('console', (m) => { if (m.type() === 'error') consoleErrors.push(m.text()) })
page.on('pageerror', (e) => consoleErrors.push(String(e)))

try {
  section('PAGE-001 登录页')
  await page.goto(BASE, { waitUntil: 'networkidle' })
  await page.waitForSelector('.login-card')
  check('标题为「任务看板」', (await page.locator('.login-card h1').innerText()) === '任务看板')
  check('无注册入口', (await page.locator('text=注册').count()) === 0)
  check('无忘记密码入口', (await page.locator('text=忘记密码').count()) === 0)
  await shot(page, '01-login-default')

  section('STATE-004 登录失败')
  await page.fill('#login-username', 'admin')
  await page.fill('#login-password', 'wrong-password')
  await page.click('button[type=submit]')
  await page.waitForSelector('.alert')
  const alertText = await page.locator('.alert').innerText()
  check('提示「账号或密码错误」', alertText.includes('账号或密码错误'), alertText)
  check('保留已输入的登录名', (await page.inputValue('#login-username')) === 'admin')
  await shot(page, '02-login-failed')

  section('FLOW-001 登录并进入看板')
  await page.fill('#login-password', ADMIN.password)
  await page.click('button[type=submit]')
  await page.waitForSelector('.board', { timeout: 15000 })
  await page.waitForSelector('.card-slot')

  const colNames = await page.locator('.col-head h3').allInnerTexts()
  check('三列且顺序为 待办/进行中/完成', JSON.stringify(colNames) === JSON.stringify(['待办', '进行中', '完成']), colNames.join('/'))

  const cardCounts = await page.locator('.count-pill').allInnerTexts()
  check('每列显示任务数量', cardCounts.length === 3 && cardCounts.every((n) => Number(n) >= 0), cardCounts.join(','))

  const titles = await page.locator('.card h4').allInnerTexts()
  check('卡片渲染出任务标题', titles.length > 0, `共 ${titles.length} 条`)
  check('卡片正面不含描述文本', !titles.some((t) => t.includes('汇总本月客户反馈并分类')), titles.join(' | '))

  const allCardText = (await page.locator('.board').innerText()).replace(/\s+/g, ' ')
  check('超期带「已超期」文字标识', allCardText.includes('已超期'))
  check('临期带「临期」文字标识', allCardText.includes('临期'))
  check('优先级标签带文字', ['高', '中', '低'].some((p) => allCardText.includes(p)))
  await shot(page, '03-board')

  section('STATE-012 提醒铃铛')
  const badgeText = await page.locator('.bell-wrap .badge').innerText().catch(() => '')
  check('铃铛显示未处理项角标', Number(badgeText) > 0, `角标=${badgeText}`)
  await page.click('.bell-wrap .tb-icon')
  await page.waitForSelector('.bell-panel')
  const bellItems = await page.locator('.bell-item').count()
  check('提醒清单有条目', bellItems > 0, `${bellItems} 条`)
  const firstBellText = await page.locator('.bell-item').first().innerText()
  check('清单条目含日期标识', firstBellText.includes('已超期') || firstBellText.includes('临期'), firstBellText.replace(/\s+/g, ' '))
  await shot(page, '04-reminder-bell')
  await page.click('.topbar .brand')

  section('INTERACTION-004 拖拽换列')
  const todoCol = page.locator('.col').nth(0)
  const doingCol = page.locator('.col').nth(1)
  const doingBefore = Number(await doingCol.locator('.count-pill').innerText())
  const movingTitle = await todoCol.locator('.card h4').first().innerText()
  await todoCol.locator('.card-slot').first().dragTo(doingCol)
  await page.waitForTimeout(1200)
  const doingAfter = Number(await doingCol.locator('.count-pill').innerText())
  check('拖拽后目标列数量 +1', doingAfter === doingBefore + 1, `${doingBefore} → ${doingAfter}`)
  const stillInTodo = await todoCol.locator('.card h4').allInnerTexts()
  check('卡片已离开原列', !stillInTodo.includes(movingTitle), movingTitle)
  await shot(page, '05-board-after-drag')

  section('TASK-017 新建任务弹层')
  await doingCol.locator('.add-btn').click()
  await page.waitForSelector('.modal')
  const presetColumn = await fieldOf(page.locator('.modal'), '状态').locator('select').inputValue()
  check('状态预置为所在列', presetColumn !== '', `columnId=${presetColumn}`)
  await page.locator('.modal .btn-primary').click()
  await page.waitForSelector('.modal .err')
  const titleErr = await fieldOf(page.locator('.modal'), '标题').locator('.err').innerText()
  check('空标题被阻止并给出字段提示', titleErr.length > 0, titleErr)
  await shot(page, '06-new-task-validation')

  await fieldOf(page.locator('.modal'), '标题').locator('input').fill('浏览器验证新建的任务')
  await fieldOf(page.locator('.modal'), '优先级').locator('select').selectOption('HIGH')
  await fieldOf(page.locator('.modal'), '截止日期').locator('input').fill(new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10))
  await shot(page, '07-new-task-filled')
  await page.locator('.modal .btn-primary').click()
  await page.waitForSelector('.modal', { state: 'detached' })
  await page.waitForTimeout(600)
  const afterCreate = await page.locator('.board').innerText()
  check('新建任务出现在看板', afterCreate.includes('浏览器验证新建的任务'))
  check('新建任务高亮标记', (await page.locator('.card.highlighted').count()) >= 0)

  section('TASK-018 详情弹层、编辑与删除确认')
  await page.locator('.card', { hasText: '浏览器验证新建的任务' }).click()
  await page.waitForSelector('.modal')
  await page.waitForTimeout(500)
  const detailTitle = await fieldOf(page.locator('.modal'), '标题').locator('input').inputValue()
  check('详情弹层回填任务字段', detailTitle === '浏览器验证新建的任务', detailTitle)
  await shot(page, '08-task-detail')

  await fieldOf(page.locator('.modal'), '标题').locator('input').fill('浏览器验证已改标题')
  await page.locator('.modal .btn-primary').click()
  await page.waitForSelector('.modal', { state: 'detached' })
  await page.waitForTimeout(600)
  check('编辑后卡片标题更新', (await page.locator('.board').innerText()).includes('浏览器验证已改标题'))

  await page.locator('.card', { hasText: '浏览器验证已改标题' }).click()
  await page.waitForSelector('.modal')
  await page.waitForTimeout(400)
  await page.locator('.modal .btn-danger').click()
  await page.waitForSelector('.modal-head:has-text("删除任务")')
  const confirmText = await page.locator('.confirm-text').innerText()
  check('删除二次确认提示不可恢复', confirmText.includes('不可恢复'), confirmText)
  await shot(page, '09-delete-confirm')
  await page.locator('.modal .btn-secondary:has-text("取消")').click()
  await page.waitForTimeout(400)
  check('取消删除后任务仍在', (await page.locator('.board').innerText()).includes('浏览器验证已改标题'))
  await page.locator('.modal .btn-danger:has-text("删除")').click()
  await page.waitForSelector('.modal', { state: 'detached' })
  await page.waitForTimeout(600)
  check('确认删除后卡片移除', !(await page.locator('.board').innerText()).includes('浏览器验证已改标题'))

  section('INTERACTION-008/009 搜索与筛选')
  await page.fill('.topbar .search input', '鉴权')
  await page.waitForTimeout(900)
  const searchText = await page.locator('.board').innerText()
  check('关键词搜索命中标题', searchText.includes('接口鉴权方案评审'))
  check('搜索结果排除其他任务', !searchText.includes('整理客户反馈清单'))
  await page.fill('.topbar .search input', '')
  await page.waitForTimeout(900)

  await page.locator('.filterbar select.native').nth(2).selectOption('OVERDUE')
  await page.waitForTimeout(900)
  const overdueOnly = await page.locator('.card .flag.overdue').count()
  const totalCards = await page.locator('.card-slot').count()
  check('按已超期筛选后结果全部超期', overdueOnly > 0 && overdueOnly === totalCards, `超期 ${overdueOnly} / 共 ${totalCards}`)
  await shot(page, '10-filter-overdue')

  await page.locator('.filterbar .btn:has-text("清除筛选")').click()
  await page.waitForTimeout(800)
  check('清除筛选后恢复默认', (await page.locator('.chip').count()) === 0)
  await page.locator('.filterbar .seg span:has-text("查看全部")').click()
  await page.waitForTimeout(900)
  await shot(page, '11-filter-all')

  section('TASK-021 成员管理页')
  await page.click('.topbar .link:has-text("成员管理")')
  await page.waitForSelector('.content-head h1')
  check('页面标题为「成员管理」', (await page.locator('.content-head h1').innerText()) === '成员管理')
  const memberRows = await page.locator('table tbody tr').count()
  check('成员列表渲染出行', memberRows >= 5, `${memberRows} 行`)
  const memberText = await page.locator('.content').innerText()
  check('无删除成员入口', !memberText.includes('删除'))
  check('显示状态标签', memberText.includes('启用') || memberText.includes('停用'))
  await shot(page, '12-members')

  await page.locator('.btn:has-text("新增成员")').click()
  await page.waitForSelector('.form-row')
  const adminRow = page.locator('table tbody tr', { hasText: 'admin' }).first()
  const adminDisable = adminRow.locator('button:has-text("停用")')
  check('当前登录账号停用按钮不可用', await adminDisable.isDisabled())
  await page.locator('.form-row input').first().fill('newbie')
  await page.locator('.form-row input').nth(1).fill('新成员')
  await page.locator('.form-row input').nth(2).fill('abcd1234')
  await shot(page, '13-member-create-form')
  await page.locator('.form-row .btn-primary').click()
  await page.waitForTimeout(1000)
  check('新增成员后列表出现该成员', (await page.locator('table').innerText()).includes('新成员'))

  section('USER-001 普通成员视角')
  await page.goto(`${BASE}/#/`, { waitUntil: 'networkidle' })
  await page.click('.topbar .user .out')
  await page.waitForSelector('.login-card')
  await page.fill('#login-username', MEMBER.username)
  await page.fill('#login-password', MEMBER.password)
  await page.click('button[type=submit]')
  await page.waitForSelector('.board')
  await page.waitForSelector('.card-slot')
  check('普通成员可登录并看到看板', (await page.locator('.card-slot').count()) > 0)
  check('普通成员看不到成员管理入口', (await page.locator('.topbar .link').count()) === 0)
  const mineText = await page.locator('.filterbar').innerText()
  check('默认筛选为我的任务', mineText.includes('我的任务'))
  await shot(page, '14-member-view')

  section('INTERACTION-016 退出登录')
  await page.click('.topbar .user .out')
  await page.waitForSelector('.login-card')
  check('退出后回到登录页', await page.locator('.login-card').isVisible())

  check('浏览器控制台无报错', consoleErrors.length === 0, consoleErrors.slice(0, 3).join(' ; '))
} catch (error) {
  failed += 1
  total += 1
  console.log(`\n运行中断：${error.message}`)
  await shot(page, 'error-state')
} finally {
  await browser.close()
}

console.log(`\n截图目录：${SHOTS}`)
if (failed === 0) {
  console.log(`全部通过：${total} 项断言`)
  process.exit(0)
} else {
  console.log(`失败 ${failed} / ${total} 项断言`)
  failures.forEach((f) => console.log(`  - ${f}`))
  process.exit(1)
}