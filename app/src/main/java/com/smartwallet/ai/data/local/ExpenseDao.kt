package com.smartwallet.ai.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.smartwallet.ai.data.model.Expense

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    fun getAllExpenses(userId: String): LiveData<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId")
    fun getTotalExpenses(userId: String): LiveData<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date >= :startTime AND date <= :endTime")
    fun getExpensesInRange(userId: String, startTime: Long, endTime: Long): LiveData<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date >= :startTime")
    fun getExpensesFrom(userId: String, startTime: Long): LiveData<Double?>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date >= :startTime ORDER BY date DESC")
    fun getExpensesFromList(userId: String, startTime: Long): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses WHERE isSynced = 0")
    suspend fun getUnsyncedExpenses(): List<Expense>

    @Query("UPDATE expenses SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
    
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?
}
