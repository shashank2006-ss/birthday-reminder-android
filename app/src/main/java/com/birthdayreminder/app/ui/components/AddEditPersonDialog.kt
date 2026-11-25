package com.birthdayreminder.app.ui.components

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
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
    var includesBirthYear by remember { mutableStateOf(initialPerson?.birthYear != null) }
    var birthYearText by remember { mutableStateOf(initialPerson?.birthYear?.toString() ?: "") }

    var reminderHour by remember { mutableIntStateOf(initialPerson?.reminderHour ?: 9) }
    var reminderMinute by remember { mutableIntStateOf(initialPerson?.reminderMinute ?: 0) }

    var selectedPlatform by remember { mutableStateOf(initialPerson?.socialPlatform ?: SocialPlatform.WHATSAPP) }
    var phoneNumber by remember { mutableStateOf(initialPerson?.phoneNumber ?: "") }
    var autoSendSms by remember { mutableStateOf(initialPerson?.autoSendSmsDirectly ?: false) }
    var customMessage by remember {
        mutableStateOf(
            initialPerson?.customMessage ?: "Happy Birthday! Wishing you a fantastic day filled with joy, health, and success! 🎉🎂"
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val monthNames = remember { DateFormatSymbols.getInstance(Locale.getDefault()).months.toList() }

    val templates = listOf(
        "🎉 Classic" to "Happy Birthday! Wishing you a fantastic day filled with joy, health, and success! 🎉🎂",
        "😂 Funny" to "Happy Birthday! You're not getting older, you're just leveling up! 🎮 Cheers to another great year!",
        "💖 Heartfelt" to "Happy Birthday! So grateful to have you in my life. Wishing you endless happiness, love, and laughter! ❤️",
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
                            if (!pickedName.isNullOrBlank()) {
                                name = pickedName
                            }
                        }

                        val contactId = if (idIndex != -1) c.getString(idIndex) else null
                        val hasPhone = if (hasPhoneIndex != -1) c.getInt(hasPhoneIndex) > 0 else true

                        if (contactId != null && hasPhone) {
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

                        // Try to auto-read contact birthday from ContactsContract.Data
                        if (contactId != null) {
                            try {
                                val bdayCursor = context.contentResolver.query(
                                    ContactsContract.Data.CONTENT_URI,
                                    arrayOf(ContactsContract.CommonDataKinds.Event.START_DATE),
                                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?",
                                    arrayOf(
                                        contactId,
                                        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                                        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
                                    ),
                                    null
                                )
                                bdayCursor?.use { bc ->
                                    if (bc.moveToFirst()) {
                                        val bdayStr = bc.getString(0)
                                        if (!bdayStr.isNullOrBlank()) {
                                            // Formats: yyyy-MM-dd or --MM-dd
                                            if (bdayStr.startsWith("--")) {
                                                val parts = bdayStr.removePrefix("--").split("-")
                                                if (parts.size >= 2) {
                                                    parts[0].toIntOrNull()?.let { birthMonth = it }
                                                    parts[1].toIntOrNull()?.let { birthDay = it }
                                                }
                                            } else {
                                                val parts = bdayStr.split("-")
                                                if (parts.size >= 3) {
                                                    parts[0].toIntOrNull()?.let {
                                                        includesBirthYear = true
                                                        birthYearText = it.toString()
                                                    }
                                                    parts[1].toIntOrNull()?.let { birthMonth = it }
                                                    parts[2].toIntOrNull()?.let { birthDay = it }
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
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Navigation Bar (matching iOS NavigationBar)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = if (initialPerson == null) "Add Birthday" else "Edit Birthday",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = {
                                val trimmedName = name.trim()
                                val trimmedPhone = phoneNumber.trim()
                                val trimmedMsg = customMessage.trim()

                                if (trimmedName.isEmpty()) {
                                    errorMessage = "Please enter the person's name."
                                    return@TextButton
                                }
                                if (trimmedPhone.isEmpty()) {
                                    errorMessage = "Please enter their phone number or handle."
                                    return@TextButton
                                }
                                if (trimmedMsg.isEmpty()) {
                                    errorMessage = "Please enter a birthday wish message."
                                    return@TextButton
                                }

                                val finalYear = if (includesBirthYear) birthYearText.toIntOrNull() else null
                                val personToSave = (initialPerson ?: Person()).copy(
                                    name = trimmedName,
                                    birthMonth = birthMonth,
                                    birthDay = birthDay,
                                    birthYear = finalYear,
                                    reminderHour = reminderHour,
                                    reminderMinute = reminderMinute,
                                    socialPlatform = selectedPlatform,
                                    phoneNumber = trimmedPhone,
                                    autoSendSmsDirectly = autoSendSms,
                                    customMessage = trimmedMsg
                                )
                                onSave(personToSave)
                            }
                        ) {
                            Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Scrollable Form Sections
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: Contact Info (matching iOS Section "Contact Info")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Contact Info",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Name Row with Contacts Picker button
                            val showContactPicker = selectedPlatform != SocialPlatform.INSTAGRAM
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Full Name *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                if (showContactPicker) {
                                    FilledTonalButton(
                                        onClick = { contactPickerLauncher.launch(null) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Contacts,
                                            contentDescription = "Contacts",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Contacts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Platform Selector
                            Text(
                                text = "Platform",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SocialPlatform.values().forEach { platform ->
                                    FilterChip(
                                        selected = selectedPlatform == platform,
                                        onClick = { selectedPlatform = platform },
                                        label = { Text(platform.displayName, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            // Phone / Handle Field
                            val placeholderText = when (selectedPlatform) {
                                SocialPlatform.WHATSAPP, SocialPlatform.SMS -> "Phone Number (with Country Code)"
                                SocialPlatform.TELEGRAM -> "Telegram Handle (@username) or Phone"
                                SocialPlatform.INSTAGRAM -> "Instagram Handle (@username)"
                            }

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text(placeholderText) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    if (showContactPicker) {
                                        IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                                            Icon(
                                                imageVector = Icons.Default.PersonSearch,
                                                contentDescription = "Search Contacts",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = when (selectedPlatform) {
                                        SocialPlatform.WHATSAPP, SocialPlatform.SMS -> KeyboardType.Phone
                                        SocialPlatform.TELEGRAM, SocialPlatform.INSTAGRAM -> KeyboardType.Text
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

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
                                        text = "Send SMS automatically in background at reminder time",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: Birthday Date (matching iOS Section "Birthday Date")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Birthday Date",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                        label = { Text("Month") },
                                        shape = RoundedCornerShape(12.dp)
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
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Include Birth Year (Calculate Age) Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Include Birth Year (Calculate Age)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Shows milestone age and turning age pills",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = includesBirthYear,
                                    onCheckedChange = { includesBirthYear = it }
                                )
                            }

                            if (includesBirthYear) {
                                OutlinedTextField(
                                    value = birthYearText,
                                    onValueChange = { birthYearText = it },
                                    label = { Text("Birth Year (e.g. 1995)") },
                                    placeholder = { Text("YYYY") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // SECTION 3: Reminder & Send Time (matching iOS Section "Reminder & Send Time")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Reminder & Send Time",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Quick Time Presets (Midnight, Morning, Noon)
                            val isMidnight = reminderHour == 0 && reminderMinute == 0
                            val isMorning = reminderHour == 9 && reminderMinute == 0
                            val isNoon = reminderHour == 12 && reminderMinute == 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        reminderHour = 0
                                        reminderMinute = 0
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isMidnight) Color(0xFF9C27B0).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "🌙 Midnight (12:00 AM)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMidnight) Color(0xFF7B1FA2) else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        reminderHour = 9
                                        reminderMinute = 0
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isMorning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "🌅 Morning (9:00 AM)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isMorning) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        reminderHour = 12
                                        reminderMinute = 0
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isNoon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "☀️ Noon (12:00 PM)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isNoon) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            // Exact Time Pickers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = String.format(Locale.US, "%02d", reminderHour),
                                    onValueChange = {
                                        val h = it.toIntOrNull()
                                        if (h != null && h in 0..23) reminderHour = h
                                    },
                                    label = { Text("Hour (0-23)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
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
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Live explanation badge of selected time (Midnight / Noon / Morning / Evening)
                            val hour12 = if (reminderHour == 0) 12 else if (reminderHour > 12) reminderHour - 12 else reminderHour
                            val amPm = if (reminderHour >= 12) "PM" else "AM"
                            val minFormatted = String.format(Locale.US, "%02d", reminderMinute)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (reminderHour) {
                                    0 -> Color(0xFF9C27B0).copy(alpha = 0.12f)
                                    12 -> Color(0xFFFF9800).copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = when (reminderHour) {
                                        0 -> "🌙 Exactly $hour12:$minFormatted $amPm (Midnight — Right at 12:00 as the date changes!)"
                                        12 -> "☀️ Exactly $hour12:$minFormatted $amPm (Noon — Midday / Lunchtime)"
                                        in 1..11 -> "🌅 $hour12:$minFormatted $amPm (Morning reminder)"
                                        else -> "🌆 $hour12:$minFormatted $amPm (Afternoon / Evening reminder)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (reminderHour == 0 || reminderHour == 12) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when (reminderHour) {
                                        0 -> Color(0xFF7B1FA2)
                                        12 -> Color(0xFFE65100)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // SECTION 4: Personalized Birthday Wish (matching iOS Section "Personalized Birthday Wish")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Personalized Birthday Wish",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Template Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                templates.forEach { (label, text) ->
                                    FilledTonalButton(
                                        onClick = { customMessage = text },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customMessage,
                                onValueChange = { customMessage = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(12.dp),
                                label = { Text("Custom Message") }
                            )
                        }
                    }

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
