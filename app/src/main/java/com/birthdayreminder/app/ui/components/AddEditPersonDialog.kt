package com.birthdayreminder.app.ui.components

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.birthdayreminder.app.data.model.Person
import com.birthdayreminder.app.data.model.SocialPlatform
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPersonDialog(
    initialPerson: Person?,
    onDismiss: () -> Unit,
    onSave: (Person) -> Unit
) {
    var name by remember { mutableStateOf(initialPerson?.name ?: "") }
    var birthMonth by remember { mutableIntStateOf(initialPerson?.birthMonth ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) }
    var birthDay by remember { mutableIntStateOf(initialPerson?.birthDay ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var birthYearText by remember { mutableStateOf(initialPerson?.birthYear?.toString() ?: "") }

    var reminderHour by remember { mutableIntStateOf(initialPerson?.reminderHour ?: 9) }
    var reminderMinute by remember { mutableIntStateOf(initialPerson?.reminderMinute ?: 0) }

    var selectedPlatform by remember { mutableStateOf(initialPerson?.socialPlatform ?: SocialPlatform.WHATSAPP) }
    var phoneNumber by remember { mutableStateOf(initialPerson?.phoneNumber ?: "") }
    var autoSendSms by remember { mutableStateOf(initialPerson?.autoSendSmsDirectly ?: false) }
    var customMessage by remember {
        mutableStateOf(
            initialPerson?.customMessage ?: "Happy Birthday! Wishing you a fantastic day filled with joy and success! 🎉🎂"
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val monthNames = remember { DateFormatSymbols.getInstance(Locale.getDefault()).months.toList() }

    val templates = listOf(
        "🎉 Classic" to "Happy Birthday! Wishing you a fantastic day filled with joy and success! 🎉🎂",
        "😂 Funny" to "Happy Birthday! You're not getting older, you're just leveling up! 🎮 Cheers!",
        "💖 Heartfelt" to "Happy Birthday! So grateful to have you in my life. Wishing you endless happiness! ❤️",
        "✨ Short" to "Happy Birthday! Have an amazing celebration today! 🥳🎈"
    )

    val context = LocalContext.current
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        contactUri?.let { uri ->
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                        if (nameIndex != -1) {
                            val pickedName = c.getString(nameIndex)
                            if (!pickedName.isNullOrBlank() && name.isBlank()) {
                                name = pickedName
                            }
                        }

                        val hasPhone = if (hasPhoneIndex != -1) c.getInt(hasPhoneIndex) > 0 else true
                        if (idIndex != -1 && hasPhone) {
                            val contactId = c.getString(idIndex)
                            val phoneCursor = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )
                            phoneCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    val numberIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numberIndex != -1) {
                                        val number = pc.getString(numberIndex)
                                        if (!number.isNullOrBlank()) {
                                            phoneNumber = number.replace("[^0-9+]".toRegex(), "")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (initialPerson == null) "Add Birthday Reminder" else "Edit Birthday Reminder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Full Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Person's Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Birthday Date Section
                    Text(
                        text = "Birthday Date",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Month Dropdown
                        var monthExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = !monthExpanded },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            OutlinedTextField(
                                value = monthNames.getOrElse(birthMonth - 1) { "Month" },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                                modifier = Modifier.menuAnchor(),
                                label = { Text("Month") }
                            )
                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false }
                            ) {
                                monthNames.forEachIndexed { index, mName ->
                                    if (mName.isNotBlank()) {
                                        DropdownMenuItem(
                                            text = { Text(mName) },
                                            onClick = {
                                                birthMonth = index + 1
                                                monthExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Day Input
                        OutlinedTextField(
                            value = birthDay.toString(),
                            onValueChange = {
                                val d = it.toIntOrNull()
                                if (d != null && d in 1..31) birthDay = d
                            },
                            label = { Text("Day (1-31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        // Birth Year (Optional)
                        OutlinedTextField(
                            value = birthYearText,
                            onValueChange = { birthYearText = it },
                            label = { Text("Year (Opt)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Notification & Send Time
                    Text(
                        text = "Reminder & Scheduled Send Time",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = String.format(Locale.US, "%02d", reminderHour),
                            onValueChange = {
                                val h = it.toIntOrNull()
                                if (h != null && h in 0..23) reminderHour = h
                            },
                            label = { Text("Hour (0-23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = String.format(Locale.US, "%02d", reminderMinute),
                            onValueChange = {
                                val m = it.toIntOrNull()
                                if (m != null && m in 0..59) reminderMinute = m
                            },
                            label = { Text("Minute (0-59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Social Media / Platform Selection
                    Text(
                        text = "Messaging Platform",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SocialPlatform.values().forEach { platform ->
                            FilterChip(
                                selected = selectedPlatform == platform,
                                onClick = { selectedPlatform = platform },
                                label = { Text(platform.displayName, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Phone Number / Handle & Device Contact Picker
                    val showContactPicker = selectedPlatform != SocialPlatform.INSTAGRAM
                    val fieldLabel = when (selectedPlatform) {
                        SocialPlatform.WHATSAPP, SocialPlatform.SMS -> "Phone Number *"
                        SocialPlatform.TELEGRAM -> "Telegram Handle or Phone Number *"
                        SocialPlatform.INSTAGRAM -> "Instagram Handle *"
                    }
                    val fieldPlaceholder = when (selectedPlatform) {
                        SocialPlatform.WHATSAPP, SocialPlatform.SMS -> "Type phone or pick from contacts"
                        SocialPlatform.TELEGRAM -> "Type @username or pick from contacts"
                        SocialPlatform.INSTAGRAM -> "Enter @handle (handle only)"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fieldLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (showContactPicker) {
                            FilledTonalButton(
                                onClick = { contactPickerLauncher.launch(null) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Search Contacts", fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        placeholder = { Text(fieldPlaceholder) },
                        trailingIcon = {
                            if (showContactPicker) {
                                IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                                    Icon(
                                        imageVector = Icons.Default.Contacts,
                                        contentDescription = "Search Contacts",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = when (selectedPlatform) {
                                SocialPlatform.WHATSAPP, SocialPlatform.SMS -> KeyboardType.Phone
                                SocialPlatform.TELEGRAM, SocialPlatform.INSTAGRAM -> KeyboardType.Text
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Auto-send checkbox for SMS
                    if (selectedPlatform == SocialPlatform.SMS) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = autoSendSms,
                                onCheckedChange = { autoSendSms = it }
                            )
                            Text(
                                text = "Send SMS automatically in background at the exact time",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Unique Message
                    Text(
                        text = "Personalized Birthday Message *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Template Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        templates.forEach { (label, text) ->
                            SuggestionChip(
                                onClick = { customMessage = text },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4,
                        label = { Text("Custom Message") }
                    )

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Please enter the person's name."
                                return@Button
                            }
                            if (phoneNumber.isBlank()) {
                                errorMessage = "Please enter the phone number or handle."
                                return@Button
                            }
                            if (customMessage.isBlank()) {
                                errorMessage = "Please write a birthday message."
                                return@Button
                            }

                            val birthYear = birthYearText.toIntOrNull()
                            val personToSave = (initialPerson ?: Person()).copy(
                                name = name.trim(),
                                birthMonth = birthMonth,
                                birthDay = birthDay,
                                birthYear = birthYear,
                                reminderHour = reminderHour,
                                reminderMinute = reminderMinute,
                                socialPlatform = selectedPlatform,
                                phoneNumber = phoneNumber.trim(),
                                autoSendSmsDirectly = autoSendSms,
                                customMessage = customMessage.trim()
                            )
                            onSave(personToSave)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Reminder")
                    }
                }
            }
        }
    }
}
