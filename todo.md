## 🚀 Open Source & Play Store Release Tasks

### Critical Security Issues
- [x] **URGENT**: Revoke exposed Hugging Face token (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)
- [x] Remove Authorization header from ModelDownloader.kt (model is public, no auth needed)
- [x] Model URL moved to public S3 bucket
- [X] Create fresh git repository to remove token from git history

### Package Name Change
- [x] Change package name from `com.example.transactiontracker` to `com.pennywiseai.tracker`
- [x] Update in AndroidManifest.xml, build.gradle, and all Kotlin files
- [X] Update Firebase project settings with new package name

### Documentation
- [ ] Create comprehensive README.md with:
  - App description and features
  - Screenshots and demo
  - Installation instructions
  - Firebase setup guide
- [ ] Add LICENSE file (Apache 2.0 or MIT)
- [ ] Create CONTRIBUTING.md for contributors
- [ ] Add privacy policy (required for SMS permission)

### Code Cleanup
- [x] Review and remove/clean 200+ debug logs (reduced from 430 to 163)
- [ ] Complete or document 12 TODO comments
- [x] Remove test crash functions
- [x] Remove debug-only features from production

### Play Store Preparation
- [ ] Generate release signing keystore
- [ ] Create app listing content
- [ ] Prepare screenshots (min 2, max 8)
- [ ] Write app description (short & full)
- [ ] Create feature graphic (1024x500)
- [ ] Set up Google Play Developer account ($25)

### Repository Setup
- [ ] Verify .gitignore includes:
  - google-services.json
  - local.properties
  - *.keystore
  - release/
- [ ] Add GitHub Actions for CI/CD
- [ ] Set up issue templates

## 📱 Feature Ideas & Improvements

Update model with every update (as applicable)

Make it very similar to MS messages.

Sync feature with Gdrive, 1Drive etc.

Make it available for ios and android both

Graphs adding which days i spend the most.

What am I spending most on?

Upcoming reminders section.

Pin feature unlimited

Saved messages section.

look at fingym, axios

try some multiple models

