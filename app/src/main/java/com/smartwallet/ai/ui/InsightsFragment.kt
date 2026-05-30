package com.smartwallet.ai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartwallet.ai.R
import com.smartwallet.ai.databinding.FragmentInsightsBinding
import com.smartwallet.ai.databinding.ItemAiInsightBinding
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.smartwallet.ai.utils.BudgetCalculator
import com.smartwallet.ai.utils.PreferenceManager
import java.util.*

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var insightsAdapter: InsightsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferenceManager = PreferenceManager(requireContext())
        setupRecyclerView()
        
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()

        viewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            val totalSpent = expenses.sumOf { it.amount }
            updateHealthCard(income, goal, totalSpent)
            viewModel.calculateInsights(income, goal)
        }

        viewModel.aiInsights.observe(viewLifecycleOwner) { insights ->
            insightsAdapter.updateInsights(insights ?: emptyList())
            binding.rvInsights.scheduleLayoutAnimation()
        }

        // Entrance animation
        binding.cardHealth.alpha = 0f
        binding.cardHealth.translationY = 50f
        binding.cardHealth.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun setupRecyclerView() {
        insightsAdapter = InsightsAdapter()
        binding.rvInsights.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = insightsAdapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
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
            
            val context = holder.itemView.context
            val color = when {
                insight.contains("Alert") || insight.contains("⚠️") || insight.contains("Danger") -> 
                    context.getColor(R.color.danger)
                insight.contains("🌟") || insight.contains("Bachat") || insight.contains("saved") -> 
                    context.getColor(R.color.success)
                else -> context.getColor(R.color.primary)
            }
            holder.binding.viewIndicator.setBackgroundColor(color)
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
