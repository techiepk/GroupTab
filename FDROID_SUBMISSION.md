# F-Droid Submission Guide for PennyWise AI

## Setup Complete ✅

The following has been configured for F-Droid:

1. **Build Flavors**: 
   - `fdroid` - For F-Droid builds (unsigned)
   - `standard` - For Play Store builds (signed)

2. **Metadata**: Located in `fastlane/metadata/android/en-US/`
   - App description, screenshots, icon, and changelogs

3. **Build Commands**:
   ```bash
   # F-Droid build
   ./gradlew assembleFdroidRelease
   
   # Play Store build  
   ./gradlew assembleStandardRelease
   ```

## Steps to Submit to F-Droid

### 1. Fork F-Droid Data Repository
Go to https://gitlab.com/fdroid/fdroiddata and fork the repository.

### 2. Clone Your Fork
```bash
git clone https://gitlab.com/YOUR_USERNAME/fdroiddata.git
cd fdroiddata
```

### 3. Create App Metadata File
Create file `metadata/com.pennywiseai.tracker.yml` with this content:

```yaml
Categories:
  - Money
License: MIT
AuthorName: Sarim
SourceCode: https://github.com/sarim2000/pennywiseai-tracker
IssueTracker: https://github.com/sarim2000/pennywiseai-tracker/issues
Changelog: https://github.com/sarim2000/pennywiseai-tracker/releases

AutoName: PennyWise AI

RepoType: git
Repo: https://github.com/sarim2000/pennywiseai-tracker.git

Builds:
  - versionName: '2.2.0'
    versionCode: 21
    commit: v2.2.0
    subdir: app
    gradle:
      - fdroid

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: '2.2.0'
CurrentVersionCode: 21
```

### 4. Test Build Locally (Optional)
If you have fdroidserver installed:
```bash
fdroid lint com.pennywiseai.tracker
fdroid build -v -l com.pennywiseai.tracker
```

### 5. Create Merge Request
1. Commit your changes:
   ```bash
   git add metadata/com.pennywiseai.tracker.yml
   git commit -m "New app: PennyWise AI"
   git push origin master
   ```

2. Go to https://gitlab.com/YOUR_USERNAME/fdroiddata
3. Click "Create merge request"
4. Title: "New app: PennyWise AI - Privacy-first expense tracker"
5. Description:
   ```
   PennyWise AI is a privacy-first expense tracker that automatically extracts 
   transaction data from bank SMS using 100% on-device AI processing.
   
   - No internet required for core functionality
   - All processing happens on-device using MediaPipe
   - Supports major Indian banks
   - Open source (MIT license)
   
   This app fills a need for privacy-conscious users who want automatic expense 
   tracking without sharing their financial data with cloud services.
   ```

### 6. Required Before Submission

- [ ] Commit and push the build.gradle.kts changes (F-Droid flavor support)
- [ ] Create a Git tag for version 2.2.0:
  ```bash
  git add .
  git commit -m "feat: add F-Droid support with dedicated build flavor"
  git push origin main
  git tag v2.2.0
  git push origin v2.2.0
  ```

- [ ] Ensure the tagged commit builds successfully
- [ ] Remove any proprietary dependencies (Google Play Services are optional in your app ✅)

## Notes

- F-Droid will build from source, so ensure your repo is public
- The build process takes 1-2 weeks for review
- Be responsive to maintainer feedback
- Join F-Droid chat for help: https://matrix.to/#/#fdroid:f-droid.org

## Build Differences

| Feature | F-Droid | Play Store |
|---------|---------|------------|
| Signing | F-Droid signs | You sign |
| Updates | F-Droid repo | Play Store |
| Analytics | None | Optional |
| Crash reporting | None | Optional |

## Important: Build Fix Applied

The build.gradle.kts has been updated to make the signing config optional, which prevents F-Droid builds from failing when local.properties doesn't exist. This allows F-Droid to build your app in their environment while keeping your Play Store signing intact.