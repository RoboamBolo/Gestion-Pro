package com.example.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.export.ExportManager
import com.example.ui.components.StatusBadge
import com.example.viewmodel.GestionUiState
import java.util.Locale

@Composable
fun ReportsScreen(
    state: GestionUiState,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedStatus by remember { mutableStateOf("TOUS") }
    var selectedTech by remember { mutableStateOf("TOUS") }

    val techNames = remember(state.employees) {
        listOf("TOUS") + state.employees.filter { it.role == "TECHNICIEN" || it.role == "CHEF_EQUIPE" }.map { it.fullName }
    }

    val filteredInterventions = remember(state.interventions, selectedStatus, selectedTech) {
        state.interventions.filter { i ->
            val matchSt = selectedStatus == "TOUS" || i.status == selectedStatus
            val matchTech = selectedTech == "TOUS" || i.technicianName == selectedTech
            matchSt && matchTech
        }
    }

    val invoices = remember(state.commercialDocs) { state.commercialDocs.filter { it.docType == "FACTURE" } }
    val totalBilled = invoices.sumOf { it.totalTtc }
    val totalCollected = invoices.sumOf { it.amountPaid }
    val stockValue = state.stockItems.sumOf { it.quantity * it.unitPrice }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Centre de Rapports & Exports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Filtrez les opérations et exportez vos rapports en PDF ou CSV compatible Microsoft Excel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val file = ExportManager.exportActivityReportPdf(
                                    context, "Novembre 2026 ($selectedStatus)", filteredInterventions, state.commercialDocs
                                )
                                ExportManager.shareFile(context, file, "application/pdf", "Rapport d'activité Gestion Pro")
                                onShowMessage("Rapport PDF généré : ${file.name}")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Exporter PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                val file = ExportManager.exportCsvForExcel(context, filteredInterventions, state.commercialDocs)
                                ExportManager.shareFile(context, file, "text/csv", "Export Excel Gestion Pro")
                                onShowMessage("Export CSV Excel généré : ${file.name}")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Excel (CSV)")
                        }
                    }
                }
            }
        }

        item {
            Text("Filtres d'analyse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("TOUS", "PLANIFIEE", "EN_COURS", "TERMINEE", "EN_ATTENTE").forEach { st ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { selectedStatus = st },
                        label = { Text(st.replace('_', ' '), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                techNames.forEach { t ->
                    FilterChip(
                        selected = selectedTech == t,
                        onClick = { selectedTech = t },
                        label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ReportSummaryCard(
                    title = "Rapport financier",
                    line1 = "Facturé : ${String.format(Locale.FRANCE, "%.0f €", totalBilled)}",
                    line2 = "Encaissé : ${String.format(Locale.FRANCE, "%.0f €", totalCollected)}",
                    modifier = Modifier.weight(1f)
                )
                ReportSummaryCard(
                    title = "Valorisation stock",
                    line1 = "${state.stockItems.size} références",
                    line2 = "${String.format(Locale.FRANCE, "%.0f € HT", stockValue)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                "Productivité techniciens",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(state.employees.filter { it.role == "TECHNICIEN" || it.role == "CHEF_EQUIPE" }, key = { "rep_emp_${it.id}" }) { tech ->
            val total = state.interventions.count { it.technicianId == tech.id }
            val done = state.interventions.count { it.technicianId == tech.id && it.status == "TERMINEE" }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tech.fullName, style = MaterialTheme.typography.titleSmall)
                        Text("${tech.teamName} • $done/$total terminée(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusBadge(label = "Score ${tech.performanceScore}%", statusKey = "TERMINEE")
                }
            }
        }

        item {
            Text(
                "Interventions filtrées (${filteredInterventions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(filteredInterventions, key = { "rep_int_${it.id}" }) { item ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.reference} — ${item.title}", style = MaterialTheme.typography.titleSmall)
                        Text("${item.clientName} • ${item.scheduledDate} (${item.technicianName})", style = MaterialTheme.typography.bodySmall)
                    }
                    StatusBadge(label = item.status.replace('_', ' '), statusKey = item.status)
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(
    title: String,
    line1: String,
    line2: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(line1, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(line2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
