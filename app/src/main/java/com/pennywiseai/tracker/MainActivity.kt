package com.pennywiseai.tracker

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.databinding.ActivityMainBinding
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch
import com.pennywiseai.tracker.ui.SimplifiedDashboardFragment
import com.pennywiseai.tracker.ui.OrganizedSettingsFragment
import com.pennywiseai.tracker.ui.AnalyticsFragment
import com.pennywiseai.tracker.ui.TransactionsSimpleFragment
import com.pennywiseai.tracker.ui.ChatFragment
import com.pennywiseai.tracker.ui.SubscriptionsFragment
import com.pennywiseai.tracker.notification.NotificationListener
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val dashboardFragment = SimplifiedDashboardFragment()
    private val transactionsFragment = TransactionsSimpleFragment()
    private val analyticsFragment = AnalyticsFragment()
    private val subscriptionsFragment = SubscriptionsFragment()
    private val chatFragment = ChatFragment()
    private var activeFragment: Fragment = dashboardFragment
    
    // private var aiFabManager: AiFabManager? = null - Removed, using nav

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkNotificationPermissions()
        } else {
            showPermissionDeniedMessage()
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showFragment(dashboardFragment)
        } else {
            showNotificationPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check onboarding completion (database first, then SharedPreferences fallback)
        checkOnboardingStatus()
    }
    
    private fun checkOnboardingStatus() {
        // Check SharedPreferences synchronously - no need for coroutine
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val hasCompletedOnboarding = prefs.getBoolean("onboarding_completed", false)
        
        if (!hasCompletedOnboarding) {
            // First time user - show onboarding
            startActivity(Intent(this, com.pennywiseai.tracker.ui.EnhancedOnboardingActivity::class.java))
            finish()
        } else {
            // Returning user - go to main app
            initializeMainActivity()
        }
    }
    
    private fun initializeMainActivity() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupFragments()
        setupBottomNavigation()
        // Removed FAB - using navigation chat instead
        checkSmsPermission()
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, dashboardFragment, "1")
            add(R.id.fragment_container, transactionsFragment, "2").hide(transactionsFragment)
            add(R.id.fragment_container, analyticsFragment, "3").hide(analyticsFragment)
            add(R.id.fragment_container, subscriptionsFragment, "4").hide(subscriptionsFragment)
            add(R.id.fragment_container, chatFragment, "5").hide(chatFragment)
        }.commit()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            binding.bottomNavigation.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    showFragment(dashboardFragment)
                    true
                }
                R.id.nav_transactions -> {
                    showFragment(transactionsFragment)
                    true
                }
                R.id.nav_analytics -> {
                    showFragment(analyticsFragment)
                    true
                }
                R.id.nav_subscriptions -> {
                    showFragment(subscriptionsFragment)
                    true
                }
                R.id.nav_chat -> {
                    showFragment(chatFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun checkSmsPermission() {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            checkNotificationPermissions()
        } else {
            smsPermissionLauncher.launch(permissions)
        }
    }
    
    private fun checkNotificationPermissions() {
        // Check POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        
        // Check notification listener permission
        if (!isNotificationListenerEnabled()) {
            showNotificationListenerDialog()
        } else {
            showFragment(dashboardFragment)
        }
    }
    
    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }
    
    private fun showNotificationListenerDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Enable Notification Access")
            .setMessage("This app needs notification access to automatically detect transactions from banking apps in real-time.\n\nThis enhances transaction tracking by monitoring payment notifications.")
            .setPositiveButton("Enable") { _, _ ->
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Skip") { _, _ ->
                showFragment(dashboardFragment)
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            "SMS permission is required to track transactions",
            Snackbar.LENGTH_LONG
        ).setAction("Grant") {
            checkSmsPermission()
        }.show()
    }
    
    private fun showNotificationPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            "Notification permission helps show scan progress",
            Snackbar.LENGTH_LONG
        ).setAction("Grant") {
            checkNotificationPermissions()
        }.show()
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }
    
    fun navigateToTransactions() {
        binding.bottomNavigation.selectedItemId = R.id.nav_transactions
    }
    
    fun navigateToAnalytics() {
        binding.bottomNavigation.selectedItemId = R.id.nav_analytics
    }
    
    fun navigateToSubscriptions() {
        binding.bottomNavigation.selectedItemId = R.id.nav_subscriptions
    }
    
    // AI FAB removed - using bottom navigation for AI Chat
    
    override fun onDestroy() {
        super.onDestroy()
    }
}
