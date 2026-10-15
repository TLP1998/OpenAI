# TLP Transfer｜无密码在线回复版

## 设计
- 网页：既有 GitHub Pages （完全保留 TLP 项目总目录、消息和附件）。
- 回复：Supabase Free 数据库，通过公开 REST API 无登录写入，并跨设备读取。
- 功能：打开消息→输入非敏感回复→**确认发送**→数据库保存→任意设备刷新看到该回复。
- **完全免费目标仅在 Supabase 免费额度内成立**；免费额度、闲置暂停和网络可用性取决于 Supabase 服务商。
- **此模式不具备保密性和身份认证**，任何人都可查看和伪造回复，也可能发送垃圾消息。仅用于允许公开的测试资料。云端回复并不自动触发 GPT 执行。

## 还需要你完成的一次性设置
1. 在 https://supabase.com/dashboard 注册，选择 Free 创建一个新项目。
2. 在 **SQL Editor** 执行同目录的 `database/setup.sql` 全文。如果先前已执行旧版密码登录 SQL，再执行本文件即可转为公开模式。
3. 打开 Supabase 的 **Connect** 或 **Settings → API Keys** 获取 **Project URL** 和 **publishable key**（或旧式 anon public key）。
4. 在 GitHub 的 `ChatGPT/TLP/TLP Transfer/online-config.js` 填入：
```js
window.TLP_TRANSFER_CONFIG = {
  supabaseUrl: "https://YOUR-PROJECT.supabase.co",
  supabasePublishableKey: "sb_publishable_YOUR_PUBLIC_KEY"
};
```
5. 提交到 `main`，等 GitHub Pages 部署后刷新中转站。若企业沙箱禁止到 Supabase 域名的请求，在线回复会失败，此时应通过正常网络管理渠道确认允许访问。

## 验证步骤
- 沙箱中打开中转站，选择 `TLP-20261015-001`，填写“云端回复测试”，点击**确认发送**。
- 网页必须显示“服务器已保存”，这才代表写入成功。
- 手机或私人电脑打开同一网页，点击**刷新云端回复**，检查记录。
- GitHub 原来的 `threads.json` 保留为历史归档；数据库里的新回复不会自动写入 GitHub。
- 即便用户在中转站发布回复，ChatGPT **不会自动得知**。需要用户主动在聊天里要求我查看已授权的数据源或粘贴合规的非敏感回复。没有连接 Supabase 时不能宣称已读取数据库。

## 严格注意
- 网站和新回复对全互联网公开，包括内容、时间和消息编号。不要发布公司机密、用户敏感信息、内部文档或账号密码。
- 因为没有登录，任何访客都能伪造回复或消耗免费数据库配额。建议测试通过后启用认证或验证码、速率限制。
- 请勿将数据库密码、Supabase secret key、service_role key、任何登录密码写入 GitHub 页面。
- 数据库 RLS 仅限制为可读取和新增回复（禁止普通访问者修改、删除），不提供身份验证。
