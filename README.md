<div align="center">
  <img src="branding/app-store/ic_launcher_180.png" alt="PennyWise Logo" width="120" height="120">
  
  # PennyWise AI
  
  ### Transform your SMS transactions into financial insights with on-device AI
  
  ![PennyWise Dashboard](screenshots/dashboard.png)
  
  <p align="center">
    <a href="#-quick-start">🚀 Quick Start</a> •
    <a href="#-features">✨ Features</a> •
    <a href="#-screenshots">📸 Screenshots</a> •
    <a href="#-privacy">🔒 Privacy</a>
  </p>
  
  <p align="center">
    <img alt="License" src="https://img.shields.io/badge/license-Apache%202.0-blue">
    <img alt="Android" src="https://img.shields.io/badge/Android-14+-3DDC84?logo=android">
    <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?logo=kotlin">
    <img alt="AI" src="https://img.shields.io/badge/AI-On--Device-FF6B6B">
  </p>
</div>

---

## ✨ Core Features

### 🤖 **AI-Powered Transaction Parsing**
MediaPipe LLM analyzes SMS messages locally on your device to extract transaction details with high accuracy. No cloud processing required.

<details>
<summary>View AI parsing in action</summary>

![AI Parsing](screenshots/ai.png)
</details>

### 📊 **Smart Analytics & Insights**  
Get instant spending breakdowns, trend analysis, and personalized financial insights. Track where your money goes with beautiful visualizations.

<details>
<summary>View Analytics dashboard</summary>

![Analytics](screenshots/analytics.png)
</details>

### 🔄 **Automatic Subscription Detection**
Never miss a recurring payment. PennyWise automatically identifies and tracks your subscriptions from transaction patterns.

<details>
<summary>View Subscription tracking</summary>

![Subscriptions](screenshots/subscription.png)
</details>

### 🏷️ **Intelligent Transaction Grouping**
Transactions are automatically categorized by merchant and type. Create custom groups for better organization.

### 💬 **AI Financial Assistant**
Chat with your personal finance AI to get insights, ask questions about spending patterns, and receive budgeting advice.

---

## 🚀 Quick Start

```bash
# Clone the repository
git clone https://github.com/yourusername/pennywiseai-tracker.git
cd pennywiseai-tracker

# Build with Gradle
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Installation

### Download from Play Store

<div align="center">
<a href="https://play.google.com/store/apps/details?id=com.pennywiseai.tracker">
<img src="https://img.shields.io/badge/Download_on_Play_Store-414141?style=for-the-badge&logo=google-play&logoColor=white" alt="Get it on Google Play" />
</a>
<p><em>Available for Android 14 and above</em></p>
</div>

### Build from Source

#### Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK 35
- JDK 21

#### Firebase Setup
1. Create a project at [Firebase Console](https://console.firebase.google.com)
2. Add Android app with package: `com.pennywiseai.tracker`
3. Download `google-services.json` to `/app` directory
4. Enable Crashlytics and Performance Monitoring

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
<td align="center"><b>AI Assistant</b></td>
<td align="center"><b>Subscriptions</b></td>
</tr>
</table>
</div>

## 🎥 Demo Video

https://github.com/yourusername/pennywiseai-tracker/assets/youruserid/demo.mp4

---

## 🛠️ Tech Stack

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
<img src="https://skillicons.dev/icons?i=firebase" width="48" height="48" alt="Firebase" />
<br>Firebase
</td>
<td align="center" width="96">
<img src="https://skillicons.dev/icons?i=sqlite" width="48" height="48" alt="SQLite" />
<br>Room DB
</td>
<td align="center" width="96">
<img src="https://skillicons.dev/icons?i=materialui" width="48" height="48" alt="Material" />
<br>Material 3
</td>
</tr>
</table>

### Key Technologies

- **AI Engine**: MediaPipe Generative AI (On-device LLM)
- **Architecture**: MVVM with Repository Pattern
- **Database**: Room with Coroutines
- **UI**: Material Design 3 with View Binding
- **Background**: WorkManager for periodic scanning
- **Analytics**: MPAndroidChart for visualizations

---

## 🔒 Privacy First

<div align="center">
  <table>
    <tr>
      <td align="center">
        <h3>🏠 100% On-Device</h3>
        <p>All AI processing happens locally.<br/>Your data never leaves your phone.</p>
      </td>
      <td align="center">
        <h3>🔐 Zero Data Collection</h3>
        <p>No user data is collected or stored.<br/>Only anonymized crash reports.</p>
      </td>
      <td align="center">
        <h3>📖 Open Source</h3>
        <p>Fully transparent codebase.<br/>Audit the code yourself.</p>
      </td>
    </tr>
  </table>
</div>

---

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

```bash
# Run tests
./gradlew test

# Check code style
./gradlew ktlintCheck

# Format code
./gradlew ktlintFormat
```

### Development Setup
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

```
Copyright 2024 PennyWise AI

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

See [LICENSE](LICENSE) for full details.

---

<div align="center">
  <p>
    <a href="https://github.com/yourusername/pennywiseai-tracker/releases">📥 Releases</a> •
    <a href="https://github.com/yourusername/pennywiseai-tracker/issues">🐛 Report Bug</a> •
    <a href="https://github.com/yourusername/pennywiseai-tracker/issues">💡 Request Feature</a>
  </p>
  
  <p>Built with ❤️ for better financial awareness</p>
  
  <br/>
  
  <sub>Made in India 🇮🇳</sub>
</div>
