package com.birthdayreminder.app.data.model

enum class SocialPlatform(
    val displayName: String,
    val description: String
) {
    WHATSAPP("WhatsApp", "Opens WhatsApp with pre-filled message (1-tap send)"),
    SMS("SMS", "Direct background SMS or SMS app"),
    TELEGRAM("Telegram", "Opens Telegram chat directly via handle or phone"),
    INSTAGRAM("Instagram", "Opens Instagram DM directly via handle")
}
