# PennyWise AI - Modern Expense Tracker

## Project Overview
A minimalist, AI-powered expense tracker that automatically extracts transaction data from SMS messages. Built with modern Android development practices focusing on simplicity, performance, and user experience.

## Core Philosophy
- **Simple over Complex**: Every feature should be intuitive
- **Automatic over Manual**: Minimize user input through AI
- **Privacy First**: All processing happens on-device
- **Modern Stack**: Latest Android technologies only

## Target Users
- Young professionals who want effortless expense tracking
- Users overwhelmed by complex finance apps
- Privacy-conscious individuals who prefer on-device processing

## Core Features (MVP)

### 1. Automatic Transaction Detection
- Background SMS scanning with user permission
- On-device AI for transaction extraction
- Support for major Indian banks (HDFC, ICICI, SBI, Axis, etc.)
- No manual entry required

### 2. Smart Categorization
- AI-powered merchant detection
- Automatic expense categorization
- Learn from user corrections

### 3. Subscription Detection
- Identify recurring payments from E-Mandate SMS
- Track subscription costs
- Upcoming payment reminders

### 4. Simple Analytics
- Monthly spending summary
- Category-wise breakdown
- Spending trends (increase/decrease)
- Budget alerts (optional)

## Technical Architecture

### Frontend Stack
```kotlin
- UI: Jetpack Compose with Material 3
- Navigation: Navigation Compose
- State Management: ViewModel + StateFlow
- Dependency Injection: Hilt
- Async: Kotlin Coroutines + Flow
```

### Backend Stack
```kotlin
- Database: Room with Kotlin extensions
- AI/ML: MediaPipe LLM (Gemma 2B)
- Background Processing: WorkManager
- Preferences: DataStore
```

### Architecture Pattern
- **MVVM with Clean Architecture**
- **Single Activity**
- **Unidirectional Data Flow**
- **Repository Pattern**

## UI/UX Design

### Design System
- **Theme**: Material You with dynamic colors
- **Typography**: Single font family (Google Sans or Inter)
- **Spacing**: 8dp grid system
- **Elevation**: Subtle shadows, no excessive depth

### Color Scheme
```kotlin
- Primary: Dynamic from wallpaper (Material You)
- Surface: Adaptive based on theme
- Error: Material Red
- Success: Material Green
- Warning: Material Amber
```

### Screen Structure

#### 1. Home Screen
```
┌─────────────────────────┐
│ Month Summary Card      │
│ ₹25,420 spent          │
│ ↑ 12% from last month  │
├─────────────────────────┤
│ Quick Stats (Chips)     │
│ [Food] [Transport] ...  │
├─────────────────────────┤
│ Recent Transactions     │
│ • Swiggy      -₹245    │
│ • Uber        -₹180    │
│ • More...              │
└─────────────────────────┘
```

#### 2. Transactions Screen
```
┌─────────────────────────┐
│ Search Bar              │
├─────────────────────────┤
│ Filter Chips            │
│ [All] [Week] [Month]    │
├─────────────────────────┤
│ Transaction List        │
│ Group by Date          │
│ • Today                │
│   - Transaction 1      │
│   - Transaction 2      │
│ • Yesterday            │
│   - Transaction 3      │
└─────────────────────────┘
```

#### 3. Insights Screen (Optional)
```
┌─────────────────────────┐
│ AI Insights Card        │
│ "You spent 30% more    │
│  on food this month"   │
├─────────────────────────┤
│ Subscriptions          │
│ Total: ₹2,340/month    │
│ • Netflix    ₹199     │
│ • Spotify    ₹119     │
└─────────────────────────┘
```

## Data Models

### Core Entities

```kotlin
@Entity
data class Transaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val merchant: String,
    val category: Category,
    val date: Instant,
    val rawSms: String,
    val isSubscription: Boolean = false,
    val accountNumber: String? = null,
    val balance: Double? = null
)

@Entity
data class Subscription(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val merchant: String,
    val amount: Double,
    val frequency: Frequency,
    val nextPaymentDate: Instant,
    val isActive: Boolean = true
)

enum class Category {
    FOOD, TRANSPORT, SHOPPING, BILLS, ENTERTAINMENT, HEALTH, EDUCATION, OTHER
}

enum class Frequency {
    WEEKLY, MONTHLY, QUARTERLY, YEARLY
}
```

## Implementation Phases

### Phase 1: Core Foundation (Week 1)
- [ ] Project setup with Compose
- [ ] Material 3 theming
- [ ] Room database setup
- [ ] Basic navigation

### Phase 2: SMS Integration (Week 2)
- [ ] SMS permission handling
- [ ] Background SMS scanner
- [ ] Bank parser framework
- [ ] Transaction extraction

### Phase 3: UI Implementation (Week 3)
- [ ] Home screen with summary
- [ ] Transaction list with search
- [ ] Settings screen
- [ ] Pull-to-refresh

### Phase 4: AI Integration (Week 4)
- [ ] MediaPipe LLM setup
- [ ] Smart categorization
- [ ] Merchant extraction
- [ ] Subscription detection

### Phase 5: Polish (Week 5)
- [ ] Animations and transitions
- [ ] Dark theme refinement
- [ ] Performance optimization
- [ ] Error handling

## Key Libraries

```gradle
dependencies {
    // Compose
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    
    // Architecture
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    
    // AI/ML
    implementation("com.google.mediapipe:tasks-genai:0.10.14")
    
    // Background
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    
    // Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
```

## Development Guidelines

### Code Style
- **Kotlin Coding Conventions**
- **Compose Best Practices**
- **Meaningful variable names**
- **No unnecessary comments**

### Git Workflow
- **Feature branches**
- **Conventional commits**
- **PR reviews required**
- **CI/CD with GitHub Actions**

### Testing Strategy
- **Unit tests for ViewModels**
- **Compose UI tests**
- **Room migration tests**
- **SMS parser tests**

## Performance Guidelines
- **Lazy loading for lists**
- **Image caching**
- **Efficient database queries**
- **Background work constraints**

## Privacy & Security
- **No cloud sync by default**
- **Optional encrypted backup**
- **No analytics without consent**
- **Clear privacy policy**

## Success Metrics
- App launch time < 1 second
- SMS scanning < 5 seconds for 1000 messages
- Memory usage < 150MB
- Crash rate < 0.1%
- User retention > 60% after 30 days

## Future Enhancements (Post-MVP)
- Export to CSV/PDF
- Budget planning
- Bill reminders
- Spending goals
- Multi-currency support
- Cloud backup (optional)

## Team Responsibilities
- **Android Developer**: Core app development
- **UI/UX Designer**: Design system and flows
- **QA Engineer**: Testing and quality assurance
- **Product Manager**: Feature prioritization

## Timeline
- **Total Duration**: 5 weeks
- **MVP Release**: End of Week 5
- **Beta Testing**: Week 6-7
- **Production Release**: Week 8

---

**Note**: This document is a living guide. Update as the project evolves.
