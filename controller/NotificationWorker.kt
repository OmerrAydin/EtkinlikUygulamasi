package com.omeraydin.etkinlikprojesi.controller

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.omeraydin.etkinlikprojesi.R

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Hatırlatıcı"
        val message = inputData.getString("message") ?: "Bir etkinlik yaklaşıyor!"
        val notificationManager = ContextCompat.getSystemService(
            applicationContext,
            NotificationManager::class.java
        )

        if (notificationManager != null) {
            val notification = NotificationCompat.Builder(applicationContext, "hatirlatici_notification_channel")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Bildirim simgesi
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(4, notification)
        } else {
            println("NotificationManager bulunamadı.")
        }

        return Result.success()
    }
}