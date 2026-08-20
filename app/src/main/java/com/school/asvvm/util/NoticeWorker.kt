package com.school.asvvm.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.school.asvvm.MainActivity
import com.school.asvvm.R
import kotlinx.coroutines.tasks.await

class NoticeWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            
            // Get the last checked timestamp from SharedPreferences
            val prefs = context.getSharedPreferences("NoticeWorkerPrefs", Context.MODE_PRIVATE)
            val lastCheckTime = prefs.getLong("last_notice_check", System.currentTimeMillis())

            // Query for notices newer than lastCheckTime
            val snapshot = firestore.collection("notices")
                .whereGreaterThan("timestamp", lastCheckTime)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                // We have new notices! Show a notification
                val firstNotice = snapshot.documents.first()
                val title = firstNotice.getString("title") ?: "New Announcement"
                val message = firstNotice.getString("message") ?: "Check the app for details."
                
                showNotification(title, message)
            }

            // Update the last checked timestamp
            prefs.edit().putLong("last_notice_check", System.currentTimeMillis()).apply()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "school_notices_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "School Notices"
            val descriptionText = "Notifications for new school announcements"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(context, channelId)
            // Use standard Android icon since we don't know the app's specific drawable
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                // Notification ID is current time to prevent overwriting
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }
    }
}
