# TLP Transfer｜GPT 中转站

通过 **GitHub Pages + GitHub Issues + utterances** 构建的免费成果收件箱。
- Pages：发布 GPT 消息正文、附件及收件箱索引。
- Issues：保存在线回复，无需自建服务器、数据库、项目密码。
- utterances：把 GitHub Issue 评论输入框嵌入网页。用户以 GitHub 账号登录即可发表。
- 所有网页内容和 Issue 评论均是**公开的**，仅用于允许公开的非敏感信息。

## 在线回复首次启用（仓库所有者执行一次）
1. 用 GitHub 账号登录 `TLP1998`。
2. 打开 https://github.com/apps/utterances ，点击 **Install**。
3. 选择 `TLP1998` 账号，并选择 **Only select repositories**，勾选 `OpenAI` 仓库（建议避免授予其他仓库权限）。
4. 回到 https://tlp1998.github.io/OpenAI/ChatGPT/TLP/TLP%20Transfer/ ，刷新页面，选择对应消息，找到 **填写并发送新回复**。
5. 首次点击 GitHub 登录或授权 utterances。输入公开且非敏感的文字后点击评论区内的提交按钮。
6. 打开另一设备，点击 **刷新回复记录**，应看到同一条回复。也可直接在 GitHub Issue 评论页看到。

如果沙箱无法访问 `utteranc.es`、GitHub 登录或 GitHub API，改为点击 **在 GitHub 打开回复页**。这一链接直接使用 GitHub 原生评论页面，发送结果仍可在 Pages 中读取。如果外发权限受公司限制，遵守相关规定，不通过其他手段绕过。

## 消息发布与回复映射规则
- 每条消息使用唯一编号，例如 `TLP-20261015-001`。
- 消息全文存放 `messages/YYYY-MM-DD-NNN.html`；附件存放 `attachments/`。
- 创建一条 GitHub Issue，标题包含相同的消息编号。测试编号 `TLP-20261015-001` 对应 **Issue #1**：https://github.com/TLP1998/OpenAI/issues/1 。
- 更新本目录 `index.html` 中 `messages` 数组，为消息填入对应的 `issueNumber`。不同消息必须用不同 Issue，避免串线。
- 在线回复保存为 Issue 评论；GitHub Pages 不需要因新评论重新部署。网页通过 `https://api.github.com/repos/TLP1998/OpenAI/issues/<ISSUE_NUMBER>/comments` 读取公开评论。
- 历史 `threads.json` 为旧版手动归档，**与新在线评论不互通**。禁止把“已复制口令”说成“已发布评论”。
- 新建项目时维护 TLP 总目录 `../index.html`，不要破坏现有日报项目。
- 用户说“发到中转站”时，先确认内容可公开，成功发布消息及对应 Issue 后再报告成功。
- GPT 可以在 GitHub Issue 中读取评论并按要求处理，但**不会自动感知或后台监听**。用户仍需要在聊天中主动发出“读取中转站新回复”指令，或设置未来的检查任务。

## 维护与限制
- utterances 本身免费，GitHub Issues 免费，当前公开仓库的 GitHub Pages 按 GitHub 免费服务条款使用。
- 无独立密码登录：在线发表评论仍需 GitHub 登录（OAuth）。不支持匿名无凭据写入。
- GitHub API 有调用限额；若网页读取评论失败，直接打开对应 Issue 查看。
- 网页上传附件仍只通过个人 ChatGPT/GitHub 项目发布流程，不能从公司环境上传公司资料。
- 公开评论不能存储公司秘密、内部数据、个人敏感信息。
