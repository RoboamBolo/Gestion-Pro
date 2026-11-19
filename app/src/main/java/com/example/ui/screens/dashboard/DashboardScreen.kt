package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.EmployeeRole
import com.example.ui.components.KpiStatCard
import com.example.ui.components.MonthlyActivityChart
import com.example.ui.components.MonthlyChartPoint
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Routes
import com.example.ui.theme.*
import com.example.viewmodel.GestionUiState
import java.util.Locale

@Composable
fun DashboardScreen(
    state: GestionUiState,
    onNavigate: (String) -> Unit,
    onSyncOffline: () -> Unit,
    onToggleOffline: () -> Unit,
    modifier: Modifier = Modifier
) {
    var annualView by remember { mutableStateOf(false) }
    val m = state.metrics
    val roleLabel = EmployeeRole.entries.find { it.name == state.currentUser?.role }?.label ?: "Collaborateur"

    val chartPoints = if (annualView) state.annualChartPoints else state.monthlyChartPoints

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Welcome Banner + Quick Actions
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dashboard_banner),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Navy900.copy(alpha = 0.94f), Navy800.copy(alpha = 0.80f))
                            )
                        )
                )
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bonjour, ${state.currentUser?.fullName ?: "Équipe"}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$roleLabel • ${state.currentUser?.teamName ?: "Gestion Pro"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFBAE6FD)
                            )
                        }
                        AssistChip(
                            onClick = if (m.pendingSyncCount > 0) onSyncOffline else onToggleOffline,
                            label = {
                                Text(
                                    when {
                                        !state.isNetworkConnected -> "Sans réseau (${m.pendingSyncCount})"
                                        state.isManualOfflineMode -> "Hors ligne (${m.pendingSyncCount})"
                                        m.pendingSyncCount > 0 -> "En attente (${m.pendingSyncCount})"
                                        else -> "En ligne"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (state.isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (state.isOfflineMode) Color(0xFFFBBF24) else Color(0xFF34D399),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.White.copy(alpha = 0.14f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(Routes.INTERVENTIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = Sky500),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Interventions", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { onNavigate(Routes.TIMETRACKING) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pointage", maxLines = 1)
                        }
                    }
                }
            }
        }

        // 2. KPI Grid (2 columns responsive)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Indicateurs clés en temps réel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Clients actifs",
                        value = "${m.totalClients}",
                        subtitle = "Particuliers & Entreprises",
                        icon = Icons.Default.Business,
                        accentColor = Blue700,
                        onClick = { onNavigate(Routes.CLIENTS) },
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_clients"
                    )
                    KpiStatCard(
                        title = "Programmées",
                        value = "${m.scheduledInterventions}",
                        subtitle = "${m.inProgressInterventions} en cours",
                        icon = Icons.Default.EventAvailable,
                        accentColor = Indigo600,
                        onClick = { onNavigate(Routes.PLANNING) },
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_scheduled"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Terminées",
                        value = "${m.completedInterventions}",
                        subtitle = "${m.overdueInterventions} en retard / attente",
                        icon = Icons.Default.CheckCircle,
                        accentColor = if (m.overdueInterventions > 0) Amber600 else Emerald600,
                        onClick = { onNavigate(Routes.INTERVENTIONS) },
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_completed"
                    )
                    KpiStatCard(
                        title = "Techniciens disp.",
                        value = "${m.availableTechnicians}",
                        subtitle = "Sur ${state.employees.count { it.role == "TECHNICIEN" || it.role == "CHEF_EQUIPE" }} terrain",
                        icon = Icons.Default.Engineering,
                        accentColor = Cyan600,
                        onClick = { onNavigate(Routes.TEAM) },
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_technicians"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Chiffre d'affaires",
                        value = "${String.format(Locale.FRANCE, "%.0f", m.totalRevenueTtc)} €",
                        subtitle = "${m.paidInvoicesCount} payées • ${m.unpaidInvoicesCount} impayées",
                        icon = Icons.Default.Euro,
                        accentColor = Emerald600,
                        onClick = { onNavigate(Routes.COMMERCIAL) },
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_revenue"
                    )
                    KpiStatCard(
                        title = "État des stocks",
                        value = "${state.stockItems.size}",
                        subtitle = if (m.lowStockCount > 0) "${m.lowStockCount} alerte(s) seuil" else "Stocks optimaux",
                        icon = Icons.Default.Inventory2,
                        accentColor = if (m.lowStockCount > 0) Red600 else Sky500,
                        onClick = { onNavigate(Routes.STOCK) },
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_stock"
                    )
                }
            }
        }

        // 3. Monthly / Annual Stats Chart
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !annualView,
                            onClick = { annualView = false },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Mensuel") }
                        SegmentedButton(
                            selected = annualView,
                            onClick = { annualView = true },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Annuel") }
                    }
                }
                MonthlyActivityChart(points = chartPoints)
            }
        }

        // 4. Alertes prioritaires
        val unreadAlerts = state.notifications.take(3)
        if (unreadAlerts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alertes & notifications récentes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigate(Routes.NOTIFICATIONS) }) {
                        Text("Tout voir")
                    }
                }
            }
            items(unreadAlerts, key = { "alert_${it.id}" }) { notif ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Routes.NOTIFICATIONS) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Amber600
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                notif.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(label = notif.category, statusKey = if (notif.category == "STOCK") "IMPAYEE" else "A_PLANIFIER")
                    }
                }
            }
        }

        // 5. Activité récente (Interventions)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interventions du jour & récentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onNavigate(Routes.INTERVENTIONS) }) {
                    Text("Ouvrir")
                }
            }
        }
        items(state.interventions.take(4), key = { "dash_int_${it.id}" }) { item ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Routes.INTERVENTIONS) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${item.reference} • ${item.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${item.clientName} — ${item.technicianName} (${item.scheduledDate} ${item.scheduledTime})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(label = item.status.replace('_', ' '), statusKey = item.status)
                }
            }
        }
    }
}
