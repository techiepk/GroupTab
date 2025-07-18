<div align="center">
  <img src="branding/app-store/ic_launcher_180.png" alt="PennyWise Logo" width="80" height="80">
  
  # PennyWise AI
  
  #### Transform your SMS transactions into financial insights with on-device AI
  
  <p align="center">
    <img alt="License" src="https://img.shields.io/badge/license-Apache%202.0-blue">
    <img alt="Android" src="https://img.shields.io/badge/Android-14+-3DDC84?logo=android">
    <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?logo=kotlin">
    <img alt="AI" src="https://img.shields.io/badge/AI-100%25_On--Device-FF6B6B">
  </p>
</div>

---

## 🎬 See It In Action

![Demo Video](screenshots/demo.mp4)

---

## 🔒 Your Privacy, Our Priority

<div align="center">
  <table>
    <tr>
      <td align="center" width="33%">
        <h3>🏠 100% On-Device AI</h3>
        <p>MediaPipe LLM runs locally.<br/>Your data never leaves your phone.</p>
      </td>
      <td align="center" width="33%">
        <h3>🔐 Zero Cloud Processing</h3>
        <p>No servers. No uploads.<br/>Complete data sovereignty.</p>
      </td>
      <td align="center" width="33%">
        <h3>📖 Open Source</h3>
        <p>Fully transparent.<br/>Audit the code yourself.</p>
      </td>
    </tr>
  </table>
</div>

---

## 📸 Screenshots

<div align="center">
<table>
<tr>
<td><img src="screenshots/dashboard.png" width="200"/></td>
<td><img src="screenshots/analytics.png" width="200"/></td>
<td><img src="screenshots/ai.png" width="200"/></td>
<td><img src="screenshots/subscription.png" width="200"/></td>
</tr>
<tr>
<td align="center"><b>Dashboard</b></td>
<td align="center"><b>Analytics</b></td>
<td align="center"><b>AI Chat</b></td>
<td align="center"><b>Subscriptions</b></td>
</tr>
</table>
</div>

---

## ✨ Features That Matter

### 🤖 **Intelligent SMS Parsing**
Automatically reads and categorizes transaction SMS from any Indian bank. Powered by MediaPipe's on-device LLM for instant, private processing.

### 📊 **Visual Spending Insights**
Beautiful charts and analytics that help you understand your spending patterns. Track trends, identify outliers, and make informed decisions.

### 🔄 **Smart Subscription Tracking**
Never miss a recurring payment again. AI automatically detects and monitors your subscriptions from transaction patterns.

### 💬 **Personal Finance Assistant**
Chat with AI about your spending. Get insights, budgeting tips, and answers to your financial questions - all processed locally.

### 🏷️ **Auto-Categorization**
Transactions are intelligently grouped by merchant and type. Create custom categories for personalized organization.

### 📤 **Export Your Data**
Your data, your control. Export transactions as CSV or PDF anytime. Perfect for tax filing or personal records.

---

## 🚀 Quick Start

### Option 1: Download APK
Coming soon on Play Store!

### Option 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/pennywiseai-tracker.git
cd pennywiseai-tracker

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK 35
- JDK 21
- Android device with API 34+ (Android 14)

#### Firebase Setup (Optional)
<details>
<summary>Enable crash reporting and analytics</summary>

1. Create project at [Firebase Console](https://console.firebase.google.com)
2. Add Android app with package: `com.pennywiseai.tracker`
3. Download `google-services.json` to `/app` directory
4. Enable Crashlytics and Performance Monitoring
</details>

---

## 🛠️ Built With

<table align="center">
<tr>
<td align="center" width="96">
<img src="https://skillicons.dev/icons?i=kotlin" width="48" height="48" alt="Kotlin" />
<br>Kotlin
</td>
<td align="center" width="96">
<img src="https://skillicons.dev/icons?i=android" width="48" height="48" alt="Android" />
<br>Android
</td>
<td align="center" width="96">
<img src="https://raw.githubusercontent.com/google/material-design-icons/master/android/drawable-xxxhdpi/ic_palette_black_48dp.png" width="48" height="48" alt="Material" />
<br>Material 3
</td>
<td align="center" width="96">
<img src="https://www.gstatic.com/devrel-devsite/prod/v89af556f3a8aa0deb9b97c5b7a1259b9ad2b219bfcc2460506822d38ec992beb/ml/images/lockup.svg" height="48" alt="MediaPipe" />
<br>MediaPipe
</td>
<td align="center" width="96">
<img src="https://skillicons.dev/icons?i=firebase" width="48" height="48" alt="Firebase" />
<br>Firebase
</td>
</tr>
</table>

**Architecture**: MVVM • Room DB • Coroutines • WorkManager • View Binding

---

## 🤝 Contributing

We love contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

```bash
# Run tests
./gradlew test

# Check code style
./gradlew ktlintCheck
```

---

## 📄 License

Apache License 2.0 - see [LICENSE](LICENSE) for details.

---

<div align="center">
  <p>
    <a href="https://github.com/yourusername/pennywiseai-tracker/releases">📥 Download</a> •
    <a href="https://github.com/yourusername/pennywiseai-tracker/issues">🐛 Report Bug</a> •
    <a href="https://github.com/yourusername/pennywiseai-tracker/issues">💡 Request Feature</a>
  </p>
  
  <p><strong>Built with ❤️ for financial awareness</strong></p>
  
  <sub>Made in India 🇮🇳</sub>
</div>