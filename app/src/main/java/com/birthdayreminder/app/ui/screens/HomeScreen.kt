package com.birthdayreminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
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
import com.birthdayreminder.app.data.model.Person
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
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🎂 Birthday Reminder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (nextBirthday != null) {
                            val days = nextBirthday.getDaysRemaining()
                            val nextText = if (days == 0) "Today is ${nextBirthday.name}'s birthday! 🎉"
                                           else "Next: ${nextBirthday.name} (in $days days)"
                            Text(
                                text = nextText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Birthday") },
                text = { Text("Add Birthday") },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs: Upcoming List vs Calendar
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Upcoming (${allPeople.size})") },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Calendar") },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        placeholder = { Text("Search by name, phone, or date...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (filteredPeople.isEmpty()) {
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
                                    text = if (searchQuery.isNotEmpty()) "No results matching \"$searchQuery\""
                                           else "No birthdays added yet! 🎈",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Try a different search term"
                                           else "Add your friends and family to automatically schedule notifications and wishes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (searchQuery.isEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { showAddDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add First Birthday")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp)
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

                1 -> {
                    // Calendar Tab
                    BirthdayCalendarView(
                        people = allPeople,
                        onPersonClick = { viewModel.quickSendPerson.value = it },
                        modifier = Modifier.fillMaxWidth()
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

    // Quick Send Dialog (Message preview & live editing)
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
