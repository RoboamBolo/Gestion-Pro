package com.example.ui.screens.equipments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.EquipmentEntity
import com.example.data.model.EquipmentStatus
import com.example.ui.components.SearchFilterBar
import com.example.ui.components.StatusBadge
import com.example.viewmodel.GestionUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EquipmentsScreen(
    state: GestionUiState,
    onSaveEquipment: (EquipmentEntity) -> Unit,
    onDeleteEquipment: (EquipmentEntity) -> Unit,
    onAddMaintenance: (EquipmentEntity, String, String, String, Double, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("TOUS") }
    var showForm by remember { mutableStateOf(false) }
    var editingEquipment by remember { mutableStateOf<EquipmentEntity?>(null) }
    var maintTarget by remember { mutableStateOf<EquipmentEntity?>(null) }

    val filtered = remember(state.equipments, query, statusFilter) {
        state.equipments.filter { eq ->
            val matchSt = statusFilter == "TOUS" || eq.status == statusFilter
            val matchQ = query.isBlank() ||
                eq.code.contains(query, ignoreCase = true) ||
                eq.name.contains(query, ignoreCase = true) ||
                eq.brand.contains(query, ignoreCase = true) ||
                eq.clientName.contains(query, ignoreCase = true)
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
                    placeholder = "Rechercher code, marque, N° série, client...",
                    filters = listOf("TOUS" to "Tous") + EquipmentStatus.entries.map { it.name to it.label },
                    selectedFilter = statusFilter,
                    onFilterSelect = { statusFilter = it }
                )
            }

            items(filtered, key = { it.id }) { eq ->
                val records = state.maintenanceRecords.filter { it.equipmentId == eq.id }
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
                            Text(eq.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            StatusBadge(label = eq.status.replace('_', ' '), statusKey = eq.status)
                        }
                        Text(eq.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${eq.brand} ${eq.model} • S/N : ${eq.serialNumber}", style = MaterialTheme.typography.bodySmall)
                        Text("Client / Site : ${eq.clientName} (${eq.location})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Prochaine maintenance : ${eq.nextMaintenanceDate} • Acquis le ${eq.acquisitionDate}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                        if (records.isNotEmpty()) {
                            HorizontalDivider()
                            records.take(2).forEach { rec ->
                                Text(
                                    "• ${rec.date} [${rec.type}] : ${rec.description} (${String.format(Locale.FRANCE, "%.0f €", rec.cost)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                IconButton(onClick = {
                                    editingEquipment = eq
                                    showForm = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifier équipement")
                                }
                                IconButton(onClick = { onDeleteEquipment(eq) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer équipement", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            OutlinedButton(onClick = { maintTarget = eq }) {
                                Icon(Icons.Default.BuildCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Maintenance")
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingEquipment = null
                showForm = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter équipement")
        }
    }

    if (showForm) {
        val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date()) }
        var code by remember(editingEquipment) { mutableStateOf(editingEquipment?.code ?: "EQ-${(100..999).random()}") }
        var name by remember(editingEquipment) { mutableStateOf(editingEquipment?.name ?: "") }
        var category by remember(editingEquipment) { mutableStateOf(editingEquipment?.category ?: "CVC / Technique") }
        var brand by remember(editingEquipment) { mutableStateOf(editingEquipment?.brand ?: "") }
        var model by remember(editingEquipment) { mutableStateOf(editingEquipment?.model ?: "") }
        var serial by remember(editingEquipment) { mutableStateOf(editingEquipment?.serialNumber ?: "") }
        var location by remember(editingEquipment) { mutableStateOf(editingEquipment?.location ?: "") }
        var status by remember(editingEquipment) { mutableStateOf(editingEquipment?.status ?: "OPERATIONNEL") }

        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (editingEquipment == null) "Nouvel équipement" else "Modifier ${editingEquipment!!.code}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Désignation *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marque") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Modèle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = serial, onValueChange = { serial = it }, label = { Text("N° de série") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Localisation") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("OPERATIONNEL", "MAINTENANCE", "EN_PANNE").forEach { st ->
                            FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.replace('_', ' '), style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveEquipment(
                                (editingEquipment ?: EquipmentEntity(
                                    code = code.trim(),
                                    name = name.trim(),
                                    category = category.trim(),
                                    brand = brand.trim(),
                                    model = model.trim(),
                                    serialNumber = serial.trim(),
                                    acquisitionDate = today,
                                    location = location.trim()
                                )).copy(
                                    code = code.trim(),
                                    name = name.trim(),
                                    category = category.trim(),
                                    brand = brand.trim(),
                                    model = model.trim(),
                                    serialNumber = serial.trim(),
                                    location = location.trim(),
                                    status = status
                                )
                            )
                            showForm = false
                        }
                    }
                ) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("Annuler") } }
        )
    }

    maintTarget?.let { eq ->
        val defaultNext = remember {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 90)
            SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(cal.time)
        }
        var type by remember { mutableStateOf("PREVENTIVE") }
        var desc by remember { mutableStateOf("") }
        var cost by remember { mutableStateOf("180") }
        var nextDate by remember { mutableStateOf(defaultNext) }
        var newStatus by remember { mutableStateOf("OPERATIONNEL") }

        AlertDialog(
            onDismissRequest = { maintTarget = null },
            title = { Text("Maintenance : ${eq.code}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == "PREVENTIVE", onClick = { type = "PREVENTIVE" }, label = { Text("Préventive") })
                        FilterChip(selected = type == "CORRECTIVE", onClick = { type = "CORRECTIVE" }, label = { Text("Corrective") })
                    }
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Opérations réalisées *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Coût (€)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nextDate, onValueChange = { nextDate = it }, label = { Text("Prochaine échéance") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = newStatus == "OPERATIONNEL", onClick = { newStatus = "OPERATIONNEL" }, label = { Text("Opérationnel") })
                        FilterChip(selected = newStatus == "MAINTENANCE", onClick = { newStatus = "MAINTENANCE" }, label = { Text("Maintenance") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (desc.isNotBlank()) {
                            onAddMaintenance(
                                eq, type, desc.trim(),
                                state.currentUser?.fullName ?: "Technicien",
                                cost.toDoubleOrNull() ?: 0.0,
                                nextDate.trim(),
                                newStatus
                            )
                            maintTarget = null
                        }
                    }
                ) { Text("Valider") }
            },
            dismissButton = { TextButton(onClick = { maintTarget = null }) { Text("Annuler") } }
        )
    }
}
