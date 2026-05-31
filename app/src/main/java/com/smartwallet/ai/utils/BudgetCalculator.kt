package com.smartwallet.ai.utils

import java.util.*
import java.util.concurrent.TimeUnit

object BudgetCalculator {

    fun getRemainingDaysInMonth(): Int {
        val calendar = Calendar.getInstance()
        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        return (totalDays - currentDay) + 1
    }

    fun calculateSafeDailyLimit(income: Double, savingsGoal: Double, currentExpenses: Double): Double {
        val spendingLimit = income - savingsGoal
        val availableToSpend = (spendingLimit - currentExpenses).coerceAtLeast(0.0)
        val remainingDays = getRemainingDaysInMonth()
        return if (remainingDays > 0) availableToSpend / remainingDays else 0.0
    }

    fun getAvailableSpendingBalance(income: Double, savingsGoal: Double, currentExpenses: Double): Double {
        val spendingLimit = income - savingsGoal
        return (spendingLimit - currentExpenses).coerceAtLeast(0.0)
    }

    fun getBudgetUsagePercentage(income: Double, savingsGoal: Double, currentExpenses: Double): Int {
        val spendingLimit = income - savingsGoal
        if (spendingLimit <= 0) return 100
        return ((currentExpenses / spendingLimit) * 100).toInt().coerceIn(0, 100)
    }

    fun getSpendingHealth(income: Double, savingsGoal: Double, currentExpenses: Double): String {
        val budgetForMonth = income - savingsGoal
        if (budgetForMonth <= 0) return "Risky"
        
        val usageRatio = currentExpenses / budgetForMonth
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val monthLength = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        val expectedRatio = dayOfMonth.toDouble() / monthLength.toDouble()

        return when {
            usageRatio > 1.0 -> "Dangerous Spending"
            usageRatio > expectedRatio + 0.2 -> "Risky Spending"
            usageRatio > expectedRatio + 0.1 -> "Moderate Spending"
            else -> "Safe Spending"
        }
    }
}
