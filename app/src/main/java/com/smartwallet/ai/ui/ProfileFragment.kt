package com.smartwallet.ai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.smartwallet.ai.databinding.FragmentProfileBinding
import com.smartwallet.ai.utils.PreferenceManager
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private val auth by lazy { FirebaseAuth.getInstance() }
    // private var isUploadInProgress = false // Removed as it's no longer needed for local storage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferenceManager = PreferenceManager(requireContext())
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
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    private fun requestPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
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
                requireContext(),
                "Permission denied. Cannot access gallery.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveProfileImageLocally(uri)
        } else {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImageLocally(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val directory = File(requireContext().filesDir, "profile_pics")
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

                Toast.makeText(requireContext(), "Profile picture updated locally!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ProfileLocal", "Error saving image: ${e.message}", e)
            Toast.makeText(requireContext(), "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
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
            
            Toast.makeText(requireContext(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Please enter name and monthly income", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

