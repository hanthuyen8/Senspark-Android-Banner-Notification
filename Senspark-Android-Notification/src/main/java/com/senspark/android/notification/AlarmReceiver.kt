package com.senspark.android.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val kNOTIFICATION = "notification"
        const val kNOTIFICATION_ID = "notificationId"

        fun schedule(
            id: Int,
            notification: Notification,
            delaySeconds: Long,
            repeatSeconds: Long,
            context: Context
        ) {
            val pendingIntent = createIntent(id, notification, context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (repeatSeconds > 0) {
                Logger.getInstance().log("Set delay after ${delaySeconds}s each ${repeatSeconds}s")
                alarmManager.cancel(pendingIntent)
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + delaySeconds * 1000,
                    repeatSeconds * 1000,
                    pendingIntent
                )
            } else {
                Logger.getInstance().log("Set delay after ${delaySeconds}s once")
                alarmManager.cancel(pendingIntent)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP, delaySeconds * 1000, pendingIntent
                )
            }
        }

        fun schedule(
            id: Int,
            notification: Notification,
            atHour: Int,
            atMinute: Int,
            repeatDays: Int,
            context: Context
        ) {
            val pendingIntent = createIntent(id, notification, context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, atHour)
            calendar.set(java.util.Calendar.MINUTE, atMinute)

            // Cộng thêm 1 ngày nếu thời gian đã trôi qua
            if (java.util.Calendar.getInstance().after(calendar)) {
                calendar.add(java.util.Calendar.DATE, 1)
                Logger.getInstance().log("Set calender at ${atHour}h${atMinute}m tomorrow")
            } else {
                Logger.getInstance().log("Set calender at ${atHour}h${atMinute}m today")
            }

            alarmManager.cancel(pendingIntent)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * repeatDays,
                pendingIntent
            )
        }

        private fun createIntent(
            id: Int, notification: Notification, context: Context
        ): PendingIntent {
            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra(kNOTIFICATION, notification)
            intent.putExtra(kNOTIFICATION_ID, id)

            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

            return PendingIntent.getBroadcast(
                context,
                id,
                intent,
                flags
            )
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: Notification? = intent.getParcelableExtra(kNOTIFICATION)
        val id = intent.getIntExtra(kNOTIFICATION_ID, -1)
        if (id >= 0) {
            Logger.getInstance().log("Received Notification from AlarmManager with id: $id")
            notificationManager.notify(id, notification)
        }
    }
}