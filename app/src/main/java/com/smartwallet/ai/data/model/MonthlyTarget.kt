package com.smartwallet.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_targets")
data class MonthlyTarget(
    @PrimaryKey val id: String, // userId_MM_YYYY
    val userId: String,
    val month: Int,
    val year: Int,
    val income: Double,
    val savingsGoal: Double,
    val actualSavings: Double = 0.0,
    val isClosed: Boolean = false
)
