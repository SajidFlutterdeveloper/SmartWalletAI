package com.smartwallet.ai.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smartwallet.ai.databinding.ActivityInsightsBinding
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.smartwallet.ai.utils.PreferenceManager

class InsightsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInsightsBinding
    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsightsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()

        viewModel.allExpenses.observe(this) {
            viewModel.calculateInsights(income, goal)
        }

        viewModel.aiInsights.observe(this) { insights ->
            if (insights.isNullOrEmpty()) {
                binding.tvInsights.text = "AI is still learning your habits. Keep adding expenses!"
            } else {
                binding.tvInsights.text = insights.joinToString("\n\n")
            }
        }
    }
}
