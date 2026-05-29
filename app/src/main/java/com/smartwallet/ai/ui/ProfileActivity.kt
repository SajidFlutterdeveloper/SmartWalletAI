package com.smartwallet.ai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.smartwallet.ai.databinding.ActivityProfileBinding
import com.smartwallet.ai.utils.PreferenceManager
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var preferenceManager: PreferenceManager
    private val auth by lazy { FirebaseAuth.getInstance() }
    // private var isUploadInProgress = false // Removed as it's no longer needed for local storage

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
            
            val profileUrl = preferenceManager.getProfilePicUrl()
            if (!profileUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(profileUrl)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(binding.ivProfilePic)
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
            auth.signOut()
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
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
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

                val file = File(directory, "profile_picture.jpg")
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                val localPath = file.absolutePath
                preferenceManager.setProfilePicUrl(localPath)

                Glide.with(this)
                    .load(file)
                    .centerCrop()
                    .into(binding.ivProfilePic)

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
            preferenceManager.saveProfile(name, mobile, income, savingsGoal, 1, "PKR")
            preferenceManager.setBiometricEnabled(biometric)
            
            Toast.makeText(this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
            
            // Check if we came from signup or just settings
            if (intent.getBooleanExtra("FIRST_TIME", false)) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                finish()
            }
        } else {
            Toast.makeText(this, "Please enter name and monthly income", Toast.LENGTH_SHORT).show()
        }
    }
}
