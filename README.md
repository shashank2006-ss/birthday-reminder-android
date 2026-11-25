# 🎂 Birthday Reminder & Auto-Wisher Android App

<div align="center">

### 📲 Quick Download:

[![Download Android APK](https://img.shields.io/badge/📥_DOWNLOAD_APK-Latest_Release-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/shashank2006-ss/birthday-reminder-android/releases/latest/download/BirthdayReminder.apk)
[![View All Releases](https://img.shields.io/badge/📦_All_Releases-GitHub-blue?style=for-the-badge&logo=github)](https://github.com/shashank2006-ss/birthday-reminder-android/releases)

### 👉 [Click Here to Download BirthdayReminder.apk directly to your phone](https://github.com/shashank2006-ss/birthday-reminder-android/releases/latest/download/BirthdayReminder.apk) 👈

*No laptop or cables required! Tap the link above on your phone browser to download and install instantly.*

---

</div>

A clean, modern Android application built with **Kotlin** and **Jetpack Compose (Material 3)**. It provides an interactive calendar, unlimited birthday management, scheduled exact-minute alarms, rich notifications, and multi-platform messaging automation for **WhatsApp**, **SMS**, **Telegram**, and **Instagram**.

---

## 🌟 Key Features

1. **Clean & Minimalist Material 3 UI**:
   - Modern cards with age calculator, countdown badges (*"Today! 🎉"*, *"Tomorrow"*, *"In 5 days"*), and platform tags.
   - Light and dark theme support with dynamic Material colors.
   - Fast instant search by name, phone number, or date.

2. **Interactive Calendar View**:
   - Monthly grid with highlighted dots for birthdays on each date.
   - Day selection reveals all people having birthdays on that day with quick-send actions.
   - Seamless month-to-month navigation.

3. **Scheduled Date & Exact Time**:
   - Pick the exact birthday (Month & Day, optional Year for age).
   - Configure the exact reminder/send time (e.g., 00:00 midnight or 09:30 AM).
   - Powered by Android's `AlarmManager.setExactAndAllowWhileIdle()` to reliably wake the phone even during Doze/sleep mode.
   - Automatically reschedules for the following year after firing.
   - Automatically restores all alarms across device reboots via `BootReceiver`.

4. **Multi-Platform Social Media Integration**:
   - **WhatsApp**: Directly opens the chat with the recipient and pre-populates your unique message for a 1-tap send.
   - **SMS**:
     - *Standard*: Opens your default SMS app with the recipient and prefilled message.
     - *Auto-Send (Headless)*: Can send SMS completely silently in the background at the exact minute without opening any app (via `SmsManager` with SMS permission).
   - **Telegram**: Directly launches the Telegram chat for the contact handle (`@username`) or shares the message.

5. **Review & Edit Before Sending**:
   - When the scheduled notification fires:
     - Tap **"🚀 Send Now"** to trigger immediate delivery.
     - Tap **"✏️ Edit & Send"** to open a live message editor, customize your wish, and then send.
   - Quick Send modal accessible directly from any person card or calendar entry.

6. **Unlimited People & Customization**:
   - Add as many contacts as you want.
   - Unique custom message per person with built-in templates (*Classic*, *Funny*, *Heartfelt*, *Short*).
   - Toggle individual reminders on/off with a single tap.

---

## 📂 Project Structure

```text
birthday-reminder-app/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Permissions (Notifications, Alarms, SMS)
│   │   ├── java/com/birthdayreminder/app/
│   │   │   ├── data/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Person.kt         # Data model with countdown & date logic
│   │   │   │   │   └── SocialPlatform.kt # WhatsApp, SMS, Telegram enums
│   │   │   │   └── repository/
│   │   │   │       └── BirthdayRepository.kt # Persistent storage & StateFlow
│   │   │   ├── receiver/
│   │   │   │   ├── BirthdayAlarmReceiver.kt # Exact alarm & notification handler
│   │   │   │   └── BootReceiver.kt          # Restores alarms on device reboot
│   │   │   ├── service/
│   │   │   │   ├── AlarmScheduler.kt        # AlarmManager scheduling
│   │   │   │   ├── MessagingHelper.kt       # Multi-platform messaging engine
│   │   │   │   └── NotificationHelper.kt    # High-priority rich notifications
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt          # Permissions & entry point
│   │   │       ├── components/
│   │   │       │   ├── AddEditPersonDialog.kt   # Dialog to add/edit contacts
│   │   │       │   ├── BirthdayCalendarView.kt  # Interactive month calendar
│   │   │       │   ├── PersonCard.kt            # Modern contact card
│   │   │       │   └── QuickSendDialog.kt       # Live message editor & sender
│   │   │       ├── screens/
│   │   │       │   └── HomeScreen.kt            # Main tabs & search
│   │   │       ├── theme/                       # Material 3 colors & typography
│   │   │       └── viewmodel/
│   │   │           └── BirthdayViewModel.kt     # State management
│   │   └── res/                             # Icons, strings, themes, styles
│   ├── build.gradle.kts                     # App-level build config
│   └── proguard-rules.pro
├── .github/workflows/
│   └── build-apk.yml                        # Cloud CI/CD workflow to build APK
├── build-apk.sh                             # Local build script
├── build.gradle.kts                         # Root Gradle config
├── settings.gradle.kts                      # Gradle settings
├── gradle.properties
└── README.md
```

---

## 🛠️ How to Build the APK

You can build the APK in any of the following 3 ways:

### Option 1: Using Android Studio (Recommended & Easiest)
1. Open **Android Studio**.
2. Select **File > Open...** and select `/Users/shashanksingh/.gemini/antigravity/scratch/birthday-reminder-app`.
3. Android Studio will automatically sync dependencies and Gradle.
4. From the top menu, click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Once built, a popup will appear at the bottom right: click **locate** to find `app-debug.apk`.

### Option 2: Cloud Build via GitHub Actions (Zero Local Setup)
If you do not want to download 10 GB of Android SDKs on your computer:
1. Initialize a git repository and push this folder to your GitHub:
   ```bash
   cd /Users/shashanksingh/.gemini/antigravity/scratch/birthday-reminder-app
   git init
   git add .
   git commit -m "Initial commit of Birthday Reminder App"
   git remote add origin https://github.com/YOUR_USERNAME/birthday-reminder.git
   git push -u origin main
   ```
2. Open your repository on GitHub and go to the **Actions** tab.
3. The **"Build Birthday Reminder APK"** workflow will compile the project automatically in ~2 minutes.
4. Download the compiled `BirthdayReminder-Debug-APK` artifact and copy the `.apk` directly to your phone!

### Option 3: Command Line (CLI)
If you have Java 17 and the Android SDK installed:
```bash
cd /Users/shashanksingh/.gemini/antigravity/scratch/birthday-reminder-app
./build-apk.sh
```
Or directly:
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Installing on Your Android Device

1. Connect your Android phone via USB and enable **USB Debugging** (in Developer Options).
2. Run:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   *(Or simply send the `app-debug.apk` file to your phone via Google Drive, Telegram, or USB cable, and tap it to install).*

---

## 🔐 Android Permissions Guide

When you first open the app, make sure to allow the following:
- **Notifications**: Required on Android 13+ to receive birthday alerts and action buttons.
- **Alarms & Reminders**: Under *Settings > Apps > Birthday Reminder > Alarms & reminders*, ensure exact alarms are permitted so reminders trigger at the precise minute.
- **SMS (Optional)**: Only needed if you check *"Send SMS automatically in background"* for SMS contacts.
