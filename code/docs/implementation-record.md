# 实现记录：团队任务看板应用

本文件记录阶段 3B 的实际执行过程、并行开发组织方式、集成阶段发现的缺陷与验证证据，作为阶段 4 测试阶段的输入之一。

## 1. 交付结果

- 代码位置：`code/`（由用户确认，对应 `TASK-TBD-001` 的最终结论）
- 目录结构：`code/backend`（Maven + Spring Boot）、`code/frontend`（Vite + Vue）、`code/deploy`（部署产物）、`code/scripts`（验证脚本）
- 构建产物：`code/backend/target/board.jar`，单个可执行 jar，内含前端构建产物与全部接口

## 2. 实现方式：契约冻结 + 按模块并行

先由协调者一次性写完全部契约层（`schema.sql`、实体、DTO/VO、枚举、Mapper 接口、Service 接口、公共层、构建配置），编译通过后再按模块切分给并行子代理，保证同一时刻只有一个代理持有某个文件。

| 批次 | 代理 | 负责范围 | 写入范围 |
|---|---|---|---|
| 第一批 | 前端骨架 | `TASK-014` | `code/frontend/src`（骨架、登录页、API 客户端） |
| 第一批 | 部署 | `TASK-022` | `code/deploy`、`code/README.md` |
| 第二批 | 后端 B1 | `TASK-004`、`TASK-005`、`TASK-012`、`TASK-013` | `auth/**`、`member/**`、`AppUserMapper.xml` |
| 第二批 | 后端 B2 | `TASK-006` | `board/**`、`column/**`、两个 XML |
| 第二批 | 后端 B3 | `TASK-007` ~ `TASK-011` | `task/**`、`query/**`、`reminder/**`、`TaskMapper.xml` |
| 第三批 | 前端看板容器 | `TASK-015`、`TASK-016`、`TASK-019` | `BoardView.vue`、`AppTopbar.vue`、`FilterBar.vue` |
| 第三批 | 前端组件 | `TASK-017`、`TASK-018`、`TASK-020` | `TaskCard.vue`、两个弹层、`ReminderBell.vue` |
| 第三批 | 前端成员页 | `TASK-021` | `MembersView.vue` |

前置工作由协调者完成：`TASK-001`、`TASK-002`、`TASK-003` 以及全部契约文件。

前端并行时额外冻结了组件契约（props / emits），使父子组件可以同时开发：`TaskCard`、`NewTaskModal`、`TaskDetailModal`、`ReminderBell` 的接口先写进双方任务书，再各自实现。

## 3. 集成阶段发现的缺陷与修复

以下缺陷在单个模块内均编译通过，只在集成或真实运行时暴露：

| 编号 | 现象 | 根因 | 修复 |
|---|---|---|---|
| D-01 | 启动报 `Invalid bound statement (not found): ...AuthService.ensureInitialAdmin` | 入口类使用包级 `@MapperScan("com.soloforge.board")`，把 Service 接口也注册成了 MyBatis 映射器 | 去掉包级扫描，改为在 4 个 Mapper 接口上标注 `@Mapper` |
| D-02 | 启动报 `Illegal overloaded getter method with ambiguous type for property 'admin'` | `AppUser` 对同一布尔属性同时提供 `getAdmin()` 与 `isAdmin()` | 删除 `isAdmin()`，保留 JavaBean 规范的 `getAdmin()`，并更新 3 处调用点 |
| D-03 | 看板名称显示为 `鍥㈤槦浠诲姟鐪嬫澘` | `spring.sql.init` 按平台字符集（Windows 为 GBK）读取 `schema.sql`，中文种子数据乱码 | 显式配置 `spring.sql.init.encoding: UTF-8` |
| D-04 | 启动报 `ArrayIndexOutOfBoundsException` 读取 jar 内 class | 并行构建期间 `target` 目录被多次清理，jar 被截断 | 串行重新打包 |
| D-05 | 看板页卡片容器无样式 | `BoardView.vue` 使用 `.card-slot`，但 `base.css` 未定义该类 | 在 `base.css` 补齐 `.card-slot` 规则 |
| D-06 | 成员管理入口跳转空白 | `/members` 路由属于共享文件，未在并行批次中注册 | 由协调者在 `router/index.js` 补注册 |
| D-07 | 访问未定义的接口返回 500，而非 404 | 全局异常处理只有兜底的 `Exception` 分支，把 Spring 的「路径/方法不匹配」异常也当成服务端异常 | 补 `NoResourceFoundException`、`NoHandlerFoundException`、`HttpRequestMethodNotSupportedException` 的处理器，统一返回 404 与 `1006` |

结论：**编译通过不等于可运行**。D-01 至 D-04 全部只能通过「启动进程 + 真实请求」发现，因此验证必须落在端到端脚本上。

## 4. 验证证据

### 4.1 接口冒烟测试

脚本：`code/scripts/smoke-test.ps1`（可重复执行，自动启动进程、跑断言、清理退出）

```text
pwsh -NoProfile -File code/scripts/smoke-test.ps1
全部通过：58 项断言
```

覆盖范围：

| 覆盖面 | 内容 |
|---|---|
| 鉴权边界 | 未登录 401/1002、会话失效、账号停用 401/1003、非管理员越权 403/1008 |
| 参数校验 | 空标题、负责人不存在、优先级非法、非法筛选值，均断言字段级错误载荷 |
| 状态派生 | `dueState` 四态（含「当天算临期」的边界）与筛选判定同源 |
| 业务规则 | 登录失败不区分原因、重复登录名 1005、停用当前账号 1007、同列切换不报错 |
| 数据一致性 | 编辑/删除/换列后再查询；重复删除返回 404 |
| 提醒 | 按负责人隔离统计、超期在前排序、计数与条目一致 |

### 4.2 重复启动幂等性

```text
首次启动：看板 1 行「团队任务看板」、三列 待办/进行中/完成、成员 1 名
重启之后：完全一致，无重复插入、无报错
```

### 4.3 单体交付形态

```text
GET /                  → HTTP 200, text/html, 含 #app 挂载点
GET /assets/index-*.js → HTTP 200, 144.2 KB
GET /assets/index-*.css→ HTTP 200
GET /api/board（未登录）→ HTTP 401 {"code":1002,...}
```

### 4.4 构建

```text
前端：npm run build        → 109 modules transformed, built in 216ms
后端：mvn clean package    → 64 source files, BUILD SUCCESS, board.jar 27.02 MB
```

## 5. 未验证项（交由阶段 4）

- 浏览器中的真实交互路径：拖拽换列、表单提交与字段级错误回填、删除二次确认、成员管理页操作、提醒面板展开收起。这些已通过编译与组件级断言，但未做浏览器端到端验证。
- 移动端、深色模式：需求已明确排除，不属于验证范围。
- 内网 Linux 服务器的实际部署：`code/deploy/README.md` 的步骤未在真机验证。

## 6. 回顾

- 有效做法：契约先冻结再并行，使 6 个代理同时工作而不发生文件冲突；同一条 Mapper XML 被三个查询复用，避免了多份口径不同的 SQL。
- 风险点：共享文件（路由、全局样式、构建配置）在并行批次之外，容易被遗漏，需要在收尾清单中显式核对。
- 方法沉淀：本过程已整理为 `docs/stages/03-implementation/coding/SKILL.md`。