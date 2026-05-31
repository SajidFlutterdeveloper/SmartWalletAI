package com.smartwallet.ai.data.repository

import androidx.lifecycle.LiveData
import com.smartwallet.ai.data.local.ExpenseDao
import com.smartwallet.ai.data.local.MonthlyTargetDao
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.data.model.MonthlyTarget

class FinancialRepository(
    private val expenseDao: ExpenseDao,
    private val monthlyTargetDao: MonthlyTargetDao
) {
    // Expense methods
    fun getAllExpenses(userId: String): LiveData<List<Expense>> = expenseDao.getAllExpenses(userId)
    fun getTotalExpenses(userId: String): LiveData<Double?> = expenseDao.getTotalExpenses(userId)
    fun getExpensesThisMonth(userId: String, startTime: Long): LiveData<List<Expense>> = 
        expenseDao.getExpensesFromList(userId, startTime)
    fun getTotalExpensesThisMonth(userId: String, startTime: Long): LiveData<Double?> = 
        expenseDao.getExpensesFrom(userId, startTime)

    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    // Monthly Target methods
    fun getAllTargets(userId: String): LiveData<List<MonthlyTarget>> = monthlyTargetDao.getAllTargets(userId)
    suspend fun getTarget(userId: String, month: Int, year: Int) = monthlyTargetDao.getTarget(userId, month, year)
    suspend fun insertTarget(target: MonthlyTarget) = monthlyTargetDao.insertTarget(target)
    suspend fun updateTarget(target: MonthlyTarget) = monthlyTargetDao.updateTarget(target)
}
