#!/bin/bash

# Quick release script for patch versions
# Automatically increments patch version
# Usage: ./scripts/quick-release.sh [--minor] [--major]

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Get current version from build.gradle.kts
CURRENT_VERSION=$(grep "versionName = " app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')

# Parse version components
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

# Determine version increment
if [ "$1" == "--major" ]; then
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    echo -e "${YELLOW}📈 Major version bump${NC}"
elif [ "$1" == "--minor" ]; then
    MINOR=$((MINOR + 1))
    PATCH=0
    echo -e "${YELLOW}📊 Minor version bump${NC}"
else
    PATCH=$((PATCH + 1))
    echo -e "${YELLOW}🔧 Patch version bump${NC}"
fi

NEW_VERSION="$MAJOR.$MINOR.$PATCH"

echo -e "${GREEN}Current version: $CURRENT_VERSION${NC}"
echo -e "${GREEN}New version: $NEW_VERSION${NC}"
echo ""

# Ask for confirmation
read -p "Proceed with release $NEW_VERSION? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Release cancelled"
    exit 1
fi

# Run the main release script
./scripts/local-release.sh "$NEW_VERSION" $2