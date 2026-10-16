# TLP Transfer｜GPT 中转站

**最简实现：GitHub Pages + GitHub Issues。**

- GitHub Pages 展示消息、附件、回复列表，浏览无需登录。
- 每条消息对应一个 GitHub Issue；GitHub Issue 评论保存公开回复。
- 在中转站点击「前往 GitHub 发表评论」，在 GitHub 原生页面登录并发布。
- 回到中转站点击「刷新回复记录」，网页通过 GitHub 公共 API 读取评论。
- 无 utterances、无独立数据库、无第三方评论插件，也不需要另设中转站密码。
- GitHub 原生写入必须进行 GitHub 身份认证；静态 Pages 无法提供匿名写入。

## 测试消息
- 编号：`TLP-20261015-001`
- 评论地址：https://github.com/TLP1998/OpenAI/issues/1
- 中转站：https://tlp1998.github.io/OpenAI/ChatGPT/TLP/TLP%20Transfer/
- TLP 项目总目录：https://tlp1998.github.io/OpenAI/ChatGPT/TLP/

## 新建内容规则
1. 发布新消息到 `messages/YYYY-MM-DD-NNN.html`；附件放入 `attachments/`。
2. 创建对应的 GitHub Issue，标题包含该消息编号。
3. 在 `index.html` 的 `messages` 数组中追加元数据和 `issueNumber`。
4. 所有在线回复写入对应 GitHub Issue；不再更新 `threads.json` 作为新回复存储。
5. 新建项目时同步维护上级 `../index.html`，勿影响其他项目。
6. GPT 如需根据评论继续工作，用户在 ChatGPT 中主动发出「读取中转站回复」的请求；网页提交不会自动触发 GPT。

## 注意
- GitHub Pages 和公开 Issue 评论均面向互联网公开，仅限允许外发的非敏感资料。
- 访问网页只需 github.io；查看及发布 GitHub 评论需要 github.com；网页内直接读取评论另外依赖 api.github.com，若沙箱阻断该域名，可直接在 GitHub Issue 中阅读。
- GitHub 网页发表评论需要登录，不要在公开源码中放置访问令牌来规避认证。
- `threads.json` 是旧版手动归档历史，和在线 Issue 评论是不同来源，不应混淆。

## 当前消息与回复管理：GitHub 网页登录模式（2026-10-16）

**当前版本不使用 Personal Access Token，不收集、存储或提交任何 GitHub 授权令牌。** 之前关于在网页输入 Fine-grained Token、直接调用 GitHub DELETE/PATCH API 的指导均已废弃。

### 删除某一条回复
1. 在收件箱打开消息，等待 GitHub 公共 API 显示已有评论。
2. 手机长按回复或电脑鼠标右键，选择「在 GitHub 删除这条回复」。
3. 浏览器打开 GitHub 官方 Issue 的对应评论。若未登录，请在 GitHub 官方网页登录有删除权限的账号。
4. 在该评论右上角的「…」菜单中选择 Delete，并确认。返回中转站点击「刷新回复记录」。
5. 不会删除其他回复或原始消息。

### 删除整条中转站消息
1. 手机长按消息、电脑鼠标右键，选择「在 GitHub 删除整个 Issue」；浏览器跳转到 GitHub 官方网站。登录后用 Delete issue 删除（Close issue 不等于删除）。
2. 已删除 Issue 返回 HTTP 404 或 410 时，中转站会在加载时从**当前浏览器显示列表**隐藏消息。此举不会自动修改仓库的静态索引或删除 HTML。
3. 在长按菜单的「打开消息 HTML 文件」中，使用 GitHub 文件管理界面删除独立 HTML；检查专属附件，确认未被其他消息共用后再删除。
4. 在「编辑中转站索引」中，编辑 `ChatGPT/TLP/TLP Transfer/index.html` 的 `const messages=[...]`，删除该消息的对象记录并提交。
5. GitHub Pages 更新后刷新页面，检查消息与独立文件是否真正删除。删除当前分支文件不等于移除 Git 历史。

### 权限与访问限制
- GitHub Pages 不能复用 GitHub 网站的登录会话直接执行写操作：**删除操作必须跳转 GitHub 官方网页**，这与 GitHub OAuth 授权后在站内直接删除不同。
- 公司沙箱如果禁止 GitHub 登录，仍然只可阅读、下载、查看公开评论；私人设备登录 GitHub 后管理消息。
- GitHub `api.github.com` 在网络不可达、被限流或返回 403 时，收件箱运行时隐藏和评论同步可能失败；不能将此类错误视为消息已删除。
- 无独立后端和 OAuth 的情况下，无法同时实现 GitHub 官方登录和在静态中转站内“一键直接删除”。
- 请撤销此前暴露在聊天中的所有旧 GitHub PAT。当前中转站不再使用它们。

## Issue 删除与静态中转站自动同步（2026-10-16）

已启用两层机制：

1. **即时显示同步**：在 GitHub Issue 中永久删除后，返回 TLP Transfer 页面（聚焦或重新显示）会再次查询对应 Issue；收到明确 404/410 则从当前页面隐藏。顶部「↻ 同步删除状态」可手动触发。网络故障或 403 不会误删。
2. **仓库静态文件同步**：仓库工作流 `.github/workflows/sync-tlp-deleted-issues.yml` 监听 GitHub Issues 的 `deleted` 事件，每 30 分钟定时巡检，还支持 GitHub Actions 页面手动 `Run workflow`。任务使用 GitHub Actions **内置 GITHUB_TOKEN**（不是用户 PAT）查询 Issue、删除对应索引项和专属消息 HTML，并提交回 `main`。
3. **保护范围**：不会删附件（可能共享），不会处理仅关闭的 Issue，不会清空其他消息，不会从 Git 历史擦除已删除文件。

**前提及限制**：仓库须允许 GitHub Actions 运行及工作流向 `main` 写入；如果 GitHub Actions 没有写权限、分支保护阻止推送、事件延迟或 Pages 部署延迟，需在 Actions 中查看 `Sync deleted TLP Issues` 的运行情况。计划任务并非严格准点。仅靠浏览器隐藏并不意味着静态仓库已清理完成。

## 附件与界面优化（2026-10-16）

- 附件清单从消息元数据 `messages[].files` 读取；仓库中已核对 `attachments/2026-10-15-001-readme.txt` 存在。
- 同源附件在收件箱展示「下载」与「预览」两个入口。下载链接使用 HTML `download` 属性指定文件名；预览在新标签中打开。不同浏览器或沙箱的下载策略可能影响实际效果。
- 限制附件引用为当前仓库下 `attachments/` 的相对路径，拒绝路径穿越与外部链接；上传新附件后需要在消息索引中补充文件名和路径。
- 调整收件箱消息摘要、选中态、移动端工具栏、阅读区域以及无附件提示。
- GitHub 文件存在与网页链接正确已检查；真实手机/公司沙箱浏览器下载仍需现场验证。如果点击下载没有保存文件，请检查浏览器下载目录及公司沙箱的下载策略。
