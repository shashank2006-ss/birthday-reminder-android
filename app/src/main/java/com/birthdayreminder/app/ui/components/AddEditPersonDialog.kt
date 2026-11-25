package com.birthdayreminder.app.ui.components

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
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
    var birthDayText by remember { mutableStateOf((initialPerson?.birthDay ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).toString()) }
    var birthYearText by remember { mutableStateOf(initialPerson?.birthYear?.toString() ?: "") }

    // 12-Hour System with AM/PM
    val initial24Hour = initialPerson?.reminderHour ?: 9
    val initial12Hour = if (initial24Hour == 0) 12 else if (initial24Hour > 12) initial24Hour - 12 else initial24Hour
    val initialAm = initial24Hour < 12

    var hour12Text by remember { mutableStateOf(initial12Hour.toString()) }
    var minuteText by remember { mutableStateOf(String.format(Locale.US, "%02d", initialPerson?.reminderMinute ?: 0)) }
    var isAm by remember { mutableStateOf(initialAm) }

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

    fun parseAndSetBirthday(rawBday: String) {
        try {
            val trimmed = rawBday.trim()
            if (trimmed.startsWith("--")) {
                // Format: --MM-dd
                val parts = trimmed.removePrefix("--").split("-")
                if (parts.size >= 2) {
                    parts[0].toIntOrNull()?.let { if (it in 1..12) birthMonth = it }
                    parts[1].toIntOrNull()?.let { if (it in 1..31) birthDayText = it.toString() }
                }
            } else if (trimmed.contains("-") || trimmed.contains("/") || trimmed.contains(".")) {
                val parts = trimmed.split('-', '/', '.')
                if (parts.size == 3) {
                    val p0 = parts[0].toIntOrNull()
                    val p1 = parts[1].toIntOrNull()
                    val p2 = parts[2].toIntOrNull()
                    if (p0 != null && p1 != null && p2 != null) {
                        if (p0 > 1000) {
                            // yyyy-MM-dd
                            birthYearText = p0.toString()
                            if (p1 in 1..12) birthMonth = p1
                            if (p2 in 1..31) birthDayText = p2.toString()
                        } else if (p2 > 1000) {
                            // dd-MM-yyyy or MM-dd-yyyy
                            birthYearText = p2.toString()
                            if (p1 in 1..12) birthMonth = p1
                            if (p0 in 1..31) birthDayText = p0.toString()
                        }
                    }
                }
            } else if (trimmed.length == 8 && trimmed.all { it.isDigit() }) {
                // yyyyMMdd
                val y = trimmed.substring(0, 4).toIntOrNull()
                val m = trimmed.substring(4, 6).toIntOrNull()
                val d = trimmed.substring(6, 8).toIntOrNull()
                if (y != null && m != null && d != null) {
                    birthYearText = y.toString()
                    if (m in 1..12) birthMonth = m
                    if (d in 1..31) birthDayText = d.toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Phone & Contact Picker Launcher (using CommonDataKinds.Phone.CONTENT_URI)
    val contactPhonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactDataUri: Uri? = result.data?.data
            if (contactDataUri != null) {
                try {
                    val cursor = context.contentResolver.query(
                        contactDataUri,
                        null,
                        null,
                        null,
                        null
                    )
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val contactIdIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

                            if (nameIndex != -1) {
                                val pickedName = c.getString(nameIndex)
                                if (!pickedName.isNullOrBlank()) {
                                    name = pickedName
                                }
                            }

                            if (numberIndex != -1) {
                                val rawNum = c.getString(numberIndex)
                                if (!rawNum.isNullOrBlank()) {
                                    phoneNumber = rawNum.replace("[^0-9+]".toRegex(), "")
                                }
                            }

                            val contactId = if (contactIdIndex != -1) c.getString(contactIdIndex) else null
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
                                                parseAndSetBirthday(bdayStr)
                                                Toast.makeText(context, "Loaded contact & birthday!", Toast.LENGTH_SHORT).show()
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
    }

    // Permission launcher for READ_CONTACTS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        contactPhonePickerLauncher.launch(pickIntent)
    }

    fun launchContactPicker() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPhonePickerLauncher.launch(pickIntent)
        }
    }

    // Native DatePickerDialog for full calendar Year, Month, Day selection
    fun showDatePickerDialog() {
        val initialYear = birthYearText.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val initialDay = birthDayText.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                birthYearText = year.toString()
                birthMonth = month + 1
                birthDayText = dayOfMonth.toString()
            },
            initialYear,
            birthMonth - 1,
            initialDay
        ).show()
    }

    // Native 12-hour TimePickerDialog
    fun showTimePickerDialog() {
        val currentH12 = hour12Text.toIntOrNull() ?: 12
        val currentM = minuteText.toIntOrNull() ?: 0
        val current24H = when {
            currentH12 == 12 && isAm -> 0
            currentH12 == 12 && !isAm -> 12
            isAm -> currentH12
            else -> currentH12 + 12
        }

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                isAm = hourOfDay < 12
                val h12 = if (hourOfDay == 0) 12 else if (hourOfDay > 12) hourOfDay - 12 else hourOfDay
                hour12Text = h12.toString()
                minuteText = String.format(Locale.US, "%02d", minute)
            },
            current24H,
            currentM,
            false // 12-hour view with AM/PM!
        ).show()
    }

    // Helper: calculate 24h equivalent from 12h inputs
    fun getCalculated24Hour(): Int {
        val h12 = hour12Text.toIntOrNull() ?: 12
        return when {
            h12 == 12 && isAm -> 0
            h12 == 12 && !isAm -> 12
            isAm -> h12.coerceIn(1, 11)
            else -> h12.coerceIn(1, 11) + 12
        }
    }

    fun getCalculatedMinute(): Int {
        val m = minuteText.toIntOrNull() ?: 0
        return m.coerceIn(0, 59)
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

                                val finalDay = birthDayText.toIntOrNull()
                                if (finalDay == null || finalDay !in 1..31) {
                                    errorMessage = "Please enter a valid birthday day (1-31)."
                                    return@TextButton
                                }

                                val finalYear = birthYearText.toIntOrNull()
                                val finalHour24 = getCalculated24Hour()
                                val finalMinute = getCalculatedMinute()

                                val personToSave = (initialPerson ?: Person()).copy(
                                    name = trimmedName,
                                    birthMonth = birthMonth,
                                    birthDay = finalDay,
                                    birthYear = finalYear,
                                    reminderHour = finalHour24,
                                    reminderMinute = finalMinute,
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
                                        onClick = { launchContactPicker() },
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
                                        IconButton(onClick = { launchContactPicker() }) {
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

                    // SECTION 2: Birthday Date with direct Month, Day, Year and Calendar DatePicker
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Birthday Date",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                FilledTonalButton(
                                    onClick = { showDatePickerDialog() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick Date", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // 3-Column Row: Month, Day, and Year all together!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Month Dropdown
                                var monthExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = monthExpanded,
                                    onExpandedChange = { monthExpanded = !monthExpanded },
                                    modifier = Modifier.weight(1.3f)
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

                                // Day Input (Fully editable and clearable)
                                OutlinedTextField(
                                    value = birthDayText,
                                    onValueChange = {
                                        val d = it.toIntOrNull()
                                        if (it.isEmpty() || (d != null && d in 1..31)) {
                                            birthDayText = it
                                        }
                                    },
                                    label = { Text("Day") },
                                    placeholder = { Text("1-31") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.8f)
                                )

                                // Birth Year (Prominently visible directly with Month and Day)
                                OutlinedTextField(
                                    value = birthYearText,
                                    onValueChange = { birthYearText = it },
                                    label = { Text("Year (Opt)") },
                                    placeholder = { Text("YYYY") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.1f)
                                )
                            }

                            // Live formatted date summary preview
                            val parsedYear = birthYearText.toIntOrNull()
                            val displayDay = if (birthDayText.isNotBlank()) birthDayText else "1"
                            val monthFormatted = monthNames.getOrElse(birthMonth - 1) { "" }
                            val turningAgeStr = parsedYear?.let {
                                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                                val turningAge = currentYear - it
                                if (turningAge >= 0) " • Turns $turningAge" else ""
                            } ?: ""

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🎂 Selected Date: $monthFormatted $displayDay${if (parsedYear != null) ", $parsedYear" else ""}$turningAgeStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // SECTION 3: Reminder & Send Time (12-Hour System with AM/PM)
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reminder & Send Time",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                FilledTonalButton(
                                    onClick = { showTimePickerDialog() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick Time", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Quick Presets: Midnight (12:00 AM), Morning (9:00 AM), Noon (12:00 PM)
                            val currentH12Val = hour12Text.toIntOrNull() ?: 12
                            val currentMVal = minuteText.toIntOrNull() ?: 0
                            val isMidnightSelected = currentH12Val == 12 && currentMVal == 0 && isAm
                            val isMorningSelected = currentH12Val == 9 && currentMVal == 0 && isAm
                            val isNoonSelected = currentH12Val == 12 && currentMVal == 0 && !isAm

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        hour12Text = "12"
                                        minuteText = "00"
                                        isAm = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isMidnightSelected) Color(0xFF9C27B0).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "🌙 Midnight (12:00 AM)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMidnightSelected) Color(0xFF7B1FA2) else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        hour12Text = "9"
                                        minuteText = "00"
                                        isAm = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isMorningSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "🌅 Morning (9:00 AM)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isMorningSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        hour12Text = "12"
                                        minuteText = "00"
                                        isAm = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isNoonSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "☀️ Noon (12:00 PM)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isNoonSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            // 12-Hour Exact Time Inputs: Hour (1-12), Minute (00-59), and AM / PM Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = hour12Text,
                                    onValueChange = {
                                        val h = it.toIntOrNull()
                                        if (it.isEmpty() || (h != null && h in 1..12)) {
                                            hour12Text = it
                                        }
                                    },
                                    label = { Text("Hour (1-12)") },
                                    placeholder = { Text("12") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = minuteText,
                                    onValueChange = {
                                        val m = it.toIntOrNull()
                                        if (it.isEmpty() || (m != null && m in 0..59)) {
                                            minuteText = it
                                        }
                                    },
                                    label = { Text("Minute (0-59)") },
                                    placeholder = { Text("00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                // AM / PM Segmented Switch
                                Row(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isAm) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { isAm = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "AM",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (!isAm) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { isAm = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "PM",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (!isAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Live explanation badge of selected time
                            val computed24H = getCalculated24Hour()
                            val computedMin = getCalculatedMinute()
                            val displayH12 = if (hour12Text.isNotBlank()) hour12Text else "12"
                            val displayMin = String.format(Locale.US, "%02d", computedMin)
                            val displayAmPm = if (isAm) "AM" else "PM"

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (computed24H) {
                                    0 -> Color(0xFF9C27B0).copy(alpha = 0.12f)
                                    12 -> Color(0xFFFF9800).copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = when (computed24H) {
                                        0 -> "🌙 Exactly $displayH12:$displayMin $displayAmPm (Midnight — Right at 12:00 as the date changes!)"
                                        12 -> "☀️ Exactly $displayH12:$displayMin $displayAmPm (Noon — Midday / Lunchtime)"
                                        in 1..11 -> "🌅 $displayH12:$displayMin $displayAmPm (Morning reminder)"
                                        else -> "🌆 $displayH12:$displayMin $displayAmPm (Afternoon / Evening reminder)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (computed24H == 0 || computed24H == 12) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when (computed24H) {
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
