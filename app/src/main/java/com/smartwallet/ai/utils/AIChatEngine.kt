package com.smartwallet.ai.utils

import com.smartwallet.ai.data.model.Expense
import java.util.*

object AIChatEngine {

    fun getResponse(query: String, expenses: List<Expense>, income: Double, savingsGoal: Double): String {
        val lowerQuery = query.lowercase(Locale.getDefault())
        
        val totalSpent = expenses.sumOf { it.amount }
        val remainingBudget = income - savingsGoal - totalSpent
        val categories = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
        
        return when {
            lowerQuery.contains("save more") || lowerQuery.contains("how to save") -> {
                generateSavingAdvice(categories, remainingBudget)
            }
            lowerQuery.contains("overspending") || lowerQuery.contains("why am i spending") -> {
                generateSpendingAnalysis(categories, income)
            }
            lowerQuery.contains("status") || lowerQuery.contains("how am i doing") -> {
                "You have spent PKR ${String.format("%.0f", totalSpent)} this month. Your remaining safe budget is PKR ${String.format("%.0f", remainingBudget.coerceAtLeast(0.0))}."
            }
            lowerQuery.contains("hello") || lowerQuery.contains("hi") -> {
                "Hello! I am your Smart Wallet AI. Ask me about your spending, savings, or budget status."
            }
            else -> "I analyzed your data. You've spent the most on ${categories.maxByOrNull { it.value }?.key ?: "nothing yet"}. Try asking 'How can I save more?' for specific tips."
        }
    }

    private fun generateSavingAdvice(categories: Map<String, Double>, remaining: Double): String {
        val topCategory = categories.maxByOrNull { it.value }?.key ?: "general items"
        return if (remaining < 0) {
            "You are currently over budget. I recommend cutting down on $topCategory immediately to stabilize your finances."
        } else {
            "You have a surplus of PKR ${String.format("%.0f", remaining)}. If you invest this or move it to a savings account, you'll reach your goal faster."
        }
    }

    private fun generateSpendingAnalysis(categories: Map<String, Double>, income: Double): String {
        val food = categories["Food"] ?: 0.0
        val shopping = categories["Shopping"] ?: 0.0
        
        return when {
            food > income * 0.3 -> "Your food expenses are quite high (over 30% of income). Consider meal prepping."
            shopping > income * 0.2 -> "Shopping is taking a large chunk of your budget. Try the 24-hour rule before buying non-essentials."
            else -> "Your spending is distributed across categories. Tracking every small PKR 50-100 expense will help identify hidden leaks."
        }
    }
}
