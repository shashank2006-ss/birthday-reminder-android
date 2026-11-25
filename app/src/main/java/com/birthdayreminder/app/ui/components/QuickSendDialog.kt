package com.birthdayreminder.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.birthdayreminder.app.data.model.Person
import com.birthdayreminder.app.data.model.SocialPlatform
import com.birthdayreminder.app.ui.theme.SmsPurple
import com.birthdayreminder.app.ui.theme.TelegramBlue
import com.birthdayreminder.app.ui.theme.WhatsAppGreen

@Composable
fun QuickSendDialog(
    person: Person,
    onDismiss: () -> Unit,
    onSend: (messageOverride: String) -> Unit
) {
    var editableMessage by remember { mutableStateOf(person.customMessage) }

    val platformColor = when (person.socialPlatform) {
        SocialPlatform.WHATSAPP -> WhatsAppGreen
        SocialPlatform.SMS -> SmsPurple
        SocialPlatform.TELEGRAM -> TelegramBlue
        SocialPlatform.INSTAGRAM -> Color(0xFFE1306C)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Send Birthday Wish",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "To: ${person.name} (${person.phoneNumber})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    SuggestionChip(
                        onClick = {},
                        label = { Text(person.socialPlatform.displayName, fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Review & edit the message below before sending:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editableMessage,
                    onValueChange = { editableMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    label = { Text("Birthday Message") }
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                        onClick = { onSend(editableMessage.trim()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = platformColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send via ${person.socialPlatform.displayName}")
                    }
                }
            }
        }
    }
}
