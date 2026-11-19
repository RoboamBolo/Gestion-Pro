package com.example.ui.screens.sav

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SavTicketEntity
import com.example.data.model.SavStatus
import com.example.data.model.SavType
import com.example.ui.components.SearchFilterBar
import com.example.ui.components.StatusBadge
import com.example.viewmodel.GestionUiState

@Composable
fun SavScreen(
    state: GestionUiState,
    onSaveTicket: (SavTicketEntity) -> Unit,
    onAppendExchange: (SavTicketEntity, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("TOUS") }
    var showForm by remember { mutableStateOf(false) }
    var exchangeTarget by remember { mutableStateOf<SavTicketEntity?>(null) }

    val filtered = remember(state.savTickets, query, statusFilter) {
        state.savTickets.filter { t ->
            val matchSt = statusFilter == "TOUS" || t.status == statusFilter
            val matchQ = query.isBlank() ||
                t.ticketNumber.contains(query, ignoreCase = true) ||
                t.clientName.contains(query, ignoreCase = true) ||
                t.equipmentName.contains(query, ignoreCase = true)
            matchSt && matchQ
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
                    placeholder = "Rechercher ticket SAV, client, équipement...",
                    filters = listOf("TOUS" to "Tous") + SavStatus.entries.map { it.name to it.label },
                    selectedFilter = statusFilter,
                    onFilterSelect = { statusFilter = it }
                )
            }

            items(filtered, key = { it.id }) { ticket ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${ticket.ticketNumber} • ${ticket.type}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusBadge(label = "SLA ${ticket.slaHours}h", statusKey = if (ticket.slaHours <= 8) "URGENTE" else "PLANIFIEE")
                                StatusBadge(label = ticket.status.replace('_', ' '), statusKey = ticket.status)
                            }
                        }
                        Text("${ticket.clientName} — ${ticket.equipmentName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(ticket.description, style = MaterialTheme.typography.bodySmall)
                        if (ticket.exchangeLog.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    ticket.exchangeLog,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = { exchangeTarget = ticket }) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Répondre / Suivre")
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showForm = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouveau dossier SAV")
        }
    }

    if (showForm) {
        var selectedClient by remember { mutableStateOf(state.clients.firstOrNull()) }
        var equipmentName by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("RECLAMATION") }
        var sla by remember { mutableStateOf("24") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text("Nouveau dossier SAV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SavType.entries.forEach { t ->
                            FilterChip(selected = type == t.name, onClick = { type = t.name }, label = { Text(t.label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.clients.take(3).forEach { c ->
                            FilterChip(selected = selectedClient?.id == c.id, onClick = { selectedClient = c }, label = { Text(c.name.take(14), maxLines = 1) })
                        }
                    }
                    OutlinedTextField(value = equipmentName, onValueChange = { equipmentName = it }, label = { Text("Équipement concerné *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sla, onValueChange = { sla = it }, label = { Text("Délai SLA cible (heures)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description du problème") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = selectedClient ?: return@Button
                        if (equipmentName.isNotBlank()) {
                            onSaveTicket(
                                SavTicketEntity(
                                    ticketNumber = "SAV-2026-${(100..999).random()}",
                                    clientId = c.id,
                                    clientName = c.name,
                                    equipmentName = equipmentName.trim(),
                                    type = type,
                                    slaHours = sla.toIntOrNull() ?: 24,
                                    description = desc.trim()
                                )
                            )
                            showForm = false
                        }
                    }
                ) { Text("Créer dossier") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("Annuler") } }
        )
    }

    exchangeTarget?.let { ticket ->
        var note by remember { mutableStateOf("") }
        var status by remember { mutableStateOf(ticket.status) }
        AlertDialog(
            onDismissRequest = { exchangeTarget = null },
            title = { Text("Suivi ${ticket.ticketNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SavStatus.entries.forEach { st ->
                            FilterChip(selected = status == st.name, onClick = { status = st.name }, label = { Text(st.label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nouvel échange / note *") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (note.isNotBlank()) {
                            onAppendExchange(ticket, note.trim(), status)
                            exchangeTarget = null
                        }
                    }
                ) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { exchangeTarget = null }) { Text("Annuler") } }
        )
    }
}
