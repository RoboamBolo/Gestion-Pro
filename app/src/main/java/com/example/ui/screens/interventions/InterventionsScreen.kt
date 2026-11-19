package com.example.ui.screens.interventions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ClientEntity
import com.example.data.local.EmployeeEntity
import com.example.data.local.InterventionEntity
import com.example.data.local.InterventionLogEntity
import com.example.data.model.InterventionStatus
import com.example.data.model.PriorityLevel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SearchFilterBar
import com.example.ui.components.SignaturePadDialog
import com.example.ui.components.StatusBadge
import com.example.viewmodel.GestionUiState
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InterventionsScreen(
    state: GestionUiState,
    observeLogs: (Long) -> Flow<List<InterventionLogEntity>>,
    onSaveIntervention: (InterventionEntity) -> Unit,
    onUpdateStatus: (InterventionEntity, String, String, String, Boolean) -> Unit,
    onDeleteIntervention: (InterventionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("TOUS") }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<InterventionEntity?>(null) }
    var selectedDetail by remember { mutableStateOf<InterventionEntity?>(null) }
    var signTarget by remember { mutableStateOf<InterventionEntity?>(null) }

    val filtered = remember(state.interventions, query, statusFilter) {
        state.interventions.filter { i ->
            val matchStatus = statusFilter == "TOUS" || i.status == statusFilter
            val matchQuery = query.isBlank() ||
                i.reference.contains(query, ignoreCase = true) ||
                i.title.contains(query, ignoreCase = true) ||
                i.clientName.contains(query, ignoreCase = true) ||
                i.technicianName.contains(query, ignoreCase = true)
            matchStatus && matchQuery
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
                    placeholder = "Rechercher référence, client, technicien...",
                    filters = listOf("TOUS" to "Toutes") + InterventionStatus.entries.map { it.name to it.label },
                    selectedFilter = statusFilter,
                    onFilterSelect = { statusFilter = it }
                )
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Build,
                        title = "Aucune intervention",
                        subtitle = "Créez une nouvelle intervention avec le bouton +."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDetail = item }
                            .testTag("intervention_card_${item.id}")
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
                                Text(
                                    text = item.reference,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (item.syncPending) {
                                        StatusBadge(label = "À sync", statusKey = "A_PLANIFIER")
                                    }
                                    StatusBadge(label = item.priority, statusKey = item.priority)
                                    StatusBadge(label = item.status.replace('_', ' '), statusKey = item.status)
                                }
                            }
                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${item.clientName} • ${item.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${item.scheduledDate} ${item.scheduledTime} (${item.durationMinutes} min) • ${item.technicianName}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Row {
                                    if (item.status != "TERMINEE" && item.status != "ANNULEE") {
                                        IconButton(onClick = { signTarget = item }) {
                                            Icon(Icons.Default.Draw, contentDescription = "Signer & Clôturer", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    IconButton(onClick = {
                                        editing = item
                                        showForm = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Modifier")
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
                editing = null
                showForm = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_intervention_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Créer une intervention")
        }
    }

    if (showForm) {
        InterventionFormDialog(
            initial = editing,
            clients = state.clients,
            employees = state.employees.filter { it.role == "TECHNICIEN" || it.role == "CHEF_EQUIPE" },
            onDismiss = { showForm = false },
            onSave = {
                onSaveIntervention(it)
                showForm = false
            }
        )
    }

    selectedDetail?.let { item ->
        val logs by observeLogs(item.id).collectAsStateWithLifecycle(initialValue = emptyList())
        var reportNotes by remember(item.id) { mutableStateOf(item.reportNotes) }
        var createInvoice by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { selectedDetail = null },
            title = { Text("${item.reference} — ${item.title}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Client : ${item.clientName}", style = MaterialTheme.typography.titleSmall)
                    Text("Lieu : ${item.location}", style = MaterialTheme.typography.bodySmall)
                    Text("Technicien : ${item.technicianName}", style = MaterialTheme.typography.bodySmall)
                    Text("Date : ${item.scheduledDate} à ${item.scheduledTime} (${item.durationMinutes} min)", style = MaterialTheme.typography.bodySmall)
                    Text("Description : ${item.description}", style = MaterialTheme.typography.bodySmall)
                    if (item.equipmentSummary.isNotBlank()) {
                        Text("Matériel : ${item.equipmentSummary}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.signatureAuthor.isNotBlank()) {
                        Text("Signature client : ${item.signatureAuthor}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider()
                    Text("Changer le statut :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("PLANIFIEE", "EN_COURS", "EN_ATTENTE").forEach { st ->
                            FilterChip(
                                selected = item.status == st,
                                onClick = {
                                    onUpdateStatus(item, st, reportNotes, item.signatureAuthor, false)
                                    selectedDetail = null
                                },
                                label = { Text(st.replace('_', ' '), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = reportNotes,
                        onValueChange = { reportNotes = it },
                        label = { Text("Compte rendu technicien") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = createInvoice, onCheckedChange = { createInvoice = it })
                        Text("Générer une facture brouillon à la clôture", style = MaterialTheme.typography.bodySmall)
                    }

                    HorizontalDivider()
                    Text("Historique des opérations (${logs.size})", style = MaterialTheme.typography.titleSmall)
                    logs.forEach { log ->
                        val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(log.timestamp))
                        Text("• [$timeStr] ${log.action} (${log.authorName})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        signTarget = item.copy(reportNotes = reportNotes)
                        selectedDetail = null
                    }
                ) { Text("Signer & Clôturer") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onDeleteIntervention(item)
                            selectedDetail = null
                        }
                    ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { selectedDetail = null }) { Text("Fermer") }
                }
            }
        )
    }

    signTarget?.let { target ->
        SignaturePadDialog(
            initialSigner = target.signatureAuthor,
            onDismiss = { signTarget = null },
            onConfirm = { signerName, _ ->
                onUpdateStatus(target, "TERMINEE", target.reportNotes.ifBlank { "Prestation réalisée avec succès." }, signerName, true)
                signTarget = null
            }
        )
    }
}

@Composable
private fun InterventionFormDialog(
    initial: InterventionEntity?,
    clients: List<ClientEntity>,
    employees: List<EmployeeEntity>,
    onDismiss: () -> Unit,
    onSave: (InterventionEntity) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date()) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var selectedClient by remember { mutableStateOf(clients.find { it.id == initial?.clientId } ?: clients.firstOrNull()) }
    var selectedTech by remember { mutableStateOf(employees.find { it.id == initial?.technicianId }) }
    var location by remember { mutableStateOf(initial?.location ?: selectedClient?.let { "${it.address}, ${it.city}" } ?: "") }
    var date by remember { mutableStateOf(initial?.scheduledDate ?: today) }
    var time by remember { mutableStateOf(initial?.scheduledTime ?: "10:00") }
    var duration by remember { mutableStateOf((initial?.durationMinutes ?: 90).toString()) }
    var priority by remember { mutableStateOf(initial?.priority ?: "NORMALE") }
    var status by remember { mutableStateOf(initial?.status ?: "PLANIFIEE") }
    var equipmentSummary by remember { mutableStateOf(initial?.equipmentSummary ?: "") }
    var photoNote by remember { mutableStateOf(initial?.photoNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvelle intervention" else "Modifier ${initial.reference}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Objet / Prestation *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description du problème") }, modifier = Modifier.fillMaxWidth())

                Text("Client :", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    clients.take(3).forEach { c ->
                        FilterChip(
                            selected = selectedClient?.id == c.id,
                            onClick = {
                                selectedClient = c
                                if (location.isBlank()) location = "${c.address}, ${c.city}"
                            },
                            label = { Text(c.name.take(14), maxLines = 1) }
                        )
                    }
                }

                Text("Technicien affecté :", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    employees.take(3).forEach { e ->
                        FilterChip(
                            selected = selectedTech?.id == e.id,
                            onClick = { selectedTech = e },
                            label = { Text(e.fullName.substringBefore(' '), maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Lieu d'intervention") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Heure") }, singleLine = true, modifier = Modifier.weight(0.7f))
                }
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Durée estimée (min)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Text("Priorité :", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PriorityLevel.entries.forEach { p ->
                        FilterChip(selected = priority == p.name, onClick = { priority = p.name }, label = { Text(p.label, style = MaterialTheme.typography.labelSmall) })
                    }
                }

                OutlinedTextField(value = equipmentSummary, onValueChange = { equipmentSummary = it }, label = { Text("Matériel / Pièces nécessaires") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = photoNote, onValueChange = { photoNote = it }, label = { Text("Pièces jointes / Réf. photo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val client = selectedClient ?: return@Button
                    if (title.isNotBlank()) {
                        val ref = initial?.reference ?: "INT-2026-${(110..999).random()}"
                        onSave(
                            (initial ?: InterventionEntity(
                                reference = ref,
                                title = title,
                                description = description,
                                clientId = client.id,
                                clientName = client.name,
                                location = location
                            )).copy(
                                title = title.trim(),
                                description = description.trim(),
                                clientId = client.id,
                                clientName = client.name,
                                location = location.trim().ifBlank { "${client.address}, ${client.city}" },
                                technicianId = selectedTech?.id,
                                technicianName = selectedTech?.fullName ?: "Non affecté",
                                status = if (initial == null && selectedTech == null) "A_PLANIFIER" else status,
                                priority = priority,
                                scheduledDate = date.trim(),
                                scheduledTime = time.trim(),
                                durationMinutes = duration.toIntOrNull() ?: 90,
                                equipmentSummary = equipmentSummary.trim(),
                                photoNote = photoNote.trim()
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
