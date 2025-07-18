package com.pennywiseai.tracker.ui

/**
 * UI Constants for the Transaction Tracker app
 * Centralizes UI-related constants including emoji usage
 * 
 * To disable emojis throughout the app, set USE_EMOJIS to false
 */
object UiConstants {
    
    // Master switch for emoji usage
    const val USE_EMOJIS = false
    
    // Icon definitions - returns emoji or empty string based on USE_EMOJIS
    object Icons {
        val MONEY = if (USE_EMOJIS) "💰" else ""
        val CREDIT_CARD = if (USE_EMOJIS) "💳" else ""
        val CHART = if (USE_EMOJIS) "📊" else ""
        val CALENDAR = if (USE_EMOJIS) "📅" else ""
        val ROBOT = if (USE_EMOJIS) "🤖" else ""
        val SPARKLES = if (USE_EMOJIS) "✨" else ""
        val LOCK = if (USE_EMOJIS) "🔒" else ""
        val DOWNLOAD = if (USE_EMOJIS) "📥" else ""
        val UPLOAD = if (USE_EMOJIS) "📤" else ""
        val TRASH = if (USE_EMOJIS) "🗑️" else ""
        val WARNING = if (USE_EMOJIS) "⚠️" else ""
        val SUCCESS = if (USE_EMOJIS) "✅" else ""
        val ERROR = if (USE_EMOJIS) "❌" else ""
        val INFO = if (USE_EMOJIS) "ℹ️" else ""
        val PACKAGE = if (USE_EMOJIS) "📦" else ""
        val ROCKET = if (USE_EMOJIS) "🚀" else ""
        val SETTINGS = if (USE_EMOJIS) "⚙️" else ""
        val FOOD = if (USE_EMOJIS) "🍔" else ""
        val TRANSPORT = if (USE_EMOJIS) "🚗" else ""
        val SHOPPING = if (USE_EMOJIS) "🛍️" else ""
        val STORE = if (USE_EMOJIS) "🏪" else ""
        val TARGET = if (USE_EMOJIS) "🎯" else ""
        val TOP = if (USE_EMOJIS) "🔝" else ""
        val EXPENSIVE = if (USE_EMOJIS) "💸" else ""
        val CHEAP = if (USE_EMOJIS) "💰" else ""
        val REPEAT = if (USE_EMOJIS) "🔄" else ""
        val ANALYSIS = if (USE_EMOJIS) "📈" else ""
    }
    
    // Helper function to format text with optional emoji
    fun formatWithIcon(icon: String, text: String): String {
        return if (icon.isNotEmpty()) "$icon $text" else text
    }
    
    // Animation durations (milliseconds)
    object AnimationDurations {
        const val QUICK = 150L
        const val NORMAL = 300L
        const val SLOW = 500L
    }
    
    // Update intervals (milliseconds)
    object UpdateIntervals {
        const val REAL_TIME = 100L
        const val FREQUENT = 500L
        const val NORMAL = 1000L
        const val INFREQUENT = 3000L
    }
    
    // Material Design 3 8dp Grid System
    object Spacing {
        const val TINY = 4      // 0.5x base unit - Used for minimal adjustments
        const val SMALL = 8     // 1x base unit - Minimum touch target spacing
        const val MEDIUM = 16   // 2x base unit - Standard component spacing
        const val LARGE = 24    // 3x base unit - Section spacing
        const val XLARGE = 32   // 4x base unit - Major layout spacing
        const val XXLARGE = 40  // 5x base unit - Screen-level spacing
        const val XXXLARGE = 48 // 6x base unit - Major content blocks
    }
    
    // Typography scale following Material Design 3
    object Typography {
        const val DISPLAY_LARGE = 32   // Page titles
        const val DISPLAY_MEDIUM = 28  // Section headers
        const val DISPLAY_SMALL = 24   // Sub-section headers
        
        const val HEADLINE_LARGE = 22  // Card titles
        const val HEADLINE_MEDIUM = 20 // List headers
        const val HEADLINE_SMALL = 18  // Subheadings
        
        const val TITLE_LARGE = 16     // Primary button text
        const val TITLE_MEDIUM = 14    // Secondary text
        const val TITLE_SMALL = 12     // Caption text
        
        const val BODY_LARGE = 16      // Main content
        const val BODY_MEDIUM = 14     // Secondary content
        const val BODY_SMALL = 12      // Supporting text
        
        const val LABEL_LARGE = 14     // Button labels
        const val LABEL_MEDIUM = 12    // Chip labels
        const val LABEL_SMALL = 10     // Badge text
    }
    
    // Component specifications
    object Components {
        // Button heights
        const val BUTTON_HEIGHT_LARGE = 56     // Primary action buttons
        const val BUTTON_HEIGHT_MEDIUM = 48    // Secondary buttons
        const val BUTTON_HEIGHT_SMALL = 40     // Compact buttons
        
        // Input field heights
        const val INPUT_HEIGHT_LARGE = 56      // Text fields
        const val INPUT_HEIGHT_MEDIUM = 48     // Dropdowns
        const val INPUT_HEIGHT_SMALL = 40      // Search bars
        
        // Card specifications
        const val CARD_CORNER_RADIUS = 12      // Standard card corner radius
        const val CARD_ELEVATION_LOW = 1       // Subtle elevation
        const val CARD_ELEVATION_MEDIUM = 2    // Standard elevation
        const val CARD_ELEVATION_HIGH = 4      // Emphasized elevation
        
        // Icon sizes
        const val ICON_SIZE_SMALL = 20         // Inline icons
        const val ICON_SIZE_MEDIUM = 24        // Standard icons
        const val ICON_SIZE_LARGE = 32         // Feature icons
        const val ICON_SIZE_XLARGE = 48        // Hero icons
        
        // Touch targets
        const val MIN_TOUCH_TARGET = 48        // Minimum touch target size
        const val COMFORTABLE_TOUCH_TARGET = 56 // Comfortable touch target
    }
    
    // Elevation system
    object Elevation {
        const val FLAT = 0          // No elevation
        const val RAISED = 1        // Subtle separation
        const val ELEVATED = 2      // Standard cards
        const val FLOATING = 4      // FABs and menus
        const val MODAL = 8         // Dialogs and bottom sheets
    }
    
    // Maximum items to show before "show more"
    object ItemLimits {
        const val DASHBOARD_GROUPS = 3
        const val TRANSACTION_PAGE_SIZE = 20
        const val CATEGORIES_INITIAL = 5
        const val MERCHANTS_INITIAL = 5
    }
}