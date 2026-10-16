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

## 消息与回复管理（2026-10-16）

中转站页面顶部包含 **「＋ 新增 / 删除」** 管理入口；消息详情还提供 **「移除 / 删除此消息」**，已加载的 GitHub 评论下提供 **「生成删除此回复指令」**。

- **新增消息**：填写标题和正文后复制 ChatGPT 管理指令，在私人设备的聊天中提交；如有附件，附在 ChatGPT 中。ChatGPT 使用已连接 GitHub 创建消息 HTML、附件文件、相应 Issue，并更新当前收件箱的 `messages` 清单。
- **移除消息**：默认只更新收件箱索引，不删除消息正文文件、附件或 Issue 评论。**隐藏不等于销毁公开数据**，旧链接仍可直接访问。
- **删除正文 / 附件**：可选；先核对 GitHub 路径、附件的共享引用与可恢复性，再经用户确认后执行。只删除本消息专属文件，不误删其他消息共用附件。
- **处理关联回复**：可选；在管理指令中明确提出。Issue 及评论属于 GitHub 数据，关闭 Issue **不会删除** 评论。当前连接器不保证支持彻底删除整个 Issue 或任意评论；若无法通过工具删除，需仓库管理员在 GitHub 中按权限手动操作。必须如实报告实际删除情况。
- **删除单条回复**：通过相应评论下的操作生成携带评论链接的删除请求。先核对归属及权限，再由用户确认；在网页点击复制不代表评论已删除。
- **重要**：管理面板不持有 GitHub 写入 Token，不会在公司沙箱直接进行写入；提交操作需在获准的个人 ChatGPT 中进行。未经审批的公司资料和敏感评论不得发布到公共仓库。

管理操作成功后保持上一级 TLP 项目总目录入口，不能删除整个 `TLP Transfer` 项目目录。

## GitHub 网页手动删除（方案 B，2026-10-16）

现行管理方式是 **GitHub 账号手动删除**，不是在 GitHub Pages 上直接写入或删除。中转站顶部「＋ 新增 / 管理」和消息内「管理 / 删除此消息」提供 GitHub 直达入口，评论下「管理 / 删除此回复」会自动填写其 GitHub 原评论链接。

### 删除一条回复（不影响原消息）
1. 在中转站打开消息，等待评论从 GitHub 公共 API 加载。
2. 在指定回复下点击「管理 / 删除此回复」，管理区点击「定位此评论并手动删除」；也可点击「查看 GitHub 原评论」。
3. 以有权限的账号登录 GitHub，在该评论右上角「…」菜单选择 Delete，并确认。
4. 返回中转站点击「刷新回复记录」，核对评论已消失。若沙箱无法打开 `api.github.com`，可直接在 GitHub Issue 页面操作。

### 删除整条中转站消息（需要同步清理三个位置）
1. 在中转站打开目标消息，点击「管理 / 删除此消息」，核对编号及附件。
2. 点击「① 前往 GitHub 删除 Issue」，登录有权限的 GitHub 账号，在 Issue 页选择 Delete issue 并确认。**Close issue 仅是关闭，不等于删除。**
3. 点击「② 打开消息 HTML 文件」，在 GitHub 文件页面选择删除文件，并提交到 `main`。
4. 点击「③ 编辑收件箱索引」，编辑 `ChatGPT/TLP/TLP Transfer/index.html` 中 `const messages=[...]`，删除包含目标 `id` 的整个消息对象，提交到 `main`，注意保证 JavaScript 数组语法正确。
5. 对该消息的专属附件，在核对没有其他消息引用后从 `attachments/` 删除，提交到 `main`。共享附件不要删。
6. 刷新 GitHub Pages 收件箱：确认目标不再显示、旧消息正文页面返回 404，其他消息正常；GitHub Pages 构建与缓存可能有延迟。

**重要**：删除 Issue 不会自动删除独立 HTML 或收件箱索引，反过来也一样。删除当前分支的 HTML 不代表清除 Git 提交历史。静态站点不能安全地以无认证方式直接执行 GitHub 删除；不要在页面源码中保存 GitHub Token。

## 右键/长按删除和已删除 Issue 同步（2026-10-16）

- **列表消息**：桌面鼠标右键、手机长按约 0.65 秒可打开“删除这条中转站消息”操作菜单。
- **已加载的评论**：在回复卡片右键/长按可选择“删除这条回复”；需要评论已从 `api.github.com` 成功读取。
- **访问授权**：在私人设备上输入由 GitHub 生成、仅授予 `TLP1998/OpenAI` 访问权限的 Fine-grained personal access token。删除评论需要 Issues 写入；删除 HTML/索引需要 Contents 写入。令牌只在本次网页打开期间保存在 JavaScript 内存，不写入 HTML、仓库、localStorage、sessionStorage 或 URL。**公司沙箱不得输入私人 Token**。直接删除要求客户端能够访问 `api.github.com` 并且 GitHub API 允许该网页发起请求。
- **删除单条回复**：网页请求 GitHub REST `DELETE /repos/TLP1998/OpenAI/issues/comments/{comment_id}`；GitHub 确认成功才移除本地回复卡片。
- **删除整条中转站消息**：网页先调用 GitHub API 关闭仍存在的 Issue（**无法通过公开 REST API 永久删除整个 Issue**），再更新 `index.html` 消息数组并删除对应专属 HTML 文件。附件默认不删，以免误删共享附件；GitHub Pages 发布与缓存刷新可能延迟。请求连续发生，**不是事务操作**；若部分失败应根据页面提示与 GitHub 提交记录继续清理。
- **GitHub 已手动删除的 Issue**：每次打开中转站，页面会尝试公开 API 检查每条关联 Issue。确认返回 HTTP 404 的消息会从当前浏览器收件箱列表中隐藏，不依赖静态索引立即重新部署；该效果是**运行时隐藏**，不会修改 GitHub Pages 已存在的索引、HTML 或附件。网络故障、403、速率限制时保留原条目，不会误当作删除。
- **权限和安全**：不要在公开仓库源码、评论、测试截图或公司沙箱输入 Token。令牌本身是高敏感凭证，设备不可信时不要使用网页直接删除，而应使用 GitHub 原生账号操作。Token 可能会因为浏览器扩展、恶意脚本或站点被篡改而泄露；请尽量缩小其仓库范围、权限和有效期，并用完撤销。网页中间层不会帮助绕过 GitHub 身份认证。
- **已删除 Issue 与永久删除的区别**：网页删除消息的操作只关闭 Issue 并删除静态入口与 HTML；要永久删除关联 Issue，仍须由有权限的仓库管理员使用 GitHub 网页执行 Delete issue。
