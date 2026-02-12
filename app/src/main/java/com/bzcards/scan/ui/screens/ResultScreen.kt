package com.bzcards.scan.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bzcards.scan.model.BusinessCard
import com.bzcards.scan.ui.components.AccountPickerDialog
import com.bzcards.scan.util.ContactExporter

private enum class CardFieldType(val label: String, val icon: ImageVector) {
    NAME("Name", Icons.Default.Person),
    JOB_TITLE("Job Title", Icons.Default.Work),
    COMPANY("Company", Icons.Default.Business),
    PHONE("Phone", Icons.Default.Phone),
    EMAIL("Email", Icons.Default.Email),
    WEBSITE("Website", Icons.Default.Language),
    ADDRESS("Address", Icons.Default.LocationOn),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    initialCard: BusinessCard,
    onSave: (BusinessCard) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialCard.name) }
    var jobTitle by remember { mutableStateOf(initialCard.jobTitle) }
    var company by remember { mutableStateOf(initialCard.company) }
    var phone by remember { mutableStateOf(initialCard.phone) }
    var email by remember { mutableStateOf(initialCard.email) }
    var website by remember { mutableStateOf(initialCard.website) }
    var address by remember { mutableStateOf(initialCard.address) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var saveBeforeExport by remember { mutableStateOf(false) }

    fun buildCurrentCard() = initialCard.copy(
        name = name,
        jobTitle = jobTitle,
        company = company,
        phone = phone,
        email = email,
        website = website,
        address = address
    )

    fun exportWithAccount(account: ContactExporter.AccountInfo?) {
        val card = buildCurrentCard()
        if (saveBeforeExport) {
            onSave(card)
        }
        val intent = ContactExporter.buildContactIntent(card, account)
        context.startActivity(intent)
    }

    fun startExport(alsoSave: Boolean) {
        saveBeforeExport = alsoSave
        val lastAccount = ContactExporter.getLastUsedAccount(context)
        val accounts = ContactExporter.getGoogleAccounts(context)
        if (lastAccount != null && accounts.any { it.name == lastAccount.name }) {
            // Use the last account directly without showing picker
            ContactExporter.saveLastUsedAccount(context, lastAccount)
            exportWithAccount(lastAccount)
        } else if (accounts.size == 1) {
            // Only one account, use it
            ContactExporter.saveLastUsedAccount(context, accounts[0])
            exportWithAccount(accounts[0])
        } else {
            // Show picker
            showAccountPicker = true
        }
    }

    if (showAccountPicker) {
        val accounts = remember { ContactExporter.getGoogleAccounts(context) }
        val lastAccount = remember { ContactExporter.getLastUsedAccount(context) }
        AccountPickerDialog(
            accounts = accounts,
            lastUsedAccount = lastAccount,
            onAccountSelected = { account ->
                ContactExporter.saveLastUsedAccount(context, account)
                showAccountPicker = false
                exportWithAccount(account)
            },
            onUseSystemPicker = {
                showAccountPicker = false
                exportWithAccount(null)
            },
            onDismiss = { showAccountPicker = false }
        )
    }

    val setField: (CardFieldType, String) -> Unit = { field, value ->
        when (field) {
            CardFieldType.NAME -> name = value
            CardFieldType.JOB_TITLE -> jobTitle = value
            CardFieldType.COMPANY -> company = value
            CardFieldType.PHONE -> {
                phone = if (phone.isBlank()) value else "$phone, $value"
            }
            CardFieldType.EMAIL -> email = value
            CardFieldType.WEBSITE -> website = value
            CardFieldType.ADDRESS -> {
                address = if (address.isBlank()) value else "$address, $value"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanned Card") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Review & edit the extracted information:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            CardField(Icons.Default.Person, "Name", name) { name = it }
            CardField(Icons.Default.Work, "Job Title", jobTitle) { jobTitle = it }
            CardField(Icons.Default.Business, "Company", company) { company = it }
            CardField(Icons.Default.Phone, "Phone", phone) { phone = it }
            CardField(Icons.Default.Email, "Email", email) { email = it }
            CardField(Icons.Default.Language, "Website", website) { website = it }
            CardField(Icons.Default.LocationOn, "Address", address) { address = it }

            Spacer(modifier = Modifier.height(16.dp))

            // Raw text — tap lines to assign to fields
            Text(
                text = "Raw scanned text (tap a line to assign to a field):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            val rawLines = initialCard.rawText.lines().filter { it.isNotBlank() }
            if (rawLines.isEmpty()) {
                Text(
                    text = "(no text detected)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp)
                ) {
                    rawLines.forEachIndexed { index, line ->
                        RawTextLine(
                            text = line.trim(),
                            onAssignToField = { field -> setField(field, line.trim()) }
                        )
                        if (index < rawLines.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save & Add to Contacts (primary action)
            Button(
                onClick = { startExport(alsoSave = true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContactPhone, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Add to Contacts")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Discard")
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { onSave(buildCurrentCard()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Only")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RawTextLine(
    text: String,
    onAssignToField: (CardFieldType) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = true }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.TouchApp,
            contentDescription = "Assign to field",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            Text(
                text = "Assign to:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            CardFieldType.entries.forEach { field ->
                DropdownMenuItem(
                    text = { Text(field.label) },
                    leadingIcon = {
                        Icon(
                            field.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        onAssignToField(field)
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CardField(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true
    )
}
