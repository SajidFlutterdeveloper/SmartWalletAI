package com.smartwallet.ai.data.repository

import androidx.lifecycle.LiveData
import com.smartwallet.ai.data.local.ExpenseDao
import com.smartwallet.ai.data.model.Expense

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun getAllExpenses(userId: String): LiveData<List<Expense>> = expenseDao.getAllExpenses(userId)

    fun getTotalExpenses(userId: String): LiveData<Double?> = expenseDao.getTotalExpenses(userId)

    fun getExpensesThisMonth(userId: String, startTime: Long): LiveData<List<Expense>> = 
        expenseDao.getExpensesFromList(userId, startTime)

    fun getTotalExpensesThisMonth(userId: String, startTime: Long): LiveData<Double?> = 
        expenseDao.getExpensesFrom(userId, startTime)

    suspend fun insert(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun update(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun delete(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun getUnsynced(): List<Expense> = expenseDao.getUnsyncedExpenses()

    suspend fun markSynced(id: Long) {
        expenseDao.markAsSynced(id)
    }
}
