package com.smartwallet.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String, // To support multi-user authentication
    val amount: Double,
    val category: String,
    val note: String = "",
    val shopName: String? = null, // For OCR results
    val date: Long = System.currentTimeMillis(), // Stores date and time
    val inputMethod: String = "Manual", // "Voice", "Scan", or "Manual"
    val isSynced: Boolean = false
) : Serializable
