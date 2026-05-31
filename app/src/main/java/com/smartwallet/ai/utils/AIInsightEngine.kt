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
        val daysRemaining = (daysInMonth - currentDay + 1).coerceAtLeast(1)

        val currentMonthExpenses = expenses.filter {
            val eCal = Calendar.getInstance().apply { timeInMillis = it.date }
            eCal.get(Calendar.MONTH) == currentMonth && eCal.get(Calendar.YEAR) == currentYear
        }

        val totalSpent = currentMonthExpenses.sumOf { it.amount }
        val spendingLimit = (monthlyIncome - savingsGoal).coerceAtLeast(0.0)
        val budgetUtilization = if (spendingLimit > 0) ((totalSpent / spendingLimit) * 100).toInt().coerceIn(0, 100) else 100
        
        // 1. Financial Health Score
        val healthScore = calculateHealthScore(totalSpent, monthlyIncome, savingsGoal, currentDay, daysInMonth)
        val budgetStatus = when {
            totalSpent >= spendingLimit -> BudgetStatus.AT_RISK
            totalSpent > spendingLimit * 0.8 -> BudgetStatus.NEEDS_ATTENTION
            else -> BudgetStatus.ON_TRACK
        }

        // 2. Daily Safe Limit & Plan
        val remainingBudget = (spendingLimit - totalSpent).coerceAtLeast(0.0)
        val dailySafeLimit = if (daysRemaining > 0) remainingBudget / daysRemaining else 0.0
        val dailySafeMessage = if (remainingBudget > 0) {
            "Coach: You have PKR ${formatAmount(remainingBudget)} left to spend this month. That's PKR ${dailySafeLimit.toInt()} per day. 🎯"
        } else {
            "CRITICAL: Spending balance is EMPTY. Your PKR ${formatAmount(savingsGoal)} savings goal is now the only buffer left. Stop all non-essential spending! 🛑"
        }

        // 3. Smart Spending Analysis & Proactive Guidance
        val categoryTotals = currentMonthExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
        val spendingInsights = generateProactiveInsights(categoryTotals, totalSpent, monthlyIncome, spendingLimit)

        // 4. Survival Prediction
        val survivalPrediction = predictSurvival(totalSpent, monthlyIncome, savingsGoal, currentDay, daysInMonth)

        // 5. Savings Coach & Wellness
        val savingsCoach = generateSavingsTips(categoryTotals, totalSpent, remainingBudget, savingsGoal)
        val wellnessInsights = generateWellnessInsights(categoryTotals, totalSpent, monthlyIncome).toMutableList()

        // 6. Stress Detection
        val stressLevel = detectStress(currentMonthExpenses, monthlyIncome, totalSpent, spendingLimit)

        // 7. Motivation
        val motivationMessage = generateMotivation(totalSpent, spendingLimit, savingsGoal, healthScore)

        // 8. Financial Personality
        val personality = determinePersonality(currentMonthExpenses, categoryTotals, totalSpent, monthlyIncome)

        // 9. Weekly Summary & Weekend Forecast
        val weeklySummary = generateWeeklySummary(currentMonthExpenses, monthlyIncome, currentDay)
        
        // Dynamic Weekend Prediction
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) {
            wellnessInsights.add("Weekend Alert: Weekends often lead to impulse buys. Stick to your PKR ${dailySafeLimit.toInt()} daily limit! 🍿")
        }

        // 10. Personalized Challenges
        val challenges = generateSmartChallenges(categoryTotals, totalSpent, spendingLimit)

        // 11. Transaction-based velocity and streaks
        val velocity = calculateVelocity(expenses)
        val streak = calculateNoSpendStreak(expenses)
        val topMerchant = expenses.filter { it.shopName != null }
            .groupBy { it.shopName }
            .maxByOrNull { it.value.size }?.key

        return AIInsightData(
            healthScore = healthScore,
            budgetStatus = budgetStatus,
            dailySafeLimit = dailySafeLimit,
            dailySafeMessage = dailySafeMessage,
            budgetUtilization = budgetUtilization,
            spendingInsights = spendingInsights,
            survivalPrediction = survivalPrediction,
            savingsCoach = savingsCoach,
            wellnessInsights = wellnessInsights,
            stressLevel = stressLevel,
            motivationMessage = motivationMessage,
            financialPersonality = personality,
            weeklySummary = weeklySummary,
            challenges = challenges,
            dynamicNarrative = generateDynamicNarrative(healthScore, budgetStatus, personality, survivalPrediction, streak, velocity),
            spendingVelocity = velocity,
            noSpendStreak = streak,
            topMerchant = topMerchant
        )
    }

    private fun calculateVelocity(expenses: List<Expense>): Double {
        if (expenses.isEmpty()) return 0.0
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - TimeUnit.DAYS.toMillis(7)
        val twoWeeksAgo = now - TimeUnit.DAYS.toMillis(14)

        val thisWeekTotal = expenses.filter { it.date in oneWeekAgo..now }.sumOf { it.amount }
        val lastWeekTotal = expenses.filter { it.date in twoWeeksAgo..oneWeekAgo }.sumOf { it.amount }

        if (lastWeekTotal <= 0.0) {
            return if (thisWeekTotal > 0) 100.0 else 0.0
        }
        return ((thisWeekTotal - lastWeekTotal) / lastWeekTotal) * 100
    }

    private fun calculateNoSpendStreak(expenses: List<Expense>): Int {
        if (expenses.isEmpty()) return 0
        
        // Normalize dates to start of day for accurate comparison
        val sortedDates = expenses.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // If spent today, streak is 0
        if (sortedDates.contains(today)) return 0

        var streak = 0
        var checkDate = today - TimeUnit.DAYS.toMillis(1)
        
        while (checkDate > 0) {
            if (sortedDates.contains(checkDate)) {
                break
            }
            streak++
            checkDate -= TimeUnit.DAYS.toMillis(1)
            if (streak > 365) break
        }
        return streak
    }

    private fun generateDynamicNarrative(
        score: Int,
        status: BudgetStatus,
        personality: FinancialPersonality,
        prediction: SurvivalPrediction,
        streak: Int,
        velocity: Double
    ): String {
        val intro = when (personality) {
            FinancialPersonality.SAVER -> "As a natural Saver, you're doing great."
            FinancialPersonality.IMPULSIVE -> "We've noticed some quick spending habits lately."
            FinancialPersonality.GOAL_ORIENTED -> "You're laser-focused on your objectives."
            FinancialPersonality.BALANCED -> "You're maintaining a steady financial rhythm."
            FinancialPersonality.GROWTH_FOCUSED -> "You're scaling your wealth effectively."
        }

        val streakText = if (streak > 0) " You are on a $streak-day no-spend streak! 🔥" else ""
        
        val velocityText = when {
            velocity > 20 -> " Spending is up ${velocity.toInt()}% compared to last week. Time to slow down."
            velocity < -20 -> " Great job! Spending is down ${Math.abs(velocity.toInt())}% vs last week."
            else -> ""
        }

        val healthContext = when {
            score >= 90 -> "Your financial health is peak—100% optimized."
            score >= 70 -> "You're in a solid position with minor room for tuning."
            score >= 50 -> "Your budget is under pressure but manageable."
            else -> "We're in a critical spending phase right now."
        }

        val outlook = prediction.message

        return "$intro $healthContext$streakText$velocityText $outlook"
    }

    fun generateSmartAlert(
        spent: Double,
        income: Double,
        goal: Double,
        health: String
    ): Pair<String, String>? {
        val spendingLimit = income - goal
        val usage = if (spendingLimit > 0) (spent / spendingLimit * 100).toInt() else 100
        
        return when {
            spent >= income -> "Balance Depleted" to "You have spent your entire monthly income. Every PKR spent now is creating a financial hole."
            spent >= spendingLimit -> "Savings Goal Compromised" to "You have exhausted your spending budget. You are now spending your PKR ${goal.toInt()} savings goal!"
            usage >= 90 -> "Budget Critical" to "90% of your spending budget is gone. Only PKR ${formatAmount(spendingLimit - spent)} left for the month."
            usage >= 80 -> "Budget Warning" to "80% used. It's time to delay non-essential purchases."
            health == "Dangerous Spending" -> "AI Risk Detection" to "Your current spending speed will deplete your budget in ${BudgetCalculator.getRemainingDaysInMonth() / 2} days."
            else -> null
        }
    }

    // Consolidated AI Feed (Requirement 2 & 9)
    fun generateAIFeed(data: AIInsightData): List<Pair<String, String>> {
        val feed = mutableListOf<Pair<String, String>>()
        
        // 1. Critical Alerts First
        if (data.budgetStatus == BudgetStatus.AT_RISK) {
            feed.add("🚨 CRITICAL" to "Your spending has exceeded the safe budget. Stop all non-essential expenses immediately.")
        } else if (data.budgetStatus == BudgetStatus.NEEDS_ATTENTION) {
            feed.add("⚠️ WARNING" to "You have used 80% of your available budget. Time to slow down.")
        }

        // 2. Spending Velocity
        if (data.spendingVelocity > 30) {
            feed.add("📈 VELOCITY" to "Your spending rate is ${data.spendingVelocity.toInt()}% higher than last week. This pace is unsustainable.")
        } else if (data.spendingVelocity < -10) {
            feed.add("📉 PROGRESS" to "Excellent! You are spending ${Math.abs(data.spendingVelocity.toInt())}% less than last week.")
        }

        // 3. Goal Prediction
        feed.add("🎯 GOAL TRACKER" to data.survivalPrediction.message)

        // 4. Category Deep Dive
        data.spendingInsights.firstOrNull()?.let {
            feed.add("🔍 ANALYSIS" to it.message)
        }

        // 5. Savings Coach
        data.savingsCoach.randomOrNull()?.let {
            feed.add("💡 COACHING" to it)
        }

        // 6. Wellness & Stress
        if (data.stressLevel == StressLevel.HIGH) {
            feed.add("🧘 WELLNESS" to "Financial stress detected. Let's look at your biggest expenses together to find relief.")
        }

        // 7. Streak
        if (data.noSpendStreak >= 3) {
            feed.add("🔥 STREAK" to "You're on a ${data.noSpendStreak}-day no-spend streak! Your discipline is building wealth.")
        }

        return feed
    }

    private fun calculateHealthScore(spent: Double, income: Double, goal: Double, day: Int, totalDays: Int): Int {
        if (income <= 0) return 0
        val budgetLimit = income - goal
        val expectedSpendingAtThisPoint = (budgetLimit / totalDays) * day
        
        var score = 100
        if (spent > expectedSpendingAtThisPoint) {
            val penalty = (((spent - expectedSpendingAtThisPoint) / income) * 50).toInt()
            score -= penalty
        }
        if (spent > budgetLimit) score -= 20
        if (spent > income) score -= 30
        
        return score.coerceIn(0, 100)
    }

    private fun generateProactiveInsights(
        categoryTotals: Map<String, Double>,
        totalSpent: Double,
        income: Double,
        limit: Double
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()
        
        categoryTotals.forEach { (cat, amount) ->
            if (income > 0) {
                val percentageOfIncome = (amount / income) * 100
                if (percentageOfIncome > 20) {
                    insights.add(SpendingInsight(cat, "Your $cat spending has consumed ${percentageOfIncome.toInt()}% of your income. Consider a tighter limit.", "UP"))
                }
            }
        }

        if (totalSpent > limit * 0.8 && totalSpent <= limit) {
            insights.add(SpendingInsight("General", "Budget alert: You've used 80% of your safe spending pool.", "UP"))
        }

        return insights
    }

    fun isTransactionUnusual(expense: Expense, allExpenses: List<Expense>): Boolean {
        val categoryExpenses = allExpenses.filter { it.category == expense.category && it.id != expense.id }
        if (categoryExpenses.size < 3) return false
        
        val average = categoryExpenses.sumOf { it.amount } / categoryExpenses.size
        return expense.amount > average * 2.0 // Lowered threshold for better detection
    }

    private fun predictSurvival(spent: Double, income: Double, goal: Double, day: Int, totalDays: Int): SurvivalPrediction {
        val spendingLimit = (income - goal).coerceAtLeast(0.0)
        
        // Dynamic Weighted Forecast
        val burnRate = if (day > 1) spent / day else if (day == 1) spent else 0.0
        val estimatedTotal = (burnRate * totalDays) * 1.02 // 2% buffer for variance
        
        val prob = when {
            income <= 0 -> 0
            spent >= income -> 0
            spent > spendingLimit -> {
                val ratio = spent / income
                (100 - (ratio * 100)).toInt().coerceIn(0, 10)
            }
            estimatedTotal <= spendingLimit -> 98
            estimatedTotal <= spendingLimit * 1.05 -> 80
            estimatedTotal <= income -> 40
            else -> 5
        }

        val message = when {
            prob >= 90 -> {
                val surplus = (spendingLimit - estimatedTotal).coerceAtLeast(0.0)
                "On track to save an extra PKR ${formatAmount(surplus)}! 🚀"
            }
            prob >= 70 -> "Goal is safe. Keep maintaining this pace! 💎"
            prob >= 40 -> "Caution: Estimated overspend of PKR ${formatAmount(estimatedTotal - spendingLimit)}. ⚠️"
            else -> "High Risk: Savings goal PKR ${formatAmount(goal)} is being depleted. 🚩"
        }
        
        return SurvivalPrediction(prob.coerceIn(0, 100), estimatedTotal, message)
    }

    private fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "%,.0f", amount)
    }

    private fun generateSavingsTips(categoryTotals: Map<String, Double>, totalSpent: Double, remaining: Double, goal: Double): List<String> {
        val tips = mutableListOf<String>()
        val topCategory = categoryTotals.maxByOrNull { it.value }?.key
        
        if (topCategory != null && (categoryTotals[topCategory] ?: 0.0) > (totalSpent * 0.3)) {
            val saving = ((categoryTotals[topCategory] ?: 0.0) * 0.1).toInt()
            tips.add(listOf(
                "Strategic Save: Reducing $topCategory by just 10% adds PKR $saving to your future. 🚀",
                "Opportunity: A small adjustment in $topCategory today means PKR $saving more in your goal! 💡",
                "Goal Accelerator: Trimming $topCategory could fast-track your wealth by PKR $saving this month."
            ).random())
        }
        
        if (remaining > 5000) {
            tips.add(listOf(
                "Bonus: You have PKR ${remaining.toInt()} surplus. Moving some to your goal now will make you feel great! 🚀",
                "Growth: Your PKR ${remaining.toInt()} buffer is a sign of mastery. Why not invest a portion?",
                "Wealth Move: PKR ${remaining.toInt()} safe balance detected. Your future self is smiling! ✨"
            ).random())
        } else if (remaining < 0 && (totalSpent < (remaining + totalSpent + goal))) {
            tips.add(listOf(
                "Tip: Avoid small 'impulse' buys this week to help protect your PKR ${goal.toInt()} savings goal.",
                "Tactical Pause: Every PKR 100 saved now protects your main objective. 💪",
                "Shield your goal: Stay strong against non-essential spending for the next 48 hours."
            ).random())
        }
        
        return tips
    }

    private fun generateWellnessInsights(categoryTotals: Map<String, Double>, totalSpent: Double, income: Double): List<String> {
        val insights = mutableListOf<String>()
        val food = categoryTotals["Food"] ?: 0.0
        val entertainment = categoryTotals["Entertainment"] ?: 0.0
        
        if (food > income * 0.25) {
            insights.add(listOf(
                "Wellness: Frequent outside meals detected. Cooking at home is better for your health and your wallet! 🍎",
                "Health First: Nutritious home-cooked meals boost your energy and your savings.",
                "Vitality Tip: Use your kitchen more this week. Your body and budget will thank you."
            ).random())
        }
        
        if (entertainment > income * 0.15) {
            insights.add(listOf(
                "Balance: You're spending a lot on fun! Maybe a free walk in the park could be just as refreshing today? 🌳",
                "Mindfulness: True happiness doesn't always have a price tag. Enjoy a sunset today for PKR 0.",
                "Mental Freshness: A digital detox or a quiet book is free and great for mental clarity."
            ).random())
        }

        if (totalSpent > income * 0.9) {
            insights.add(listOf(
                "Breathe: Financial pressure can be tough. Take 5 minutes for deep breathing today. We'll fix this together! 🧘",
                "Support: You're more than your numbers. Take a moment to relax and refocus.",
                "Stress Relief: Remember to prioritize self-care. A calm mind manages money better."
            ).random())
        }

        return insights
    }

    private fun detectStress(expenses: List<Expense>, income: Double, totalSpent: Double, limit: Double): StressLevel {
        val recentSpikes = expenses.takeLast(3).any { it.amount > income * 0.15 }
        return when {
            totalSpent > income * 0.95 || (totalSpent > limit && recentSpikes) -> StressLevel.HIGH
            totalSpent > limit || recentSpikes -> StressLevel.MODERATE
            else -> StressLevel.LOW
        }
    }

    private fun generateMotivation(spent: Double, limit: Double, goal: Double, score: Int): String {
        val highMotivations = listOf(
            "🌟 Wow! Your self-control is inspiring. You're building a great future!",
            "💎 Diamond Hands! Your discipline is setting you up for true wealth.",
            "🚀 Exceptional growth! You're mastering your money with every choice."
        )
        val goodMotivations = listOf(
            "👏 You're handling your money like a boss! Stay consistent.",
            "📈 Positive momentum! You are consistently aligned with your objectives.",
            "✨ Great job! You're proving that smart habits lead to big wins."
        )
        val cautionMotivations = listOf(
            "🧘 Doing well, but let's stay mindful. Your peace of mind is worth more than a quick purchase.",
            "⚖️ Balance is key. Let's prioritize your PKR ${goal.toInt()} goal for a few days.",
            "💡 Strategic pause: Before the next spend, think about your long-term dream."
        )
        val recoveryMotivations = listOf(
            "❤️ Every month is a fresh start. Don't worry about the past, let's plan for a better tomorrow together!",
            "🌱 Growth happens through awareness. Let's focus on small wins today.",
            "💪 You've got the power to turn this around. I'm with you all the way!"
        )

        return when {
            score >= 90 -> highMotivations.random()
            score >= 70 -> goodMotivations.random()
            spent < limit -> cautionMotivations.random()
            else -> recoveryMotivations.random()
        }
    }

    private fun determinePersonality(expenses: List<Expense>, categories: Map<String, Double>, totalSpent: Double, income: Double): FinancialPersonality {
        val shopping = categories["Shopping"] ?: 0.0
        val entertainment = categories["Entertainment"] ?: 0.0
        return when {
            totalSpent < income * 0.4 -> FinancialPersonality.SAVER
            (shopping + entertainment) > totalSpent * 0.4 -> FinancialPersonality.IMPULSIVE
            totalSpent > income * 0.8 -> FinancialPersonality.GOAL_ORIENTED // Assuming high spend might be goal related if not impulsive
            else -> FinancialPersonality.BALANCED
        }
    }

    private fun generateWeeklySummary(expenses: List<Expense>, income: Double, day: Int): WeeklySummary {
        val totalSpent = expenses.sumOf { it.amount }
        val topCategory = expenses.groupBy { it.category }.maxByOrNull { it.value.sumOf { e -> e.amount } }?.key ?: "None"
        
        return WeeklySummary(
            totalIncome = income,
            totalExpenses = totalSpent,
            savingsAchieved = (income - totalSpent).coerceAtLeast(0.0),
            topCategory = topCategory,
            wins = if (totalSpent < (income / 30 * day)) listOf("Staying under daily average") else emptyList(),
            risks = if (totalSpent > (income * 0.8)) listOf("Low end-of-month liquidity") else emptyList(),
            actionPlan = listOf("Review $topCategory spending", "Aim for 2 no-spend days")
        )
    }

    private fun generateSmartChallenges(categoryTotals: Map<String, Double>, totalSpent: Double, limit: Double): List<SmartChallenge> {
        val challenges = mutableListOf<SmartChallenge>()
        val topCategory = categoryTotals.maxByOrNull { it.value }
        
        if (topCategory != null && topCategory.value > limit * 0.2) {
            val target = (topCategory.value * 0.8).toInt()
            challenges.add(SmartChallenge(
                "Category Cutback", 
                "Keep ${topCategory.key} spending below PKR $target this month", 
                ((limit / (topCategory.value + 1)) * 100).toInt().coerceIn(0, 100), 
                false
            ))
        }
        
        if (totalSpent > limit * 0.7) {
            challenges.add(SmartChallenge("Budget Survival", "Spend less than PKR 500 daily for the next 3 days", 0, false))
        } else {
            val dailyLimit = if (limit > totalSpent) (limit - totalSpent) / 30 else 1000.0
            challenges.add(SmartChallenge("Efficiency Master", "Stay under PKR ${dailyLimit.toInt()} daily for a week", 1, false))
        }
        
        return challenges
    }

    // Legacy support - Now enhanced for the Dashboard Mini Card
    fun generateInsights(expenses: List<Expense>, monthlyIncome: Double, savingsGoal: Double): List<String> {
        val data = generateAdvancedInsights(expenses, monthlyIncome, savingsGoal)
        val list = mutableListOf<String>()
        
        // Pick the most urgent message for the top of the list
        val survival = data.survivalPrediction.message
        val urgentSpending = data.spendingInsights.firstOrNull()?.message
        val coachTip = data.savingsCoach.randomOrNull()
        val wellness = data.wellnessInsights.randomOrNull()
        
        // Priority order: Survival > Urgent Spending > Coaching > Motivation
        if (data.survivalPrediction.probability < 50) {
            list.add("⚠️ $survival")
        } else if (urgentSpending != null) {
            list.add("📊 $urgentSpending")
        } else if (coachTip != null) {
            list.add("💡 $coachTip")
        } else if (wellness != null) {
            list.add("🌱 $wellness")
        } else {
            // Add a 30% chance of a joke if everything is fine
            if (Random().nextInt(10) < 3) {
                list.add("😄 Why did the man put his money in the freezer? He wanted cold hard cash!")
            } else {
                list.add("✨ ${data.motivationMessage}")
            }
        }

        // Add the rest for expanded views
        list.add("Health Score: ${data.healthScore}%")
        list.add(survival)
        list.addAll(data.savingsCoach)
        list.addAll(data.wellnessInsights)

        return list
    }
}
