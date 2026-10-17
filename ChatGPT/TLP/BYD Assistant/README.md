# BYD Assistant ｜ 小破车语音车控项目

位于 `ChatGPT/TLP/BYD Assistant/` 的统一项目归档。所有 ChatGPT 生成的车辆助手/超级小爱适配源码及需求记录统一存放在这里；其他 TLP 项目不变。

## 版本目录

| 版本 | 内容 | 状态 | 源码 / 发布 |
| --- | --- | --- | --- |
| [V1](./V1/) | 两个独立 APK 验证小爱按应用名称启动 | 已验证 | [测试版发布](https://github.com/TLP1998/OpenAI/releases/tag/byd-xiaoai-v1-test) |
| [V2.0](./V2.0/) | 主程序 + 两个语音入口；实车锁车与解锁基础功能 | 已发布，历史版 | [V2 发布](https://github.com/TLP1998/OpenAI/releases/tag/byd-xiaoai-v2) |
| [V2.2](./V2.2/) | 三个语音入口+主程序；上锁、解锁、上锁并发送 CLOSEAIR | 已通过小米15实车上锁与解锁验证 | [下载 V2.2](https://github.com/TLP1998/OpenAI/releases/tag/byd-xiaoai-v2-2) |
| [V3](./V3/) | **单 APK**：车控主界面与三个独立桌面/语音入口 | 待小米 HyperOS 实机验证 | [V3 发布页](https://github.com/TLP1998/OpenAI/releases/tag/byd-xiaoai-v3) |
| [V3.1](./V3.1/) | **单 APK、单图标，内置可修改的语音口令与系统语音识别** | 正在测试，小爱单句直达仍未实现 | [V3.1 APK](https://github.com/TLP1998/OpenAI/releases/tag/byd-xiaoai-v3-1) |

## 重要约定
- **单 APK 多桌面入口**是 Android 系统支持的技术模式，但超级小爱未必把它们索引为不同 App，V3 必须在小米15上实测。
- V2.2 是已验证可用的回退方案；**不要在 V3 验证成功之前卸载它**。
- 语音锁屏解锁不额外请求生物认证意味着周围人可能使用你的手机唤醒词触发车门解锁。仅在理解其风险后使用。
- **“锁车并熄火”**仅沿用上游代码中的 `CLOSEAIR` 远程命令（关闭空调），不保证发动机下电或真正熄火。
- 本项目使用非官方逆向 BYD 车控协议；发布的构建是实验用途。任何账号、密码、操作 PIN 不得入库。
- GitHub Actions 工作流 **必须位于仓库根目录 `.github/workflows/` 才能运行**；已废弃的工作流归档到各版目录，V3 的活动工作流保留必要的根目录入口。
- APK 二进制放在 GitHub Releases，源码、构建脚本、记录按版本在本目录管理；历史 Release 标签不删除。

## 项目结构
```text
ChatGPT/TLP/BYD Assistant/
├── README.md
├── index.html
├── V1/
│   ├── README.md
│   ├── source/
│   └── build-workflow.archived.yml
├── V2.0/
│   └── README.md
├── V2.2/
│   ├── README.md
│   ├── source/
│   └── build-workflow.archived.yml
├── V3/
│   ├── README.md
│   ├── patch_upstream.py
│   └── overlay/
└── V3.1/
    ├── README.md
    ├── patch_upstream.py
    └── overlay/
```

## V3.1 的小爱联动边界
修改 V3.1 应用内口令只影响 App 自己的匹配和执行，不能自动注册为小米超级小爱的自定义技能。V3.1 的实际流程是先说「打开小笛车控助手APP」，再说已保存的口令。系统级一句话直达仍待专门适配。
