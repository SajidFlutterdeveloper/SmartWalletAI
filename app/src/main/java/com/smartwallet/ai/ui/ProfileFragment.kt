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
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.smartwallet.ai.R
import com.google.firebase.auth.FirebaseAuth
import com.smartwallet.ai.data.model.AIInsightData
import com.smartwallet.ai.databinding.FragmentProfileBinding
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.smartwallet.ai.utils.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private val viewModel: ExpenseViewModel by viewModels()

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
        setupObservers()
        animateUI()
    }

    private fun animateUI() {
        binding.cardHeader.translationY = -300f
        binding.cardHeader.animate().translationY(0f).setDuration(600).setInterpolator(DecelerateInterpolator()).start()
        
        binding.layoutAiSummary.alpha = 0f
        binding.layoutAiSummary.animate().alpha(1f).setStartDelay(400).setDuration(600).start()
        
        binding.layoutForm.alpha = 0f
        binding.layoutForm.translationY = 100f
        binding.layoutForm.animate().alpha(1f).translationY(0f).setStartDelay(600).setDuration(600).start()
    }

    private fun loadProfileData() {
        val name = preferenceManager.getUserName()
        binding.etName.setText(name)
        binding.tvDisplayUserName.text = name
        binding.etIncome.setText(preferenceManager.getMonthlyIncome().toString())
        binding.etSavingsGoal.setText(preferenceManager.getSavingsGoal().toString())
        binding.switchBiometric.isChecked = preferenceManager.isBiometricEnabled()
        
        updateProfileImage()
    }

    private fun updateProfileImage() {
        val profileUrl = preferenceManager.getProfilePicUrl()
        if (!profileUrl.isNullOrEmpty()) {
            val file = File(profileUrl)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .centerCrop()
                    .into(binding.ivProfilePic)
            }
        }
    }

    private fun setupListeners() {
        binding.fabEditPhoto.setOnClickListener {
            requestPermissionAndPickImage()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveData()
        }

        binding.btnExportData.setOnClickListener {
            val expenses = viewModel.allExpenses.value ?: emptyList()
            com.smartwallet.ai.utils.ExportHelper.exportExpensesToCSV(requireContext(), expenses)
        }

        binding.btnPrivacy.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://smartwallet.ai/privacy"))
            startActivity(browserIntent)
        }
    }

    private fun setupObservers() {
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()
        
        viewModel.calculateInsights(income, goal)
        viewModel.advancedInsights.observe(viewLifecycleOwner) { data ->
            updateAiQuickSummary(data)
        }
    }

    private fun updateAiQuickSummary(data: AIInsightData) {
        binding.tvQuickHealth.text = String.format(Locale.getDefault(), "%d%%", data.healthScore)
        binding.tvQuickScore.text = (data.healthScore * 10).toString()
        binding.tvUserPersonalityTag.text = data.financialPersonality.name.replace("_", " ").lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        binding.tvQuickDays.text = (daysInMonth - currentDay).toString()
    }

    private fun requestPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) imagePickerLauncher.launch("image/*")
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveProfileImageLocally(it) }
    }

    private fun saveProfileImageLocally(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                val directory = File(requireContext().filesDir, "profile_pics")
                if (!directory.exists()) directory.mkdirs()

                // Delete old profile pictures to save space
                directory.listFiles()?.forEach { it.delete() }

                // Create a unique filename to bypass Glide's cache
                val fileName = "profile_${System.currentTimeMillis()}.jpg"
                val file = File(directory, fileName)

                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }

                preferenceManager.setProfilePicUrl(file.absolutePath)
                updateProfileImage()
                Toast.makeText(requireContext(), "Photo updated everywhere!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("Profile", "Error saving image", e)
        }
    }

    private fun saveData() {
        val name = binding.etName.text.toString()
        val income = binding.etIncome.text.toString().toDoubleOrNull() ?: 0.0
        val goal = binding.etSavingsGoal.text.toString().toDoubleOrNull() ?: 0.0
        
        if (name.isNotEmpty() && income > 0) {
            preferenceManager.saveProfile(name, "", income, goal, 1, "PKR")
            binding.tvDisplayUserName.text = name
            Toast.makeText(requireContext(), "Profile Synchronized!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
