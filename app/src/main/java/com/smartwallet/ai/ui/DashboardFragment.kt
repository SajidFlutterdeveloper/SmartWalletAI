package com.smartwallet.ai.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartwallet.ai.R
import com.smartwallet.ai.databinding.FragmentDashboardBinding
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.smartwallet.ai.utils.BudgetCalculator
import com.smartwallet.ai.utils.PreferenceManager
import com.bumptech.glide.Glide
import java.io.File
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager = PreferenceManager(requireContext())
        setupListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun setupListeners() {
        binding.btnVoice.setOnClickListener {
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            intent.putExtra("ACTION", "VOICE")
            startActivity(intent)
        }

        binding.btnScan.setOnClickListener {
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            intent.putExtra("ACTION", "SCAN")
            startActivity(intent)
        }

        binding.btnAddManual.setOnClickListener {
            startActivity(Intent(requireContext(), AddTransactionActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.navigation_profile)
        }
    }

    private fun loadUserData() {
        binding.tvHeader.text = "Hi, ${preferenceManager.getUserName()}!"
        val income = preferenceManager.getMonthlyIncome()
        val currency = preferenceManager.getCurrency()
        binding.tvIncome.text = "$currency ${String.format(Locale.getDefault(), "%.0f", income)}"

        // Load Profile Picture
        val profileUrl = preferenceManager.getProfilePicUrl()
        if (!profileUrl.isNullOrEmpty()) {
            val imgFile = File(profileUrl)
            if (imgFile.exists()) {
                Glide.with(this)
                    .load(imgFile)
                    .placeholder(android.R.drawable.ic_menu_preferences)
                    .error(android.R.drawable.ic_menu_preferences)
                    .centerCrop()
                    .into(binding.btnSettings)
            } else {
                binding.btnSettings.setImageResource(android.R.drawable.ic_menu_preferences)
            }
        } else {
            binding.btnSettings.setImageResource(android.R.drawable.ic_menu_preferences)
        }
    }

    private fun setupObservers() {
        val income = preferenceManager.getMonthlyIncome()
        val currency = preferenceManager.getCurrency()
        val goal = preferenceManager.getSavingsGoal()

        viewModel.totalExpenses.observe(viewLifecycleOwner) { total ->
            val spent = total ?: 0.0
            val remaining = income - spent

            binding.tvExpenses.text = "$currency ${String.format(Locale.getDefault(), "%.0f", spent)}"
            binding.tvRemaining.text = "$currency ${String.format(Locale.getDefault(), "%.0f", remaining)}"

            // Advanced Budget Calculations
            val safeLimit = BudgetCalculator.calculateSafeDailyLimit(income, goal, spent)
            val usagePercent = BudgetCalculator.getBudgetUsagePercentage(income, spent)
            val health = BudgetCalculator.getSpendingHealth(income, goal, spent)
            val remainingDays = BudgetCalculator.getRemainingDaysInMonth()

            binding.tvDailyLimit.text = "$currency ${String.format(Locale.getDefault(), "%.0f", safeLimit)}"
            binding.tvBudgetHealth.text = health
            binding.tvBudgetProgressLabel.text = "Budget Used: $usagePercent%"
            binding.pbBudgetUsage.progress = usagePercent.coerceIn(0, 100)
            binding.tvDaysRemaining.text = "$remainingDays Days Remaining in Month"

            // Health-based color coding
            when (health) {
                "Safe Spending" -> binding.tvBudgetHealth.setTextColor(android.graphics.Color.parseColor("#48BB78"))
                "Moderate Spending" -> binding.tvBudgetHealth.setTextColor(android.graphics.Color.parseColor("#ECC94B"))
                "Risky Spending" -> binding.tvBudgetHealth.setTextColor(android.graphics.Color.parseColor("#ED8936"))
                "Dangerous Spending" -> binding.tvBudgetHealth.setTextColor(android.graphics.Color.parseColor("#F56565"))
            }

            viewModel.calculateInsights(income, goal)
        }

        viewModel.aiInsights.observe(viewLifecycleOwner) { insights ->
            if (insights.isNotEmpty()) {
                binding.tvAICoachMessage.text = insights[0]
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


