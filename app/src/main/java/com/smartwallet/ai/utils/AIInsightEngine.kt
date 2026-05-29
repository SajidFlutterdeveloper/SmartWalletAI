package com.smartwallet.ai.utils

import com.smartwallet.ai.data.model.Expense
import java.util.Locale

object AIInsightEngine {

    fun generateInsights(expenses: List<Expense>, monthlyIncome: Double, savingsGoal: Double): List<String> {
        val insights = mutableListOf<String>()
        
        if (expenses.isEmpty()) return listOf("Welcome! Start adding expenses so I can help you manage your wealth and wellness.")

        val totalSpent = expenses.sumOf { it.amount }
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val remainingBudget = monthlyIncome - savingsGoal - totalSpent

        // 1. Savings Goal Analysis
        if (totalSpent > (monthlyIncome - savingsGoal)) {
            insights.add("⚠️ Goal Alert: Your spending is affecting your savings goal of ${String.format(Locale.getDefault(), "%.0f", savingsGoal)}. Try to reduce unnecessary expenses.")
        } else {
            insights.add("✅ On Track: You are doing great! You are moving steadily towards your savings goal.")
        }

        // 2. Budget Warning
        if (remainingBudget < 0) {
            insights.add("🔴 Critical: You have exceeded your financial capacity this month. Please review your 'Shopping' and 'Entertainment' categories.")
        } else if (remainingBudget < monthlyIncome * 0.1) {
            insights.add("🟡 Warning: You are close to exceeding your monthly budget. Be careful with new purchases.")
        }

        // 3. Health & Wellness (Food Habits)
        val foodSpent = categoryTotals["Food"] ?: 0.0
        if (foodSpent > monthlyIncome * 0.2) {
            insights.add("🍔 Wellness: You are spending a lot on outside food. This affects both your health and savings. Cooking at home could save you significantly.")
        }

        // 4. Mental Wellness (Stress/Impulsive Spending)
        val shoppingSpent = categoryTotals["Shopping"] ?: 0.0
        if (shoppingSpent > monthlyIncome * 0.15) {
            insights.add("🧘 Mindful Spending: Your shopping expenses are high this week. Sometimes we shop when stressed. Take a deep breath and ask if you really need it.")
        }

        // 5. Motivational Guidance
        if (totalSpent < monthlyIncome * 0.5 && expenses.size > 5) {
            insights.add("🌟 Discipline: Your financial discipline is impressive! Small savings today create a stress-free future.")
        } else {
            insights.add("💡 Motivation: Financial discipline can significantly reduce mental stress. You have the power to control your future.")
        }

        // 6. Specific Category Insights
        val fuelSpent = categoryTotals["Fuel"] ?: 0.0
        if (fuelSpent > monthlyIncome * 0.1) {
            insights.add("🚗 Transport: Fuel costs are rising. Consider carpooling or planning trips better to save more.")
        }

        return insights
    }
}
