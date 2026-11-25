package com.birthdayreminder.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.birthdayreminder.app.data.model.Person
import com.birthdayreminder.app.data.model.SocialPlatform
import com.birthdayreminder.app.ui.theme.BirthdayGold
import com.birthdayreminder.app.ui.theme.SmsPurple
import com.birthdayreminder.app.ui.theme.TelegramBlue
import com.birthdayreminder.app.ui.theme.TodayHighlight
import com.birthdayreminder.app.ui.theme.WhatsAppGreen

@Composable
fun PersonCard(
    person: Person,
    onEditClick: (Person) -> Unit,
    onDeleteClick: (Person) -> Unit,
    onSendClick: (Person) -> Unit,
    onToggleEnabled: (Person) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysRemaining = person.getDaysRemaining()
    val isToday = daysRemaining == 0
    val isTomorrow = daysRemaining == 1

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val platformColor = when (person.socialPlatform) {
        SocialPlatform.WHATSAPP -> WhatsAppGreen
        SocialPlatform.SMS -> SmsPurple
        SocialPlatform.TELEGRAM -> TelegramBlue
        SocialPlatform.INSTAGRAM -> Color(0xFFE1306C)
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Name, turning age, and Days remaining badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(platformColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = person.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = platformColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = person.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = person.getFormattedDate(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            person.getTurningAge()?.let { age ->
                                Text(
                                    text = " • Turns $age",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Days Remaining Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isToday -> TodayHighlight
                        isTomorrow -> BirthdayGold
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = when {
                            isToday -> "Today! 🎉"
                            isTomorrow -> "Tomorrow"
                            else -> "In $daysRemaining days"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isToday -> Color.White
                            isTomorrow -> Color.Black
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reminder time & Platform tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("⏰ ${person.getFormattedTime()}") }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(person.socialPlatform.displayName) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        labelColor = platformColor
                    )
                )
                if (person.autoSendSmsDirectly && person.socialPlatform == SocialPlatform.SMS) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("⚡ Auto-Send") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message preview
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "\"${person.customMessage}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Send button, Edit, Enabled Switch, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onSendClick(person) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = platformColor),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Wish",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Wish", fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggleEnabled(person) }) {
                        Icon(
                            imageVector = if (person.isEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = "Toggle Reminder",
                            tint = if (person.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(onClick = { onEditClick(person) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Person",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Person",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Reminder") },
            text = { Text("Are you sure you want to remove ${person.name} from birthday reminders?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick(person)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
