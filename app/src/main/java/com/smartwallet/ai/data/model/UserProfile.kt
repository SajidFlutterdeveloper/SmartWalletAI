package com.smartwallet.ai.data.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val mobile: String = "",
    val monthlyIncome: Double = 0.0,
    val savingsGoal: Double = 0.0,
    val salaryDate: Int = 1,
    val currency: String = "PKR",
    val profilePicUrl: String? = null,
    val profileCompleted: Boolean = false,
    val lastSync: Long = System.currentTimeMillis()
)
