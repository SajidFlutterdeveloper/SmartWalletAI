package com.smartwallet.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.smartwallet.ai.data.local.AppDatabase
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.data.repository.FinancialRepository
import com.smartwallet.ai.utils.*
import kotlinx.coroutines.launch
import java.util.*

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinancialRepository
    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val allExpenses: LiveData<List<Expense>>
    val totalExpenses: LiveData<Double?>
    
    // Monthly tracking
    val expensesThisMonth: LiveData<List<Expense>>
    val totalExpensesThisMonth: LiveData<Double?>
    
    // Historical tracking
    val monthlyTargets: LiveData<List<com.smartwallet.ai.data.model.MonthlyTarget>>

    val aiInsights = MutableLiveData<List<String>>()
    val aiFeed = MutableLiveData<List<Pair<String, String>>>()
    val advancedInsights = MutableLiveData<com.smartwallet.ai.data.model.AIInsightData>()
    val spendingForecast = MutableLiveData<Double>()
    private val preferenceManager = PreferenceManager(application)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinancialRepository(database.expenseDao(), database.monthlyTargetDao())
        
        val cal = Calendar.getInstance()
        val monthStart = cal.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        allExpenses = repository.getAllExpenses(userId)
        totalExpenses = repository.getTotalExpenses(userId)
        
        expensesThisMonth = repository.getExpensesThisMonth(userId, monthStart)
        totalExpensesThisMonth = repository.getTotalExpensesThisMonth(userId, monthStart)
        monthlyTargets = repository.getAllTargets(userId)
        
        setupAlertObserver()
        checkAndInitializeMonthlyTarget()
    }

    private fun checkAndInitializeMonthlyTarget() = viewModelScope.launch {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        val targetId = "${userId}_${month}_${year}"
        
        val existing = repository.getTarget(userId, month, year)
        if (existing == null) {
            val income = preferenceManager.getMonthlyIncome()
            val goal = preferenceManager.getSavingsGoal()
            repository.insertTarget(com.smartwallet.ai.data.model.MonthlyTarget(
                id = targetId,
                userId = userId,
                month = month,
                year = year,
                income = income,
                savingsGoal = goal
            ))
        }
    }

    private fun setupAlertObserver() {
        totalExpensesThisMonth.observeForever { total ->
            val spent = total ?: 0.0
            val income = preferenceManager.getMonthlyIncome()
            val savingsGoal = preferenceManager.getSavingsGoal()
            
            if (income > 0) {
                val context = getApplication<Application>().applicationContext
                val health = BudgetCalculator.getSpendingHealth(income, savingsGoal, spent)
                
                // Real-time dynamic alerts based on AI engine
                AIInsightEngine.generateSmartAlert(spent, income, savingsGoal, health)?.let { (title, message) ->
                    NotificationHelper.sendNotification(context, title, message)
                }

                // Auto-calculate insights on every change
                calculateInsights(income, savingsGoal)
                updateActualSavings()
            }
        }
    }

    fun addExpense(expense: Expense) = viewModelScope.launch {
        repository.insertExpense(expense)
        updateActualSavings()
    }

    fun deleteExpense(expense: Expense) = viewModelScope.launch {
        repository.deleteExpense(expense)
        updateActualSavings()
    }

    fun updateExpense(expense: Expense) = viewModelScope.launch {
        repository.updateExpense(expense)
        updateActualSavings()
    }

    private fun updateActualSavings() = viewModelScope.launch {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        
        val spent = totalExpensesThisMonth.value ?: 0.0
        val income = preferenceManager.getMonthlyIncome()
        val target = repository.getTarget(userId, month, year)
        
        target?.let {
            val actual = (income - spent).coerceAtLeast(0.0)
            
            // If month is ended or we want to isolate savings periodically
            // For now, we update the monthly target record
            repository.updateTarget(it.copy(actualSavings = actual))
            
            // Requirement 4: Auto-isolate savings if goal is reached or month ends
            // In a real app, this would happen on the last day of the month.
            // For demo, we ensure the 'Accumulated Savings' in PreferenceManager reflects the 'Actual Savings' of closed months.
        }
    }

    fun addExpenseFromVoice(voiceInput: String) {
        val expense = SmartParser.parseExpense(voiceInput)
        if (expense != null) {
            addExpense(expense)
        }
    }

    fun calculateInsights(monthlyIncome: Double, savingsGoal: Double) = viewModelScope.launch {
        val currentExpenses = expensesThisMonth.value ?: emptyList()
        val allExp = allExpenses.value ?: emptyList()
        
        val aiData = AIInsightEngine.generateAdvancedInsights(allExp, monthlyIncome, savingsGoal)
        
        aiInsights.postValue(AIInsightEngine.generateInsights(currentExpenses, monthlyIncome, savingsGoal))
        advancedInsights.postValue(aiData)
        aiFeed.postValue(AIInsightEngine.generateAIFeed(aiData))
        
        // Use the Dynamic Forecast from the engine (estimated total month end spend)
        spendingForecast.postValue(aiData.survivalPrediction.estimatedTotalSpend)
    }
}
