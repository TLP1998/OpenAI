# 小迪车控助手 V4 · 固定签名与后续更新

## 用户决定（2026-10-18）
本次接受安装新软件，以后必须固定签名覆盖更新，而不是反复卸载。

- V4 新版 `applicationId = com.tlp1998.xiaodi`，显示名为 **小迪车控助手**，与原版 `dev.vehiclebridge.byd` 并行安装。
- 首个版本 `versionName = 4.0.0`，`versionCode = 400`；后续更新 versionCode 必须严格递增（例如 V4.0.1=401），**始终沿用同一包名和证书**。
- 用户已上传原 APK，确认其 Debug 签名与此前 GitHub V4 Debug 构建不相同，因此无法直接更新原版。不能从旧 APK 恢复旧的私钥。
- 本次创建了独立的 V4 长期签名证书；**私钥和签名密码仅通过用户私下保存的备份包交付，不在 GitHub 仓库中保存**。
- 新版固定签名证书公开 SHA-256：`66:3E:8E:46:A5:B3:8D:BF:9D:4E:CB:7F:75:BC:B9:00:EF:EC:0C:69:43:52:5B:42:67:65:60:32:32:B2:38:FD`（公开证书指纹不是私钥）。

## 一次性配置 GitHub Secrets
到 [Actions 仓库私密变量设置](https://github.com/TLP1998/OpenAI/settings/secrets/actions) 添加两个 **Repository secret**：
1. `XIAODI_SIGNING_KEYSTORE_BASE64`：粘贴私密备份中 `xiaodi_v4_keystore_base64.txt` 的完整一行文本；
2. `XIAODI_SIGNING_PASSWORD`：粘贴 `xiaodi_v4_signing_password.txt` 的完整一行文本。

不要把两项设置为 GitHub Variables（明文可见），也不要通过 commit、Issue、Pull Request、Actions 日志或聊天截图分享秘密。

## 运行首次固定签名构建
到 [XiaoDi V4 - Fixed-Signature Install and Updates](https://github.com/TLP1998/OpenAI/actions/workflows/xiaodi-v4-signed.yml) 选择 `Run workflow` → `main` → `Run workflow`。

该工作流支持手动触发，也支持**仅当明确创建/修改 SIGNED_BUILD_TRIGGER.txt 时**自动触发一次；日常代码或文档提交不再重复构建。用户可完成两个仓库 Secrets 后告知助手，再由助手提交触发文件，无需用户自行点 Run workflow。
产物是 `XiaoDi-V4.0.0-Fixed-Signature`，其中预期含 `xiaodi-v4.0.0-signed.apk` 和 SHA256/证书指纹检查报告。下载后请先做静态核验，尚未实车测试，不应直接视为生产稳定版本。

## 实车测试安全
- 原版车辆助手暂不卸载、不清除数据。首次 V4 安装需要重新在新应用中配置账户与车辆 PIN；不得在 GitHub 提交私密凭据。
- 两个应用如果配置同一个巴法 Topic，可能发生重复处理或 MQTT 相互抢占。实车测试 V4 前，应在原版应用内**停用其巴法后台联动**。
- 自动解锁、蓝牙锁车、可能未锁提醒应默认关闭并分别验证；空调消息 `on#1#温度#0#0#0` 不得解锁车门。
- `CLOSEAIR` 代表关闭空调，不能称为真实整车动力系统熄火。
- 若未来遗失这份签名私钥或密码，则无法签署与旧 V4 安装版证书相同的新 APK；请妥善保管离线备份。

## 开发者版本更新规则
修改 `patch_signed_identity.py` 的 `versionCode` 与 `versionName`，同时在工作流中更新文件名与 versionCode 校验。保持 `applicationId`、`keyAlias` 和证书不变。验签不匹配时构建应失败，不得发布。
