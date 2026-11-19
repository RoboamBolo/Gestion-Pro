package com.example.ui.screens.planning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.EmployeeEntity
import com.example.data.local.InterventionEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Amber600
import com.example.ui.theme.Red600
import com.example.viewmodel.GestionUiState

@Composable
fun PlanningScreen(
    state: GestionUiState,
    onReschedule: (InterventionEntity, String, String, EmployeeEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE).format(java.util.Date())
    }
    var mode by remember { mutableStateOf("JOUR") } // JOUR, SEMAINE, MOIS
    var selectedDate by remember { mutableStateOf(today) }
    var selectedTeam by remember { mutableStateOf("TOUTES") }
    var movingItem by remember { mutableStateOf<InterventionEntity?>(null) }

    val dates = remember(state.interventions, today) {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE)
        val systemWindow = (-1..3).map { offset ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, offset)
            fmt.format(cal.time)
        }
        (systemWindow + state.interventions.map { it.scheduledDate }).distinct().sorted()
    }
    val teams = remember(state.employees) {
        listOf("TOUTES") + state.employees.map { it.teamName }.distinct()
    }

    val technicians = remember(state.employees, selectedTeam) {
        state.employees.filter {
            (it.role == "TECHNICIEN" || it.role == "CHEF_EQUIPE") &&
                (selectedTeam == "TOUTES" || it.teamName == selectedTeam)
        }
    }

    val visibleInterventions = remember(state.interventions, mode, selectedDate, technicians, selectedTeam) {
        val allowedTechIds = technicians.map { it.id }.toSet()
        state.interventions.filter { i ->
            val matchMode = when (mode) {
                "JOUR" -> i.scheduledDate == selectedDate
                "SEMAINE" -> i.scheduledDate in dates
                else -> true
            }
            val matchTeam = selectedTeam == "TOUTES" || (i.technicianId != null && i.technicianId in allowedTechIds)
            matchMode && matchTeam
        }
    }

    // Detect planning conflicts: same technician + same date + same time
    val conflicts = remember(state.interventions) {
        state.interventions
            .filter { it.technicianId != null && it.status !in listOf("TERMINEE", "ANNULEE") }
            .groupBy { "${it.technicianId}|${it.scheduledDate}|${it.scheduledTime}" }
            .filter { it.value.size > 1 }
            .values.flatten().map { it.id }.toSet()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("JOUR" to "Quotidien", "SEMAINE" to "Hebdo", "MOIS" to "Mensuel").forEachIndexed { idx, (key, label) ->
                    SegmentedButton(
                        selected = mode == key,
                        onClick = { mode = key },
                        shape = SegmentedButtonDefaults.itemShape(idx, 3)
                    ) { Text(label) }
                }
            }
        }

        if (mode == "JOUR") {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dates.forEach { d ->
                        FilterChip(
                            selected = selectedDate == d,
                            onClick = { selectedDate = d },
                            label = { Text(d.substring(5)) }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                teams.forEach { t ->
                    FilterChip(
                        selected = selectedTeam == t,
                        onClick = { selectedTeam = t },
                        label = { Text(t) }
                    )
                }
            }
        }

        if (conflicts.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Red600)
                        Text(
                            "Conflit de planning détecté (${conflicts.size} interventions se chevauchent). Touchez une carte pour réaffecter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Disponibilités des techniciens",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                technicians.forEach { tech ->
                    val count = visibleInterventions.count { it.technicianId == tech.id }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(tech.fullName, style = MaterialTheme.typography.titleSmall)
                            Text("${tech.teamName} • $count mission(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            StatusBadge(label = tech.availability.replace('_', ' '), statusKey = tech.availability)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Créneaux & affectations (${visibleInterventions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (visibleInterventions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.CalendarMonth,
                    title = "Aucune intervention sur cette période",
                    subtitle = "Changez de jour ou passez en vue Hebdo / Mensuel."
                )
            }
        } else {
            items(visibleInterventions, key = { "plan_${it.id}" }) { item ->
                val isConflict = item.id in conflicts
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isConflict) 1.5.dp else 1.dp,
                        color = if (isConflict) Red600 else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { movingItem = item }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${item.scheduledDate} • ${item.scheduledTime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (isConflict) {
                                    Text("CONFLIT", style = MaterialTheme.typography.labelSmall, color = Amber600, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("${item.reference} — ${item.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${item.clientName} • ${item.technicianName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusBadge(label = item.status.replace('_', ' '), statusKey = item.status)
                            TextButton(onClick = { movingItem = item }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Déplacer", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    movingItem?.let { item ->
        var newDate by remember { mutableStateOf(item.scheduledDate) }
        var newTime by remember { mutableStateOf(item.scheduledTime) }
        var newTech by remember { mutableStateOf(state.employees.find { it.id == item.technicianId }) }
        val assignable = state.employees.filter { it.role == "TECHNICIEN" || it.role == "CHEF_EQUIPE" }

        AlertDialog(
            onDismissRequest = { movingItem = null },
            title = { Text("Déplacer / Réaffecter ${item.reference}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Date rapide :", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dates.forEach { d ->
                            FilterChip(selected = newDate == d, onClick = { newDate = d }, label = { Text(d.substring(5)) })
                        }
                    }
                    OutlinedTextField(value = newTime, onValueChange = { newTime = it }, label = { Text("Heure (HH:mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Technicien :", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        assignable.forEach { tech ->
                            FilterChip(
                                selected = newTech?.id == tech.id,
                                onClick = { newTech = tech },
                                label = { Text("${tech.fullName} (${tech.availability})") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReschedule(item, newDate, newTime, newTech)
                        movingItem = null
                    }
                ) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { movingItem = null }) { Text("Annuler") }
            }
        )
    }
}
