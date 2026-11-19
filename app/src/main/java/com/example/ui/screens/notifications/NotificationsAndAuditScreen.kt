package com.example.ui.screens.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SyncQueueEntity
import com.example.data.model.EmployeeRole
import com.example.ui.components.StatusBadge
import com.example.viewmodel.GestionUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsAndAuditScreen(
    state: GestionUiState,
    onMarkAllRead: () -> Unit,
    onMarkRead: (Long) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleOfflineMode: () -> Unit,
    onSyncNow: () -> Unit,
    onRetrySyncItem: (SyncQueueEntity) -> Unit,
    onResolveSyncConflict: (SyncQueueEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val canAudit = EmployeeRole.entries.find { it.name == state.currentUser?.role }?.canAccessAudit() == true

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Paramètres & Synchronisation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.DarkMode, contentDescription = null)
                            Text("Thème sombre", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(checked = state.isDarkMode, onCheckedChange = { onToggleDarkMode() })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(if (state.isNetworkConnected) Icons.Default.Wifi else Icons.Default.WifiOff, contentDescription = null)
                            Column {
                                Text(
                                    if (state.isNetworkConnected) "Connexion réseau détectée" else "Aucune connexion Internet",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${state.metrics.pendingSyncCount} opération(s) dans la file locale sync_queue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(
                            label = if (state.isNetworkConnected) "Réseau OK" else "Hors réseau",
                            statusKey = if (state.isNetworkConnected) "DISPONIBLE" else "IMPAYEE"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CloudOff, contentDescription = null)
                            Text("Forcer mode hors ligne", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(checked = state.isManualOfflineMode, onCheckedChange = { onToggleOfflineMode() })
                    }
                    if (state.metrics.pendingSyncCount > 0 || state.isOfflineMode) {
                        Button(onClick = onSyncNow, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tenter synchronisation API REST")
                        }
                    }
                }
            }
        }

        if (state.syncQueue.isNotEmpty()) {
            item {
                Text(
                    "File d'attente de synchronisation (${state.syncQueue.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(state.syncQueue.take(15), key = { "sync_${it.id}" }) { q ->
                val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(q.localUpdatedAt))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${q.entityType} #${q.entityId} • ${q.operation}", style = MaterialTheme.typography.titleSmall)
                            StatusBadge(
                                label = q.status,
                                statusKey = when (q.status) {
                                    "SYNCED" -> "TERMINEE"
                                    "FAILED", "CONFLICT" -> "IMPAYEE"
                                    else -> "A_PLANIFIER"
                                }
                            )
                        }
                        Text("Enregistré localement le $timeStr • Tentatives : ${q.retryCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (q.lastError.isNotBlank()) {
                            Text(q.lastError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        if (q.status == "FAILED" || q.status == "CONFLICT") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onRetrySyncItem(q) }) { Text("Réessayer") }
                                if (q.status == "CONFLICT") {
                                    TextButton(onClick = { onResolveSyncConflict(q) }) { Text("Garder local") }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifications & Alertes (${state.notifications.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onMarkAllRead) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tout lire")
                }
            }
        }

        items(state.notifications, key = { "notif_${it.id}" }) { notif ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMarkRead(notif.id) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold)
                        Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(label = notif.category, statusKey = if (notif.isRead) "BROUILLON" else "A_PLANIFIER")
                }
            }
        }

        if (canAudit) {
            item {
                Text(
                    "Journal d'audit sécurité (${state.auditLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(state.auditLogs.take(20), key = { "audit_${it.id}" }) { log ->
                val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(log.timestamp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("[$timeStr] ${log.actorName} (${log.actorRole}) • ${log.moduleName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(log.action, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
