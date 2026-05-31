package com.smartwallet.ai.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferenceManager = PreferenceManager(context)
        
        // Only notify if profile is completed and it's time for a reminder
        if (preferenceManager.isProfileCompleted()) {
            NotificationHelper.sendNotification(
                context,
                "Daily Expense Reminder 📝",
                "Did you track all your expenses today? AI is waiting to analyze your savings!"
            )
        }
    }
}
