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
