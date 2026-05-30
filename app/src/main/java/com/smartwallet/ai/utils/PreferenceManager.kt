package com.smartwallet.ai.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveProfile(name: String, mobile: String, income: Double, savingsGoal: Double, salaryDate: Int, currency: String) {
        prefs.edit().apply {
            putString("name", name)
            putString("mobile", mobile)
            putLong("income", income.toLong())
            putLong("savings_goal", savingsGoal.toLong())
            putInt("salary_date", salaryDate)
            putString("currency", currency)
            putBoolean("profile_completed", true)
            apply()
        }
    }

    fun getUserName(): String = prefs.getString("name", "User") ?: "User"
    fun getMobileNumber(): String = prefs.getString("mobile", "") ?: ""
    fun getMonthlyIncome(): Double = prefs.getLong("income", 0L).toDouble()
    fun getSavingsGoal(): Double = prefs.getLong("savings_goal", 0L).toDouble()
    fun getSalaryDate(): Int = prefs.getInt("salary_date", 1)
    fun getCurrency(): String = prefs.getString("currency", "PKR") ?: "PKR"
    fun isProfileCompleted(): Boolean = prefs.getBoolean("profile_completed", false)

    // New Security Settings
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }
    fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", false)

    // Profile Pic
    fun setProfilePicUrl(url: String) {
        prefs.edit().putString("profile_pic_url", url).apply()
    }
    fun getProfilePicUrl(): String? = prefs.getString("profile_pic_url", null)

    fun clearData() {
        prefs.edit().clear().apply()
    }
}
