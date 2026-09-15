import fs from "node:fs";
import { fileURLToPath } from "node:url";

const dir = fileURLToPath(new URL(".", import.meta.url));

const icoSearch = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.6-3.6"/></svg>`;
const icoBell = `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#F8FAFC" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 15.5V10a6 6 0 1 0-12 0v5.5L4.5 18h15L18 15.5z"/><path d="M10.2 21a2 2 0 0 0 3.6 0"/></svg>`;

const topbar = ({ bell = 0, admin = true } = {}) => `<header class="topbar">
  <div class="brand"><span class="mark">S</span>任务看板</div>
  <div class="search">${icoSearch}<input placeholder="搜索任务标题或描述"></div>
  <div class="spacer"></div>
  <div class="tb-btn">筛选</div>
  <div class="tb-icon">${icoBell}${bell ? `<span class="badge">${bell}</span>` : ""}</div>
  <div class="user"><span class="av">张</span>张明<span class="out">退出</span></div>
  ${admin ? `<div class="link">成员管理</div>` : ""}
</header>`;

const filterbar = `<div class="filterbar">
  <div class="seg"><span class="off">我的任务</span><span class="on">查看全部</span></div>
  <div class="select"><span class="ph">负责人：全部</span><span class="caret">&#9660;</span></div>
  <div class="select"><span class="ph">优先级：全部</span><span class="caret">&#9660;</span></div>
  <div class="select"><span class="ph">截止日期：全部</span><span class="caret">&#9660;</span></div>
  <div class="result">共 10 项任务 · 1 项临期 · 1 项已超期</div>
</div>`;

const PRIO = { high: "高", mid: "中", low: "低" };

const card = (c) => `<article class="card${c.done ? " done" : ""}">
  <h4>${c.t}</h4>
  <div class="meta">
    <span class="who"><span class="av-s ${c.tone || ""}">${c.ini}</span>${c.who}</span>
    <span class="right">
      <span class="due ${c.dueCls || ""}">${c.due}</span>
      ${c.flag ? `<span class="flag ${c.flagCls}">${c.flag}</span>` : ""}
      ${c.prio ? `<span class="pill ${c.prio}">${PRIO[c.prio]}</span>` : ""}
    </span>
  </div>
</article>`;

const column = (name, cards, extra = "") => `<section class="col">
  <div class="col-head"><h3>${name}</h3><span class="count-pill">${cards.length}</span></div>
  ${cards.join("\n  ")}
  ${extra}
  <div class="add-btn">+ 添加任务</div>
</section>`;

const todoCards = [
  { t: "整理客户反馈清单", who: "李婷", ini: "婷", due: "09-18", prio: "mid" },
  { t: "接口鉴权方案评审", who: "王强", ini: "强", tone: "b", due: "09-15", dueCls: "soon", flag: "临期", flagCls: "soon", prio: "high" },
  { t: "内网部署文档补充", who: "张明", ini: "明", tone: "c", due: "09-25", prio: "low" },
  { t: "看板字段口径确认", who: "李婷", ini: "婷", due: "09-30", prio: "low" },
];
const doingCards = [
  { t: "看板拖拽交互实现", who: "张明", ini: "明", tone: "c", due: "09-13", dueCls: "overdue", flag: "已超期", flagCls: "overdue", prio: "high" },
  { t: "成员管理页联调", who: "王强", ini: "强", tone: "b", due: "09-20", prio: "mid" },
  { t: "筛选与搜索性能优化", who: "赵磊", ini: "磊", tone: "d", due: "09-22", prio: "mid" },
];
const doneCards = [
  { t: "需求评审完成", who: "张明", ini: "明", tone: "c", due: "09-08", prio: "mid", done: true },
  { t: "交互原型确认", who: "李婷", ini: "婷", due: "09-10", prio: "low", done: true },
  { t: "技术选型确定", who: "王强", ini: "强", tone: "b", due: "09-11", prio: "high", done: true },
];

const board = (doingExtra = "") => `<div class="board">
${column("待办", todoCards.map(card))}
${column("进行中", doingCards.map(card), doingExtra)}
${column("完成", doneCards.map(card))}
</div>`;

const frame = (id, desc, body, cls = "", bodyStyle = "") =>
  `<div class="frame"><div class="cap"><b>${id}</b><span>${desc}</span></div><div class="body ${cls}"${bodyStyle ? ` style="${bodyStyle}"` : ""}>${body}</div></div>`;

const page = (title, body) => `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>${title}</title><link rel="stylesheet" href="styles.css"></head>
<body>${body}</body></html>`;

/* ================= PAGE-001 登录页 ================= */
const loginCard = ({ error = false, filled = false, loading = false } = {}) => `<div class="login-card">
  <div class="brand-lg"><span class="mark-lg">S</span><h1>任务看板</h1></div>
  <p class="sub">团队任务记录与进度跟踪</p>
  ${error ? `<div class="alert">账号或密码错误，请重新输入</div>` : ""}
  <div class="field"><label>登录名</label><div class="control ${filled ? "focus" : "ph"}">${filled ? "zhangming" : "请输入登录名"}</div></div>
  <div class="field"><label>密码</label><div class="control ${filled ? "" : "ph"}">${filled ? "••••••••" : "请输入密码"}</div></div>
  <div class="btn btn-primary${loading ? " disabled" : ""}">${loading ? `<span class="spin dark"></span>登录中` : "登录"}</div>
  <div class="hint">账号由管理员分配，如无法登录请联系管理员</div>
</div>`;

const loginStates = [
  ["STATE-002", "PAGE-001 · 输入中", loginCard({ filled: true })],
  ["STATE-003", "PAGE-001 · 提交中", loginCard({ filled: true, loading: true })],
  ["STATE-004", "PAGE-001 · 登录失败", loginCard({ error: true, filled: true })],
];

const page001 = page("UI-001 · PAGE-001 登录页", `
<div class="login-wrap">${loginCard()}</div>
<div class="sec-title">页面状态 · STATE-001 ~ STATE-004</div>
<div class="gallery g3">
  ${loginStates.map(([id, d, c]) => frame(id, d, `<div style="display:grid;place-items:center;min-height:404px">${c}</div>`, "", "background:#F1F5F9")).join("\n  ")}
</div>`);

/* ================= PAGE-002 看板页 ================= */
const skCol = `<div class="col" style="background:#fff">
  <div class="sk h8" style="width:52px"></div>
  <div class="sk-card"><div class="sk" style="width:72%"></div><div class="sk h8" style="width:48%"></div></div>
  <div class="sk-card"><div class="sk" style="width:58%"></div><div class="sk h8" style="width:36%"></div></div>
</div>`;

const stateEmpty = (icon, title, desc, acts) => `<div class="empty">
  <div class="ic">${icon}</div><h5>${title}</h5><p>${desc}</p><div class="acts">${acts}</div></div>`;

const dragMini = `<div class="mini">
  <div class="col" style="background:#fff">
    <div class="col-head" style="padding:0 0 2px"><h3 style="font-size:12px">待办</h3></div>
    <article class="card" style="padding:9px;gap:6px"><h4 style="font-size:12px">整理客户反馈清单</h4><div class="meta" style="font-size:11px"><span class="who"><span class="av-s">婷</span>李婷</span></div></article>
    <article class="card dragging" style="padding:9px;gap:6px"><h4 style="font-size:12px">接口鉴权方案评审</h4><div class="meta" style="font-size:11px"><span class="who"><span class="av-s b">强</span>王强</span></div></article>
  </div>
  <div class="col" style="background:#fff">
    <div class="col-head" style="padding:0 0 2px"><h3 style="font-size:12px">进行中</h3></div>
    <div class="dropzone">松手放入「进行中」</div>
    <article class="card" style="padding:9px;gap:6px"><h4 style="font-size:12px">成员管理页联调</h4><div class="meta" style="font-size:11px"><span class="who"><span class="av-s b">强</span>王强</span></div></article>
  </div>
  <div class="col" style="background:#fff">
    <div class="col-head" style="padding:0 0 2px"><h3 style="font-size:12px">完成</h3></div>
    <article class="card done" style="padding:9px;gap:6px"><h4 style="font-size:12px">需求评审完成</h4><div class="meta" style="font-size:11px"><span class="who"><span class="av-s c">明</span>张明</span></div></article>
  </div>
</div>`;

const bellItems = `<div class="bell-panel" style="margin-top:12px">
  <div class="ph"><span>我的提醒</span><span style="font-size:12px;color:#64748B">2 项待处理</span></div>
  <div class="bell-item"><div class="t">看板拖拽交互实现</div><div class="m"><span class="flag overdue">已超期</span><span>截止 09-13 · 已超期 1 天</span></div></div>
  <div class="bell-item"><div class="t">接口鉴权方案评审</div><div class="m"><span class="flag soon">临期</span><span>截止 09-15 · 明天到期</span></div></div>
</div>`;

const page002 = page("UI-002 · PAGE-002 看板页", `
${topbar({ bell: 2 })}
${filterbar}
${board()}
<div class="sec-title">页面状态 · STATE-005 ~ STATE-013</div>
<div class="gallery g2">
  ${frame("STATE-005", "PAGE-002 · 加载中", `<div class="mini">${skCol}${skCol}${skCol}</div>`)}
  ${frame("STATE-006", "PAGE-002 · 加载失败", `<div style="min-height:178px;display:grid;place-items:center"><div style="width:100%"><div class="notice">看板加载失败，请检查内网连接后重试</div><div style="display:flex;justify-content:center;margin-top:12px"><span class="btn btn-secondary">重试</span></div></div></div>`)}
  ${frame("STATE-007", "PAGE-002 · 看板无任务", stateEmpty("&#9638;", "看板还没有任务", "先创建第一个任务，团队成员登录后即可看到", `<span class="btn btn-primary">+ 新建任务</span>`))}
  ${frame("STATE-008", "PAGE-002 · 我的任务为空", stateEmpty("&#9678;", "你还没有任务", "当前筛选为「负责人 = 我」，可以查看全部任务或创建新任务", `<span class="btn btn-secondary">查看全部任务</span><span class="btn btn-primary">+ 新建任务</span>`))}
  ${frame("STATE-009", "PAGE-002 · 筛选无结果", stateEmpty("&#9906;", "没有匹配的任务", "当前条件：负责人 王强 · 优先级 高 · 已超期", `<span class="btn btn-secondary">清除筛选条件</span>`))}
  ${frame("STATE-011", "PAGE-002 · 拖拽中", dragMini)}
  ${frame("STATE-012", "PAGE-002 · 提醒有未处理项", `<div style="display:flex;align-items:center;gap:10px"><span style="font-size:12px;color:#94A3B8">顶栏提醒入口</span><div class="spacer"></div><div class="tb-icon">${icoBell}<span class="badge">2</span></div></div>${bellItems}`, "dark", "min-height:210px")}
  ${frame("STATE-013", "PAGE-002 · 提醒无未处理项", `<div style="display:flex;align-items:center;gap:10px"><span style="font-size:12px;color:#94A3B8">顶栏提醒入口</span><div class="spacer"></div><div class="tb-icon">${icoBell}</div></div><div style="margin-top:14px;font-size:12px;color:#94A3B8">暂无临期或超期任务</div>`, "dark", "min-height:210px")}
</div>`);

/* ================= PAGE-003 成员管理页 ================= */
const members = [
  { login: "zhangming", name: "张明", on: true, admin: true, self: true },
  { login: "wangqiang", name: "王强", on: true },
  { login: "liting", name: "李婷", on: true },
  { login: "zhaolei", name: "赵磊", on: true },
  { login: "sunqi", name: "孙琦", on: false },
];

const memberRow = (m) => `<tr>
  <td>${m.login}</td>
  <td>${m.name}${m.admin ? ` <span class="tag admin">管理员</span>` : ""}</td>
  <td><span class="tag ${m.on ? "on" : "off"}">${m.on ? "启用" : "停用"}</span></td>
  <td><div class="row-actions">
    <span class="btn btn-secondary btn-sm">重置密码</span>
    ${m.self
      ? `<span class="btn btn-secondary btn-sm disabled">停用</span><span style="font-size:12px;color:#94A3B8;align-self:center">不可停用当前账号</span>`
      : `<span class="btn btn-secondary btn-sm">${m.on ? "停用" : "启用"}</span>`}
  </div></td>
</tr>`;

const memberTable = (rows) => `<div class="panel">
  <table>
    <thead><tr><th style="width:180px">登录名</th><th>显示名</th><th style="width:120px">状态</th><th style="width:300px">操作</th></tr></thead>
    <tbody>${rows.join("\n    ")}</tbody>
  </table>
</div>`;

const addForm = `<div class="panel">
  <div class="panel-title">新增成员</div>
  <div class="form-row">
    <div class="field"><label>登录名 <span class="req">*</span></label><div class="control focus">wangqiang</div><div class="err">登录名已存在，请更换</div></div>
    <div class="field"><label>显示名 <span class="req">*</span></label><div class="control">王强</div></div>
    <div class="field"><label>密码 <span class="req">*</span></label><div class="control ph">至少 6 位</div></div>
    <div class="field"><label>状态</label><div class="control">启用<span class="caret">&#9660;</span></div></div>
    <div class="btns" style="padding-top:22px"><span class="btn btn-primary">保存</span><span class="btn btn-secondary">取消</span></div>
  </div>
</div>`;

const resetPwdModal = `<div class="modal">
  <div class="modal-head">重置密码</div>
  <div class="modal-body">
    <p class="confirm-text" style="margin:0">为成员「王强」设置新密码，保存后立即生效。</p>
    <div class="field"><label>新密码 <span class="req">*</span></label><div class="control focus">••••••</div></div>
  </div>
  <div class="modal-foot"><span class="btn btn-secondary">取消</span><span class="btn btn-primary">确认</span></div>
</div>`;

const stopModal = `<div class="modal w400">
  <div class="modal-head">停用成员</div>
  <div class="modal-body"><p class="confirm-text">停用后「孙琦」将无法登录，其名下任务保留、负责人不变。</p></div>
  <div class="modal-foot"><span class="btn btn-secondary">取消</span><span class="btn btn-danger">确认停用</span></div>
</div>`;

const page003 = page("UI-003 · PAGE-003 成员管理页", `
${topbar()}
<div class="content">
  <div class="content-head"><h1>成员管理</h1><span class="btn btn-primary">+ 新增成员</span></div>
  ${memberTable(members.map(memberRow))}
</div>
<div class="sec-title">页面状态 · STATE-014 ~ STATE-017</div>
<div class="gallery g1">${frame("STATE-015", "PAGE-003 · 新增表单展开", addForm, "", "background:#F1F5F9")}</div>
<div class="gallery g2">
  ${frame("STATE-016", "PAGE-003 · 重置密码弹窗", `<div class="overlay" style="padding:20px">${resetPwdModal}</div>`, "flush fixed", "height:300px;position:relative")}
  ${frame("STATE-017", "PAGE-003 · 停用确认", `<div class="overlay" style="padding:20px">${stopModal}</div>`, "flush fixed", "height:300px;position:relative")}
</div>`);

/* ================= PAGE-004 新建任务弹层 ================= */
const newTaskModal = ({ invalid = false, saving = false, state = "待办" } = {}) => `<div class="modal">
  <div class="modal-head">新建任务</div>
  <div class="modal-body">
    <div class="field"><label>标题 <span class="req">*</span></label>
      <div class="control ${invalid || saving ? "" : "ph"}">${saving ? "接口鉴权方案评审" : "例如：整理客户反馈清单"}</div>
      ${invalid ? `<div class="err">请输入任务标题</div>` : ""}</div>
    <div class="row2">
      <div class="field"><label>负责人</label><div class="control ${saving ? "" : "ph"}">${saving ? "王强" : "选择负责人"}<span class="caret">&#9660;</span></div></div>
      <div class="field"><label>状态</label><div class="control">${state}<span class="caret">&#9660;</span></div></div>
    </div>
    <div class="row2">
      <div class="field"><label>截止日期</label><div class="control ${saving ? "" : "ph"}">${saving ? "2026-09-15" : "选择日期"}</div></div>
      <div class="field"><label>优先级</label><div class="control">${saving ? "高" : "中"}<span class="caret">&#9660;</span></div></div>
    </div>
    <div class="field"><label>描述</label><textarea class="control">${saving ? "补充鉴权方案对比结论与验收要点" : "补充说明、验收要点等"}</textarea></div>
  </div>
  <div class="modal-foot">
    <span class="btn btn-secondary">取消</span>
    <span class="btn ${saving ? "disabled" : "btn-primary"}">${saving ? `<span class="spin dark"></span>保存中` : "保存"}</span>
  </div>
</div>`;

const page004 = page("UI-004 · PAGE-004 新建任务弹层", `
<div class="sec-title" style="padding-top:14px">主视图 · STATE-018（PAGE-004 弹层覆盖在 PAGE-002 看板之上）</div>
<div class="page-back">
  ${topbar({ bell: 2 })}
  ${filterbar}
  ${board()}
  <div class="overlay">${newTaskModal({ state: "待办" })}</div>
</div>
<div class="sec-title">页面状态 · STATE-018 ~ STATE-020</div>
<div class="gallery g2">
  ${frame("STATE-019", "PAGE-004 · 校验失败", `<div class="overlay" style="padding:20px">${newTaskModal({ invalid: true, state: "进行中" })}</div>`, "flush fixed", "height:700px;position:relative")}
  ${frame("STATE-020", "PAGE-004 · 提交中", `<div class="overlay" style="padding:20px">${newTaskModal({ saving: true, state: "待办" })}</div>`, "flush fixed", "height:700px;position:relative")}
</div>`);

/* ================= PAGE-005 任务详情弹层 ================= */
const detailModal = ({ saving = false } = {}) => `<div class="modal w560">
  <div class="modal-head">任务详情</div>
  <div class="modal-body">
    <div class="field"><label>标题</label><div class="control">看板拖拽交互实现</div></div>
    <div class="row2">
      <div class="field"><label>负责人</label><div class="control">张明<span class="caret">&#9660;</span></div></div>
      <div class="field"><label>状态</label><div class="control">进行中<span class="caret">&#9660;</span></div></div>
    </div>
    <div class="row2">
      <div class="field"><label>截止日期</label>
        <div class="control" style="border-color:#FECACA">2026-09-13<span class="flag overdue">已超期</span></div></div>
      <div class="field"><label>优先级</label><div class="control">高<span class="caret">&#9660;</span></div></div>
    </div>
    <div class="field"><label>描述</label><textarea class="control" style="color:#0F172A;min-height:96px">实现卡片在三列之间拖拽换列，拖动中目标列给出放置反馈，失败时回到原列并提示。</textarea></div>
  </div>
  <div class="modal-foot split">
    <span class="btn btn-danger">删除</span>
    <span style="display:flex;gap:10px">
      <span class="btn btn-secondary">取消</span>
      <span class="btn ${saving ? "disabled" : "btn-primary"}">${saving ? `<span class="spin dark"></span>保存中` : "保存"}</span>
    </span>
  </div>
</div>`;

const deleteConfirmModal = `<div class="modal w400">
  <div class="modal-head">删除任务</div>
  <div class="modal-body"><p class="confirm-text">删除后不可恢复，任务「看板拖拽交互实现」将被永久移除。</p></div>
  <div class="modal-foot"><span class="btn btn-secondary">取消</span><span class="btn btn-danger">确认删除</span></div>
</div>`;

const page005 = page("UI-005 · PAGE-005 任务详情弹层", `
<div class="sec-title" style="padding-top:14px">主视图 · STATE-021（PAGE-005 弹层覆盖在 PAGE-002 看板之上）</div>
<div class="page-back">
  ${topbar({ bell: 2 })}
  ${filterbar}
  ${board()}
  <div class="overlay">${detailModal()}</div>
</div>
<div class="sec-title">页面状态 · STATE-021 ~ STATE-024</div>
<div class="gallery g2">
  ${frame("STATE-023", "PAGE-005 · 删除确认", `<div class="overlay" style="padding:20px"><div class="stack"><div class="dim">${detailModal()}</div><div class="on-top">${deleteConfirmModal}</div></div></div>`, "flush fixed", "height:640px;position:relative")}
  ${frame("STATE-024", "PAGE-005 · 保存中", `<div class="overlay" style="padding:20px">${detailModal({ saving: true })}</div>`, "flush fixed", "height:640px;position:relative")}
</div>`);

/* ================= write ================= */
const files = [
  ["page-001-login.html", page001],
  ["page-002-board.html", page002],
  ["page-003-members.html", page003],
  ["page-004-new-task.html", page004],
  ["page-005-task-detail.html", page005],
];

const sizes = [
  { file: "page-001-login.html", out: "UI-001-login.png", width: 1440, height: 1290 },
  { file: "page-002-board.html", out: "UI-002-board.png", width: 1440, height: 1815 },
  { file: "page-003-members.html", out: "UI-003-members.png", width: 1440, height: 1170 },
  { file: "page-004-new-task.html", out: "UI-004-new-task.png", width: 1440, height: 1660 },
  { file: "page-005-task-detail.html", out: "UI-005-task-detail.png", width: 1440, height: 1580 },
];

for (const [name, content] of files) fs.writeFileSync(new URL(name, import.meta.url), content, "utf8");
fs.writeFileSync(new URL("sizes.json", import.meta.url), JSON.stringify(sizes, null, 2), "utf8");
console.log("built " + files.length + " pages in " + dir);
