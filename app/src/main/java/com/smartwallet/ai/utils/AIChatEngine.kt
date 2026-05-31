package com.smartwallet.ai.utils

import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.data.model.ChatMessage
import java.util.*

object AIChatEngine {

    private val jokes = listOf(
        "Why did the man put his money in the freezer? He wanted cold hard cash!",
        "Why is a bank account like a book? Because they both have chapters and balance!",
        "My wallet is like an onion. Every time I open it, I cry.",
        "I have enough money to last me the rest of my life, unless I buy something.",
        "A budget is just a mathematical way to confirm you can't afford that pizza!"
    )

    private val wellnessPhrases = listOf(
        "Financial peace of mind is the best stress-buster. 🧘",
        "Take a deep breath. Money is just a tool, and you're the master. ✨",
        "Remember, your value isn't defined by your bank balance! ❤️",
        "A walk in the fresh air is free and great for your budget. 🌳"
    )

    private fun getGreeting(name: String, remaining: Double): String {
        val time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreet = when (time) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        val intros = listOf(
            "$timeGreet, $name! I've analyzed your data.",
            "Hello $name! Hope you're having a productive day.",
            "Hi $name, your financial coach is ready to help!",
            "Welcome back, $name. Let's look at your progress."
        )
        val statusMsg = if (remaining > 0) {
            "You have a comfortable PKR ${format(remaining)} safety margin."
        } else {
            "We need to be careful with our PKR ${format(Math.abs(remaining))} over-limit."
        }
        return "${intros.random()} $statusMsg How can I assist you?"
    }

    fun getResponse(
        query: String, 
        currentMonthExpenses: List<Expense>, 
        allTimeExpenses: List<Expense>,
        income: Double, 
        savingsGoal: Double,
        history: List<ChatMessage> = emptyList()
    ): String {
        val lowerQuery = query.lowercase(Locale.getDefault())
        val monthSpent = currentMonthExpenses.sumOf { it.amount }
        val remainingBudget = (income - savingsGoal - monthSpent)
        val monthCategories = currentMonthExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
        
        val lastUserMessage = history.filter { it.isUser }.lastOrNull()?.text?.lowercase() ?: ""

        return when {
            isMatch(lowerQuery, listOf("hi", "hello", "hey", "who are you")) -> {
                getGreeting("Friend", remainingBudget)
            }

            isMatch(lowerQuery, listOf("joke", "funny", "laugh")) -> {
                "${jokes.random()} Hope that brightens your day! Happiness is the best investment."
            }

            isMatch(lowerQuery, listOf("stress", "worried", "anxious", "sad", "loan", "debt")) -> {
                val wellness = wellnessPhrases.random()
                val debtAdvice = if (lowerQuery.contains("loan") || lowerQuery.contains("debt")) {
                    "Regarding borrowing: Let's first see if we can save PKR 1,000 this week by skipping a few non-essentials. Debt adds stress, and I want you to be free! 💪"
                } else {
                    "You're doing better than you think. Every transaction is a step toward mastery."
                }
                "$wellness\n\n$debtAdvice"
            }

            isMatch(lowerQuery, listOf("spent", "spending", "kharch", "status", "balance")) -> {
                val templates = listOf(
                    "You've recorded PKR ${format(monthSpent)} in expenses. This leaves you with PKR ${format(remainingBudget.coerceAtLeast(0.0))} to spend safely.",
                    "Right now, your total outflow is PKR ${format(monthSpent)}. Your available 'Safe Zone' is PKR ${format(remainingBudget.coerceAtLeast(0.0))}.",
                    "I see a total of PKR ${format(monthSpent)} spent. To stay on track for your goal, your remaining buffer is PKR ${format(remainingBudget.coerceAtLeast(0.0))}."
                )
                templates.random()
            }

            isMatch(lowerQuery, listOf("save", "goal", "advice", "tips")) -> {
                val top = findTopCategory(monthCategories)
                val advice = listOf(
                    "Try to reduce spending in **$top**. Even a 5% cut could boost your savings significantly!",
                    "Did you know? Small daily habits in **$top** often add up to big leaks. Let's watch it this week.",
                    "You're protecting your PKR ${format(savingsGoal)} goal. To make it easier, consider a 'No-Spend' day tomorrow!"
                )
                advice.random()
            }

            isMatch(lowerQuery, listOf("forecast", "predict", "end")) -> {
                val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                val totalDays = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
                val projected = ((monthSpent / day.coerceAtLeast(1)) * totalDays) * 1.1
                
                if (projected > income) {
                    "⚠️ **Alert**: If we don't adjust, we might hit PKR ${format(projected)} by month-end. That's over your income! Let's prioritize needs over wants."
                } else {
                    "✨ **Great News**: You're projected to finish the month with a PKR ${format(income - projected)} surplus. Your discipline is paying off!"
                }
            }

            isMatch(lowerQuery, listOf("category", "where", "kahan", "food", "shopping", "fuel", "bills")) -> {
                val catList = listOf("Food", "Grocery", "Fuel", "Bills", "Health", "Transport", "Shopping", "Education", "Transfer", "Entertainment")
                val matchedCat = catList.firstOrNull { lowerQuery.contains(it.lowercase()) }
                if (matchedCat != null) {
                    val amt = monthCategories[matchedCat] ?: 0.0
                    "You've allocated PKR ${format(amt)} to $matchedCat this month. This is ${((amt/income)*100).toInt()}% of your monthly income."
                } else {
                    val top = findTopCategory(monthCategories)
                    "Your largest spending area is **$top**, totaling PKR ${format(monthCategories[top] ?: 0.0)}. Should we look for some savings there?"
                }
            }

            lowerQuery == "why" || lowerQuery == "how" || lowerQuery == "explain" -> {
                handleFollowUp(lastUserMessage, monthCategories, monthSpent, income)
            }

            else -> {
                val defaultMsgs = listOf(
                    "I've analyzed your ${currentMonthExpenses.size} transactions. You have PKR ${format(remainingBudget.coerceAtLeast(0.0))} left to spend safely.",
                    "Your financial patterns are clear. We've spent PKR ${format(monthSpent)} so far. What's our next move?",
                    "I'm here to guide you. Want to talk about your **$income income** or your **$savingsGoal goal**?"
                )
                defaultMsgs.random()
            }
        }
    }

    private fun handleFollowUp(lastQuery: String, categories: Map<String, Double>, spent: Double, income: Double): String {
        return when {
            lastQuery.contains("spent") || lastQuery.contains("status") -> {
                val top = findTopCategory(categories)
                "Looking at the details, **$top** is the main factor (PKR ${format(categories[top] ?: 0.0)}). Controlling this will give you more freedom."
            }
            lastQuery.contains("save") || lastQuery.contains("tips") -> {
                "The 'How' is simple but powerful: Track every PKR 100. It's the small leaks that sink big ships!"
            }
            else -> "It's all about the balance between your PKR ${format(income)} income and your daily choices. I'm here to help you win!"
        }
    }

    private fun isMatch(query: String, keywords: List<String>): Boolean = keywords.any { query.contains(it) }

    private fun findTopCategory(categories: Map<String, Double>): String = categories.maxByOrNull { it.value }?.key ?: "Other"

    private fun format(amount: Double): String = String.format(Locale.getDefault(), "%,.0f", amount)
}
