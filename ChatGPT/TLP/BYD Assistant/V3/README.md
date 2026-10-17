# V3 · 小破车智能车控（单 APK）

**目标：将 V2.2 实测可用的 4 个 APK（1 个主程序 + 3 个入口）合并成一个 Android APK。**

## 安装与测试
1. [下载 V3 单 APK](https://github.com/TLP1998/OpenAI/releases/tag/byd-xiaoai-v3)：`xiaopoche-v3-unified.apk`。
2. V3 使用新的 `applicationId`（`dev.vehiclebridge.byd.xiaoai.v3`），**不会覆盖 V2.2**。请保留 V2.2 以便回退。
3. 安装 V3，打开「小破车智能车控」主程序，完成登录、车辆选择及操作 PIN 保存（不同包不能共享原版 Keystore 数据）。
4. 桌面应出现四个独立启动入口：主程序以及「小破车上锁」「小破车解锁」「小破车上锁并熄火」。
5. 在小米15对小爱分别说：
   - 「小爱同学，打开小破车上锁APP」
   - 「小爱同学，打开小破车解锁APP」
   - 「小爱同学，打开小破车上锁并熄火APP」

**关键未验证点：** Android 能在单 APK 内注册多个 `MAIN/LAUNCHER` Activities；但小米 HyperOS 可能将它们归为同一个包、只提供一个语音可搜索条目。若小爱无法精确区分，先保留/回退到 [V2.2](https://github.com/TLP1998/OpenAI/releases/tag/byd-xiaoai-v2-2)，不代表车控协议失效。

## 功能与安全
- 复用 [fm1223/byd-android](https://github.com/fm1223/byd-android) 逆向 BYD 中国区协议；非官方授权。
- 保留「仅上锁」「解锁」「上锁 + CLOSEAIR（关闭空调）」三种真实车控动作。
- 不使用巴法云，没有45秒固定冷却；同一时刻其他指令正在执行时不并发重复发送，也不自动重试。
- 组合操作只在锁车**确认成功**后才继续发送 CLOSEAIR，不能把它理解成已确认发动机熄火或整车下电。
- 保留用户要求的不额外身份认证及 Android 锁屏显示尝试。**任何能唤醒小爱的附近人员都可能触发车门解锁，风险较高。** HyperOS 是否允许在锁屏时启动和执行需由实机验证。
- 三个语音启动 Activity 为 Android `exported`，不具备声纹鉴权，**其他本机 App 也可能唤起这些 Activity**。V3 单 APK 不会自动改善这一点。
- 不要上传账号、密码、PIN 或车辆 VIN；V3 只在手机自身 Keystore 保存必要信息。

## 源码组织
- `patch_upstream.py`: 用固定上游提交复制并改造主程序，实现单 APK。
- `overlay/VoiceCommandActivity.kt`: 3 个具体语音 Activity + 统一车控实现。
- 活跃编译流程放在仓库根目录 `.github/workflows/byd-assistant-v3.yml`（GitHub Actions 限制）；输出到 GitHub Releases，**不会把 APK 二进制混进源码目录**。
- 旧版本均归档在上层 `../` 下，V2.2 APK 已验证可用。

## 升级注意
首次试验构建使用临时 Android Debug 证书；重复构建可能无法直接覆盖 V3 旧包。正式稳定版需持久签名和迁移策略。
