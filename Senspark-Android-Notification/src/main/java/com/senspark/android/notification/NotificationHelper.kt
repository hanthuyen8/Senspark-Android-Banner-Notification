package com.senspark.android.notification

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.emoji2.text.EmojiCompat
import org.json.JSONObject

class NotificationHelper(context: Context) : ContextWrapper(context) {
    companion object {
        const val kCHANNEL_GENERAL_NOTIFICATIONS = "general_notifications"
        const val kNOTIFICATION_EXTRA_DATA = "notificationData"
        const val kNOTIFICATION_ID = "notificationId"

        @JvmStatic
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create Channel if SDK is Oreo (API 26 - Android 8.0) or higher
                val channel = NotificationChannel(
                    kCHANNEL_GENERAL_NOTIFICATIONS,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var _unityActivity: Activity? = null

    fun init(enableLog: Boolean, unityActivity: Activity) {
        _unityActivity = unityActivity
        Logger(enableLog)
        createNotificationChannel(unityActivity)
    }

    fun unitySchedule(
        notificationId: Int,
        title: String,
        body: String,
        extraData: String?,
        delaySeconds: Long,
        repeatSeconds: Long
    ) {
        val notification =
            createNotification(_unityActivity!!, notificationId, title, body, extraData)
        if (delaySeconds > 0) {
            AlarmReceiver.schedule(
                notificationId,
                notification,
                delaySeconds,
                repeatSeconds,
                _unityActivity!!
            )
        } else {
            notificationManager.notify(notificationId, notification)
        }
    }

    fun unitySchedule(
        notificationId: Int,
        title: String,
        body: String,
        extraData: String?,
        atHour: Int,
        atMinute: Int,
        repeatDays: Int
    ) {
        val notification =
            createNotification(_unityActivity!!, notificationId, title, body, extraData)
        AlarmReceiver.schedule(
            notificationId,
            notification,
            atHour,
            atMinute,
            repeatDays,
            _unityActivity!!
        )
    }

    fun cocosCreateNotificationBuilder(body: String): NotificationCompat.Builder {
        val customLayout = createCustomLayoutNotification(getString(R.string.app_name), body)
        customLayout.setImageViewResource(R.id.notification_launcher_icon, R.mipmap.ic_launcher)
        return createNotificationBuilder(kCHANNEL_GENERAL_NOTIFICATIONS, customLayout)
    }

    // Lấy ra extraData của notification gần nhất
    fun getIntentExtraData(): String {
        if (_unityActivity == null) return ""
        val intent = _unityActivity!!.intent
        val id = intent.getIntExtra(kNOTIFICATION_ID, -1)
        if (id >= 0) {
            notificationManager.cancel(id)
        }
        val jsonObject = JSONObject();
        jsonObject.put("id", id)
        jsonObject.put("extraData", intent.getStringExtra(kNOTIFICATION_EXTRA_DATA))
        return jsonObject.toString()
    }

    private fun createNotification(
        activity: Activity,
        notificationId: Int,
        title: String,
        body: String,
        extraData: String?
    ): Notification {
        val customLayout = createCustomLayoutNotification(title, body)
        customLayout.setImageViewResource(R.id.notification_launcher_icon, R.mipmap.app_icon)

        val builder = createNotificationBuilder(kCHANNEL_GENERAL_NOTIFICATIONS, customLayout)
        val clickIntent = createExtrasClickIntent(activity, notificationId, extraData)
        builder.setContentIntent(clickIntent)

        return builder.build()
    }

    private fun createNotificationBuilder(
        channelId: String,
        layout: RemoteViews
    ): NotificationCompat.Builder {
        val value = TypedValue()
        applicationContext.theme.resolveAttribute(
            androidx.appcompat.R.attr.colorPrimary,
            value,
            true
        )
        return NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(value.data)
            .setCustomContentView(layout)
    }

    private fun createCustomLayoutNotification(
        title: String,
        body: String
    ): RemoteViews {
        val customLayoutId = R.layout.custom_notification_layout
        val customLayout = RemoteViews(packageName, customLayoutId)

        // Set text
        customLayout.setTextViewText(
            R.id.notification_title,
            EmojiCompat.get().process(unescapeString(title))
        )

        customLayout.setTextViewText(
            R.id.notification_body,
            EmojiCompat.get().process(unescapeString(body))
        )

        return customLayout;
    }

    private fun createExtrasClickIntent(
        activity: Activity,
        notificationId: Int,
        extraData: String?
    ): PendingIntent {
        val it = Intent(activity, activity.javaClass)
        it.putExtra(kNOTIFICATION_ID, notificationId)
        if (extraData != null) {
            it.putExtra(kNOTIFICATION_EXTRA_DATA, extraData)
        }
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

        return PendingIntent.getActivity(
            activity,
            notificationId,
            it,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun unescapeString(escaped: String): String? {
        var es = escaped
        if (es.indexOf("\\u") == -1) return es
        var processed = ""
        var position = es.indexOf("\\u")
        while (position != -1) {
            if (position != 0) processed += es.substring(0, position)
            val token = es.substring(position + 2, position + 6)
            es = es.substring(position + 6)
            processed += token.toInt(16).toChar()
            position = es.indexOf("\\u")
        }
        processed += es
        return processed
    }
}