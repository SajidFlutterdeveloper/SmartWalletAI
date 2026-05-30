package com.smartwallet.ai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.smartwallet.ai.R
import com.smartwallet.ai.databinding.ActivityMainBinding
import com.smartwallet.ai.utils.BiometricHelper
import com.smartwallet.ai.utils.NotificationHelper
import com.smartwallet.ai.utils.PreferenceManager
import android.content.Context
import android.view.View
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var preferenceManager: PreferenceManager

    private var lastAuthTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferenceManager = PreferenceManager(this)

        if (!preferenceManager.isProfileCompleted()) {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("FIRST_TIME", true)
            startActivity(intent)
            finish()
            return
        }

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()
        scheduleDailyReminder()
        
        performSecurityCheck()
    }

    private fun scheduleDailyReminder() {
        val intent = Intent(this, com.smartwallet.ai.utils.ReminderReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 21) // 9 PM
            set(Calendar.MINUTE, 0)
        }
        
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setInexactRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            android.app.AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun onResume() {
        super.onResume()
        // Enterprise Security: If app was in background for more than 3 minutes, re-authenticate
        val sessionTimeout = 180000L // 3 Minutes
        if (System.currentTimeMillis() - lastAuthTime > sessionTimeout) {
            performSecurityCheck()
        }
    }

    private fun performSecurityCheck() {
        if (preferenceManager.isBiometricEnabled() && BiometricHelper.isBiometricAvailable(this)) {
            // Anti-Peek: Ensure UI is hidden during authentication
            binding.root.visibility = View.GONE
            
            BiometricHelper.showBiometricPrompt(this, onSuccess = {
                lastAuthTime = System.currentTimeMillis()
                binding.root.visibility = View.VISIBLE
                if (!::navController.isInitialized) {
                    initUI()
                }
            }, onError = { error ->
                Toast.makeText(this, "Security Access Denied: $error", Toast.LENGTH_SHORT).show()
                finish()
            })
        } else {
            if (!::binding.isInitialized) {
                initUI()
            }
        }
    }

    private fun initUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupFab()
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            binding.fabAdd.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            
            // Animation for FAB
            binding.fabAdd.animate()
                .rotationBy(45f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(150)
                .withEndAction {
                    binding.fabAdd.animate().scaleX(1.0f).scaleY(1.0f).rotation(0f).setDuration(150).start()
                    navController.navigate(R.id.navigation_add_expense)
                }
                .start()
        }
    }

    private fun setupNavigation() {
        // Get the NavHostFragment from the FragmentContainerView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Connect bottom navigation to navigation controller
        binding.bottomNavigation.setupWithNavController(navController)
        
        // WhatsApp-like smooth selection animation with Haptic Feedback
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId != navController.currentDestination?.id) {
                // Haptic feedback
                binding.bottomNavigation.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

                // Bounce animation for icon
                val itemView = binding.bottomNavigation.findViewById<View>(item.itemId)
                itemView?.animate()
                    ?.scaleX(1.15f)
                    ?.scaleY(1.15f)
                    ?.setDuration(150)
                    ?.setInterpolator(android.view.animation.OvershootInterpolator())
                    ?.withEndAction {
                        itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    }
                    ?.start()

                // Navigate with smooth fragment transitions (Slide Left/Right)
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build()
                
                navController.navigate(item.itemId, null, navOptions)
            }
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Budget alerts will not be shown", Toast.LENGTH_LONG).show()
        }
    }
}
