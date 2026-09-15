# API 设计：团队任务看板应用

本文件基于 `code/docs/tech-architecture.md` 生成，与 `tasks.md` 同为阶段 4 测试阶段的输入。接口范围与架构文档的「接口清单」一致，未新增未确认的接口或字段。

## 1. 接口约定

### 通用约定

| 项 | 约定 |
|---|---|
| 路径前缀 | `/api` |
| 传输格式 | 请求与响应均为 `application/json; charset=UTF-8` |
| 响应结构 | `{ "code": 0, "message": "ok", "data": ... }`，`code` 为 0 表示成功，非 0 表示业务失败并携带可展示的 `message` |
| 日期格式 | `yyyy-MM-dd` |
| 时间格式 | `yyyy-MM-dd HH:mm:ss` |
| 时区 | `Asia/Shanghai` |
| 字段命名 | camelCase |
| 请求方法 | 查询用 `GET`，创建用 `POST`，整体更新用 `PUT`，局部更新用 `PATCH`，删除用 `DELETE` |

### 鉴权约定

- 登录成功后由服务端创建会话，通过 `JSESSIONID` Cookie 维持；Cookie 设置 HttpOnly 与 SameSite=Lax，不使用 HTTPS 故不设置 Secure。
- 除 `API-001` 外，所有接口都要求已登录；未登录或会话失效返回 HTTP 401。
- 每次请求都校验当前账号状态，被停用账号的请求立即被拒绝并返回 HTTP 401。
- `API-004` 至 `API-007`（成员管理）仅管理员可访问，非管理员访问返回 HTTP 403。

### 通用校验规则

| 字段 | 规则 |
|---|---|
| 标题 | 必填，去除首尾空格后长度 1 至 100 |
| 描述 | 可空，最多 1000 字符 |
| 负责人 | 必填，必须指向存在的成员 |
| 所属列 | 必填，必须为待办、进行中、完成之一 |
| 优先级 | 可空，取 `HIGH`、`MEDIUM`、`LOW`，缺省为 `MEDIUM` |
| 截止日期 | 可空，格式 `yyyy-MM-dd`；为空时不参与临期与超期计算 |
| 登录名 | 必填，长度 3 至 32，仅允许字母、数字与下划线，全局唯一 |
| 显示名 | 必填，长度 1 至 32 |
| 密码 | 必填，长度 6 至 64 |

### 截止日期状态约定

任务返回体中的 `dueState` 由服务端按当前日期实时计算，不落库：

| 取值 | 条件 | 界面表现 |
|---|---|---|
| `NONE` | 截止日期为空 | 不显示日期标识 |
| `OVERDUE` | 截止日期早于当天 | 标红并显示「已超期」 |
| `DUE_SOON` | 截止日期为当天或次日 | 标橙并显示「临期」 |
| `NORMAL` | 截止日期晚于次日 | 正常显示日期 |

当天到期算临期：需求中的表述为「超过截止日则标为已超期」，当天尚未超过。

## 2. 接口清单

| 接口编号 | 名称 | 形态 | 对应模块 | 用途 | 状态 |
|---|---|---|---|---|---|
| `API-001` | 登录 | HTTP `POST /api/auth/login` | `MODULE-001` | 校验账号密码并建立会话 | 待处理 |
| `API-002` | 退出登录 | HTTP `POST /api/auth/logout` | `MODULE-001` | 使当前会话失效 | 待处理 |
| `API-003` | 当前用户 | HTTP `GET /api/auth/me` | `MODULE-001` | 获取当前登录用户及是否管理员 | 待处理 |
| `API-004` | 成员列表 | HTTP `GET /api/members` | `MODULE-001` | 查询全部成员 | 待处理 |
| `API-005` | 新增成员 | HTTP `POST /api/members` | `MODULE-001` | 创建成员账号 | 待处理 |
| `API-006` | 重置密码 | HTTP `PUT /api/members/{id}/password` | `MODULE-001` | 设置成员新密码并立即生效 | 待处理 |
| `API-007` | 停用或启用成员 | HTTP `PUT /api/members/{id}/status` | `MODULE-001` | 切换成员账号状态 | 待处理 |
| `API-008` | 看板信息 | HTTP `GET /api/board` | `MODULE-002` | 获取唯一看板的标识与名称 | 待处理 |
| `API-009` | 状态列 | HTTP `GET /api/board/columns` | `MODULE-003` | 获取三个状态列及顺序 | 待处理 |
| `API-010` | 新建任务 | HTTP `POST /api/tasks` | `MODULE-004` | 在指定列创建任务 | 待处理 |
| `API-011` | 编辑任务 | HTTP `PUT /api/tasks/{id}` | `MODULE-004` | 更新任务全部可编辑字段 | 待处理 |
| `API-012` | 删除任务 | HTTP `DELETE /api/tasks/{id}` | `MODULE-004` | 删除任务 | 待处理 |
| `API-013` | 任务详情 | HTTP `GET /api/tasks/{id}` | `MODULE-004` | 查询单个任务 | 待处理 |
| `API-014` | 更新任务状态 | HTTP `PATCH /api/tasks/{id}/status` | `MODULE-004`、`MODULE-005` | 拖拽换列与详情弹窗改状态共用 | 待处理 |
| `API-015` | 条件查询任务 | HTTP `GET /api/tasks` | `MODULE-006` | 按关键词与筛选条件查询任务 | 待处理 |
| `API-016` | 我的提醒 | HTTP `GET /api/reminders/mine` | `MODULE-007` | 查询当前用户的临期与超期任务 | 待处理 |

未列入本表的接口均为未确认能力，不在本期实现范围。特别说明：成员管理没有删除接口，列管理没有增删改接口，任务查询没有分页参数。

## 3. 数据结构

### 公共结构

**统一响应体**

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | number | 0 表示成功，非 0 表示业务失败 |
| `message` | string | 成功时为 `ok`，失败时为可展示的提示文案 |
| `data` | object 或 array 或 null | 业务数据，失败时为 null |

**字段错误（参数校验失败时 `data` 的内容）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `field` | string | 出错的字段名 |
| `reason` | string | 该字段的失败原因 |

### 用户与成员

**UserVO（当前用户）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 用户 id |
| `username` | string | 登录名 |
| `displayName` | string | 显示名 |
| `isAdmin` | boolean | 是否管理员，仅决定成员管理入口与接口可见性 |

**MemberVO（成员）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 成员 id |
| `username` | string | 登录名 |
| `displayName` | string | 显示名 |
| `isAdmin` | boolean | 是否管理员 |
| `status` | string | `ENABLED` 或 `DISABLED` |
| `createdAt` | string | 创建时间 |

### 看板与列

**BoardVO**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 看板 id |
| `name` | string | 看板名称 |

**ColumnVO**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 列 id |
| `code` | string | `TODO`、`DOING`、`DONE` |
| `name` | string | 列名称：待办、进行中、完成 |
| `sortOrder` | number | 顺序，升序排列 |

### 任务

**TaskVO**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 任务 id |
| `title` | string | 标题 |
| `assigneeId` | number | 负责人 id |
| `assigneeName` | string | 负责人显示名，便于卡片直接渲染 |
| `columnId` | number | 所属列 id |
| `columnCode` | string | 所属列编码，等价于任务状态 |
| `priority` | string | `HIGH`、`MEDIUM`、`LOW` |
| `dueDate` | string 或 null | 截止日期，可为空 |
| `dueState` | string | `NONE`、`OVERDUE`、`DUE_SOON`、`NORMAL` |
| `description` | string 或 null | 描述，纯文本 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

**TaskSaveRequest（新建与编辑共用）**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `title` | string | 是 | 标题 |
| `assigneeId` | number | 是 | 负责人 |
| `columnId` | number | 是 | 所属列，新建时由所在列预置 |
| `priority` | string | 否 | 缺省为 `MEDIUM` |
| `dueDate` | string | 否 | 截止日期 |
| `description` | string | 否 | 描述 |

**TaskStatusRequest**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `columnId` | number | 是 | 目标列，与当前列相同时视为无变化 |

**TaskQuery（查询参数）**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | string | 否 | 关键词，匹配标题与描述，去除首尾空格后为空则忽略 |
| `assigneeId` | number | 否 | 负责人，传 0 或省略表示全部 |
| `priority` | string | 否 | 优先级，省略表示全部 |
| `dueFilter` | string | 否 | `ALL`、`OVERDUE`、`DUE_SOON`，省略按 `ALL` 处理；不支持任意日期区间 |

### 提醒

**ReminderVO**

| 字段 | 类型 | 说明 |
|---|---|---|
| `items` | TaskVO 数组 | 当前用户临期与超期的任务，按截止日期升序、超期在前 |
| `overdueCount` | number | 超期数量 |
| `dueSoonCount` | number | 临期数量 |

数量与 `items` 中对应状态的任务数一致；无匹配任务时 `items` 为空数组且两个计数为 0。

## 4. 接口明细

### `API-001` 登录

- 请求：`POST /api/auth/login`
- 请求体：`username`（必填）、`password`（必填）
- 响应 `data`：`UserVO`
- 行为：校验密码哈希；成功后建立会话并返回用户信息
- 失败：账号或密码错误返回 HTTP 400 与统一文案「账号或密码错误」，不区分账号不存在与密码错误；账号已停用返回 HTTP 401 与错误码 1003
- 说明：登录失败使用 HTTP 400 而非 401，避免前端把登录失败误判为会话失效而触发跳转
- 说明：账号已停用使用 HTTP 401 与错误码 1003，与使用过程中被停用的处理保持一致；此时用户本就在登录页，前端只需展示 1003 的文案

### `API-002` 退出登录

- 请求：`POST /api/auth/logout`
- 请求体：无
- 响应 `data`：null
- 行为：使当前会话失效，重复调用不报错

### `API-003` 当前用户

- 请求：`GET /api/auth/me`
- 响应 `data`：`UserVO`
- 用途：前端在进入页面时确认会话有效并获取是否管理员

### `API-004` 成员列表

- 请求：`GET /api/members`，仅管理员
- 响应 `data`：`MemberVO` 数组，按创建时间升序
- 说明：包含已停用成员

### `API-005` 新增成员

- 请求：`POST /api/members`，仅管理员
- 请求体：`username`（必填）、`displayName`（必填）、`password`（必填）、`status`（可空，缺省 `ENABLED`）、`isAdmin`（可空，缺省 false）
- 响应 `data`：`MemberVO`
- 失败：登录名重复返回 HTTP 400，`data` 中 `field` 为 `username`，`reason` 为「登录名已存在」

### `API-006` 重置密码

- 请求：`PUT /api/members/{id}/password`，仅管理员
- 请求体：`password`（必填）
- 响应 `data`：null
- 行为：新密码立即生效，不强制成员下次登录时修改
- 失败：成员不存在返回 HTTP 404

### `API-007` 停用或启用成员

- 请求：`PUT /api/members/{id}/status`，仅管理员
- 请求体：`status`（必填，`ENABLED` 或 `DISABLED`）
- 响应 `data`：`MemberVO`
- 行为：停用后该成员无法登录，其名下任务保留且负责人不变；已登录的会话在下一次请求时被拒绝
- 失败：目标为当前登录账号且状态为 `DISABLED` 时返回 HTTP 400 与「不可停用当前登录账号」；成员不存在返回 HTTP 404

### `API-008` 看板信息

- 请求：`GET /api/board`
- 响应 `data`：`BoardVO`，返回唯一看板

### `API-009` 状态列

- 请求：`GET /api/board/columns`
- 响应 `data`：`ColumnVO` 数组，按 `sortOrder` 升序，固定三列

### `API-010` 新建任务

- 请求：`POST /api/tasks`
- 请求体：`TaskSaveRequest`
- 响应 `data`：`TaskVO`
- 失败：标题为空或超出长度返回 HTTP 400 并在 `data` 中给出字段级原因；负责人或所属列不存在返回 HTTP 400

### `API-011` 编辑任务

- 请求：`PUT /api/tasks/{id}`
- 请求体：`TaskSaveRequest`
- 响应 `data`：`TaskVO`
- 行为：更新请求体中给出的全部可编辑字段，校验规则与新建一致
- 失败：任务不存在返回 HTTP 404

### `API-012` 删除任务

- 请求：`DELETE /api/tasks/{id}`
- 响应 `data`：null
- 行为：物理删除任务，删除后不可恢复
- 失败：任务不存在返回 HTTP 404

### `API-013` 任务详情

- 请求：`GET /api/tasks/{id}`
- 响应 `data`：`TaskVO`
- 失败：任务不存在返回 HTTP 404

### `API-014` 更新任务状态

- 请求：`PATCH /api/tasks/{id}/status`
- 请求体：`TaskStatusRequest`
- 响应 `data`：`TaskVO`
- 行为：按目标列更新任务状态；目标列与当前列相同时不产生变更，仍返回成功
- 用途：拖拽换列与详情弹窗改状态共用同一接口，两条路径结果一致
- 失败：任务不存在返回 HTTP 404；目标列不存在返回 HTTP 400

### `API-015` 条件查询任务

- 请求：`GET /api/tasks`，查询参数见 `TaskQuery`
- 响应 `data`：`TaskVO` 数组，按创建时间倒序
- 行为：条件之间为与关系；不传条件返回全部任务；不分页，一次返回全部匹配结果
- 说明：`dueFilter` 的判定与 `dueState` 同源，均按截止日期实时计算；筛选能力独立于提醒接口，两者不共享状态

### `API-016` 我的提醒

- 请求：`GET /api/reminders/mine`
- 响应 `data`：`ReminderVO`
- 行为：只统计当前登录用户名下的任务；临期与超期按截止日期实时计算，不落库；截止日期为空的任务不进入结果
- 使用方式：前端在登录时调用一次，其后每 5 分钟调用一次

## 5. 错误与异常约定

### 业务错误码

| `code` | HTTP 状态 | 含义 | 前端处理 |
|---|---|---|---|
| 0 | 200 | 成功 | 正常使用 `data` |
| 1001 | 400 | 参数校验失败 | 展示 `message`；若 `data` 含字段错误，定位到对应字段提示并保留用户输入 |
| 1002 | 401 | 未登录或会话失效 | 清除本地状态并跳转登录页 |
| 1003 | 401 | 账号已停用（登录时被拒绝，或使用过程中被停用） | 清除本地状态并跳转登录页，同时展示「账号已停用，请联系管理员」 |
| 1004 | 400 | 账号或密码错误 | 在登录页展示统一提示，保留已输入的登录名 |
| 1005 | 400 | 登录名已存在 | 在新增成员表单的登录名字段处提示 |
| 1006 | 404 | 目标不存在（任务或成员） | 提示记录不存在，并刷新当前列表 |
| 1007 | 400 | 不允许的操作（如停用当前登录账号） | 展示 `message`，不做状态变更 |
| 1008 | 403 | 无权限访问（非管理员访问成员管理） | 提示无权限；不跳转登录页 |
| 9999 | 500 | 服务端异常 | 提示「操作失败，请重试」，保留用户已输入的内容 |

### 异常处理约定

- 服务端统一捕获未处理异常，记录日志后返回 `code` 9999，避免异常直接暴露堆栈给前端。
- 参数校验失败一律返回字段级信息，便于前端做字段提示。
- 任何写操作失败都不产生部分写入，事务边界在服务层。

## 6. 待确认事项处理结果

技术架构文档的 `ARCH-TBD-###` 已全部处理完毕，本文件据此确定字段与错误码。实现阶段新发现的问题统一记录在 `tasks.md` 的 `TASK-TBD-###` 清单中，不另开编号。与本文件直接相关的默认结论如下，均随 `tasks.md` 一并待确认：

| 相关事项 | 结论在本文件中的体现 |
|---|---|
| `TASK-TBD-002` 成员管理接口的权限校验 | `API-004` 至 `API-007` 标注「仅管理员」，非管理员返回 `code` 1008 |
| `TASK-TBD-003` 负责人是否必填 | `TaskSaveRequest.assigneeId` 标为必填 |
| `TASK-TBD-004` 长度上限 | 见「通用校验规则」中的标题与描述规则 |
| `TASK-TBD-005` 停用账号的会话失效 | `API-007` 的失败说明与错误码 1003 |
| `TASK-TBD-006` 截止日期可空 | `TaskVO.dueDate` 可为 null，`dueState` 为 `NONE` |
| `TASK-TBD-007` 截止日期筛选范围 | `TaskQuery.dueFilter` 只提供 `ALL`、`OVERDUE`、`DUE_SOON` |
| `TASK-TBD-008` 当天到期的归属 | 见「截止日期状态约定」，当天算 `DUE_SOON` |
