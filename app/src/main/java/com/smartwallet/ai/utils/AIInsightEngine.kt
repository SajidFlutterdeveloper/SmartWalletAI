package com.smartwallet.ai.utils

import com.smartwallet.ai.data.model.Expense
import java.util.*

object AIInsightEngine {

    fun generateInsights(expenses: List<Expense>, monthlyIncome: Double, savingsGoal: Double): List<String> {
        val insights = mutableListOf<String>()
        val locale = Locale.getDefault()
        
        if (expenses.isEmpty()) return listOf("Welcome! Start adding expenses so I can help you manage your wealth and wellness.")

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        
        cal.add(Calendar.MONTH, -1)
        val prevMonth = cal.get(Calendar.MONTH)
        val prevYear = cal.get(Calendar.YEAR)

        val currentExpenses = expenses.filter { 
            val eCal = Calendar.getInstance().apply { timeInMillis = it.date }
            eCal.get(Calendar.MONTH) == currentMonth && eCal.get(Calendar.YEAR) == currentYear
        }

        val prevExpenses = expenses.filter { 
            val eCal = Calendar.getInstance().apply { timeInMillis = it.date }
            eCal.get(Calendar.MONTH) == prevMonth && eCal.get(Calendar.YEAR) == prevYear
        }

        val totalSpent = currentExpenses.sumOf { it.amount }
        val prevTotalSpent = prevExpenses.sumOf { it.amount }
        val categoryTotals = currentExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // 1. Month-over-Month Comparison (Bachat Focus)
        if (prevTotalSpent > 0) {
            val savingsDiff = prevTotalSpent - totalSpent
            if (savingsDiff > 0) {
                insights.add("🌟 Bachat King/Queen: You have saved PKR ${String.format(locale, "%.0f", savingsDiff)} more than last month! This is great financial progress.")
            } else if (savingsDiff < 0) {
                insights.add("📈 Alert: You are spending PKR ${String.format(locale, "%.0f", Math.abs(savingsDiff))} more than last month. Let's look at which category is causing this.")
            }
        }

        // 2. Savings Goal Analysis
        val remainingBudget = monthlyIncome - savingsGoal - totalSpent
        if (totalSpent > (monthlyIncome - savingsGoal)) {
            insights.add("⚠️ Goal Alert: You've crossed your savings threshold. Every rupee saved now is a step back to your goal!")
        }

        // 3. Category Leaks (Business/Mindful Analysis)
        categoryTotals.forEach { (category, amount) ->
            val percentage = (amount / totalSpent) * 100
            if (percentage > 30 && totalSpent > 5000) {
                insights.add("📊 Strategy: Your spending on '$category' is ${String.format(locale, "%.1f", percentage)}% of your total. Reducing this by just 10% could increase your savings significantly.")
            }
        }

        // 4. Transport & Wellness (Practical Saving)
        val fuelSpent = categoryTotals["Fuel"] ?: 0.0
        if (fuelSpent > monthlyIncome * 0.1) {
            insights.add("🚗 Transport: Fuel costs are rising. Carpooling this week could save you approx. PKR ${String.format(locale, "%.0f", fuelSpent * 0.2)}.")
        }

        val foodSpent = categoryTotals["Food"] ?: 0.0
        if (foodSpent > monthlyIncome * 0.2) {
            insights.add("🥗 Health & Wealth: High food spending detected. Meal prepping is not just healthy but also a 'Bachat' strategy for smart people.")
        }

        // 5. Impulsive Spending (Psychological Analysis)
        val shoppingSpent = categoryTotals["Shopping"] ?: 0.0
        if (shoppingSpent > monthlyIncome * 0.15) {
            insights.add("🧘 Mindfulness: Your shopping is high. Businessman's Tip: Delay non-essential purchases for 24 hours. Most impulses fade away!")
        }

        // 6. Final Surplus Advice
        if (remainingBudget > 0) {
            insights.add("💰 Smart Move: You have a surplus of PKR ${String.format(locale, "%.0f", remainingBudget)}. Moving this to a profit-bearing account now will grow your wealth.")
        }

        return if (insights.isEmpty()) listOf("You're doing great! Keep tracking to unlock deeper AI insights.") else insights
    }
}
