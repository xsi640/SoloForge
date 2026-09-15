# 团队任务看板 · 部署运行手册

本手册面向内网 Linux 服务器，按章节顺序执行即可从零完成部署。命令均以构建机（开发机）与服务器两个角色标注，服务器 IP 用 `<服务器IP>` 占位，实际使用时替换。

技术形态：单个 `java -jar` 进程同时提供页面与 `/api` 接口，由 systemd 常驻守护；不使用 HTTPS、Docker、Nginx，也不依赖外部数据库。

## 1. 环境要求

| 角色 | 组件 | 版本要求 | 说明 |
|---|---|---|---|
| 构建机 | JDK | 17 | 与服务器保持一致，避免编译产物版本不符 |
| 构建机 | Maven | 3.9.x | 后端构建 |
| 构建机 | Node.js | 22.12 及以上 | 前端构建，开发机实测 v24.14 |
| 服务器 | Linux + systemd | 主流发行版 | Debian/Ubuntu、CentOS/Rocky 均可 |
| 服务器 | JDK | 17 | 只需 JRE/JDK，无需 Maven 与 Node |

服务器上不需要安装数据库、反向代理或容器运行时，数据存在本地 `data/` 目录。

## 2. 构建产物

必须**先构建前端、再打包后端**：前端产物要落到后端静态资源目录，才能被一起打进 jar。

### 2.1 构建前端

在仓库根目录执行：

```bash
cd code/frontend
npm install
npm run build
```

构建结果输出到 `code/backend/src/main/resources/static/`，由后端打包进 jar。

### 2.2 打包后端

```bash
cd code/backend
mvn clean package
```

产物为单个可执行 jar：`code/backend/target/board.jar`。

### 2.3 本地验证（可选，建议部署前做一次）

```bash
cd code/backend
java -jar target/board.jar
```

浏览器访问 `http://localhost:8080` 能看到登录页即可。首次启动会在当前工作目录生成 `data/board.mv.db`，并创建初始管理员账号。

前端单独开发时，在 `code/frontend` 执行 `npm run dev` 启动 Vite 开发服务器，它会把 `/api` 请求代理到 `http://localhost:8080`。

## 3. 上传到服务器 /opt/board

以下命令若无特别说明，均在服务器上以 root 身份执行。

### 3.1 创建运行用户与目录

```bash
useradd --system --home-dir /opt/board --shell /usr/sbin/nologin board
install -d -o board -g board -m 750 /opt/board
```

以专用低权限用户 `board` 运行，不要用 root。数据目录 `data/` 由应用以该用户身份在首次启动时自动创建，因此 `/opt/board` 必须对 `board` 可写。

### 3.2 上传 jar、配置与手册

在构建机上执行：

```bash
scp code/backend/target/board.jar root@<服务器IP>:/opt/board/board.jar
scp code/deploy/application.yml root@<服务器IP>:/opt/board/application.yml
scp code/deploy/README.md root@<服务器IP>:/opt/board/README.md
```

第三行是本手册自身，放到 `/opt/board/README.md`，排障时可直接在服务器上查看，systemd 单元里的 `Documentation` 也指向该文件。

回到服务器修正属主：

```bash
chown board:board /opt/board/board.jar /opt/board/application.yml /opt/board/README.md
```

### 3.3 按需修改配置

编辑 `/opt/board/application.yml`，至少确认以下几项：

- `server.port`：默认 `8080`，如需改动，后续放行端口与访问地址同步改。
- `board.admin.username` / `board.admin.password`：初始管理员账号，默认 `admin` / `admin123`，**密码务必在部署前改掉**。
- `spring.datasource.url`：默认 `jdbc:h2:file:./data/board;DB_CLOSE_DELAY=-1`，即数据文件 `/opt/board/data/board.mv.db`，通常无需改动。

不允许或不想把密码写进文件时，改用环境变量覆盖，见第 4 节的 `Environment` 段。

### 3.4 开放端口（使用 firewalld 时）

```bash
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload
```

端口号必须与 `server.port` 一致。若服务器由运维统一管理防火墙，改为提工单放行同一端口。

## 4. 安装并启用 systemd 单元

在构建机上把单元文件传到服务器：

```bash
scp code/deploy/board.service root@<服务器IP>:/etc/systemd/system/board.service
```

在服务器上启用：

```bash
systemctl daemon-reload
systemctl enable --now board
systemctl status board --no-pager
```

单元文件要点：

- `WorkingDirectory=/opt/board`：数据目录 `data/` 相对工作目录生成，即 `/opt/board/data/`。
- `ExecStart=/usr/bin/java -jar /opt/board/board.jar`：若 `java` 不在 `/usr/bin/java`（例如通过 SDKMAN 或自定义路径安装），先执行 `readlink -f "$(which java)"` 查出绝对路径再替换该行，然后重新 `systemctl daemon-reload` 并 `systemctl restart board`。
- `Restart=always` 加 `RestartSec=5`：进程异常退出后 5 秒自动拉起；开机自启由 `WantedBy=multi-user.target` 提供。
- `User=board`：以低权限用户运行。
- 日志走 journald，不额外写日志文件，也不需要配置日志轮转。
- 环境变量覆盖方式：把 `Environment=` 行的注释去掉并改成实际值。例如只覆盖初始管理员密码：

```ini
Environment=BOARD_ADMIN_PASSWORD=your-own-password
```

覆盖端口同理，把 `BOARD_PORT` 设为需要的端口号；改了端口要同步改防火墙放行规则与访问地址。

可用的环境变量：`BOARD_PORT`、`BOARD_DB_URL`、`BOARD_DB_USERNAME`、`BOARD_DB_PASSWORD`、`BOARD_ADMIN_USERNAME`、`BOARD_ADMIN_DISPLAY_NAME`、`BOARD_ADMIN_PASSWORD`。

## 5. 查看日志

```bash
journalctl -u board -f                          # 实时跟踪
journalctl -u board -n 200 --no-pager           # 最近 200 行
journalctl -u board --since today --no-pager    # 今天的日志
journalctl -u board -p err -n 50 --no-pager     # 只看错误级别
```

启动成功的日志中可以看到 Tomcat 监听端口的记录；首次启动还会出现创建初始管理员的记录。服务起不来时优先看 `journalctl -u board -n 100 --no-pager` 的末尾报错。

## 6. 验证部署

在服务器本机先确认进程与端口：

```bash
systemctl status board --no-pager
ss -lntp | grep 8080
curl -i http://127.0.0.1:8080/api/auth/me
```

`curl` 返回 `401` 是预期的（未携带会话），说明服务与接口都已就绪；返回连接失败说明进程没起来。

再用浏览器从另一台内网机器访问：

```text
http://<内网IP>:8080
```

用第 3.3 节配置的管理员账号登录，能看到看板页面即为部署成功。

**首次登录后立即重置密码**：初始管理员密码以明文形式写在配置文件里，登录后马上在成员管理里把它改成只有本人知道的新密码，并确认 `/opt/board/application.yml` 中的初始密码不再具备实际作用。

## 7. 备份与恢复

数据全部位于 `/opt/board/data/`，备份方式就是定期复制该目录，不引入任何备份工具。

单次备份（停服备份可避免复制过程中数据文件被写入）：

```bash
systemctl stop board
tar -czf /var/backups/board-data-$(date +%F).tar.gz -C /opt/board data
systemctl start board
```

`/var/backups` 不存在时先执行 `mkdir -p /var/backups`。

每日自动备份（root 的 crontab，每天 3:00 执行）：

```bash
crontab -e
```

```text
0 3 * * * systemctl stop board && tar -czf /var/backups/board-data-$(date +\%F).tar.gz -C /opt/board data && systemctl start board
```

备份文件不要只留在同一块磁盘上，用 `scp` 复制到另一台机器或内网共享目录即可。

从备份恢复：

```bash
systemctl stop board
tar -xzf /var/backups/board-data-2026-09-15.tar.gz -C /opt/board
chown -R board:board /opt/board/data
systemctl start board
```

恢复会覆盖当前数据，执行前先把现用的 `data/` 另存一份。

## 8. 升级与回滚

升级前先按第 7 节备份 `data/`，并保留上一版 jar。

在构建机上构建新版本后上传：

```bash
scp code/backend/target/board.jar root@<服务器IP>:/opt/board/board.jar.new
```

在服务器上先留存旧版，再替换并重启：

```bash
cp /opt/board/board.jar /opt/board/board.jar.bak-$(date +%F)
systemctl stop board
mv /opt/board/board.jar.new /opt/board/board.jar
chown board:board /opt/board/board.jar
systemctl start board
```

回滚：

```bash
systemctl stop board
tar -czf /var/backups/board-data-before-rollback-$(date +%F).tar.gz -C /opt/board data
cp /opt/board/board.jar.bak-2026-09-15 /opt/board/board.jar
chown board:board /opt/board/board.jar
systemctl start board
```

回滚前先保存当前 `data/` 快照；如果新版已经改动过数据，再用升级前的备份覆盖 `data/` 后重启。

## 9. 常见问题

**端口被占用**：`ss -lntp | grep 8080` 查出占用进程。换端口时同时改 `application.yml` 的 `server.port`（或 `Environment=BOARD_PORT=`）、防火墙放行规则与访问地址，三处必须一致。

**数据目录没有写权限**：日志里出现 `Permission denied` 或数据库文件打开失败。执行 `chown -R board:board /opt/board/data`；`data/` 不存在时先 `install -d -o board -g board /opt/board/data`。注意 `WorkingDirectory` 决定了 `data/` 落在哪里。

**服务重启后所有人都要重新登录**：会话保存在服务端内存中，进程重启即清空，这是已知且已接受的行为，不是故障。升级或维护尽量安排在非工作时段。

**访问根路径 404 或页面空白**：前端没有构建，或构建产物不在 `code/backend/src/main/resources/static/`。重新执行第 2.1 节后重新打包 jar 并重新上传。

**systemd 启动失败提示找不到 java**：`ExecStart` 用的是绝对路径，按第 4 节查出真实路径替换后 `systemctl daemon-reload`。

**登录提示账号已停用**：管理员在成员管理里停用了该账号，被停用账号的下一次请求就会被拒绝，需要管理员在成员管理里重新启用。

**首次启动没有创建管理员**：仅当用户表为空时才自动创建。若 `data/` 下已有旧数据，需改用已有账号登录；确实忘记密码时，停服后移走 `data/` 目录重新启动，会以全新的空库和初始管理员账号启动。

## 10. 配置项速查

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `server.port` | `BOARD_PORT` | `8080` | 页面与 `/api` 共用的唯一端口 |
| `spring.datasource.url` | `BOARD_DB_URL` | `jdbc:h2:file:./data/board;DB_CLOSE_DELAY=-1` | 相对工作目录，数据文件为 `data/board.mv.db` |
| `spring.datasource.username` | `BOARD_DB_USERNAME` | `sa` | 嵌入式数据库账号，通常不改 |
| `spring.datasource.password` | `BOARD_DB_PASSWORD` | 空 | 嵌入式数据库密码，通常不改 |
| `board.admin.username` | `BOARD_ADMIN_USERNAME` | `admin` | 首次启动创建的管理员登录名 |
| `board.admin.display-name` | `BOARD_ADMIN_DISPLAY_NAME` | `管理员` | 管理员在看板上显示的名称 |
| `board.admin.password` | `BOARD_ADMIN_PASSWORD` | `admin123` | 初始密码，部署前应改掉，首次登录后立即重置 |
| `server.servlet.session.timeout` | 无 | `8h` | 会话有效期，会话存于内存，重启即失效 |
