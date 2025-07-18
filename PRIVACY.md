# Privacy Policy for PennyWise AI

**Last Updated: January 2025**

## Overview

PennyWise AI is committed to protecting your privacy. This privacy policy explains how our app handles your data, particularly SMS messages containing financial transactions.

## Data Collection and Processing

### What We Access
- **SMS Messages**: We read SMS messages to identify and extract transaction information from banks and financial institutions
- **Device Storage**: We store extracted transaction data locally on your device

### What We DON'T Do
- ❌ **No Cloud Upload**: Your SMS messages and transaction data are NEVER uploaded to any server
- ❌ **No Data Sharing**: We do not share, sell, or transmit your data to third parties
- ❌ **No Personal Information Collection**: We don't collect names, phone numbers, or other personal identifiers
- ❌ **No Analytics on Transaction Data**: Your financial data is not used for analytics or marketing

## On-Device Processing

All data processing happens locally on your device:
- SMS parsing uses MediaPipe's on-device LLM
- Transaction categorization runs locally
- All analytics are computed on your device
- Data never leaves your phone

## Data Storage

- Transaction data is stored in a local SQLite database on your device
- Data is only accessible to the PennyWise AI app
- You can export or delete your data at any time
- Uninstalling the app removes all stored data

## Permissions

### SMS Permission (READ_SMS, RECEIVE_SMS)
- **Purpose**: To read transaction SMS from banks
- **Usage**: Only processes messages from identified financial institutions
- **Control**: You can revoke this permission at any time in Android settings

### Other Permissions
- **INTERNET**: Only for downloading the AI model on first launch and Firebase crash reporting
- **POST_NOTIFICATIONS**: To notify you of new transactions (optional)
- **WRITE_EXTERNAL_STORAGE**: To export transaction data (only when you request)

## Third-Party Services

### Firebase
- **Crashlytics**: Collects anonymous crash reports to improve app stability
- **Performance Monitoring**: Tracks app performance metrics
- **What's NOT sent**: Transaction data, SMS content, or personal information

## Data Security

- All data is stored locally in Android's secure app-specific storage
- No encryption keys or passwords leave your device
- No network transmission of financial data
- App follows Android security best practices

## User Rights

You have complete control over your data:
- **Access**: View all your transaction data in the app
- **Export**: Download your data as CSV/PDF anytime
- **Delete**: Remove individual transactions or all data
- **Portability**: Export and import your data

## Children's Privacy

PennyWise AI is not intended for users under 18 years of age. We do not knowingly collect data from children.

## Changes to Privacy Policy

We will notify you of any material changes to this privacy policy through the app. Continued use of the app after changes indicates acceptance.

## Open Source

PennyWise AI is open source. You can review our code at: https://github.com/sarim2000/pennywiseai-tracker

## Contact

For privacy concerns or questions:
- Create an issue: https://github.com/sarim2000/pennywiseai-tracker/issues
- Email: [sarimahmed3520@gmail.com]

## Compliance

This app is designed to comply with:
- Android's SMS and Call Log Permission Policy
- Google Play Store policies
- Indian data protection regulations

---

**Your privacy is our priority. Your financial data stays on your device, always.**
