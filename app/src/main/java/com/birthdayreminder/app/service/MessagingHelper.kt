package com.birthdayreminder.app.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import com.birthdayreminder.app.data.model.Person
import com.birthdayreminder.app.data.model.SocialPlatform
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object MessagingHelper {

    private const val TAG = "MessagingHelper"

    /**
     * Executes the appropriate messaging action based on person's configuration.
     */
    fun triggerSend(context: Context, person: Person, messageOverride: String? = null): Boolean {
        val message = (messageOverride ?: person.customMessage).trim()
        val phoneOrHandle = person.phoneNumber.trim()

        return when (person.socialPlatform) {
            SocialPlatform.WHATSAPP -> {
                sendViaWhatsApp(context, phoneOrHandle, message)
            }
            SocialPlatform.SMS -> {
                if (person.autoSendSmsDirectly && hasSmsPermission(context)) {
                    val directSent = sendDirectSms(context, phoneOrHandle, message)
                    if (!directSent) {
                        openSmsApp(context, phoneOrHandle, message)
                    } else {
                        Toast.makeText(context, "SMS sent to ${person.name}!", Toast.LENGTH_SHORT).show()
                        true
                    }
                } else {
                    openSmsApp(context, phoneOrHandle, message)
                }
            }
            SocialPlatform.TELEGRAM -> {
                sendViaTelegram(context, phoneOrHandle, message)
            }
            SocialPlatform.INSTAGRAM -> {
                sendViaInstagram(context, phoneOrHandle, message)
            }
        }
    }

    /**
     * Opens Instagram Direct Message chat with the specified handle.
     * Automatically copies the birthday wish to the clipboard so the user can easily paste it.
     */
    fun sendViaInstagram(context: Context, rawHandle: String, message: String): Boolean {
        return try {
            val handle = rawHandle.replace("@", "").trim()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Birthday Wish", message)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "Wish copied to clipboard! Paste it in the Instagram chat.", Toast.LENGTH_LONG).show()

            val uri = Uri.parse("https://ig.me/m/$handle")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val pm = context.packageManager
            val instaAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse("instagram://user?username=$handle"))
            if (instaAppIntent.resolveActivity(pm) != null) {
                intent.setPackage("com.instagram.android")
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Instagram: ${e.message}", e)
            openShareSheet(context, message)
        }
    }

    /**
     * Sends SMS silently in the background using SmsManager.
     * Requires Manifest.permission.SEND_SMS.
     */
    fun sendDirectSms(context: Context, rawPhone: String, message: String): Boolean {
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val cleanedPhone = cleanPhoneNumber(rawPhone)
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(cleanedPhone, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(cleanedPhone, null, message, null, null)
            }
            Log.d(TAG, "Direct SMS successfully queued for $cleanedPhone")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Direct SMS failed: ${e.message}", e)
            false
        }
    }

    /**
     * Opens WhatsApp with recipient and prefilled message.
     */
    fun sendViaWhatsApp(context: Context, rawPhone: String, message: String): Boolean {
        return try {
            val cleanedPhone = cleanPhoneNumber(rawPhone)
            val encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanedPhone&text=$encodedMsg")

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Check if WhatsApp is installed
            val pm = context.packageManager
            val whatsappIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp")
            if (whatsappIntent.resolveActivity(pm) != null) {
                intent.setPackage("com.whatsapp")
            } else {
                val whatsappBizIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp.w4b")
                if (whatsappBizIntent.resolveActivity(pm) != null) {
                    intent.setPackage("com.whatsapp.w4b")
                }
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WhatsApp: ${e.message}", e)
            // Fallback to regular SMS
            Toast.makeText(context, "WhatsApp not available, falling back to SMS", Toast.LENGTH_SHORT).show()
            openSmsApp(context, rawPhone, message)
        }
    }

    /**
     * Opens Telegram with username or share intent.
     */
    fun sendViaTelegram(context: Context, rawRecipient: String, message: String): Boolean {
        return try {
            val handle = rawRecipient.replace("@", "").trim()
            val uri = if (handle.isNotEmpty() && !handle.all { it.isDigit() }) {
                Uri.parse("https://t.me/$handle")
            } else {
                val encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
                Uri.parse("tg://msg?text=$encodedMsg")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val pm = context.packageManager
            val telegramIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("org.telegram.messenger")
            if (telegramIntent.resolveActivity(pm) != null) {
                intent.setPackage("org.telegram.messenger")
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Telegram: ${e.message}", e)
            // Fallback to share sheet
            openShareSheet(context, message)
        }
    }

    /**
     * Opens default SMS app with prefilled text and recipient.
     */
    fun openSmsApp(context: Context, rawPhone: String, message: String): Boolean {
        return try {
            val cleanedPhone = cleanPhoneNumber(rawPhone)
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanedPhone")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open SMS app: ${e.message}", e)
            openShareSheet(context, message)
        }
    }

    /**
     * Generic share sheet fallback.
     */
    fun openShareSheet(context: Context, message: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, "Send Birthday Wish via").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Share sheet failed: ${e.message}")
            false
        }
    }

    private fun cleanPhoneNumber(phone: String): String {
        return phone.replace("[^0-9+]".toRegex(), "")
    }

    fun hasSmsPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }
}
