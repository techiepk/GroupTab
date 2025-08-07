<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="PennyWise" width="80" height="80">
  
  # PennyWise AI
  
  Transform your SMS transactions into financial insights with on-device AI
  
  <p>
    <img alt="License" src="https://img.shields.io/badge/license-MIT-blue">
    <img alt="Android" src="https://img.shields.io/badge/Android-12+-3DDC84">
    <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF">
    <img alt="Privacy" src="https://img.shields.io/badge/AI-100%25_On--Device-FF6B6B">
  </p>
  
  <p>
    <a href="https://play.google.com/store/apps/details?id=com.pennywiseai.tracker">
      <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80">
    </a>
  </p>
</div>

## Overview

PennyWise automatically reads transaction SMS messages and transforms them into organized financial data using on-device AI. No manual entry, no cloud processing, complete privacy.

> ### 🚨 **BETA TESTING OPEN** 🚨
> 
> <a href="https://forms.gle/7FTkx7Z6BAwqzWJE9">
>   <img src="https://img.shields.io/badge/JOIN_BETA-Limited_Spots_Available-FF0000?style=for-the-badge&labelColor=FF0000&color=FF4444" alt="Join Beta" />
> </a>
> 
> **Get early access** • Test new features first • Shape the future of PennyWise

## Key Features

- **🤖 Smart SMS Parsing** - Automatically extracts transaction details from any Indian bank SMS
- **📊 Spending Analytics** - Visual insights into your spending patterns and trends  
- **🔄 Subscription Tracking** - Detects and monitors recurring payments automatically
- **💬 AI Assistant** - Chat about your finances with a locally-running AI
- **🏷️ Auto-Categorization** - Intelligent grouping by merchant and transaction type
- **📤 Data Export** - Export as CSV or PDF for taxes or personal records

## Privacy First

All processing happens on your device using MediaPipe's LLM. Your financial data never leaves your phone. No servers, no uploads, no tracking.

## Screenshots

<table>
<tr>
<td><img src="screenshots/home.png" width="160"/></td>
<td><img src="screenshots/analytics-v2.png" width="160"/></td>
<td><img src="screenshots/chat.png" width="160"/></td>
<td><img src="screenshots/subscription-v2.png" width="160"/></td>
<td><img src="screenshots/transactions.png" width="160"/></td>
</tr>
<tr>
<td align="center">Home</td>
<td align="center">Analytics</td>
<td align="center">AI Chat</td>
<td align="center">Subscriptions</td>
<td align="center">Transactions</td>
</tr>
</table>

## Quick Start

```bash
# Clone repository
git clone https://github.com/sarim2000/pennywiseai-tracker.git
cd pennywiseai-tracker

# Build APK
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Requirements

- Android 12+ (API 31)
- Android Studio Ladybug or newer
- JDK 11

### Optional: Firebase Setup

<details>
<summary>Enable crash reporting</summary>

1. Create project at [Firebase Console](https://console.firebase.google.com)
2. Add app with package: `com.pennywiseai.tracker`
3. Download `google-services.json` to `/app`
4. Enable Crashlytics

</details>

## Tech Stack

<p align="center">
<img src="https://skillicons.dev/icons?i=kotlin,firebase" height="32" />
</p>

**Architecture**: MVVM • Room • Coroutines • MediaPipe AI • Material Design 3

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

```bash
./gradlew test          # Run tests
./gradlew ktlintCheck   # Check style
```

## License

MIT License - see [LICENSE](LICENSE)

---

<p align="center">
<a href="https://github.com/sarim2000/pennywiseai-tracker/releases">Download</a> •
<a href="https://github.com/sarim2000/pennywiseai-tracker/issues">Report Bug</a> •
<a href="https://github.com/sarim2000/pennywiseai-tracker/issues">Request Feature</a>
</p>
