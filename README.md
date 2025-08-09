<a name="top"></a>
[![PennyWise AI Banner](banner.png)](https://github.com/sarim2000/pennywiseai-tracker)
[![GitHub stars](https://img.shields.io/github/stars/sarim2000/pennywiseai-tracker?style=social)](https://github.com/sarim2000/pennywiseai-tracker)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-12+-3DDC84)](https://developer.android.com/about/versions/12)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF)](https://kotlinlang.org/)
[![Privacy](https://img.shields.io/badge/AI-100%25_On--Device-FF6B6B)](https://developers.google.com/mediapipe)
[![Beta Testers](https://img.shields.io/badge/Beta_Testers-50+-orange)](https://forms.gle/7FTkx7Z6BAwqzWJE9)
[![GitHub release](https://img.shields.io/github/v/release/sarim2000/pennywiseai-tracker)](https://github.com/sarim2000/pennywiseai-tracker/releases)
[![GitHub last commit](https://img.shields.io/github/last-commit/sarim2000/pennywiseai-tracker)](https://github.com/sarim2000/pennywiseai-tracker/commits)
[![Discord](https://img.shields.io/badge/Discord-Join_Community-5865F2)](https://discord.gg/eqbPUYSR)

## PennyWise AI — Free & Open‑Source, private SMS‑powered expense tracker for India

Turn bank SMS into a clean, searchable money timeline. 100% on‑device AI.


⭐ **Star us on GitHub — help us reach 100 stars!**

[![Share](https://img.shields.io/badge/share-000000?logo=x&logoColor=white)](https://x.com/intent/tweet?text=Check%20out%20PennyWise%20AI%20-%20Privacy-first%20expense%20tracker%20with%20on-device%20AI:%20https://github.com/sarim2000/pennywiseai-tracker%20%23Android%20%23PrivacyFirst%20%23OnDeviceAI)
[![Share](https://img.shields.io/badge/share-0A66C2?logo=linkedin&logoColor=white)](https://www.linkedin.com/sharing/share-offsite/?url=https://github.com/sarim2000/pennywiseai-tracker)
[![Share](https://img.shields.io/badge/share-FF4500?logo=reddit&logoColor=white)](https://www.reddit.com/submit?title=PennyWise%20AI%20-%20Privacy-first%20expense%20tracker&url=https://github.com/sarim2000/pennywiseai-tracker)
[![Share](https://img.shields.io/badge/share-0088CC?logo=telegram&logoColor=white)](https://t.me/share/url?url=https://github.com/sarim2000/pennywiseai-tracker&text=Check%20out%20PennyWise%20AI)

## Overview

For Android users in India who want automatic expense tracking from bank SMS — clean categories, subscription detection, and clear insights.

> ### 🚨 **BETA TESTING OPEN** 🚨
> 
> <a href="https://forms.gle/7FTkx7Z6BAwqzWJE9">
>   <img src="https://img.shields.io/badge/JOIN_THE_BETA-Get_Early_Access-FF0000?style=for-the-badge&labelColor=FF0000&color=FF4444" alt="Join Beta" />
> </a>
> 
> **Get early access** • Test new features first • Shape the future of PennyWise

### How it works

1. Grant SMS permission (read‑only). No inbox changes, no messages sent.
2. PennyWise parses transaction SMS, extracts amount, merchant, category, and date.
3. View analytics, subscriptions, and the full transaction timeline — and chat with the on‑device AI.

## Why PennyWise

- **🤖 Smart SMS Parsing** - Automatically extracts transaction details from Indian bank SMS
- **📊 Clear Insights** - Analytics and charts to instantly see where money goes
- **🔄 Subscription Tracking** - Detects and monitors recurring payments
- **💬 On‑device AI Chat** - Ask questions like “What did I spend on food last month?”
- **🏷️ Auto‑Categorization** - Clean merchant names and sensible categories
- **📤 Data Export** - Export as CSV or PDF for taxes or records

## Supported Banks

Currently supporting major Indian banks:

- **HDFC Bank**
- **State Bank of India (SBI)**
- **ICICI Bank**
- **Axis Bank**
- **Punjab National Bank (PNB)**
- **IDBI Bank**
- **Indian Bank**
- **Federal Bank**
- **Karnataka Bank**
- **Jupiter (CSB Bank)**
- **Amazon Pay (Juspay)**

More banks being added regularly! [Request your bank →](https://github.com/sarim2000/pennywiseai-tracker/issues/new?template=bank_support_request.md)

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

## Tech Stack

<p align="center">
  <img src="https://skillicons.dev/icons?i=kotlin,jetpackcompose,androidstudio,materialui" /><br>
  <img src="https://skillicons.dev/icons?i=hilt,room,coroutines" />
</p>

**Architecture**: MVVM • Jetpack Compose • Room • Coroutines • Hilt • MediaPipe AI • Material Design 3

## Community & Support

- **Discord**: Join the community, share feedback, and get help — [Join Discord](https://discord.gg/eqbPUYSR)
- **Issues**: Report bugs or request features — [Open an issue](https://github.com/sarim2000/pennywiseai-tracker/issues)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

```bash
./gradlew test          # Run tests
./gradlew ktlintCheck   # Check style
```

## Security

Please review our [Security Policy](SECURITY.md) for how to report vulnerabilities.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Lucifer1590"><img src="https://avatars.githubusercontent.com/Lucifer1590?v=4&s=100" width="100px;" alt="Lucifer1590"/><br /><sub><b>Lucifer1590</b></sub></a><br /><a href="#community-Lucifer1590" title="Community Management">👥</a> <a href="https://github.com/sarim2000/pennywiseai-tracker/issues?q=author%3ALucifer1590" title="Bug reports">🐛</a> <a href="#userTesting-Lucifer1590" title="User Testing">📓</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!

## License

MIT License - see [LICENSE](LICENSE)

---

<p align="center">
<a href="https://github.com/sarim2000/pennywiseai-tracker/releases">Download</a> •
<a href="https://github.com/sarim2000/pennywiseai-tracker/issues">Report Bug</a> •
<a href="https://github.com/sarim2000/pennywiseai-tracker/issues">Request Feature</a>
</p>
