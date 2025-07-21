## 📱 Feature Ideas & Improvements

- [ ] better transaction page (date improvement)
- [ ] backup feature
- [ ] more detailed graphs.
- [ ] Pin feature unlimited
- [ ] try some multiple models

## 🚀 Open Source & Play Store Release Tasks

### Critical Security Issues
- [x] **URGENT**: Revoke exposed Hugging Face token (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)
- [x] Remove Authorization header from ModelDownloader.kt (model is public, no auth needed)
- [x] Model URL moved to public S3 bucket
- [X] Create fresh git repository to remove token from git history

### Package Name Change
- [x] Change package name from `com.pennywiseai.tracker` to `com.pennywiseai.tracker`
- [x] Update in AndroidManifest.xml, build.gradle, and all Kotlin files
- [X] Update Firebase project settings with new package name

### Documentation
- [X] Create comprehensive README.md with:
  - App description and features
  - Screenshots and demo
  - Installation instructions
  - Firebase setup guide
- [X] Add LICENSE file (Apache 2.0 or MIT)
- [X] Create CONTRIBUTING.md for contributors
- [X] Add privacy policy (required for SMS permission)

### Code Cleanup
- [x] Review and remove/clean 200+ debug logs (reduced from 430 to 163)
- [ ] Complete or document 12 TODO comments
- [x] Remove test crash functions
- [x] Remove debug-only features from production

### Play Store Preparation
- [X] Generate release signing keystore
- [X] Create app listing content
- [X] Prepare screenshots (min 2, max 8)
- [X] Write app description (short & full)
- [X] Create feature graphic (1024x500)
- [X] Set up Google Play Developer account ($25)

### Repository Setup
- [X] Verify .gitignore includes:
  - google-services.json
  - local.properties
  - *.keystore
  - release/
- [X] Add GitHub Actions for CI/CD
- [ ] Set up issue templates (set this up after sometime)



