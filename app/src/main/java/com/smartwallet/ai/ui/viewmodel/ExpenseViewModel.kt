package com.smartwallet.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.smartwallet.ai.data.local.AppDatabase
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.data.repository.ExpenseRepository
import com.smartwallet.ai.utils.BudgetCalculator
import com.smartwallet.ai.utils.NotificationHelper
import com.smartwallet.ai.utils.PreferenceManager
import com.smartwallet.ai.utils.SmartParser
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    val allExpenses: LiveData<List<Expense>>
    val totalExpenses: LiveData<Double?>
    val aiInsights = MutableLiveData<List<String>>()
    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val preferenceManager = PreferenceManager(application)

    init {
        val expenseDao = AppDatabase.getDatabase(application).expenseDao()
        repository = ExpenseRepository(expenseDao)
        allExpenses = repository.getAllExpenses(userId)
        totalExpenses = repository.getTotalExpenses(userId)
        
        setupAlertObserver()
    }

    private fun setupAlertObserver() {
        totalExpenses.observeForever { total ->
            val spent = total ?: 0.0
            val income = preferenceManager.getMonthlyIncome()
            val savingsGoal = preferenceManager.getSavingsGoal()
            
            if (income > 0) {
                val context = getApplication<Application>().applicationContext
                val usagePercent = BudgetCalculator.getBudgetUsagePercentage(income, spent)
                val health = BudgetCalculator.getSpendingHealth(income, savingsGoal, spent)
                
                // Real-time alerts based on spending behavior
                when {
                    usagePercent >= 100 -> NotificationHelper.sendNotification(context, "Budget Alert!", "You have used 100% of your budget. Immediate control required.")
                    usagePercent >= 80 -> NotificationHelper.sendNotification(context, "Budget Warning!", "You have used 80% of your budget. Slow down your spending.")
                    usagePercent >= 60 -> NotificationHelper.sendNotification(context, "Spending Update", "You have used 60% of your budget for this month.")
                }
                
                if (health == "Dangerous Spending") {
                    NotificationHelper.sendNotification(context, "AI Wellness Alert", "At this speed, your balance may finish before month-end. Try reducing non-essential expenses.")
                }
            }
        }
    }

    fun addExpense(expense: Expense) = viewModelScope.launch {
        repository.insert(expense)
    }

    fun deleteExpense(expense: Expense) = viewModelScope.launch {
        repository.delete(expense)
    }

    fun updateExpense(expense: Expense) = viewModelScope.launch {
        repository.update(expense)
    }

    fun addExpenseFromVoice(voiceInput: String) {
        val expense = SmartParser.parseExpense(voiceInput)
        if (expense != null) {
            addExpense(expense)
        }
    }

    fun calculateInsights(monthlyIncome: Double, savingsGoal: Double) {
        val currentExpenses = allExpenses.value ?: emptyList()
        aiInsights.value = com.smartwallet.ai.utils.AIInsightEngine.generateInsights(currentExpenses, monthlyIncome, savingsGoal)
    }
}
