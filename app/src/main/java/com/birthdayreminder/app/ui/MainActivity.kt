package com.birthdayreminder.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.birthdayreminder.app.service.NotificationHelper
import com.birthdayreminder.app.ui.screens.HomeScreen
import com.birthdayreminder.app.ui.theme.BirthdayReminderTheme
import com.birthdayreminder.app.ui.viewmodel.BirthdayViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BirthdayViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Notification permission handled
    }

    private val requestSmsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // SMS permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannel(this)

        // Request runtime permissions
        checkAndRequestPermissions()

        // Handle intent if launched from notification
        handleIncomingIntent(intent)

        setContent {
            BirthdayReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val personId = intent.getStringExtra(EXTRA_PERSON_ID) ?: return
        val isAutoEdit = intent.getBooleanExtra(EXTRA_AUTO_EDIT, false)

        val targetPerson = viewModel.allPeople.value.find { it.id == personId } ?: return
        if (isAutoEdit) {
            viewModel.quickSendPerson.value = targetPerson
        } else {
            viewModel.quickSendPerson.value = targetPerson
        }
    }

    private fun checkAndRequestPermissions() {
        // Android 13+ Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Android 12+ Exact Alarm Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Ignore if device doesn't support the settings intent directly
                }
            }
        }

        // Optional SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    companion object {
        const val ACTION_VIEW_PERSON = "com.birthdayreminder.app.ACTION_VIEW_PERSON"
        const val EXTRA_PERSON_ID = "extra_person_id"
        const val EXTRA_AUTO_EDIT = "extra_auto_edit"
    }
}
