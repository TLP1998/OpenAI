# BYD × XiaoAI V1 — 两个独立 Android 语音入口
本项目仅验证小爱能否按名字启动两个独立的安卓应用。
**绝不连接比亚迪，不保存账户或 PIN，不联网，不执行锁车解锁。**

## 安装
从 [Releases](https://github.com/TLP1998/OpenAI/releases/tag/byd-xiaoai-v1-test) 下载：
- `app-lock-debug.apk`：比亚迪锁车测试
- `app-unlock-debug.apk`：比亚迪解锁测试

两份 APK 同时安装。先手动打开一次，再分别对小爱说：
- “小爱同学，打开比亚迪锁车测试”
- “小爱同学，打开比亚迪解锁测试”

核对屏幕显示的入口名称及启动记录，区分语音成功和误启动。
APK 为 GitHub Actions 自动编译的 Debug 测试包，不应用于真正的车辆控制。
