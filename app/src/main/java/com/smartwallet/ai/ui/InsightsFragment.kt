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
    }

    private fun updateUI(data: AIInsightData) {
        // Health Score
        binding.pbHealthScore.progress = data.healthScore
        binding.tvHealthScore.text = data.healthScore.toString()
        binding.tvHealthStatus.text = when(data.budgetStatus) {
            BudgetStatus.ON_TRACK -> "On Track"
            BudgetStatus.NEEDS_ATTENTION -> "Needs Attention"
            BudgetStatus.AT_RISK -> "Budget Risk"
        }
        
        // Personality
        binding.tvPersonality.text = "Personality: ${data.financialPersonality.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
        
        // Budget Usage
        binding.pbBudgetUsage.progress = data.budgetUtilization
        binding.tvBudgetText.text = "${data.budgetUtilization}%"
        binding.tvDailyLimit.text = data.dailySafeMessage.ifEmpty { 
            String.format(Locale.getDefault(), "Daily safe spending limit: PKR %.0f", data.dailySafeLimit) 
        }
        
        // Survival Prediction
        binding.tvSurvivalProb.text = "${data.survivalPrediction.probability}% Probability"
        binding.tvSurvivalMsg.text = data.survivalPrediction.message
        
        // Motivation
        binding.tvMotivation.text = data.motivationMessage
        
        // Adapters
        insightsAdapter.updateInsights(data.spendingInsights)
        challengesAdapter.updateChallenges(data.challenges)
        
        // Weekly Summary
        data.weeklySummary?.let {
            binding.tvWeeklyTopCat.text = "Top Category: ${it.topCategory}"
            binding.tvWeeklySavings.text = "Savings Achieved: PKR ${String.format("%.0f", it.savingsAchieved)}"
            binding.tvWeeklyAction.text = it.actionPlan.joinToString("\n") { plan -> "• $plan" }
        }
    }

    class InsightsAdapter : RecyclerView.Adapter<InsightsAdapter.ViewHolder>() {
        private var items = listOf<SpendingInsight>()

        fun updateInsights(newItems: List<SpendingInsight>) {
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
            holder.binding.tvInsightText.text = insight.message
            
            val context = holder.itemView.context
            val color = if (insight.trend == "UP") ContextCompat.getColor(context, R.color.danger) 
                        else ContextCompat.getColor(context, R.color.success)
            holder.binding.viewIndicator.setBackgroundColor(color)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
