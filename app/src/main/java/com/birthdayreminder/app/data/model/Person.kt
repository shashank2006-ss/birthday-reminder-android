package com.birthdayreminder.app.data.model

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class Person(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var birthYear: Int? = null,
    var birthMonth: Int = 1, // 1 to 12
    var birthDay: Int = 1,   // 1 to 31
    var reminderHour: Int = 9,   // 0 to 23
    var reminderMinute: Int = 0, // 0 to 59
    var phoneNumber: String = "", // Phone number or handle
    var socialPlatform: SocialPlatform = SocialPlatform.WHATSAPP,
    var customMessage: String = "Happy Birthday! Wishing you a fantastic day filled with joy and success! 🎉🎂",
    var autoSendSmsDirectly: Boolean = false,
    var isEnabled: Boolean = true,
    var lastSentYear: Int = 0
) {
    /**
     * Calculates the exact next Calendar instance for this person's reminder.
     * Takes into account whether the birthday/reminder time for this year has already passed.
     */
    fun getNextReminderCalendar(): Calendar {
        val now = Calendar.getInstance()
        val reminderCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, birthMonth - 1)
            set(Calendar.DAY_OF_MONTH, birthDay)
            set(Calendar.HOUR_OF_DAY, reminderHour)
            set(Calendar.MINUTE, reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If reminder time has already passed this year, schedule for next year
        if (reminderCal.before(now)) {
            reminderCal.add(Calendar.YEAR, 1)
        }
        return reminderCal
    }

    /**
     * Returns days remaining until the next birthday (0 means today!).
     */
    fun getDaysRemaining(): Int {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val bday = Calendar.getInstance().apply {
            set(Calendar.MONTH, birthMonth - 1)
            set(Calendar.DAY_OF_MONTH, birthDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (bday.before(now)) {
            bday.add(Calendar.YEAR, 1)
        }

        val diffMillis = bday.timeInMillis - now.timeInMillis
        return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * Formats Month, Day, and optional Birth Year, e.g. "Oct 14, 1995" or "Oct 14"
     */
    fun getFormattedDate(): String {
        val monthName = DateFormatSymbols.getInstance(Locale.getDefault()).shortMonths.getOrNull(birthMonth - 1) ?: "$birthMonth"
        return if (birthYear != null) "$monthName $birthDay, $birthYear" else "$monthName $birthDay"
    }

    /**
     * Formats reminder time, e.g. "09:00 AM"
     */
    fun getFormattedTime(): String {
        val hour12 = if (reminderHour == 0) 12 else if (reminderHour > 12) reminderHour - 12 else reminderHour
        val amPm = if (reminderHour >= 12) "PM" else "AM"
        val minStr = String.format(Locale.US, "%02d", reminderMinute)
        return String.format(Locale.US, "%02d:%s %s", hour12, minStr, amPm)
    }

    /**
     * Calculates upcoming age if birthYear is provided
     */
    fun getTurningAge(): Int? {
        val year = birthYear ?: return null
        val nextYear = getNextReminderCalendar().get(Calendar.YEAR)
        return nextYear - year
    }
}
