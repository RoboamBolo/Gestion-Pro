package com.example.ui.screens.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ClientEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SearchFilterBar
import com.example.ui.components.StatusBadge
import com.example.viewmodel.GestionUiState
import java.util.Locale

@Composable
fun ClientsScreen(
    state: GestionUiState,
    onSaveClient: (ClientEntity) -> Unit,
    onDeleteClient: (ClientEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("TOUS") }
    var editingClient by remember { mutableStateOf<ClientEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var detailClient by remember { mutableStateOf<ClientEntity?>(null) }

    val filtered = remember(state.clients, query, selectedType) {
        state.clients.filter { c ->
            val matchType = selectedType == "TOUS" || c.type == selectedType
            val matchQuery = query.isBlank() ||
                c.name.contains(query, ignoreCase = true) ||
                c.companyName.contains(query, ignoreCase = true) ||
                c.city.contains(query, ignoreCase = true) ||
                c.phone.contains(query, ignoreCase = true)
            matchType && matchQuery
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SearchFilterBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Rechercher un client, ville, téléphone...",
                    filters = listOf(
                        "TOUS" to "Tous (${state.clients.size})",
                        "ENTREPRISE" to "Entreprises",
                        "PARTICULIER" to "Particuliers"
                    ),
                    selectedFilter = selectedType,
                    onFilterSelect = { selectedType = it }
                )
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Business,
                        title = "Aucun client trouvé",
                        subtitle = "Ajoutez un client particulier ou entreprise via le bouton +."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { client ->
                    val intCount = state.interventions.count { it.clientId == client.id }
                    val docsCount = state.commercialDocs.count { it.clientId == client.id }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { detailClient = client }
                            .testTag("client_card_${client.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(client.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    if (client.companyName.isNotBlank()) {
                                        Text(client.companyName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                StatusBadge(
                                    label = if (client.type == "ENTREPRISE") "Entreprise" else "Particulier",
                                    statusKey = if (client.type == "ENTREPRISE") "PLANIFIEE" else "NOUVELLE"
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("${client.address}, ${client.city}", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("${client.phone} • ${client.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "$intCount intervention(s) • $docsCount document(s)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row {
                                    IconButton(onClick = {
                                        editingClient = client
                                        showForm = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Modifier client")
                                    }
                                    IconButton(onClick = { onDeleteClient(client) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer client", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingClient = null
                showForm = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_client_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouveau client")
        }
    }

    if (showForm) {
        ClientFormDialog(
            initial = editingClient,
            onDismiss = { showForm = false },
            onSave = {
                onSaveClient(it)
                showForm = false
            }
        )
    }

    detailClient?.let { client ->
        val clientInterventions = state.interventions.filter { it.clientId == client.id }
        val clientDocs = state.commercialDocs.filter { it.clientId == client.id }
        AlertDialog(
            onDismissRequest = { detailClient = null },
            title = { Text(client.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type : ${client.type}", style = MaterialTheme.typography.labelMedium)
                    Text("Adresse : ${client.address}, ${client.city}", style = MaterialTheme.typography.bodySmall)
                    Text("Contact : ${client.phone} | ${client.email}", style = MaterialTheme.typography.bodySmall)
                    if (client.notes.isNotBlank()) {
                        Text("Observations : ${client.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                    Text("Historique interventions (${clientInterventions.size})", style = MaterialTheme.typography.titleSmall)
                    clientInterventions.take(3).forEach {
                        Text("• ${it.reference} (${it.status}) — ${it.title}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Devis & Factures (${clientDocs.size})", style = MaterialTheme.typography.titleSmall)
                    clientDocs.take(3).forEach {
                        Text("• ${it.docNumber} (${it.status}) — ${String.format(Locale.FRANCE, "%.2f €", it.totalTtc)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailClient = null }) { Text("Fermer") }
            }
        )
    }
}

@Composable
private fun ClientFormDialog(
    initial: ClientEntity?,
    onDismiss: () -> Unit,
    onSave: (ClientEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var company by remember { mutableStateOf(initial?.companyName ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "ENTREPRISE") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var city by remember { mutableStateOf(initial?.city ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau client" else "Modifier client") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "ENTREPRISE", onClick = { type = "ENTREPRISE" }, label = { Text("Entreprise") })
                    FilterChip(selected = type == "PARTICULIER", onClick = { type = "PARTICULIER" }, label = { Text("Particulier") })
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom / Raison sociale *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (type == "ENTREPRISE") {
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Société / Groupe") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Adresse") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Ville") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observations") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: ClientEntity(name = name)).copy(
                                name = name.trim(),
                                companyName = company.trim(),
                                type = type,
                                email = email.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                city = city.trim(),
                                notes = notes.trim()
                            )
                        )
                    }
                }
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
