package com.pennywiseai.tracker.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.pennywiseai.tracker.MainActivity
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.AppSettings
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.databinding.ActivityOnboardingEnhancedBinding
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch

class EnhancedOnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingEnhancedBinding
    private lateinit var onboardingAdapter: OnboardingPagerAdapter
    private lateinit var repository: TransactionRepository
    
    private val onboardingScreens = listOf(
        R.layout.onboarding_welcome,
        R.layout.onboarding_privacy,
        R.layout.onboarding_how_it_works,
        R.layout.onboarding_permissions
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingEnhancedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        repository = TransactionRepository(AppDatabase.getDatabase(this))
        
        setupViewPager()
        setupPageIndicators()
        setupNavigationButtons()
    }

    private fun setupViewPager() {
        onboardingAdapter = OnboardingPagerAdapter(this, onboardingScreens)
        binding.onboardingViewPager.adapter = onboardingAdapter
        
        binding.onboardingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicators(position)
                updateNavigationButtons(position)
            }
        })
    }

    private fun setupPageIndicators() {
        val indicators = binding.pageIndicators
        indicators.removeAllViews()
        
        for (i in onboardingScreens.indices) {
            val indicator = View(this)
            val params = ViewGroup.MarginLayoutParams(24, 24)
            params.setMargins(8, 0, 8, 0)
            indicator.layoutParams = params
            indicator.background = getDrawable(R.drawable.page_indicator_inactive)
            indicators.addView(indicator)
        }
        
        // Set first indicator as active
        updatePageIndicators(0)
    }

    private fun updatePageIndicators(position: Int) {
        val indicators = binding.pageIndicators
        for (i in 0 until indicators.childCount) {
            val indicator = indicators.getChildAt(i)
            indicator.background = if (i == position) {
                getDrawable(R.drawable.page_indicator_active)
            } else {
                getDrawable(R.drawable.page_indicator_inactive)
            }
        }
    }

    private fun setupNavigationButtons() {
        binding.skipButton.setOnClickListener {
            completeOnboarding()
        }
        
        binding.nextButton.setOnClickListener {
            val currentPosition = binding.onboardingViewPager.currentItem
            if (currentPosition < onboardingScreens.size - 1) {
                binding.onboardingViewPager.currentItem = currentPosition + 1
            }
        }
        
        binding.getStartedButton.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun updateNavigationButtons(position: Int) {
        val isLastPage = position == onboardingScreens.size - 1
        
        binding.skipButton.visibility = if (isLastPage) View.GONE else View.VISIBLE
        binding.nextButton.visibility = if (isLastPage) View.GONE else View.VISIBLE
        binding.getStartedButton.visibility = if (isLastPage) View.VISIBLE else View.GONE
    }

    private fun completeOnboarding() {
        lifecycleScope.launch {
            try {
                // Save onboarding completion to database
                val existingSettings = repository.getSettingsSync()
                val updatedSettings = if (existingSettings != null) {
                    existingSettings.copy(
                        hasCompletedOnboarding = true,
                        onboardingCompletedAt = System.currentTimeMillis()
                    )
                } else {
                    AppSettings(
                        hasCompletedOnboarding = true,
                        onboardingCompletedAt = System.currentTimeMillis()
                    )
                }
                
                repository.insertSettings(updatedSettings)
                
                // Also set SharedPreferences for backward compatibility
                val prefs = getSharedPreferences("prefs", MODE_PRIVATE).edit()
                prefs.putBoolean("firstRun", false)
                prefs.putBoolean("onboarding_completed", true)
                prefs.apply()
                
                // Navigate to main activity
                startActivity(Intent(this@EnhancedOnboardingActivity, MainActivity::class.java))
                finish()
                
            } catch (e: Exception) {
                // Fallback to SharedPreferences only
                val prefs = getSharedPreferences("prefs", MODE_PRIVATE).edit()
                prefs.putBoolean("firstRun", false)
                prefs.putBoolean("onboarding_completed", true)
                prefs.apply()
                
                startActivity(Intent(this@EnhancedOnboardingActivity, MainActivity::class.java))
                finish()
            }
        }
    }

    class OnboardingPagerAdapter(
        activity: FragmentActivity,
        private val layouts: List<Int>
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = layouts.size

        override fun createFragment(position: Int): Fragment {
            return OnboardingFragment.newInstance(layouts[position])
        }
    }

    class OnboardingFragment : Fragment() {
        
        companion object {
            private const val ARG_LAYOUT = "layout"
            
            fun newInstance(layoutResId: Int): OnboardingFragment {
                val fragment = OnboardingFragment()
                val args = Bundle()
                args.putInt(ARG_LAYOUT, layoutResId)
                fragment.arguments = args
                return fragment
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val layoutResId = arguments?.getInt(ARG_LAYOUT) ?: R.layout.onboarding_welcome
            return inflater.inflate(layoutResId, container, false)
        }
    }
}