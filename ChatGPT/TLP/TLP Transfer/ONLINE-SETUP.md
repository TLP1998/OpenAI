# TLP Transfer｜免费在线回复接入说明

网页继续由 **GitHub Pages** 托管；云端身份验证及在线回复使用 **Supabase Free**。
此文件夹不存储数据库密码、用户密码、secret key 或 service_role key。

## 1. 新建 Supabase 免费项目
1. 打开 https://supabase.com/dashboard ，注册/登录，创建一个 Free 项目，例如 `tlp-transfer`。
2. 等待数据库完成初始化。
3. 打开 **SQL Editor**，粘贴并运行本目录 `database/setup.sql` 中的完整 SQL。
4. 打开 **Authentication → Users → Add user**，创建本人使用的邮箱账号（必要时确认邮箱或完成邀请设置）。建议禁用公开注册：**Authentication → Settings/General → Allow new users to sign up = Off**（菜单文字可能有变化）。不要在 GitHub 文件里保存密码。
5. 在 Supabase **SQL Editor** 中再次执行下面的授权 SQL，替换邮箱为刚才创建的用户邮箱：

```sql
insert into public.tlp_transfer_members(user_id, display_name, active)
select id, 'Owner', true
from auth.users
where email = 'YOUR_ACCOUNT_EMAIL@example.com'
on conflict (user_id) do update set active = true;
```

如果查不到用户，先到 Authentication → Users 确认账号已建立。需要授权第二个人时，也同样先创建账号，再添加成员记录。

## 2. 把网页连接到 Supabase
1. 在 Supabase 项目中点击 **Connect**，找到 **Project URL**（例如 `https://xxxx.supabase.co`）及 **Publishable key**（一般以 `sb_publishable_` 开头）。
2. 编辑 GitHub 仓库文件 `ChatGPT/TLP/TLP Transfer/online-config.js`，分别填入两个字符串：
```js
window.TLP_TRANSFER_CONFIG = {
  supabaseUrl: "https://YOUR-PROJECT.supabase.co",
  supabasePublishableKey: "sb_publishable_YOUR_PUBLIC_KEY"
};
```
3. 保存提交到 `main`，等 GitHub Pages 部署完成后刷新中转站。此时出现邮箱密码登录框。
4. 在允许的环境中输入 Supabase 账号密码登录。网页通过 Supabase Auth 验证身份，并经数据库行级安全策略判定账号是否被批准。

**只有 URL 和 publishable key 可以写入公开网页。这两个值不是登录密码。不要填写 secret key、service_role、数据库密码或账户密码。**

## 3. 使用在线回复
- 在消息详情选择 **在线回复**，登录后输入非敏感文字（最多 1,000 字）。
- 点击 **确认发布**，等待接口返回成功。失败时不能视为已提交，输入框保留原内容。
- 另一个已经登录的设备刷新网页，或点击「刷新在线回复」，即可看到最新记录。页面活跃期间也会定期轮询。
- 记录存于 Supabase 数据库，不依赖 GitHub Pages 再次部署或 GitHub `threads.json` 更新。
- 已有 `threads.json` 仍作为旧版 GitHub 回复归档显示。
- GPT 不会自动收到在线回复。若要 GPT 读取并操作，需要在聊天里要求“读取中转站在线回复”，并授权连接相应 Supabase 账号；或复制非敏感回复提交给 GPT。网页直接保存不等于 GPT 已完成后续任务。

## 4. 安全与运维
- GitHub Pages 整站和历史消息/附件目前仍是公开的，**在线回复仅对已授权 Supabase 成员可读**；不要往公开网页发布公司内部材料。
- Supabase 免费项目会因低活跃而暂停，也有限额。暂停时在线回复会暂时不可用，GitHub 静态页面仍可阅读。
- 如果沙箱无法向 `*.supabase.co` 发 HTTPS 请求，登录或同步将失败；联系公司网络管理员确认允许的地址，**不要绕过防护**。
- 绝不要在沙箱输入敏感内容；仅限公司明确允许外发的非敏感文字。
- 账号被盗用时在 Supabase Auth 管理界面处理账号，并将 `tlp_transfer_members.active` 设为 false；此后该账号的在线回复访问将被 RLS 拦截。
- 云端回复是追加写入，没有网页修改或删除权限；需要纠正时另发一条回复说明。
