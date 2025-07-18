package com.pennywiseai.tracker.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.databinding.ViewEmptyStateBinding
import com.google.android.material.button.MaterialButton

/**
 * A reusable empty state view component for PennyWise AI
 * Provides consistent empty state design across all screens
 */
class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewEmptyStateBinding = ViewEmptyStateBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    init {
        // Apply any custom attributes if needed
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.EmptyStateView)
            // Process custom attributes here if defined
            typedArray.recycle()
        }
    }

    /**
     * Configure the empty state view with all necessary information
     */
    fun configure(config: EmptyStateConfig) {
        with(binding) {
            // Set illustration
            emptyStateIcon.setImageResource(config.iconResId)
            
            // Set title
            emptyStateTitle.text = config.title
            
            // Set description
            emptyStateDescription.text = config.description
            
            // Configure primary action
            if (config.primaryActionText != null) {
                emptyStatePrimaryAction.apply {
                    visibility = View.VISIBLE
                    text = config.primaryActionText
                    config.primaryActionIcon?.let { icon = ContextCompat.getDrawable(context, it) }
                    setOnClickListener { config.primaryActionCallback?.invoke() }
                }
            } else {
                emptyStatePrimaryAction.visibility = View.GONE
            }
            
            // Configure secondary action
            if (config.secondaryActionText != null) {
                emptyStateSecondaryAction.apply {
                    visibility = View.VISIBLE
                    text = config.secondaryActionText
                    setOnClickListener { config.secondaryActionCallback?.invoke() }
                }
            } else {
                emptyStateSecondaryAction.visibility = View.GONE
            }
            
            // Configure features list
            if (config.features.isNotEmpty()) {
                featuresContainer.visibility = View.VISIBLE
                featuresContainer.removeAllViews()
                
                config.features.forEach { feature ->
                    val featureView = LayoutInflater.from(context).inflate(
                        R.layout.item_empty_state_feature, featuresContainer, false
                    )
                    featureView.findViewById<android.widget.TextView>(R.id.feature_text)?.text = feature
                    featuresContainer.addView(featureView)
                }
            } else {
                featuresContainer.visibility = View.GONE
            }
            
            // Enable pulse animation if requested
            if (config.animateIcon) {
                pulseAnimation.visibility = View.VISIBLE
                startPulseAnimation()
            } else {
                pulseAnimation.visibility = View.GONE
            }
        }
    }
    
    private fun startPulseAnimation() {
        binding.pulseAnimation.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .alpha(0f)
            .setDuration(1500)
            .withEndAction {
                binding.pulseAnimation.scaleX = 1f
                binding.pulseAnimation.scaleY = 1f
                binding.pulseAnimation.alpha = 0.3f
                if (binding.pulseAnimation.visibility == View.VISIBLE) {
                    startPulseAnimation()
                }
            }
            .start()
    }
    
    /**
     * Configuration data class for empty state
     */
    data class EmptyStateConfig(
        @DrawableRes val iconResId: Int,
        val title: String,
        val description: String,
        val primaryActionText: String? = null,
        @DrawableRes val primaryActionIcon: Int? = null,
        val primaryActionCallback: (() -> Unit)? = null,
        val secondaryActionText: String? = null,
        val secondaryActionCallback: (() -> Unit)? = null,
        val features: List<String> = emptyList(),
        val animateIcon: Boolean = false
    )
    
    companion object {
        /**
         * Pre-configured empty states for common scenarios
         */
        fun getDashboardEmptyConfig(context: Context, onScanClick: () -> Unit): EmptyStateConfig {
            return EmptyStateConfig(
                iconResId = R.drawable.ic_empty_dashboard,
                title = context.getString(R.string.empty_dashboard_title),
                description = context.getString(R.string.empty_dashboard_description),
                primaryActionText = context.getString(R.string.start_scanning),
                primaryActionIcon = R.drawable.ic_scan_messages,
                primaryActionCallback = onScanClick,
                secondaryActionText = context.getString(R.string.how_it_works),
                features = listOf(
                    "🤖 AI automatically finds transactions",
                    "📊 Smart categorization and insights",
                    "🔒 All processing on your device",
                    "💳 Track subscriptions automatically"
                ),
                animateIcon = true
            )
        }
        
        fun getTransactionsEmptyConfig(
            context: Context,
            hasFilters: Boolean,
            onClearFilters: () -> Unit,
            onScanClick: () -> Unit
        ): EmptyStateConfig {
            return EmptyStateConfig(
                iconResId = R.drawable.ic_empty_transactions,
                title = if (hasFilters) {
                    context.getString(R.string.no_transactions_found)
                } else {
                    context.getString(R.string.no_transactions_yet)
                },
                description = if (hasFilters) {
                    context.getString(R.string.empty_transactions_filtered_description)
                } else {
                    context.getString(R.string.empty_transactions_description)
                },
                primaryActionText = if (hasFilters) {
                    context.getString(R.string.clear_filters)
                } else {
                    context.getString(R.string.scan_messages)
                },
                primaryActionIcon = if (hasFilters) null else R.drawable.ic_scan_messages,
                primaryActionCallback = if (hasFilters) onClearFilters else onScanClick,
                secondaryActionText = if (hasFilters) {
                    context.getString(R.string.scan_messages)
                } else {
                    null
                },
                secondaryActionCallback = if (hasFilters) onScanClick else null
            )
        }
        
        fun getChatEmptyConfig(context: Context, onSuggestionClick: (String) -> Unit): EmptyStateConfig {
            return EmptyStateConfig(
                iconResId = R.drawable.ic_empty_chat,
                title = context.getString(R.string.empty_chat_title),
                description = context.getString(R.string.empty_chat_description),
                animateIcon = true
            )
        }
        
        fun getSearchEmptyConfig(
            context: Context,
            searchQuery: String,
            onClearSearch: () -> Unit
        ): EmptyStateConfig {
            return EmptyStateConfig(
                iconResId = R.drawable.ic_empty_search,
                title = context.getString(R.string.no_search_results),
                description = context.getString(R.string.empty_search_description, searchQuery),
                primaryActionText = context.getString(R.string.clear_search),
                primaryActionIcon = R.drawable.ic_close,
                primaryActionCallback = onClearSearch
            )
        }
        
        fun getAnalyticsEmptyConfig(context: Context, onScanClick: () -> Unit): EmptyStateConfig {
            return EmptyStateConfig(
                iconResId = R.drawable.ic_analytics,
                title = context.getString(R.string.empty_analytics_title),
                description = context.getString(R.string.empty_analytics_description),
                primaryActionText = context.getString(R.string.start_analyzing),
                primaryActionIcon = R.drawable.ic_scan_messages,
                primaryActionCallback = onScanClick,
                features = listOf(
                    "📊 Spending breakdown by category",
                    "📈 Monthly spending trends",
                    "💰 Daily average expenses",
                    "🔄 Recurring payment patterns"
                )
            )
        }
        
        fun getSubscriptionsEmptyConfig(context: Context): EmptyStateConfig {
            return EmptyStateConfig(
                iconResId = R.drawable.ic_autorenew,
                title = context.getString(R.string.empty_subscriptions_title),
                description = context.getString(R.string.empty_subscriptions_description),
                features = listOf(
                    "📺 Netflix - ₹649/month",
                    "🎵 Spotify - ₹119/month",
                    "💪 Gym membership - ₹2000/month"
                )
            )
        }
    }
}