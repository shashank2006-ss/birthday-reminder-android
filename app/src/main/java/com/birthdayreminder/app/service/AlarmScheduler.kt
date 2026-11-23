package com.birthdayreminder.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.birthdayreminder.app.data.model.Person
import com.birthdayreminder.app.data.repository.BirthdayRepository
import com.birthdayreminder.app.receiver.BirthdayAlarmReceiver

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    fun scheduleBirthdayAlarm(context: Context, person: Person) {
        if (!person.isEnabled) {
            cancelBirthdayAlarm(context, person)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val targetCalendar = person.getNextReminderCalendar()
        val triggerAtMillis = targetCalendar.timeInMillis

        val intent = Intent(context, BirthdayAlarmReceiver::class.java).apply {
            action = BirthdayAlarmReceiver.ACTION_BIRTHDAY_ALARM
            putExtra(BirthdayAlarmReceiver.EXTRA_PERSON_ID, person.id)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            person.id.hashCode(),
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Allows waking up the CPU even in Doze mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        // Fallback if exact alarm permission is not granted
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for ${person.name} at ${targetCalendar.time}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule exact alarm for ${person.name}: ${e.message}")
        }
    }

    fun cancelBirthdayAlarm(context: Context, person: Person) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BirthdayAlarmReceiver::class.java).apply {
            action = BirthdayAlarmReceiver.ACTION_BIRTHDAY_ALARM
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            person.id.hashCode(),
            intent,
            flags
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled alarm for ${person.name}")
    }

    fun rescheduleAll(context: Context) {
        val repository = BirthdayRepository.getInstance(context)
        val allPeople = repository.getAll()
        for (person in allPeople) {
            scheduleBirthdayAlarm(context, person)
        }
    }
}
