package com.example.sharkfin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class BillReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val db = FirebaseFirestore.getInstance()

        try {
            val snapshot = db.collection("users").document(uid).collection("bills").get().await()
            val bills = snapshot.toObjects(Bill::class.java)
            
            val today = Calendar.getInstance()
            val todayDay = today.get(Calendar.DAY_OF_MONTH)

            bills.filter { !it.isPaid }.forEach { bill ->
                // Simple logic: if the bill's day of month is coming up within 14 days
                val daysUntil = bill.dayOfMonth - todayDay
                
                // If daysUntil is negative, it might be for next month, 
                // but for this simple worker we fire if it's within the next 14 days of the calendar cycle.
                if (daysUntil in 0..14) {
                    showNotification(bill)
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    private fun showNotification(bill: Bill) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "bill_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Bill Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Bill Due Soon")
            .setContentText("${bill.name} is due on the ${ordinal(bill.dayOfMonth)} of the month.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(bill.id.hashCode(), notification)
    }
    
    private fun ordinal(n: Int): String {
        val suffix = when {
            n in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else        -> "th"
        }
        return "$n$suffix"
    }
}
