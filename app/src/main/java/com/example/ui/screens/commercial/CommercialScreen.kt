package com.example.ui.screens.commercial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.export.ExportManager
import com.example.data.local.ClientEntity
import com.example.data.local.CommercialDocEntity
import com.example.ui.components.SearchFilterBar
import com.example.ui.components.StatusBadge
import com.example.viewmodel.GestionUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CommercialScreen(
    state: GestionUiState,
    onSaveDoc: (CommercialDocEntity) -> Unit,
    onDeleteDoc: (CommercialDocEntity) -> Unit,
    onConvertDevis: (CommercialDocEntity) -> Unit,
    onRecordPayment: (CommercialDocEntity, Double) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var docFilter by remember { mutableStateOf("TOUS") }
    var showForm by remember { mutableStateOf(false) }
    var editingDoc by remember { mutableStateOf<CommercialDocEntity?>(null) }
    var payTarget by remember { mutableStateOf<CommercialDocEntity?>(null) }

    val filtered = remember(state.commercialDocs, query, docFilter) {
        state.commercialDocs.filter { d ->
            val matchType = when (docFilter) {
                "DEVIS" -> d.docType == "DEVIS"
                "FACTURE" -> d.docType == "FACTURE"
                "IMPAYEE" -> d.docType == "FACTURE" && (d.status == "IMPAYEE" || d.status == "PARTIELLE")
                else -> true
            }
            val matchQ = query.isBlank() ||
                d.docNumber.contains(query, ignoreCase = true) ||
                d.clientName.contains(query, ignoreCase = true) ||
                d.title.contains(query, ignoreCase = true)
            matchType && matchQ
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
                    placeholder = "Rechercher devis, facture, client...",
                    filters = listOf(
                        "TOUS" to "Tous (${state.commercialDocs.size})",
                        "DEVIS" to "Devis",
                        "FACTURE" to "Factures",
                        "IMPAYEE" to "Impayés"
                    ),
                    selectedFilter = docFilter,
                    onFilterSelect = { docFilter = it }
                )
            }

            items(filtered, key = { it.id }) { doc ->
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
                            Text(
                                "${doc.docType} • ${doc.docNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatusBadge(label = doc.status, statusKey = doc.status)
                        }
                        Text(doc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${doc.clientName} • Émis le ${doc.issueDate} (Éch. ${doc.dueDate})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Total TTC : ${String.format(Locale.FRANCE, "%.2f €", doc.totalTtc)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (doc.docType == "FACTURE") {
                                    Text(
                                        "Réglé : ${String.format(Locale.FRANCE, "%.2f €", doc.amountPaid)} • Reste : ${String.format(Locale.FRANCE, "%.2f €", doc.remainingDue)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                IconButton(onClick = {
                                    editingDoc = doc
                                    showForm = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifier document")
                                }
                                IconButton(onClick = { onDeleteDoc(doc) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer document", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val file = ExportManager.exportCommercialDocPdf(context, doc)
                                        ExportManager.shareFile(context, file, "application/pdf", "${doc.docType} ${doc.docNumber}")
                                        onShowMessage("PDF généré : ${file.name}")
                                    }
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PDF")
                                }

                                if (doc.docType == "DEVIS") {
                                    Button(onClick = { onConvertDevis(doc) }) {
                                        Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Facturer")
                                    }
                                } else if (doc.remainingDue > 0.01) {
                                    Button(onClick = { payTarget = doc }) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Encaisser")
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
                editingDoc = null
                showForm = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouveau devis ou facture")
        }
    }

    if (showForm) {
        CommercialFormDialog(
            initial = editingDoc,
            clients = state.clients,
            onDismiss = { showForm = false },
            onSave = {
                onSaveDoc(it)
                showForm = false
            }
        )
    }

    payTarget?.let { doc ->
        var amountStr by remember { mutableStateOf(String.format(Locale.US, "%.2f", doc.remainingDue)) }
        AlertDialog(
            onDismissRequest = { payTarget = null },
            title = { Text("Règlement ${doc.docNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reste dû : ${String.format(Locale.FRANCE, "%.2f €", doc.remainingDue)}")
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Montant encaissé / acompte (€)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.replace(',', '.').toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onRecordPayment(doc, amt)
                            payTarget = null
                        }
                    }
                ) { Text("Enregistrer règlement") }
            },
            dismissButton = { TextButton(onClick = { payTarget = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun CommercialFormDialog(
    initial: CommercialDocEntity?,
    clients: List<ClientEntity>,
    onDismiss: () -> Unit,
    onSave: (CommercialDocEntity) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date()) }
    val duePlus30 = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(cal.time)
    }

    var docType by remember(initial) { mutableStateOf(initial?.docType ?: "DEVIS") }
    var selectedClient by remember(initial, clients) {
        mutableStateOf(clients.find { it.id == initial?.clientId } ?: clients.firstOrNull())
    }
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var amountHt by remember(initial) { mutableStateOf((initial?.amountHt ?: 950.0).toString()) }
    var itemsSummary by remember(initial) { mutableStateOf(initial?.itemsSummary ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau document commercial" else "Modifier ${initial.docNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = docType == "DEVIS", onClick = { docType = "DEVIS" }, label = { Text("Devis") })
                    FilterChip(selected = docType == "FACTURE", onClick = { docType = "FACTURE" }, label = { Text("Facture") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    clients.take(3).forEach { c ->
                        FilterChip(
                            selected = selectedClient?.id == c.id,
                            onClick = { selectedClient = c },
                            label = { Text(c.name.take(14), maxLines = 1) }
                        )
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Objet *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amountHt, onValueChange = { amountHt = it }, label = { Text("Montant HT (€)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = itemsSummary, onValueChange = { itemsSummary = it }, label = { Text("Lignes / Prestations") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val client = selectedClient ?: return@Button
                    if (title.isNotBlank()) {
                        val prefix = if (docType == "DEVIS") "DEV" else "FAC"
                        val year = SimpleDateFormat("yyyy", Locale.FRANCE).format(Date())
                        onSave(
                            (initial ?: CommercialDocEntity(
                                docNumber = "$prefix-$year-${(200..999).random()}",
                                docType = docType,
                                clientId = client.id,
                                clientName = client.name,
                                title = title.trim(),
                                amountHt = amountHt.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                status = if (docType == "DEVIS") "ENVOYE" else "IMPAYEE",
                                issueDate = today,
                                dueDate = duePlus30,
                                itemsSummary = itemsSummary.trim()
                            )).copy(
                                docType = docType,
                                clientId = client.id,
                                clientName = client.name,
                                title = title.trim(),
                                amountHt = amountHt.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                itemsSummary = itemsSummary.trim()
                            )
                        )
                    }
                }
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
