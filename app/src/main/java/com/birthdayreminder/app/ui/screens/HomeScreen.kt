package com.birthdayreminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.birthdayreminder.app.ui.components.AddEditPersonDialog
import com.birthdayreminder.app.ui.components.BirthdayCalendarView
import com.birthdayreminder.app.ui.components.PersonCard
import com.birthdayreminder.app.ui.components.QuickSendDialog
import com.birthdayreminder.app.ui.viewmodel.BirthdayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BirthdayViewModel,
    modifier: Modifier = Modifier
) {
    val allPeople by viewModel.allPeople.collectAsState()
    val filteredPeople by viewModel.filteredPeople.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val editingPerson by viewModel.editingPerson.collectAsState()
    val quickSendPerson by viewModel.quickSendPerson.collectAsState()

    val nextBirthday = allPeople.firstOrNull()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎂 Birthday Reminder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (nextBirthday != null) {
                            val days = nextBirthday.getDaysRemaining()
                            val nextText = when (days) {
                                0 -> "🎉 Today is ${nextBirthday.name}'s birthday!"
                                1 -> "Next: ${nextBirthday.name} tomorrow"
                                else -> "Next: ${nextBirthday.name} in $days days"
                            }
                            Text(
                                text = nextText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Birthday",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Upcoming") },
                    label = { Text("Upcoming") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                    label = { Text("Calendar") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar (Matching iOS search prompt and layout)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Search name, phone, or date...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (allPeople.isEmpty()) {
                            // Empty state view matching iOS UpcomingListView emptyStateView
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CardGiftcard,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No Birthdays Added Yet",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Add friends and family to automatically receive notifications and send personalized wishes via WhatsApp, SMS, or Telegram.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { showAddDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add First Birthday", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (filteredPeople.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "No results matching \"$searchQuery\"",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Try searching for a different name, date, or phone number.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(filteredPeople, key = { it.id }) { person ->
                                    PersonCard(
                                        person = person,
                                        onEditClick = { viewModel.editingPerson.value = it },
                                        onDeleteClick = { viewModel.deletePerson(it.id) },
                                        onSendClick = { viewModel.quickSendPerson.value = it },
                                        onToggleEnabled = { viewModel.toggleEnabled(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    BirthdayCalendarView(
                        people = allPeople,
                        onPersonClick = { viewModel.quickSendPerson.value = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Add New Person Dialog
    if (showAddDialog) {
        AddEditPersonDialog(
            initialPerson = null,
            onDismiss = { showAddDialog = false },
            onSave = { newPerson ->
                showAddDialog = false
                viewModel.savePerson(newPerson)
            }
        )
    }

    // Edit Existing Person Dialog
    editingPerson?.let { person ->
        AddEditPersonDialog(
            initialPerson = person,
            onDismiss = { viewModel.editingPerson.value = null },
            onSave = { updatedPerson ->
                viewModel.editingPerson.value = null
                viewModel.savePerson(updatedPerson)
            }
        )
    }

    // Quick Send Dialog
    quickSendPerson?.let { person ->
        QuickSendDialog(
            person = person,
            onDismiss = { viewModel.quickSendPerson.value = null },
            onSend = { messageOverride ->
                viewModel.quickSendPerson.value = null
                viewModel.sendWish(person, messageOverride)
            }
        )
    }
}
