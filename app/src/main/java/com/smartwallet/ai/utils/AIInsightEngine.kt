package com.smartwallet.ai.utils

import com.smartwallet.ai.data.model.*
import java.util.*
import java.util.concurrent.TimeUnit

object AIInsightEngine {

    fun generateAdvancedInsights(
        expenses: List<Expense>,
        monthlyIncome: Double,
        savingsGoal: Double
    ): AIInsightData {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysRemaining = daysInMonth - currentDay + 1

        val currentMonthExpenses = expenses.filter {
            val eCal = Calendar.getInstance().apply { timeInMillis = it.date }
            eCal.get(Calendar.MONTH) == currentMonth && eCal.get(Calendar.YEAR) == currentYear
        }

        val totalSpent = currentMonthExpenses.sumOf { it.amount }
        val budgetLimit = monthlyIncome - savingsGoal
        val budgetUtilization = if (monthlyIncome > 0) ((totalSpent / monthlyIncome) * 100).toInt() else 0
        
        // 1. Financial Health Score
        val healthScore = calculateHealthScore(totalSpent, monthlyIncome, savingsGoal, currentDay, daysInMonth)
        val budgetStatus = when {
            totalSpent > monthlyIncome -> BudgetStatus.AT_RISK
            totalSpent > budgetLimit -> BudgetStatus.NEEDS_ATTENTION
            else -> BudgetStatus.ON_TRACK
        }

        // 2. Daily Safe Limit
        val remainingBudget = (monthlyIncome - savingsGoal - totalSpent).coerceAtLeast(0.0)
        val dailySafeLimit = if (daysRemaining > 0) remainingBudget / daysRemaining else 0.0

        // 3. Smart Spending Analysis
        val categoryTotals = currentMonthExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
        val spendingInsights = generateSpendingInsights(categoryTotals, totalSpent, expenses, currentMonth, currentYear)

        // 4. Survival Prediction
        val survivalPrediction = predictSurvival(totalSpent, monthlyIncome, savingsGoal, currentDay, daysInMonth)

        // 5. Savings Coach & Wellness
        val savingsCoach = generateSavingsTips(categoryTotals, totalSpent, remainingBudget)
        val wellnessInsights = generateWellnessInsights(categoryTotals)

        // 6. Stress Detection
        val stressLevel = detectStress(currentMonthExpenses, monthlyIncome, totalSpent)

        // 7. Motivation
        val motivationMessage = generateMotivation(totalSpent, budgetLimit, savingsGoal)

        // 8. Financial Personality
        val personality = determinePersonality(currentMonthExpenses, categoryTotals, totalSpent, monthlyIncome)

        // 9. Weekly Summary (Simplified for current week)
        val weeklySummary = generateWeeklySummary(currentMonthExpenses, monthlyIncome)

        // 10. Challenges
        val challenges = listOf(
            SmartChallenge("No Fast Food", "Avoid fast food for 3 days", 2, false),
            SmartChallenge("Budget Master", "Stay under daily limit for 5 days", 4, false)
        )

        return AIInsightData(
            healthScore = healthScore,
            budgetStatus = budgetStatus,
            dailySafeLimit = dailySafeLimit,
            budgetUtilization = budgetUtilization,
            spendingInsights = spendingInsights,
            survivalPrediction = survivalPrediction,
            savingsCoach = savingsCoach,
            wellnessInsights = wellnessInsights,
            stressLevel = stressLevel,
            motivationMessage = motivationMessage,
            financialPersonality = personality,
            weeklySummary = weeklySummary,
            challenges = challenges
        )
    }

    private fun calculateHealthScore(spent: Double, income: Double, goal: Double, day: Int, totalDays: Int): Int {
        if (income <= 0) return 0
        val budgetLimit = income - goal
        val expectedSpendingAtThisPoint = (budgetLimit / totalDays) * day
        
        var score = 100
        if (spent > expectedSpendingAtThisPoint) {
            val penalty = ((spent - expectedSpendingAtThisPoint) / income * 100).toInt()
            score -= penalty
        }
        if (spent > budgetLimit) score -= 20
        if (spent > income) score -= 30
        
        return score.coerceIn(0, 100)
    }

    private fun generateSpendingInsights(
        categoryTotals: Map<String, Double>,
        totalSpent: Double,
        allExpenses: List<Expense>,
        currentMonth: Int,
        currentYear: Int
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()
        categoryTotals.forEach { (cat, amount) ->
            val percentage = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0
            if (percentage > 25) {
                insights.add(SpendingInsight(cat, "High spending in $cat. It makes up ${percentage.toInt()}% of your budget.", "UP"))
            }
        }
        return insights
    }

    private fun predictSurvival(spent: Double, income: Double, goal: Double, day: Int, totalDays: Int): SurvivalPrediction {
        val budgetLimit = income - goal
        val remaining = (budgetLimit - spent).coerceAtLeast(0.0)
        val burnRate = if (day > 0) spent / day else 0.0
        val estimatedTotal = burnRate * totalDays
        
        val prob = if (estimatedTotal <= budgetLimit) 90 else ( (budgetLimit / estimatedTotal) * 100).toInt()
        val message = when {
            prob > 80 -> "On track to survive the month comfortably."
            prob > 50 -> "You might need to tighten your belt to reach month-end."
            else -> "High risk of running out of budget before month-end."
        }
        
        return SurvivalPrediction(prob.coerceIn(0, 100), (income - estimatedTotal).coerceAtLeast(0.0), message)
    }

    private fun generateSavingsTips(categoryTotals: Map<String, Double>, totalSpent: Double, remaining: Double): List<String> {
        val tips = mutableListOf<String>()
        if (categoryTotals.containsKey("Entertainment") && categoryTotals["Entertainment"]!! > 2000) {
            tips.add("Reduce entertainment spending to boost your savings.")
        }
        if (remaining > 5000) {
            tips.add("You have a good surplus. Consider moving PKR 2000 to your savings account.")
        }
        return tips
    }

    private fun generateWellnessInsights(categoryTotals: Map<String, Double>): List<String> {
        val insights = mutableListOf<String>()
        val fastFood = categoryTotals["Fast Food"] ?: 0.0
        if (fastFood > 5000) {
            insights.add("Frequent fast food detected. Home-cooked meals are better for your health and wallet.")
        }
        return insights
    }

    private fun detectStress(expenses: List<Expense>, income: Double, totalSpent: Double): StressLevel {
        val recentSpikes = expenses.takeLast(5).any { it.amount > income * 0.2 }
        return when {
            totalSpent > income * 0.9 || recentSpikes -> StressLevel.HIGH
            totalSpent > income * 0.7 -> StressLevel.MODERATE
            else -> StressLevel.LOW
        }
    }

    private fun generateMotivation(spent: Double, limit: Double, goal: Double): String {
        return if (spent < limit * 0.5) "Excellent discipline! You are a savings champion."
        else "Keep going! Small adjustments today lead to big freedom tomorrow."
    }

    private fun determinePersonality(expenses: List<Expense>, categories: Map<String, Double>, totalSpent: Double, income: Double): FinancialPersonality {
        val shopping = categories["Shopping"] ?: 0.0
        return when {
            totalSpent < income * 0.4 -> FinancialPersonality.SAVER
            shopping > totalSpent * 0.3 -> FinancialPersonality.IMPULSIVE
            else -> FinancialPersonality.BALANCED
        }
    }

    private fun generateWeeklySummary(expenses: List<Expense>, income: Double): WeeklySummary {
        val totalSpent = expenses.sumOf { it.amount }
        return WeeklySummary(
            totalIncome = income,
            totalExpenses = totalSpent,
            savingsAchieved = (income - totalSpent).coerceAtLeast(0.0),
            topCategory = expenses.groupBy { it.category }.maxByOrNull { it.value.sumOf { e -> e.amount } }?.key ?: "None",
            wins = listOf("Stayed under budget for 4 days"),
            risks = if (totalSpent > income * 0.25) listOf("High weekend spending") else emptyList(),
            actionPlan = listOf("Limit eating out this week")
        )
    }

    // Keep the old method for backward compatibility if needed, or update it
    fun generateInsights(expenses: List<Expense>, monthlyIncome: Double, savingsGoal: Double): List<String> {
        // This can just call the new engine and extract messages or we keep it as is
        val data = generateAdvancedInsights(expenses, monthlyIncome, savingsGoal)
        val list = mutableListOf<String>()
        list.add("Health Score: ${data.healthScore}")
        list.addAll(data.spendingInsights.map { it.message })
        list.add(data.survivalPrediction.message)
        list.addAll(data.savingsCoach)
        return list
    }
}
