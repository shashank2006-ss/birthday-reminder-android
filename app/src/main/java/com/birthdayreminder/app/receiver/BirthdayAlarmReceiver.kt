package com.birthdayreminder.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.birthdayreminder.app.data.model.SocialPlatform
import com.birthdayreminder.app.data.repository.BirthdayRepository
import com.birthdayreminder.app.service.AlarmScheduler
import com.birthdayreminder.app.service.MessagingHelper
import com.birthdayreminder.app.service.NotificationHelper
import java.util.Calendar

class BirthdayAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val personId = intent.getStringExtra(EXTRA_PERSON_ID) ?: return
        val repository = BirthdayRepository.getInstance(context)
        val person = repository.getById(personId)

        if (person == null) {
            Log.w(TAG, "No person found for id: $personId")
            return
        }

        when (action) {
            ACTION_BIRTHDAY_ALARM -> {
                Log.d(TAG, "Birthday alarm triggered for ${person.name}")
                if (!person.isEnabled) return

                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                // Check for automated direct background SMS
                if (person.socialPlatform == SocialPlatform.SMS &&
                    person.autoSendSmsDirectly &&
                    MessagingHelper.hasSmsPermission(context)
                ) {
                    val sent = MessagingHelper.sendDirectSms(context, person.phoneNumber, person.customMessage)
                    if (sent) {
                        NotificationHelper.showAutoSentNotification(context, person)
                    } else {
                        // Fallback to rich notification if background sending failed
                        NotificationHelper.showBirthdayNotification(context, person)
                    }
                } else {
                    // Standard flow: Show notification with Send / Edit actions
                    NotificationHelper.showBirthdayNotification(context, person)
                }

                repository.updateLastSentYear(person.id, currentYear)
                // Reschedule for next year automatically
                AlarmScheduler.scheduleBirthdayAlarm(context, person)
            }

            ACTION_SEND_NOW -> {
                Log.d(TAG, "Quick Send action clicked for ${person.name}")
                NotificationHelper.dismissNotification(context, person.id)
                MessagingHelper.triggerSend(context, person)
            }
        }
    }

    companion object {
        const val TAG = "BirthdayAlarmReceiver"
        const val ACTION_BIRTHDAY_ALARM = "com.birthdayreminder.app.ACTION_BIRTHDAY_ALARM"
        const val ACTION_SEND_NOW = "com.birthdayreminder.app.ACTION_SEND_NOW"
        const val EXTRA_PERSON_ID = "extra_person_id"
    }
}
