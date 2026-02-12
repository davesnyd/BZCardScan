package com.bzcards.scan.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bzcards.scan.model.BusinessCard
import com.bzcards.scan.ui.components.AccountPickerDialog
import com.bzcards.scan.util.ContactExporter
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedCardsScreen(
    cardsFlow: Flow<List<BusinessCard>>,
    onDelete: (BusinessCard) -> Unit,
    onBack: () -> Unit
) {
    val cards by cardsFlow.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var showAccountPicker by remember { mutableStateOf(false) }
    var cardToExport by remember { mutableStateOf<BusinessCard?>(null) }

    fun exportCard(card: BusinessCard) {
        val lastAccount = ContactExporter.getLastUsedAccount(context)
        val accounts = ContactExporter.getGoogleAccounts(context)
        if (lastAccount != null && accounts.any { it.name == lastAccount.name }) {
            val intent = ContactExporter.buildContactIntent(card, lastAccount)
            context.startActivity(intent)
        } else if (accounts.size == 1) {
            ContactExporter.saveLastUsedAccount(context, accounts[0])
            val intent = ContactExporter.buildContactIntent(card, accounts[0])
            context.startActivity(intent)
        } else {
            cardToExport = card
            showAccountPicker = true
        }
    }

    if (showAccountPicker && cardToExport != null) {
        val accounts = remember { ContactExporter.getGoogleAccounts(context) }
        val lastAccount = remember { ContactExporter.getLastUsedAccount(context) }
        AccountPickerDialog(
            accounts = accounts,
            lastUsedAccount = lastAccount,
            onAccountSelected = { account ->
                ContactExporter.saveLastUsedAccount(context, account)
                showAccountPicker = false
                val intent = ContactExporter.buildContactIntent(cardToExport!!, account)
                context.startActivity(intent)
                cardToExport = null
            },
            onUseSystemPicker = {
                showAccountPicker = false
                val chooser = Intent.createChooser(
                    ContactExporter.buildContactIntent(cardToExport!!, null),
                    "Save contact to..."
                )
                context.startActivity(chooser)
                cardToExport = null
            },
            onDismiss = {
                showAccountPicker = false
                cardToExport = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Cards (${cards.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (cards.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No saved cards yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Scan a business card to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards, key = { it.id }) { card ->
                    SavedCardItem(
                        card = card,
                        onDelete = { onDelete(card) },
                        onExportContact = { exportCard(card) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedCardItem(
    card: BusinessCard,
    onDelete: () -> Unit,
    onExportContact: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (card.jobTitle.isNotBlank()) {
                        Text(
                            text = card.jobTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row {
                    IconButton(onClick = onExportContact) {
                        Icon(
                            Icons.Default.ContactPhone,
                            contentDescription = "Add to Contacts",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (card.company.isNotBlank()) {
                Text(
                    text = card.company,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(modifier = Modifier.padding(top = 4.dp)) {
                if (card.phone.isNotBlank()) {
                    Text(
                        text = card.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                if (card.email.isNotBlank()) {
                    Text(
                        text = card.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "Scanned ${dateFormat.format(Date(card.scannedAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
