package com.smartwallet.ai.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.data.model.UserProfile
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import android.net.Uri
import android.util.Log

object FirestoreManager {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    suspend fun uploadExpense(expense: Expense): Boolean {
        val userId = expense.userId
        
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

    suspend fun saveUserProfile(profile: UserProfile): Boolean {
        return try {
            withTimeout(5000) { // 5 Second timeout to prevent sticking
                db.collection("users").document(profile.uid)
                    .set(profile)
                    .await()
            }
            true
        } catch (e: Exception) {
            Log.e("Firestore", "Error saving profile: ${e.message}")
            false
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            withTimeout(5000) {
                val doc = db.collection("users").document(uid).get().await()
                doc.toObject(UserProfile::class.java)
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching profile: ${e.message}")
            null
        }
    }

    suspend fun uploadProfilePicture(uid: String, imageUri: Uri): String? {
        return try {
            val ref = storage.reference.child("profile_pics/$uid.jpg")
            ref.putFile(imageUri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
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
