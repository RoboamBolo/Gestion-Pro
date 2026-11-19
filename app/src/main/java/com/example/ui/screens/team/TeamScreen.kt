package com.example.ui.screens.team

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.EmployeeEntity
import com.example.data.model.EmployeeAvailability
import com.example.data.model.EmployeeRole
import com.example.ui.components.SearchFilterBar
import com.example.ui.components.StatusBadge
import com.example.viewmodel.GestionUiState

@Composable
fun TeamScreen(
    state: GestionUiState,
    onSaveEmployee: (EmployeeEntity) -> Unit,
    onUpdateAvailability: (EmployeeEntity, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf("TOUS") }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EmployeeEntity?>(null) }

    val filtered = remember(state.employees, query, roleFilter) {
        state.employees.filter { e ->
            val matchRole = roleFilter == "TOUS" || e.role == roleFilter
            val matchQ = query.isBlank() ||
                e.fullName.contains(query, ignoreCase = true) ||
                e.teamName.contains(query, ignoreCase = true)
            matchRole && matchQ
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
                    placeholder = "Rechercher un collaborateur, une équipe...",
                    filters = listOf("TOUS" to "Tous") + EmployeeRole.entries.map { it.name to it.label },
                    selectedFilter = roleFilter,
                    onFilterSelect = { roleFilter = it }
                )
            }

            items(filtered, key = { it.id }) { emp ->
                val roleLabel = EmployeeRole.entries.find { it.name == emp.role }?.label ?: emp.role
                val completedCount = state.interventions.count { it.technicianId == emp.id && it.status == "TERMINEE" }
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(emp.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("$roleLabel • ${emp.teamName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(label = emp.availability.replace('_', ' '), statusKey = emp.availability)
                        }

                        Text("${emp.phone} • ${emp.email}", style = MaterialTheme.typography.bodySmall)
                        if (emp.leaveNote.isNotBlank()) {
                            Text("Absence/Congé : ${emp.leaveNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Performance : ${emp.performanceScore}%", style = MaterialTheme.typography.labelMedium)
                            LinearProgressIndicator(
                                progress = { emp.performanceScore / 100f },
                                modifier = Modifier.weight(1f)
                            )
                            Text("$completedCount clôturée(s)", style = MaterialTheme.typography.labelSmall)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EmployeeAvailability.entries.forEach { av ->
                                FilterChip(
                                    selected = emp.availability == av.name,
                                    onClick = {
                                        val note = if (av == EmployeeAvailability.CONGE) "Congé planifié" else ""
                                        onUpdateAvailability(emp, av.name, note)
                                    },
                                    label = { Text(av.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            IconButton(onClick = {
                                editing = emp
                                showForm = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier collaborateur")
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
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter collaborateur")
        }
    }

    if (showForm) {
        var fullName by remember { mutableStateOf(editing?.fullName ?: "") }
        var role by remember { mutableStateOf(editing?.role ?: "TECHNICIEN") }
        var team by remember { mutableStateOf(editing?.teamName ?: "Équipe Terrain A") }
        var phone by remember { mutableStateOf(editing?.phone ?: "") }
        var email by remember { mutableStateOf(editing?.email ?: "") }

        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (editing == null) "Nouveau collaborateur" else "Modifier collaborateur") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Nom complet *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = team, onValueChange = { team = it }, label = { Text("Équipe / Pôle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Rôle :", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EmployeeRole.entries.forEach { r ->
                            FilterChip(selected = role == r.name, onClick = { role = r.name }, label = { Text(r.label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fullName.isNotBlank()) {
                            onSaveEmployee(
                                (editing ?: EmployeeEntity(fullName = fullName)).copy(
                                    fullName = fullName.trim(),
                                    role = role,
                                    teamName = team.trim(),
                                    phone = phone.trim(),
                                    email = email.trim()
                                )
                            )
                            showForm = false
                        }
                    }
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { showForm = false }) { Text("Annuler") }
            }
        )
    }
}
