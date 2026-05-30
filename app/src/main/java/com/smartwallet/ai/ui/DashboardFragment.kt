package com.smartwallet.ai.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartwallet.ai.R
import com.smartwallet.ai.databinding.FragmentDashboardBinding
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.smartwallet.ai.utils.BudgetCalculator
import com.smartwallet.ai.utils.PreferenceManager
import com.bumptech.glide.Glide
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
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
        setupCharts()
        setupListeners()
        setupObservers()
        applyAnimations()
    }

    private fun applyAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        binding.cardSummary.startAnimation(fadeIn)
        binding.cardAIInsights.startAnimation(fadeIn)
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun setupListeners() {
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.navigation_profile)
        }

        binding.chipGroupReport.setOnCheckedChangeListener { _, _ ->
            viewModel.allExpenses.value?.let { updateReportChart(it) }
        }

        binding.btnDownloadReport.setOnClickListener {
            generateStatement("This Month")
        }

        binding.btnStatement.setOnClickListener {
            showStatementFilter()
        }
    }

    private fun showStatementFilter() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_statement_filter, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        val btnGenerate = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGenerate)
        val chipGroup = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupPeriod)

        btnGenerate.setOnClickListener {
            val period = when (chipGroup.checkedChipId) {
                R.id.chipThisWeek -> "This Week"
                R.id.chipThisMonth -> "This Month"
                R.id.chipThisYear -> "This Year"
                else -> "This Month"
            }
            generateStatement(period)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun generateStatement(period: String) {
        val allExpenses = viewModel.allExpenses.value ?: emptyList()
        val cal = Calendar.getInstance()
        val filtered = when (period) {
            "This Week" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                allExpenses.filter { it.date >= cal.timeInMillis }
            }
            "This Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                allExpenses.filter { it.date >= cal.timeInMillis }
            }
            "This Year" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                allExpenses.filter { it.date >= cal.timeInMillis }
            }
            else -> allExpenses
        }

        val intent = android.content.Intent(requireContext(), StatementActivity::class.java)
        intent.putExtra("PERIOD", period)
        intent.putExtra("DATA", java.util.ArrayList(filtered))
        startActivity(intent)
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
                    .placeholder(R.drawable.ic_wallet_ai_logo)
                    .error(R.drawable.ic_wallet_ai_logo)
                    .circleCrop()
                    .into(binding.btnSettings)
            } else {
                binding.btnSettings.setImageResource(R.drawable.ic_wallet_ai_logo)
            }
        } else {
            binding.btnSettings.setImageResource(R.drawable.ic_wallet_ai_logo)
        }
    }

    private fun setupObservers() {
        val income = preferenceManager.getMonthlyIncome()
        val currency = preferenceManager.getCurrency()
        val goal = preferenceManager.getSavingsGoal()

        binding.tvGoal.text = "$currency ${String.format(Locale.getDefault(), "%.0f", goal)}"

        viewModel.totalExpenses.observe(viewLifecycleOwner) { total ->
            val spent = total ?: 0.0
            val remaining = income - spent

            binding.tvExpenses.text = "$currency ${String.format(Locale.getDefault(), "%.0f", spent)}"
            binding.tvRemaining.text = "$currency ${String.format(Locale.getDefault(), "%.0f", remaining)}"

            val usagePercent = BudgetCalculator.getBudgetUsagePercentage(income, spent)
            val health = BudgetCalculator.getSpendingHealth(income, goal, spent)
            val remainingDays = BudgetCalculator.getRemainingDaysInMonth()

            binding.tvBudgetHealth.text = health
            binding.pbBudgetUsage.progress = usagePercent.coerceIn(0, 100)
            binding.tvDaysRemaining.text = "$remainingDays Days Remaining"

            // Professional Goal Progress Logic
            val currentSavings = income - spent
            val progress = if (goal > 0) (currentSavings / goal * 100).toInt() else 0
            
            binding.goalProgress.progress = progress.coerceIn(0, 100)
            binding.tvGoalPercent.text = "$progress%"
            val remToGoal = (goal - currentSavings).coerceAtLeast(0.0)
            binding.tvGoalRem.text = "$currency ${String.format(Locale.getDefault(), "%.0f", remToGoal)} to reach your goal"

            // Color coding health
            when (health) {
                "Safe Spending" -> binding.tvBudgetHealth.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success))
                "Risky Spending", "Moderate Spending" -> binding.tvBudgetHealth.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.warning))
                else -> binding.tvBudgetHealth.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.danger))
            }

            viewModel.calculateInsights(income, goal)
        }

        viewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            if (expenses != null) {
                updatePieChart(expenses)
                updateReportChart(expenses)
                calculateMonthComparison(expenses)
            }
        }

        viewModel.aiInsights.observe(viewLifecycleOwner) { insights ->
            if (insights.isNotEmpty()) {
                binding.tvAICoachMessage.text = insights[0]
            }
        }
    }

    private fun calculateMonthComparison(expenses: List<com.smartwallet.ai.data.model.Expense>) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        
        cal.add(Calendar.MONTH, -1)
        val prevMonth = cal.get(Calendar.MONTH)
        val prevYear = cal.get(Calendar.YEAR)

        val currentTotal = expenses.filter { 
            val eCal = Calendar.getInstance().apply { timeInMillis = it.date }
            eCal.get(Calendar.MONTH) == currentMonth && eCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }

        val prevTotal = expenses.filter { 
            val eCal = Calendar.getInstance().apply { timeInMillis = it.date }
            eCal.get(Calendar.MONTH) == prevMonth && eCal.get(Calendar.YEAR) == prevYear
        }.sumOf { it.amount }

        if (prevTotal > 0) {
            val diff = currentTotal - prevTotal
            val percent = (Math.abs(diff) / prevTotal) * 100
            val locale = Locale.getDefault()
            if (diff > 0) {
                binding.tvMonthComparison.text = "Spending ${String.format(locale, "%.0f", percent)}% more than last month"
                binding.tvMonthComparison.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.danger))
            } else {
                binding.tvMonthComparison.text = "Saved ${String.format(locale, "%.0f", percent)}% more than last month!"
                binding.tvMonthComparison.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success))
            }
        } else {
            binding.tvMonthComparison.text = "Tracking your first month of savings"
            binding.tvMonthComparison.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    private fun setupCharts() {
        // Donut Chart
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(android.graphics.Color.TRANSPARENT)
            holeRadius = 70f
            setDrawCenterText(true)
            centerText = "Goal Tracking"
            setCenterTextSize(14f)
            setCenterTextColor(android.graphics.Color.GRAY)
            legend.isEnabled = false
            setEntryLabelColor(android.graphics.Color.WHITE)
            animateXY(1500, 1500)
        }

        // Trends Chart
        binding.reportBarChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
            animateY(1500)
        }
    }

    private fun updatePieChart(expenses: List<com.smartwallet.ai.data.model.Expense>) {
        if (expenses.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }

        val entries = categoryTotals.map { (cat, total) -> PieEntry(total.toFloat(), cat) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                requireContext().getColor(R.color.chart_1),
                requireContext().getColor(R.color.chart_2),
                requireContext().getColor(R.color.chart_3),
                requireContext().getColor(R.color.chart_4),
                requireContext().getColor(R.color.chart_5)
            )
            valueTextSize = 10f
            valueTextColor = android.graphics.Color.WHITE
            sliceSpace = 3f
        }

        binding.pieChart.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter())
        }
        binding.pieChart.invalidate()
    }

    private fun updateReportChart(expenses: List<com.smartwallet.ai.data.model.Expense>) {
        val isWeekly = binding.chipWeekly.isChecked
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        if (isWeekly) {
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val dateStr = sdf.format(cal.time)
                val total = expenses.filter { 
                    val expCal = Calendar.getInstance().apply { timeInMillis = it.date }
                    expCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                }.sumOf { it.amount }
                
                entries.add(BarEntry((6-i).toFloat(), total.toFloat()))
                labels.add(dateStr)
            }
        } else {
            // Monthly weeks
            for (i in 1..5) {
                val total = expenses.filter { 
                    val expCal = Calendar.getInstance().apply { timeInMillis = it.date }
                    expCal.get(Calendar.WEEK_OF_MONTH) == i && 
                    expCal.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)
                }.sumOf { it.amount }
                entries.add(BarEntry((i-1).toFloat(), total.toFloat()))
                labels.add("W$i")
            }
        }

        val dataSet = BarDataSet(entries, "Spending").apply {
            color = requireContext().getColor(R.color.primary)
            setDrawValues(false)
        }

        binding.reportBarChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.5f }
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
