package com.smartwallet.ai.data.model

data class AIInsightData(
    val healthScore: Int,
    val budgetStatus: BudgetStatus,
    val dailySafeLimit: Double,
    val dailySafeMessage: String = "",
    val budgetUtilization: Int,
    val spendingInsights: List<SpendingInsight>,
    val survivalPrediction: SurvivalPrediction,
    val savingsCoach: List<String>,
    val wellnessInsights: List<String>,
    val stressLevel: StressLevel,
    val motivationMessage: String,
    val financialPersonality: FinancialPersonality,
    val weeklySummary: WeeklySummary?,
    val challenges: List<SmartChallenge>
)

enum class BudgetStatus { ON_TRACK, NEEDS_ATTENTION, AT_RISK }
enum class StressLevel { LOW, MODERATE, HIGH }
enum class FinancialPersonality { SAVER, GOAL_ORIENTED, IMPULSIVE, BALANCED, GROWTH_FOCUSED }

data class SpendingInsight(
    val category: String,
    val message: String,
    val trend: String // "UP", "DOWN", "STABLE"
)

data class SurvivalPrediction(
    val probability: Int, // 0-100%
    val estimatedRemainingBalance: Double,
    val message: String
)

data class WeeklySummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val savingsAchieved: Double,
    val topCategory: String,
    val wins: List<String>,
    val risks: List<String>,
    val actionPlan: List<String>
)

data class SmartChallenge(
    val title: String,
    val description: String,
    val progress: Int,
    val isCompleted: Boolean
)
