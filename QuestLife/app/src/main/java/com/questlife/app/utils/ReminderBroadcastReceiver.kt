package com.questlife.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id") ?: return
        val taskTitle = intent.getStringExtra("task_title") ?: "Напоминание"
        val taskDescription = intent.getStringExtra("task_description") ?: ""
        
        NotificationHelper.showTaskNotification(
            context = context,
            taskId = taskId,
            title = taskTitle,
            description = taskDescription
        )
    }
}
