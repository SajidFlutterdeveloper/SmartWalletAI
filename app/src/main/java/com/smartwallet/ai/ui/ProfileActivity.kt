package com.smartwallet.ai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.smartwallet.ai.R
import com.smartwallet.ai.data.model.UserProfile
import com.smartwallet.ai.data.remote.FirestoreManager
import com.smartwallet.ai.databinding.ActivityProfileBinding
import com.smartwallet.ai.utils.PreferenceManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var preferenceManager: PreferenceManager
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        loadProfileData()
        setupListeners()
    }

    private fun loadProfileData() {
        if (preferenceManager.isProfileCompleted()) {
            binding.etName.setText(preferenceManager.getUserName())
            binding.etMobile.setText(preferenceManager.getMobileNumber())
            binding.etIncome.setText(preferenceManager.getMonthlyIncome().toString())
            binding.etSavingsGoal.setText(preferenceManager.getSavingsGoal().toString())
            binding.switchBiometric.isChecked = preferenceManager.isBiometricEnabled()
            
            updateProfileImage()
        } else {
            // Check if we have a name from Google Signup
            val googleName = intent.getStringExtra("GOOGLE_NAME")
            if (!googleName.isNullOrEmpty()) {
                binding.etName.setText(googleName)
            }
        }
    }

    private fun updateProfileImage() {
        val profileUrl = preferenceManager.getProfilePicUrl()
        if (!profileUrl.isNullOrEmpty()) {
            if (profileUrl.startsWith("http")) {
                Glide.with(this)
                    .load(profileUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_app_logo)
                    .into(binding.ivProfilePic)
            } else {
                val file = File(profileUrl)
                if (file.exists()) {
                    Glide.with(this)
                        .load(file)
                        .centerCrop()
                        .placeholder(R.drawable.ic_app_logo)
                        .into(binding.ivProfilePic)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.ivProfilePic.setOnClickListener {
            requestPermissionAndPickImage()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveData()
        }

        binding.btnLogout.setOnClickListener {
            // 1. Sign out from Firebase
            auth.signOut()

            // 2. Sign out from Google to clear session
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            GoogleSignIn.getClient(this, gso).signOut()

            // 3. Clear local preferences
            preferenceManager.clearData()

            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun requestPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, open image picker
            imagePickerLauncher.launch("image/*")
        } else {
            // Request permission
            permissionLauncher.launch(permission)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted, open image picker
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(
                this,
                "Permission denied. Cannot access gallery.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveProfileImageLocally(uri)
            uploadProfileImageToCloud(uri)
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadProfileImageToCloud(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            val cloudUrl = FirestoreManager.uploadProfilePicture(uid, uri)
            if (cloudUrl != null) {
                // Update local storage with cloud URL to keep in sync
                preferenceManager.setProfilePicUrl(cloudUrl)
                Log.d("ProfileCloud", "Image uploaded: $cloudUrl")
            }
        }
    }

    private fun saveProfileImageLocally(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val directory = File(filesDir, "profile_pics")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                // Delete old profile pictures to save space
                directory.listFiles()?.forEach { it.delete() }

                // Create a unique filename to bypass Glide's cache
                val fileName = "profile_${System.currentTimeMillis()}.jpg"
                val file = File(directory, fileName)
                
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                val localPath = file.absolutePath
                preferenceManager.setProfilePicUrl(localPath)
                updateProfileImage()

                Toast.makeText(this, "Profile picture updated locally!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ProfileLocal", "Error saving image: ${e.message}", e)
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveData() {
        val name = binding.etName.text.toString()
        val mobile = binding.etMobile.text.toString()
        val income = binding.etIncome.text.toString().toDoubleOrNull() ?: 0.0
        val savingsGoal = binding.etSavingsGoal.text.toString().toDoubleOrNull() ?: 0.0
        val biometric = binding.switchBiometric.isChecked

        if (name.isNotEmpty() && income > 0) {
            val user = auth.currentUser
            if (user != null) {
                val profile = UserProfile(
                    uid = user.uid,
                    name = name,
                    mobile = mobile,
                    monthlyIncome = income,
                    savingsGoal = savingsGoal,
                    profilePicUrl = preferenceManager.getProfilePicUrl(),
                    profileCompleted = true
                )

                lifecycleScope.launch {
                    try {
                        showLoading(true)
                        // Save to Firestore (Professional cloud sync)
                        val success = FirestoreManager.saveUserProfile(profile)
                        
                        // Save to Local Preferences even if Firestore is slow
                        preferenceManager.saveProfile(name, mobile, income, savingsGoal, 1, "PKR")
                        preferenceManager.setBiometricEnabled(biometric)
                        
                        if (success) {
                            Toast.makeText(this@ProfileActivity, "Profile Cloud-Synchronized!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ProfileActivity, "Profile saved locally (Offline)", Toast.LENGTH_SHORT).show()
                        }
                        
                        showLoading(false)
                        startActivity(Intent(this@ProfileActivity, MainActivity::class.java))
                        finishAffinity()
                    } catch (e: Exception) {
                        showLoading(false)
                        Log.e("ProfileSync", "Sync failed", e)
                        startActivity(Intent(this@ProfileActivity, MainActivity::class.java))
                        finishAffinity()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Please enter name and monthly income", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.loadingOverlay.visibility = View.VISIBLE
            val rotate = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.rotate_universe)
            findViewById<View>(R.id.ivLoadingLogo)?.startAnimation(rotate)
        } else {
            binding.loadingOverlay.visibility = View.GONE
            findViewById<View>(R.id.ivLoadingLogo)?.clearAnimation()
        }
        binding.btnSaveProfile.isEnabled = !show
    }
}
