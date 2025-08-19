#!/bin/bash

# Local release script for PennyWise
# This replaces the GitHub Actions workflow for local releases
# Usage: ./scripts/local-release.sh <version> [--skip-build]

set -e  # Exit on error

VERSION=$1
SKIP_BUILD=$2

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

if [ -z "$VERSION" ]; then
    echo -e "${RED}Error: Version not provided${NC}"
    echo "Usage: $0 <version> [--skip-build]"
    echo "Example: $0 2.16.0"
    exit 1
fi

echo -e "${GREEN}🚀 Starting local release for version $VERSION${NC}"

# 1. Update version in build.gradle.kts
echo -e "${YELLOW}📝 Updating version...${NC}"
./scripts/update-version.sh "$VERSION"

# 2. Update changelog
echo -e "${YELLOW}📋 Updating changelog...${NC}"
CHANGELOG_FILE="fastlane/metadata/android/en-US/changelogs/default.txt"
if [ -f "$CHANGELOG_FILE" ]; then
    echo "Version $VERSION" > "$CHANGELOG_FILE.tmp"
    echo "Released on $(date +%Y-%m-%d)" >> "$CHANGELOG_FILE.tmp"
    echo "" >> "$CHANGELOG_FILE.tmp"
    cat "$CHANGELOG_FILE" >> "$CHANGELOG_FILE.tmp"
    mv "$CHANGELOG_FILE.tmp" "$CHANGELOG_FILE"
    echo -e "${GREEN}✅ Changelog updated${NC}"
else
    echo -e "${YELLOW}⚠️  No changelog file found, skipping...${NC}"
fi

# 3. Build APKs (unless skipped)
if [ "$SKIP_BUILD" != "--skip-build" ]; then
    echo -e "${YELLOW}🔨 Building release APKs...${NC}"
    
    # Clean previous builds
    ./gradlew clean
    
    # Build standard release APK
    echo -e "${YELLOW}📦 Building standard release...${NC}"
    ./gradlew assembleStandardRelease
    
    # Build F-Droid release APK
    echo -e "${YELLOW}📦 Building F-Droid release...${NC}"
    ./gradlew assembleFdroidRelease
    
    echo -e "${GREEN}✅ APKs built successfully${NC}"
    
    # List generated APKs
    echo -e "${YELLOW}📁 Generated APKs:${NC}"
    find app/build/outputs/apk -name "*.apk" -type f | while read apk; do
        SIZE=$(du -h "$apk" | cut -f1)
        echo "  - $(basename "$apk") ($SIZE)"
    done
else
    echo -e "${YELLOW}⏭️  Skipping build (--skip-build flag)${NC}"
fi

# 4. Create git tag and commit
echo -e "${YELLOW}📌 Creating git commit and tag...${NC}"

# Stage changes
git add app/build.gradle.kts
git add fastlane/metadata/android/en-US/changelogs/default.txt 2>/dev/null || true

# Commit
git commit -m "chore(release): bump version to $VERSION [skip ci]"

# Create tag
git tag -a "v$VERSION" -m "Release version $VERSION"

echo -e "${GREEN}✅ Git commit and tag created${NC}"

# 5. Show summary
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}✨ Release $VERSION prepared successfully!${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo ""

# 6. Optional: Push to GitHub
echo -e "${YELLOW}Push to GitHub?${NC}"
read -p "Do you want to push commits and tags to GitHub? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}📤 Pushing to GitHub...${NC}"
    git push
    git push --tags
    echo -e "${GREEN}✅ Pushed to GitHub${NC}"
    
    # Show GitHub release URL
    REPO_URL=$(git remote get-url origin | sed 's/\.git$//' | sed 's/git@github.com:/https:\/\/github.com\//')
    echo -e "${GREEN}Create release at: $REPO_URL/releases/new?tag=v$VERSION${NC}"
else
    echo -e "${YELLOW}⏭️  Skipping push. To push later, run:${NC}"
    echo "  git push && git push --tags"
fi

echo ""

# 7. Optional: Build Play Store bundle
echo -e "${YELLOW}Build Play Store Bundle?${NC}"
read -p "Do you want to build AAB for Play Store? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}📦 Building Play Store bundle...${NC}"
    ./gradlew bundleStandardRelease
    echo -e "${GREEN}✅ AAB built successfully${NC}"
    echo -e "${GREEN}AAB location: app/build/outputs/bundle/standardRelease/${NC}"
    
    # Show AAB file details
    AAB_FILE=$(find app/build/outputs/bundle/standardRelease -name "*.aab" -type f | head -1)
    if [ -f "$AAB_FILE" ]; then
        SIZE=$(du -h "$AAB_FILE" | cut -f1)
        echo -e "${GREEN}  $(basename "$AAB_FILE") ($SIZE)${NC}"
    fi
else
    echo -e "${YELLOW}⏭️  Skipping AAB build. To build later, run:${NC}"
    echo "  ./gradlew bundleStandardRelease"
fi

echo ""

# 8. Final summary
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}📋 Release Summary${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "Version: ${GREEN}$VERSION${NC}"
echo -e "Git Tag: ${GREEN}v$VERSION${NC}"

if [ "$SKIP_BUILD" != "--skip-build" ]; then
    echo -e "${YELLOW}Generated APKs:${NC}"
    find app/build/outputs/apk -name "*.apk" -type f | while read apk; do
        SIZE=$(du -h "$apk" | cut -f1)
        echo "  - $(basename "$apk") ($SIZE)"
    done
fi

echo ""
echo -e "${YELLOW}📝 Release Checklist:${NC}"
echo "[ ] Push to GitHub (if not done)"
echo "[ ] Create GitHub release and upload APKs"
echo "[ ] Upload AAB to Play Store (if applicable)"
echo "[ ] Update F-Droid metadata (if needed)"
echo "[ ] Announce release on Discord/social media"