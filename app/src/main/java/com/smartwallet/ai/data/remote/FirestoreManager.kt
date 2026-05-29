package com.smartwallet.ai.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartwallet.ai.data.model.Expense
import kotlinx.coroutines.tasks.await

object FirestoreManager {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    suspend fun uploadExpense(expense: Expense): Boolean {
        val userId = expense.userId // Use the userId from the expense object
        
        return try {
            val expenseMap = hashMapOf(
                "amount" to expense.amount,
                "category" to expense.category,
                "note" to expense.note,
                "date" to expense.date,
                "inputMethod" to expense.inputMethod,
                "shopName" to expense.shopName,
                "userId" to expense.userId
            )
            
            db.collection("users").document(userId)
                .collection("expenses")
                .document(expense.id.toString())
                .set(expenseMap)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncAllUnsynced(unsyncedExpenses: List<Expense>, onSyncComplete: suspend (Expense) -> Unit) {
        for (expense in unsyncedExpenses) {
            if (uploadExpense(expense)) {
                onSyncComplete(expense)
            }
        }
    }
}
