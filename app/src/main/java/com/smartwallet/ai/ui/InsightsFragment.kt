package com.smartwallet.ai.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartwallet.ai.R
import com.smartwallet.ai.data.model.*
import com.smartwallet.ai.databinding.FragmentInsightsBinding
import com.smartwallet.ai.databinding.ItemAiInsightBinding
import com.smartwallet.ai.databinding.ItemSmartChallengeBinding
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.smartwallet.ai.utils.PreferenceManager
import java.util.*

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var insightsAdapter: InsightsAdapter
    private lateinit var challengesAdapter: ChallengesAdapter
    private lateinit var savingsHistoryAdapter: SavingsHistoryAdapter

    private val calendar = Calendar.getInstance()
    private val currentMonth = calendar.get(Calendar.MONTH) + 1
    private val currentYear = calendar.get(Calendar.YEAR)

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
        setupRecyclerViews()
        
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()

        viewModel.allExpenses.observe(viewLifecycleOwner) {
            viewModel.calculateInsights(income, goal)
        }

        viewModel.advancedInsights.observe(viewLifecycleOwner) { insights ->
            updateUI(insights)
        }

        viewModel.aiFeed.observe(viewLifecycleOwner) { feed ->
            insightsAdapter.updateInsights(feed)
        }

        viewModel.monthlyTargets.observe(viewLifecycleOwner) { targets ->
            savingsHistoryAdapter.updateTargets(targets)
        }
        
        binding.btnAskAI.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), ChatActivity::class.java))
        }
    }

    private fun setupRecyclerViews() {
        insightsAdapter = InsightsAdapter()
        binding.rvInsights.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = insightsAdapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        }

        challengesAdapter = ChallengesAdapter()
        binding.rvChallenges.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = challengesAdapter
        }

        savingsHistoryAdapter = SavingsHistoryAdapter()
        binding.rvSavingsHistory.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = savingsHistoryAdapter
        }
    }

    private fun updateUI(data: AIInsightData) {
        val context = requireContext()
        // Dynamic Title
        val calendar = Calendar.getInstance()
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        binding.tvTitle.text = "AI $monthName Intelligence"
        
        binding.tvInsightsHeader.text = "Smart Analysis"
        binding.tvChallengesHeader.text = "Financial Missions"

        // Accumulated Savings (Isolation Feature)
        val accumulated = preferenceManager.getAccumulatedSavings()
        val currency = preferenceManager.getCurrency()
        binding.tvMotivation.text = "🌟 Isolated Savings: $currency ${String.format("%,.0f", accumulated)}\n${data.motivationMessage}"
        
        // Show Health Score
        binding.pbHealthScore.progress = data.healthScore
        binding.tvHealthScore.text = data.healthScore.toString()
        
        binding.pbHealthScore.setIndicatorColor(ContextCompat.getColor(context, R.color.white))

        binding.tvHealthStatus.text = when(data.budgetStatus) {
            BudgetStatus.ON_TRACK -> "Safe & Balanced"
            BudgetStatus.NEEDS_ATTENTION -> "Moderate Risk"
            BudgetStatus.AT_RISK -> "Critical Overspend"
        }
        
        // Personality
        binding.tvPersonality.text = "Personality: ${data.financialPersonality.name.lowercase().replaceFirstChar { it.uppercase() }}"
        
        // Budget Usage
        binding.pbBudgetUsage.progress = data.budgetUtilization
        binding.tvBudgetText.text = "${data.budgetUtilization}%"
        
        val usageColor = if (data.budgetUtilization > 90) R.color.danger else R.color.white
        binding.tvBudgetText.setTextColor(ContextCompat.getColor(context, usageColor))

        binding.tvDailyLimit.text = data.dailySafeMessage
        
        // Survival Prediction
        binding.tvSurvivalProb.text = "${data.survivalPrediction.probability}% Success Probability"
        binding.tvSurvivalMsg.text = data.survivalPrediction.message
        
        // Top Merchant
        if (data.topMerchant != null) {
            binding.cardTopMerchant.visibility = View.VISIBLE
            binding.tvTopMerchantName.text = data.topMerchant
        } else {
            binding.cardTopMerchant.visibility = View.GONE
        }
        
        // Adapters
        challengesAdapter.updateChallenges(data.challenges)
        
        // Weekly Summary
        data.weeklySummary?.let {
            binding.tvWeeklyTopCat.text = "Major Expense: ${it.topCategory}"
            binding.tvWeeklySavings.text = "Week's Surplus: $currency ${String.format("%,.0f", it.savingsAchieved)}"
            binding.tvWeeklyAction.text = it.actionPlan.joinToString("\n") { plan -> "🚀 $plan" }
        }
    }

    class InsightsAdapter : RecyclerView.Adapter<InsightsAdapter.ViewHolder>() {
        private var items = listOf<Pair<String, String>>()

        fun updateInsights(newItems: List<Pair<String, String>>) {
            items = newItems
            notifyDataSetChanged()
        }

        class ViewHolder(val binding: ItemAiInsightBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemAiInsightBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (tag, message) = items[position]
            holder.binding.tvInsightText.text = message
            
            val context = holder.itemView.context
            val color = when {
                tag.contains("CRITICAL") || tag.contains("🚨") -> ContextCompat.getColor(context, R.color.danger)
                tag.contains("WARNING") || tag.contains("⚠️") || tag.contains("VELOCITY") -> ContextCompat.getColor(context, R.color.warning)
                else -> ContextCompat.getColor(context, R.color.primary)
            }
            holder.binding.viewIndicator.setBackgroundColor(color)
            
            // Optionally show the tag
            // holder.binding.tvTag.text = tag
        }

        override fun getItemCount() = items.size
    }

    class ChallengesAdapter : RecyclerView.Adapter<ChallengesAdapter.ViewHolder>() {
        private var items = listOf<SmartChallenge>()

        fun updateChallenges(newItems: List<SmartChallenge>) {
            items = newItems
            notifyDataSetChanged()
        }

        class ViewHolder(val binding: ItemSmartChallengeBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSmartChallengeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val challenge = items[position]
            holder.binding.tvChallengeTitle.text = challenge.title
            holder.binding.tvChallengeDesc.text = challenge.description
            holder.binding.pbChallenge.progress = challenge.progress
        }

        override fun getItemCount() = items.size
    }

    class SavingsHistoryAdapter : RecyclerView.Adapter<SavingsHistoryAdapter.ViewHolder>() {
        private var items = listOf<com.smartwallet.ai.data.model.MonthlyTarget>()

        fun updateTargets(newItems: List<com.smartwallet.ai.data.model.MonthlyTarget>) {
            items = newItems
            notifyDataSetChanged()
        }

        class ViewHolder(val binding: com.smartwallet.ai.databinding.ItemSavingsHistoryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = com.smartwallet.ai.databinding.ItemSavingsHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val target = items[position]
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            holder.binding.tvMonthYear.text = "${monthNames[target.month - 1]} ${target.year}"
            holder.binding.tvAchieved.text = "PKR ${String.format("%,.0f", target.actualSavings)}"
            holder.binding.tvGoal.text = "Goal: PKR ${String.format("%,.0f", target.savingsGoal)}"
            
            val progress = if (target.savingsGoal > 0) {
                ((target.actualSavings / target.savingsGoal) * 100).toInt()
            } else {
                100
            }
            holder.binding.pbSavingsProgress.progress = progress.coerceIn(0, 100)
            
            val color = if (target.actualSavings >= target.savingsGoal) R.color.success else R.color.warning
            holder.binding.pbSavingsProgress.setIndicatorColor(ContextCompat.getColor(holder.itemView.context, color))
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
