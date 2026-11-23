package com.birthdayreminder.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.birthdayreminder.app.R
import com.birthdayreminder.app.data.model.Person
import com.birthdayreminder.app.receiver.BirthdayAlarmReceiver
import com.birthdayreminder.app.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "birthday_reminders_channel"
    private const val CHANNEL_NAME = "Birthday Reminders"
    private const val CHANNEL_DESC = "Notifications and automated action triggers for birthdays"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBirthdayNotification(context: Context, person: Person) {
        createNotificationChannel(context)

        val notificationId = person.id.hashCode()

        // Content Intent: Open MainActivity with Edit/View Dialog
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_PERSON
            putExtra(MainActivity.EXTRA_PERSON_ID, person.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: "Send Now"
        val sendNowIntent = Intent(context, BirthdayAlarmReceiver::class.java).apply {
            action = BirthdayAlarmReceiver.ACTION_SEND_NOW
            putExtra(BirthdayAlarmReceiver.EXTRA_PERSON_ID, person.id)
        }
        val sendNowPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            sendNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "Edit Message"
        val editIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_PERSON
            putExtra(MainActivity.EXTRA_PERSON_ID, person.id)
            putExtra(MainActivity.EXTRA_AUTO_EDIT, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val editPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 2,
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val turningAgeStr = person.getTurningAge()?.let { " ($it years old)" } ?: ""
        val title = "🎉 Today is ${person.name}'s Birthday!$turningAgeStr"
        val platformLabel = person.socialPlatform.displayName

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(person.customMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Message for ${person.name}:\n\"${person.customMessage}\""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "🚀 Send Now ($platformLabel)", sendNowPendingIntent)
            .addAction(android.R.drawable.ic_menu_edit, "✏️ Edit & Send", editPendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun showAutoSentNotification(context: Context, person: Person) {
        createNotificationChannel(context)
        val notificationId = person.id.hashCode() + 100

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("✅ Birthday Message Sent!")
            .setContentText("Automated SMS was delivered to ${person.name}.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    fun dismissNotification(context: Context, personId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(personId.hashCode())
    }
}
