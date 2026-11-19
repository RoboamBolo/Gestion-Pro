package com.example.ui.screens.timetracking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.TimeEntryEntity
import com.example.ui.components.StatusBadge
import com.example.viewmodel.GestionUiState

@Composable
fun TimeTrackingScreen(
    state: GestionUiState,
    onCheckIn: (String) -> Unit,
    onCheckOut: (TimeEntryEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeShift = remember(state.timeEntries, state.currentUser) {
        state.timeEntries.firstOrNull { it.employeeId == state.currentUser?.id && it.status == "EN_POSTE" }
    }
    var activityNote by remember(activeShift?.id) {
        mutableStateOf(activeShift?.activityNote ?: "")
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Pointage express", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                state.currentUser?.fullName ?: "Collaborateur",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(
                            label = if (activeShift != null) "En poste depuis ${activeShift.checkInTime}" else "Hors poste",
                            statusKey = if (activeShift != null) "EN_POSTE" else "BROUILLON"
                        )
                    }

                    OutlinedTextField(
                        value = activityNote,
                        onValueChange = { activityNote = it },
                        label = { Text("Activité / Chantier du jour") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (activeShift == null) {
                        Button(
                            onClick = { onCheckIn(activityNote) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Pointer l'arrivée")
                        }
                    } else {
                        Button(
                            onClick = { onCheckOut(activeShift, activityNote) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Pointer le départ")
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Feuilles de temps & absences (${state.timeEntries.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(state.timeEntries, key = { it.id }) { entry ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${entry.employeeName} • ${entry.date}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        StatusBadge(label = entry.status.replace('_', ' '), statusKey = entry.status)
                    }
                    Text(
                        "Arrivée : ${entry.checkInTime}" +
                            (if (entry.checkOutTime.isNotBlank()) " • Départ : ${entry.checkOutTime}" else " • En cours"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (entry.workedMinutes > 0 || entry.overtimeMinutes > 0 || entry.lateMinutes > 0) {
                        Text(
                            "Travail : ${entry.workedMinutes / 60}h${entry.workedMinutes % 60}m • Sup : ${entry.overtimeMinutes} min • Retard : ${entry.lateMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (entry.activityNote.isNotBlank()) {
                        Text(entry.activityNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
