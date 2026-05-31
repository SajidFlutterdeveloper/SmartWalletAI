package com.smartwallet.ai.ui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
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
        checkNotificationPermission()
        setupCharts()
        setupListeners()
        setupObservers()
        applyAnimations()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }
    }

    private fun applyAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        binding.cardSummary.startAnimation(fadeIn)
        binding.cardAIInsights.startAnimation(fadeIn)
        
        // Add subtle slide up to other components
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.pieChart.startAnimation(slideUp)
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun setupListeners() {
        binding.chipGroupReport.setOnCheckedChangeListener { _, _ ->
            viewModel.allExpenses.value?.let { updateReportChart(it) }
        }

        binding.btnDownloadReport.setOnClickListener {
            try {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            } catch (e: Exception) {}
            exportCSV("This Month")
        }

        binding.btnStatement.setOnClickListener {
            try {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            } catch (e: Exception) {}
            showStatementFilter()
        }
    }

    private fun showStatementFilter() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_statement_filter, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        val btnGenerate = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGenerate)
        val chipGroupPeriod = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupPeriod)
        val chipGroupFormat = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupFormat)

        btnGenerate.setOnClickListener {
            val period = when (chipGroupPeriod.checkedChipId) {
                R.id.chipThisWeek -> "This Week"
                R.id.chipThisMonth -> "This Month"
                R.id.chipThisYear -> "This Year"
                else -> "This Month"
            }
            
            val isCSV = chipGroupFormat.checkedChipId == R.id.chipCSV
            
            if (isCSV) {
                exportCSV(period)
            } else {
                generateStatement(period)
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun exportCSV(period: String) {
        val allExpenses = viewModel.allExpenses.value ?: emptyList()
        val filtered = filterByPeriod(allExpenses, period)
        com.smartwallet.ai.utils.ExportHelper.exportExpensesToCSV(requireContext(), filtered)
    }

    private fun filterByPeriod(allExpenses: List<com.smartwallet.ai.data.model.Expense>, period: String): List<com.smartwallet.ai.data.model.Expense> {
        val cal = Calendar.getInstance()
        return when (period) {
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
    }

    private fun generateStatement(period: String) {
        val allExpenses = viewModel.allExpenses.value ?: emptyList()
        val filtered = filterByPeriod(allExpenses, period)

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

        // Professional Branding: Use new Logo as high-quality default
        binding.ivProfileIcon.setImageResource(R.drawable.ic_app_logo)
        
        // Load User Profile Picture if available
        val profileUrl = preferenceManager.getProfilePicUrl()
        if (!profileUrl.isNullOrEmpty()) {
            val imgFile = File(profileUrl)
            if (imgFile.exists()) {
                Glide.with(this)
                    .load(imgFile)
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .circleCrop()
                    .into(binding.ivProfileIcon)
            }
        }
    }

    private fun setupObservers() {
        viewModel.totalExpensesThisMonth.observe(viewLifecycleOwner) { total ->
            val spent = total ?: 0.0
            val income = preferenceManager.getMonthlyIncome()
            val goal = preferenceManager.getSavingsGoal()
            val currency = preferenceManager.getCurrency()

            binding.tvGoal.text = "$currency ${formatAmount(goal)}"

            // Requirement 3 & 5: Reserve Savings and Prevent Negative Balance
            val spendingBudget = (income - goal).coerceAtLeast(0.0)
            val availableToSpend = BudgetCalculator.getAvailableSpendingBalance(income, goal, spent)

            binding.tvRemaining.text = "$currency ${formatAmount(availableToSpend)}"
            binding.tvIncome.text = "$currency ${formatAmount(income)}"
            binding.tvExpenses.text = "$currency ${formatAmount(spent)}"

            val health = BudgetCalculator.getSpendingHealth(income, goal, spent)
            val remainingDays = BudgetCalculator.getRemainingDaysInMonth()

            updateBudgetHealthUI(health, remainingDays)

            // Goal Progress: Shows how much of the "Reserved" savings are still intact
            // If spent exceeds spendingBudget, we are eating into the goal.
            val totalLeft = (income - spent).coerceAtLeast(0.0)
            val progress = if (income > 0) {
                ((totalLeft / income) * 100).toInt()
            } else {
                0
            }
            
            val coercedProgress = progress.coerceIn(0, 100)
            binding.goalProgress.setProgressCompat(coercedProgress, true)
            binding.tvGoalPercent.text = "$coercedProgress%"
            
            if (spent > spendingBudget) {
                val deficit = spent - spendingBudget
                binding.tvGoalRem.text = "⚠️ Goal Impacted: Eating PKR ${formatAmount(deficit)} into savings"
                binding.tvGoalRem.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.danger))
                binding.tvGoalPercent.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.danger))
                binding.goalProgress.setIndicatorColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.danger))
            } else {
                val leftToSpend = spendingBudget - spent
                binding.tvGoalRem.text = "$currency ${formatAmount(leftToSpend)} left to spend safely"
                binding.tvGoalRem.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
                binding.tvGoalPercent.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success))
                binding.goalProgress.setIndicatorColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success))
            }

            viewModel.calculateInsights(income, goal)
        }

        viewModel.expensesThisMonth.observe(viewLifecycleOwner) { expenses ->
            if (expenses != null) {
                updatePieChart(expenses)
            }
        }

        viewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            if (expenses != null) {
                updateReportChart(expenses)
                calculateMonthComparison(expenses)
            }
        }

        viewModel.aiInsights.observe(viewLifecycleOwner) { insights ->
            if (insights.isNotEmpty()) {
                val currentText = binding.tvAICoachMessage.text.toString()
                val newText = insights[0]
                
                // Update Mini Card
                if (currentText != newText) {
                    binding.tvAICoachMessage.animate().alpha(0f).setDuration(300).withEndAction {
                        binding.tvAICoachMessage.text = newText
                        binding.tvAICoachMessage.animate().alpha(1f).setDuration(300).start()
                    }.start()
                }
            }
        }

        viewModel.advancedInsights.observe(viewLifecycleOwner) { data ->
            // Update Sub-Header with a dynamic narrative of the whole situation
            binding.tvSubHeader.text = data.dynamicNarrative
            
            // Highlight Sub-Header if health is poor
            if (data.healthScore < 50) {
                binding.tvSubHeader.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.danger))
            } else {
                binding.tvSubHeader.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }

            // Update new dynamic cards
            binding.tvVelocity.text = "${if (data.spendingVelocity > 0) "+" else ""}${data.spendingVelocity.toInt()}%"
            binding.tvVelocity.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), 
                if (data.spendingVelocity > 10) R.color.danger else if (data.spendingVelocity < -10) R.color.success else R.color.text_main))
            
            binding.tvStreak.text = "${data.noSpendStreak} Days"
            binding.tvStreak.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),
                if (data.noSpendStreak >= 1) R.color.success else R.color.text_main))

            // Forecast Label
            binding.tvForecastLabel.text = data.survivalPrediction.message
            binding.tvForecastLabel.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), 
                if (data.survivalPrediction.probability > 60) R.color.text_secondary else R.color.danger)
            )
        }

        viewModel.spendingForecast.observe(viewLifecycleOwner) { forecast ->
            val currency = preferenceManager.getCurrency()
            binding.tvForecast.text = "$currency ${formatAmount(forecast)}"
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
                binding.tvMonthComparison.alpha = 1.0f
            } else {
                binding.tvMonthComparison.text = "Saved ${String.format(locale, "%.0f", percent)}% more than last month!"
                binding.tvMonthComparison.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success))
                binding.tvMonthComparison.alpha = 1.0f
            }
        } else {
            binding.tvMonthComparison.text = "Tracking your first month of saving"
            binding.tvMonthComparison.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white))
            binding.tvMonthComparison.alpha = 1.0f
        }
    }

    private fun setupCharts() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(android.graphics.Color.TRANSPARENT)
            setTransparentCircleColor(android.graphics.Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 65f
            transparentCircleRadius = 70f
            setDrawCenterText(true)
            centerText = "Spending\nIntelligence"
            setCenterTextSize(18f)
            setCenterTextTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD))
            setCenterTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_main))
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.apply {
                isEnabled = true
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 10f
                yEntrySpace = 5f
                yOffset = 10f
                textSize = 11f
                textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_secondary)
                form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
            }
            setEntryLabelColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_main))
            setEntryLabelTextSize(10f)
            setDrawEntryLabels(false)
            animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        }

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
        val categoryTotals = expenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> exp.amount } }
        val entries = categoryTotals.map { (cat, total) -> PieEntry(total.toFloat(), cat) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                requireContext().getColor(R.color.chart_1),
                requireContext().getColor(R.color.chart_2),
                requireContext().getColor(R.color.chart_3),
                requireContext().getColor(R.color.chart_4),
                requireContext().getColor(R.color.chart_5),
                requireContext().getColor(R.color.chart_6)
            )
            valueTextSize = 12f
            valueTextColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_main)
            sliceSpace = 4f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.45f
            valueLinePart2Length = 0.45f
            valueLineWidth = 2f
            valueLineColor = requireContext().getColor(R.color.primary)
        }
        binding.pieChart.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChart))
            setValueTextSize(11f)
            setValueTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_main))
        }
        binding.pieChart.invalidate()
    }

    private fun updateReportChart(expenses: List<com.smartwallet.ai.data.model.Expense>) {
        if (expenses.isEmpty()) {
            binding.reportBarChart.clear()
            return
        }
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        for (i in 6 downTo 0) {
            val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayStart = d.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val dayEnd = d.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
            val dailySum = expenses.filter { it.date in dayStart..dayEnd }.sumOf { it.amount }
            entries.add(BarEntry((6 - i).toFloat(), dailySum.toFloat()))
            labels.add(sdf.format(d.time))
        }
        val dataSet = BarDataSet(entries, "Spending Trend").apply {
            color = requireContext().getColor(R.color.primary)
            valueTextColor = requireContext().getColor(R.color.text_secondary)
            valueTextSize = 10f
            setDrawValues(false)
        }
        binding.reportBarChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            invalidate()
        }
    }

    private fun updateBudgetHealthUI(health: String, remainingDays: Int) {
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()
        val spent = viewModel.totalExpensesThisMonth.value ?: 0.0
        val dailyLimit = BudgetCalculator.calculateSafeDailyLimit(income, goal, spent)
        val currency = preferenceManager.getCurrency()

        binding.tvBudgetHealth.text = health
        binding.tvDaysRemaining.text = "$currency ${formatAmount(dailyLimit)} / day | $remainingDays Days Left"
        
        val color = when (health) {
            "Safe Spending" -> R.color.success
            "Moderate Spending" -> R.color.warning
            else -> R.color.danger
        }
        val colorInt = androidx.core.content.ContextCompat.getColor(requireContext(), color)
        binding.tvBudgetHealth.setTextColor(colorInt)
    }

    private fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "%.0f", amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}