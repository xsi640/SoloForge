# 团队任务看板 · 工程总览

本目录是「团队任务看板」项目的实现代码，对应仓库 `docs/stages/` 下的阶段产物。文档回答「为什么这么做」，本目录是这么做的结果。

## 目录结构

```text
code/
├── backend/        Spring Boot 3.5.3 + Java 17 + 纯 MyBatis + H2，Maven 工程，产物 target/board.jar
├── frontend/       Vue 3 + Vite 前端工程，构建产物输出到 backend/src/main/resources/static/
├── deploy/         部署产物：外置 application.yml、systemd 单元、部署运行手册
├── docs/           阶段 1~3 的产物文档：需求分析、UX / UI 设计、技术架构、API 设计、任务拆分、实现记录
├── scripts/        验证脚本：smoke-test.ps1（接口冒烟）、e2e/（浏览器端到端）
├── test-artifacts/ 脚本生成的测试产物，可整目录删除后重新运行脚本再生成
└── README.md       本文件
```

前端产物放进后端静态资源目录，由同一个 Java 进程同时提供页面与 `/api` 接口，最终只有一个 jar、一个进程、一个端口。部署步骤见 `deploy/README.md`。

`docs/` 是阶段 1~3 的产物文档，原先分散在 `docs/stages/01-requirement-analysis/`、`docs/stages/02-product-design/` 与 `docs/stages/03-implementation/` 下，随代码一起移到本目录，便于实现与文档对照。阶段 2 的设计图源码与截图在 `docs/mockups/`。`scripts/` 存放验证脚本，`test-artifacts/` 存放它们生成的产物，二者都不参与构建。

构建与运行产生的目录（`target/`、`node_modules/`、`data/`、`*.log`、`*.mv.db`）已被根目录 `.gitignore` 忽略，例如本地起服务时生成的 `run/data/board.mv.db`。

## 技术栈与固定约定

| 部分 | 选型 | 说明 |
|---|---|---|
| 后端 | Java 17 + Spring Boot 3.5.3 | 内嵌 Tomcat，单 jar 起服务 |
| 持久层 | 纯 MyBatis 3.0.4（SQL 写在 XML） | 不使用 MyBatis-Plus 与 JPA |
| 数据库 | H2 2.3.232 嵌入式文件模式 | 数据文件 `<工作目录>/data/board.mv.db` |
| 后端构建 | Maven 3.9.x，`mvn clean package` | `finalName` 为 `board`，产物 `target/board.jar` |
| 前端 | Vue 3 + Vite + vue-router 4 + axios，手写 CSS | 不引入 TypeScript 与组件库 |
| 前端构建 | Node.js 22.12 及以上，`npm run build` | 产物输出到 `backend/src/main/resources/static/` |
| 运行 | `java -jar board.jar` + systemd | 不用 HTTPS、Docker、Nginx |

后端代码按业务模块分包，模块编号与需求文档中的 `MODULE-###` 一一对应。

## 本地开发与构建

后端：

```bash
cd code/backend
mvn clean package
java -jar target/board.jar
```

前端开发（Vite 开发服务器把 `/api` 代理到 `http://localhost:8080`）：

```bash
cd code/frontend
npm install
npm run dev
```

完整构建必须先构建前端、再打包后端，否则 jar 里没有页面：

```bash
cd code/frontend && npm install && npm run build
cd code/backend && mvn clean package
```

启动后访问 `http://localhost:8080`。首次启动会在工作目录生成 `data/board.mv.db`，并用配置中的初始管理员账号创建第一个管理员，默认 `admin` / `admin123`，登录后立即重置。

## 配置项

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `server.port` | `BOARD_PORT` | `8080` | 页面与 `/api` 共用的端口 |
| `spring.datasource.url` | `BOARD_DB_URL` | `jdbc:h2:file:./data/board;DB_CLOSE_DELAY=-1` | 相对工作目录，生成 `data/board.mv.db` |
| `spring.datasource.username` | `BOARD_DB_USERNAME` | `sa` | 嵌入式数据库账号 |
| `board.admin.username` | `BOARD_ADMIN_USERNAME` | `admin` | 初始管理员登录名 |
| `board.admin.display-name` | `BOARD_ADMIN_DISPLAY_NAME` | `管理员` | 初始管理员显示名称 |
| `board.admin.password` | `BOARD_ADMIN_PASSWORD` | `admin123` | 初始密码，首次登录后立即重置 |

会话保存在服务端内存中，服务重启后所有用户需要重新登录，这是已知且已接受的行为。

## 部署

按 `deploy/README.md` 执行：构建产物 → 上传 `/opt/board` → 安装 systemd 单元并开机自启 → 用 `journalctl -u board` 查看日志 → 定期复制 `data/` 目录备份。

`deploy/` 内含四个文件：`application.yml`（服务器外置配置样例）、`board.service`（systemd 单元样例）、`README.md`（部署运行手册）、`.gitignore`（忽略 `data/` 等运行时产物）。

`tasks.md` 中 `TASK-TBD-001` 曾建议把代码放在 `app/backend` 与 `app/frontend`，实际落位改到本目录，与阶段产物同仓便于对照。

## 与 docs/stages 阶段产物的对应关系

| 阶段产物 | 对本目录的影响 |
|---|---|
| `code/docs/requirement-analysis.md` | 划定功能范围与本期不做清单，决定后端有多少个业务模块 |
| `code/docs/ux-design.md` | 页面与页面状态（`PAGE-###` / `STATE-###`）决定前端路由与页面组件 |
| `code/docs/ui-design.md` 与 `code/docs/mockups/` | 视觉稿决定前端布局与手写 CSS 的实现目标 |
| `code/docs/tech-architecture.md` | 技术栈、模块划分、数据表、接口约定与部署形态（含 12 条 ADR） |
| `code/docs/api-design.md` | 字段级接口定义，前后端按此对接 |
| `code/docs/tasks.md` | 任务拆分与并行分组，`TASK-###` 与本目录的提交范围一一对应 |

阶段 4（测试）与阶段 5（发布）的产物同样落在本目录之外，测试与发布流程以对应阶段的文档为准。
