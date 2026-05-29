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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var preferenceManager: PreferenceManager

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
        
        if (preferenceManager.isBiometricEnabled() && BiometricHelper.isBiometricAvailable(this)) {
            BiometricHelper.showBiometricPrompt(this, onSuccess = {
                initUI()
            }, onError = { error ->
                Toast.makeText(this, "Security Error: $error", Toast.LENGTH_SHORT).show()
                finish()
            })
        } else {
            initUI()
        }
    }

    private fun initUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    private fun setupNavigation() {
        // Get the NavHostFragment from the FragmentContainerView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Connect bottom navigation to navigation controller
        binding.bottomNavigation.setupWithNavController(navController)
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
