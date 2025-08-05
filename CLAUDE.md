# PennyWise Project Context

## Project Overview
PennyWise is a minimalist, AI-powered expense tracker for Android that automatically extracts transaction data from SMS messages using on-device processing.

## Important Documents
Please reference these documents when working on this project:
- **Architecture**: `/docs/architecture.md` - MVVM + Clean Architecture patterns, layer responsibilities
- **Design System**: `/docs/design.md` - Material 3 theming, colors, typography, components
- **PRD**: `/prd.md` - Product requirements, features, timeline

## Key Technical Decisions
1. **UI Framework**: Jetpack Compose with Material 3
2. **Architecture**: MVVM with Clean Architecture (UI, Domain, Data layers)
3. **State Management**: Unidirectional Data Flow with StateFlow
4. **DI**: Hilt for dependency injection
5. **Database**: Room for local storage
6. **AI/ML**: MediaPipe LLM (Gemma 2B) for on-device processing
7. **Background**: WorkManager for SMS scanning

## Design Principles
- **Material You**: Dynamic color from wallpaper (Android 12+)
- **Light/Dark Theme**: Full support with semantic color roles
- **Spacing**: 8dp grid system
- **Typography**: Material 3 type scale
- **Navigation**: NavigationBar for phones, NavigationRail for tablets
- **Edge-to-Edge**: All screens use PennyWiseScaffold with default TopAppBar for consistent system bar handling
- **Consistent UI**: PennyWiseScaffold provides default TopAppBar with options for title, navigation, actions, and transparency

## Code Style Guidelines
- Follow Kotlin coding conventions
- Use meaningful variable names
- Implement proper error handling with sealed classes
- Ensure UI components are reusable and testable
- Always test on both light and dark themes

## Current Phase
Working on Phase 1: Core Foundation (Project setup, Material 3 theming, Room database, Navigation)

## Commands to Run
- Build: `./gradlew build`
- Test: `./gradlew test`
- Lint: `./gradlew lint`

## Versioning Strategy
We follow Semantic Versioning (SemVer) - MAJOR.MINOR.PATCH:
- **MAJOR**: Breaking changes, major UI overhauls, architecture changes
- **MINOR**: New features, significant improvements
- **PATCH**: Bug fixes, minor improvements, performance optimizations

Current version: 2.1.2 (versionCode: 12)

Recent version history:
- 2.1.2: Spotlight tutorial, SBI/Indian Bank support, auto-scan on launch
- 2.0.1: Previous release

When implementing any feature, please ensure it aligns with the architecture patterns and design system defined in the documentation.