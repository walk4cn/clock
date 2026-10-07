# 悬浮时钟 FloatClock

Android 悬浮窗时钟，**时间精确到 0.01 秒**，可悬浮在任意应用上层显示。

## 功能

- 悬浮窗时钟，显示 `HH:mm:ss.S`，小数位与时分秒同大小，走时精确到 0.1 秒
- 点「启动悬浮时钟」后设置页自动收起，回到桌面即见悬浮钟
- 两种刷新模式：默认按屏幕帧率逐帧刷新（顺滑省电），可切换为定时刷新
- 拖动时钟即可移动位置，位置自动记忆
- 单击时钟展开按钮：打开设置 / 关闭悬浮窗
- 可自定义：文字大小（12–72sp）、背景不透明度、文字颜色（白/黄/绿/蓝/红）、24/12 小时制、是否显示 0.1 秒
- 前台服务保活，通知栏可随时回到设置页
- 等宽粗体字体，数字跳动不抖动；带阴影保证任意背景下可读
- 无广告、不联网、零第三方依赖（仅 Kotlin 标准库）

## 构建

环境要求：JDK 17+，Android SDK（compileSdk 36）。

```bash
# Windows（已配好 JAVA_HOME 与 local.properties 时）
gradlew.bat assembleDebug
# macOS / Linux
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

也可以直接用 Android Studio 打开本项目点 Run。

> 注：`gradle/wrapper/gradle-wrapper.properties` 的 distributionUrl 指向腾讯云镜像（国内网络更稳）；
> `settings.gradle.kts` 中已配置阿里云 Maven 镜像。

## 安装使用

1. 安装 APK，打开「悬浮时钟」
2. 点「启动悬浮时钟」→ 系统跳转到「显示在其他应用上层」授权页 → 允许后返回，悬浮窗自动出现
3. 拖动到想要的位置；在任何其他 App 中都可以继续显示和调整
4. 单击时钟可展开「设置 / 关闭」按钮

## 常见问题

- **悬浮窗一闪就没 / 被杀**：在系统设置里给本 App 关闭电池优化，或允许后台自启动（国产 ROM 常见）。
- **时钟不走**：检查前台服务通知是否在；部分省电策略会冻结后台。

## GitHub 同类项目调研

动手前先在 GitHub 搜了同类项目：

| 项目 | 说明 | 结论 |
| --- | --- | --- |
| [BayuBatam2008/floating-clock-timing](https://github.com/BayuBatam2008/floating-clock-timing) | Kotlin + Compose，NTP 对时悬浮钟（GPL-3.0） | 功能最接近，但偏重量级，无 0.1s 精度 |
| [Monosz/HoverClock](https://github.com/Monosz/HoverClock) | 悬浮时钟/秒表/计时器（MIT） | 作者自述为 AI 生成的 MVP，未覆盖 0.1s |
| [XiTu893/clocktool](https://github.com/XiTu893/clocktool) | Java 极简悬浮时钟，整秒显示 | 无 license，精度只到秒 |

均未覆盖「悬浮 + 0.1 秒精度」这一需求，因此本项目自行实现。
