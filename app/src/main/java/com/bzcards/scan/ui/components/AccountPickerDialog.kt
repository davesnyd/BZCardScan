package com.bzcards.scan.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bzcards.scan.util.ContactExporter

@Composable
fun AccountPickerDialog(
    accounts: List<ContactExporter.AccountInfo>,
    lastUsedAccount: ContactExporter.AccountInfo?,
    onAccountSelected: (ContactExporter.AccountInfo) -> Unit,
    onUseSystemPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save contact to which account?") },
        text = {
            Column {
                if (accounts.isEmpty()) {
                    Text(
                        "No Google accounts found on this device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    accounts.forEach { account ->
                        val isLastUsed = account.name == lastUsedAccount?.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAccountSelected(account) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = if (isLastUsed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (isLastUsed) {
                                    Text(
                                        text = "Last used",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (isLastUsed) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Last used",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (lastUsedAccount != null && accounts.any { it.name == lastUsedAccount.name }) {
                TextButton(onClick = {
                    onAccountSelected(lastUsedAccount)
                }) {
                    Text("Use ${lastUsedAccount.displayName}")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onUseSystemPicker) {
                Text("Other...")
            }
        }
    )
}
