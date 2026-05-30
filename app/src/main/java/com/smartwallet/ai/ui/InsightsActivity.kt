package com.smartwallet.ai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartwallet.ai.R
import com.smartwallet.ai.databinding.ActivityInsightsBinding
import com.smartwallet.ai.databinding.ItemAiInsightBinding
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.smartwallet.ai.utils.BudgetCalculator
import com.smartwallet.ai.utils.PreferenceManager
import java.util.*

class InsightsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInsightsBinding
    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var insightsAdapter: InsightsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsightsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        setupRecyclerView()
        
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()

        viewModel.allExpenses.observe(this) { expenses ->
            val totalSpent = expenses.sumOf { it.amount }
            updateHealthCard(income, goal, totalSpent)
            viewModel.calculateInsights(income, goal)
        }

        viewModel.aiInsights.observe(this) { insights ->
            insightsAdapter.updateInsights(insights ?: emptyList())
            binding.rvInsights.scheduleLayoutAnimation()
        }

        // Entrance animation for header
        binding.cardHealth.alpha = 0f
        binding.cardHealth.translationY = 50f
        binding.cardHealth.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun setupRecyclerView() {
        insightsAdapter = InsightsAdapter()
        binding.rvInsights.apply {
            layoutManager = LinearLayoutManager(this@InsightsActivity)
            adapter = insightsAdapter
            val resId = R.anim.layout_animation_fall_down
            val animation = AnimationUtils.loadLayoutAnimation(context, resId)
            layoutAnimation = animation
        }
    }

    private fun updateHealthCard(income: Double, goal: Double, totalSpent: Double) {
        val health = BudgetCalculator.getSpendingHealth(income, goal, totalSpent)
        val usagePercent = BudgetCalculator.getBudgetUsagePercentage(income, totalSpent)
        val dailyLimit = BudgetCalculator.calculateSafeDailyLimit(income, goal, totalSpent)

        binding.tvHealthStatus.text = health
        binding.pbBudgetUsage.progress = usagePercent
        binding.tvBudgetText.text = String.format(Locale.getDefault(), "%d%% used", usagePercent)
        binding.tvDailyLimit.text = String.format(Locale.getDefault(), "PKR %.0f", dailyLimit)
    }

    class InsightsAdapter : RecyclerView.Adapter<InsightsAdapter.ViewHolder>() {
        private var items = listOf<String>()

        fun updateInsights(newItems: List<String>) {
            items = newItems
            notifyDataSetChanged()
        }

        class ViewHolder(val binding: ItemAiInsightBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemAiInsightBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val insight = items[position]
            holder.binding.tvInsightText.text = insight
            
            // Set indicator color based on content
            val color = when {
                insight.contains("Alert") || insight.contains("⚠️") || insight.contains("Danger") -> 
                    holder.itemView.context.getColor(R.color.danger)
                insight.contains("🌟") || insight.contains("Bachat") || insight.contains("saved") -> 
                    holder.itemView.context.getColor(R.color.success)
                else -> holder.itemView.context.getColor(R.color.primary)
            }
            holder.binding.viewIndicator.setBackgroundColor(color)
        }

        override fun getItemCount() = items.size
    }
}
