# TLP Transfer｜在线回复（单密码简易版）

网页继续使用 **GitHub Pages**，回复同步和密码验证使用 **Supabase Free**。
不需要独立服务器、复杂的成员审批或付费 GitHub 套餐；但需要先在 Supabase 创建一个免费的项目。
目前 GitHub 上只部署了前端和数据库初始化脚本：**不创建并配置 Supabase 项目就不能发送在线回复。**

## 启用步骤（一次性）

1. 打开 https://supabase.com/dashboard ，注册登录并新建一个 **Free** 项目。
2. 在 **SQL Editor** 执行本项目的 `database/setup.sql` 完整内容。
3. 进入 **Authentication → Providers → Email**，确认邮箱密码（Email）登录可用。进入 Auth 的注册设置，**关闭公开注册（Allow new users to sign up）**。界面菜单可能随版本变化。
4. 在 **Authentication → Users** 手动创建你自己的一个登录账号（邮箱 + 密码）。可以使用专用邮箱；确认账户已可用。对于网页用户，只需输入密码，邮箱在网页配置里预先指定。
5. 在 Supabase 项目 **Connect** 或 **Settings → API Keys** 找到 Project URL 和 **publishable** key。
6. 打开 GitHub 仓库文件 `ChatGPT/TLP/TLP Transfer/online-config.js`，填入三个值，保存到 `main`：
```js
window.TLP_TRANSFER_CONFIG = {
  supabaseUrl: "https://YOUR-PROJECT.supabase.co",
  supabasePublishableKey: "sb_publishable_YOUR_PUBLIC_KEY",
  accountEmail: "YOUR_ACCOUNT_EMAIL@example.com"
};
```
   - **只允许将公开 URL、publishable key、邮箱写入源码。**
   - 登录密码仅输入网页上的密码框，由 Supabase 服务器验证；不要把它写入 GitHub。
   - 绝对不要在公开仓库中保存 secret key、service_role key、数据库密码或 Supabase 账号的管理密码。
7. 等待 Pages 重新部署，然后在沙箱浏览器打开 [TLP Transfer](https://tlp1998.github.io/OpenAI/ChatGPT/TLP/TLP%20Transfer/)，输入设置的密码登录。
8. 在测试消息 `TLP-20261015-001` 下输入「云端在线回复测试」，点击 **确认发送**，只有网页显示服务器返回成功才算提交。另一台设备登录后刷新在线回复，应能看到同一条记录。

## 实现方式
- 网页通过 Supabase Auth 的密码登录接口获取用户 session。
- 带认证令牌直接请求 Supabase REST 数据表，回复追加保存且由 Supabase 数据库 RLS 校验。
- 其他已登录设备定期轮询，或点击「刷新在线回复」，取得相同记录；**不依赖 GitHub Pages 重新部署**。
- 登录信息仅存于当前浏览器标签的 sessionStorage，关闭会话或退出登录后不再使用；页面源代码不含密码。
- 旧 `threads.json` 的「GPT 回复记录」仍保留，属于手动归档，和新增的在线回复分开显示。
- **GPT 不会自动接收 Supabase 新回复**：要让 GPT 做下一步，需要用户在聊天中提出「处理中转站在线回复」，并接入可读取该 Supabase 项目的工具。连接之前，GPT 仍可通过用户手动提供的非敏感文字处理。

## 费用、安全和注意事项
- Supabase Free 有使用配额与低活跃暂停机制，并非永久无条件的无限免费。超过免费限制时应停止升级，不自动转为付费。
- 中转站的 GitHub Pages 静态消息/附件仍是公开的，在线回复仅登录后可读写。
- 如果打开沙箱后登录/发送失败，确认公司的外网政策**允许**访问对应 `*.supabase.co` 地址的 HTTPS POST 请求，不能绕过公司限制。
- 单密码账户意味着知道密码的人可以使用此账户；请使用独立密码，并不要共享到公开 GitHub。
- 若以后需要细分权限，可升级账号分离和数据库成员控制，但目前不做。
