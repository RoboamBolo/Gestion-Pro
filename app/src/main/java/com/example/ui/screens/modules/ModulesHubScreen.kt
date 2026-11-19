package com.example.ui.screens.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EmployeeRole
import com.example.ui.navigation.Routes
import com.example.ui.theme.*
import com.example.viewmodel.GestionUiState

private data class ModuleTile(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val allowed: Boolean = true
)

@Composable
fun ModulesHubScreen(
    state: GestionUiState,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val role = EmployeeRole.entries.find { it.name == state.currentUser?.role } ?: EmployeeRole.TECHNICIEN
    val m = state.metrics

    val tiles = listOf(
        ModuleTile(Routes.TEAM, "Équipe & Techniciens", "${state.employees.size} collaborateurs", Icons.Default.Badge, Blue700, role.canAccessTeamAdmin() || role == EmployeeRole.PLANIFICATEUR || role == EmployeeRole.CHEF_EQUIPE),
        ModuleTile(Routes.EQUIPMENTS, "Équipements & SAV Tech", "${state.equipments.size} matériels", Icons.Default.PrecisionManufacturing, Indigo600),
        ModuleTile(Routes.STOCK, "Gestion des Stocks", "${m.lowStockCount} alerte(s) seuil", Icons.Default.Inventory2, if (m.lowStockCount > 0) Red600 else Cyan600),
        ModuleTile(Routes.COMMERCIAL, "Devis & Facturation", "${state.commercialDocs.size} documents", Icons.Default.ReceiptLong, Emerald600, role.canAccessBilling()),
        ModuleTile(Routes.SAV, "Gestion du SAV", "${state.savTickets.count { it.status != "CLOS" }} dossier(s) actif(s)", Icons.Default.SupportAgent, Amber600),
        ModuleTile(Routes.TIMETRACKING, "Pointage Personnel", "${state.timeEntries.count { it.status == "EN_POSTE" }} en poste", Icons.Default.AccessTimeFilled, Sky500),
        ModuleTile(Routes.REPORTS, "Rapports & Exports", "PDF & Excel (CSV)", Icons.Default.Assessment, Blue600, role.canAccessReports()),
        ModuleTile(Routes.NOTIFICATIONS, "Alertes & Paramètres", "${m.unreadNotificationsCount} non lue(s)", Icons.Default.Notifications, Navy800)
    ).filter { it.allowed }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 155.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tiles, key = { it.route }) { tile ->
            Surface(
                onClick = { onNavigate(tile.route) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("module_tile_${tile.route}")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Surface(
                        color = tile.color.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            imageVector = tile.icon,
                            contentDescription = tile.title,
                            tint = tile.color,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(tile.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(tile.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
