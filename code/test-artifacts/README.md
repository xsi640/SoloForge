# 测试产物

本目录存放测试与验证产生的产物，由 `code/scripts` 下的脚本自动生成，不手工维护。

```text
code/test-artifacts/
├── smoke/           接口冒烟测试
│   ├── report.md      最近一次的测试记录（断言明细、结论、运行目录）
│   └── run-<时间戳>/  每次运行的原始产物
│       └── runtime/   本次使用的 H2 数据库与服务日志
└── e2e/             浏览器端到端验证
    └── shots/         各页面与状态的截图
```

## 如何生成

接口冒烟测试：

```powershell
pwsh -NoProfile -File code/scripts/smoke-test.ps1
```

- 自动启动被测 jar、执行断言、写入 `smoke/report.md`，然后停止进程。
- 默认产物目录为 `code/test-artifacts/smoke`，可用 `-ArtifactDir` 指定其他位置。
- 常用参数：`-Port` 指定端口（默认 18080）、`-Jar` 指定被测产物、`-KeepRunning` 保留进程便于排查。

浏览器端到端验证：

```bash
npm install --prefix code/scripts/e2e
node code/scripts/e2e/board-e2e.mjs
```

- 需要先自行启动服务（默认读取 `http://localhost:8080`，可用环境变量 `BOARD_URL` 覆盖）。
- 截图输出到 `e2e/shots/`，可用环境变量 `BOARD_SHOTS` 指定其他位置。

## 约定

- 产物是「可重复生成」的，随时可以整目录删除，重新运行脚本即可再生成。
- `smoke/report.md` 记录最近一次结果，可入库作为测试记录；`run-*/` 与 `e2e/shots/` 属于原始产物，体量较大，是否入库由使用者决定。
- 运行目录里的 H2 数据库只用于本次测试，不要用于生产或长期演示。