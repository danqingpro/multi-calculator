# 多功能计算器 (Multi-Calculator)

一款 Android 原生计算器应用，对标 Windows 10 计算器并进行了功能扩展。使用 Kotlin + Jetpack Compose 开发，支持深色主题和中英双语。

## 功能概览

| 模块 | 说明 |
|------|------|
| 标准计算器 | 四则运算、百分比、正负切换、退格、记忆功能 |
| 科学计算器 | 三角函数、对数、指数、幂运算、阶乘、排列组合、常数 π/e |
| 绘图模式 | 函数绘图、捏合缩放、拖拽平移、X/Y 轴刻度标签、函数分析 |
| 程序员模式 | 十六进制/十进制/八进制/二进制互换、位运算 |
| 单位转换器 | 长度、面积、体积、质量、温度、时间、速度、数据、角度、压强、能量、功率、**货币（实时汇率）** |
| 日期计算 | 日期差值计算、日期加减天数 |
| 意见反馈 | 支持发送图片/文件/消息，通过 QQ 邮箱 SMTP 直接发送 |
| 分享功能 | 在「关于」页面可分享 APK 安装包给好友 |

### 货币汇率

货币转换使用 [ExchangeRate-API](https://www.exchangerate-api.com/) 免费开放接口（无需 API Key），支持 160+ 货币，每日更新。进入货币转换界面后自动获取实时汇率，也可手动刷新。离线时使用内置的备用汇率数据。

## 环境要求

| 项目 | 版本 |
|------|------|
| JDK | 17 (推荐 OpenJDK 17) |
| Android SDK | compileSdk 34, minSdk 26 (Android 8.0+) |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.5.2 |
| Jetpack Compose | BOM 2024.09.02 |
| Gradle | 8.x（项目自带 Gradle Wrapper） |

### 工具安装

#### 1. 安装 JDK 17

```bash
# Ubuntu / Debian
sudo apt install openjdk-17-jdk

# macOS (Homebrew)
brew install openjdk@17

# Windows: 下载并安装 Adoptium / Oracle JDK 17
```

确认安装：
```bash
java -version
# 输出应包含 "17" 字样
```

#### 2. 安装 Android Studio 或仅 Android SDK

**方式 A：安装 Android Studio（推荐，自动配置 SDK）**

从 https://developer.android.com/studio 下载并安装。
打开 Android Studio → Settings → Languages & Frameworks → Android SDK：
- 勾选 Android 14 (API 34) 安装
- 勾选 Android SDK Build-Tools 34
- 勾选 Android SDK Platform-Tools

**方式 B：仅安装命令行工具**

```bash
# 下载 commandlinetools
# https://developer.android.com/studio#command-line-tools-only
export ANDROID_HOME=/path/to/android-sdk
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

## 编译指南

### 方式一：Android Studio（推荐）

1. 打开 Android Studio
2. File → Open → 选择项目根目录 `calculator-android/`
3. 等待 Gradle Sync 完成（首次会下载依赖，需联网）
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. 生成的 APK 在 `app/build/outputs/apk/release/app-release.apk`

### 方式二：命令行编译

```bash
# 进入项目目录
cd calculator-android

# 给 Gradle Wrapper 执行权限 (Linux/macOS)
chmod +x gradlew

# Debug 版本
./gradlew assembleDebug

# Release 版本 (需配置签名)
./gradlew assembleRelease
```

### 签名配置

项目已内置一个 debug 签名用于 release 构建。如果你要生成自己的签名：

1. 生成 keystore：
```bash
keytool -genkey -v -keystore release.keystore -alias calc -keyalg RSA -keysize 2048 -validity 10000
```

2. 修改 `app/build.gradle.kts` 中的签名配置：
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("/path/to/your/release.keystore")
        storePassword = "your_store_password"
        keyAlias = "your_alias"
        keyPassword = "your_key_password"
    }
}
```

### 运行单元测试

```bash
./gradlew test
```

## 项目结构

```
calculator-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/microsoft/calculator/
│   │   │   │   ├── engine/          # 核心逻辑
│   │   │   │   │   ├── ExpressionEvaluator.kt   # 表达式求值
│   │   │   │   │   ├── MathEngine.kt            # 科学计算引擎
│   │   │   │   │   ├── NumberFormatter.kt       # 数字格式化
│   │   │   │   │   ├── ProgrammerAndDate.kt      # 程序员/日期逻辑
│   │   │   │   │   ├── UnitConverter.kt         # 单位换算(含货币)
│   │   │   │   │   ├── CurrencyApiService.kt    # 汇率API服务
│   │   │   │   │   ├── QqMailSender.kt          # QQ邮箱SMTP发送
│   │   │   │   │   └── QqOAuth.kt              # QQ授权登录
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/    # 通用组件
│   │   │   │   │   ├── i18n/          # 国际化
│   │   │   │   │   ├── screens/      # 各界面
│   │   │   │   │   ├── theme/         # 主题颜色
│   │   │   │   │   └── CalculatorApp.kt  # 主应用
│   │   │   │   ├── viewmodel/         # ViewModel
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/                   # 资源文件
│   │   │   └── AndroidManifest.xml
│   │   └── test/                      # 单元测试
│   └── build.gradle.kts              # 模块构建配置
├── build.gradle.kts                  # 根构建配置
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 技术栈

- **Kotlin** 2.0.21
- **Jetpack Compose** (Material 3)
- **MVVM 架构** (ViewModel + StateFlow)
- **Coroutines** 协程异步处理
- **Compose Navigation** 页面导航
- **javax.mail** SMTP 邮件发送

## 开源协议

本项目基于 Windows 计算器开源代码修改扩展。

## 贡献

欢迎提交 Issue 和 Pull Request。

## 汇率数据来源

[ExchangeRate-API](https://www.exchangerate-api.com/) - 免费开放接口，无需 API Key
