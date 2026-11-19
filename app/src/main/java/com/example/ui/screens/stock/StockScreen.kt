package com.example.ui.screens.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.StockItemEntity
import com.example.ui.components.SearchFilterBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Red600
import com.example.viewmodel.GestionUiState
import java.util.Locale

@Composable
fun StockScreen(
    state: GestionUiState,
    onSaveStockItem: (StockItemEntity) -> Unit,
    onDeleteStockItem: (StockItemEntity) -> Unit,
    onRecordMovement: (StockItemEntity, String, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("TOUS") }
    var showForm by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<StockItemEntity?>(null) }
    var moveTarget by remember { mutableStateOf<StockItemEntity?>(null) }

    val lowItems = remember(state.stockItems) {
        state.stockItems.filter { it.quantity <= it.minThreshold }
    }

    val filtered = remember(state.stockItems, query, filterMode) {
        state.stockItems.filter { item ->
            val matchFilter = when (filterMode) {
                "ALERTES" -> item.quantity <= item.minThreshold
                else -> true
            }
            val matchQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.sku.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.supplierName.contains(query, ignoreCase = true)
            matchFilter && matchQuery
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (lowItems.isNotEmpty()) {
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
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Red600)
                            Text(
                                "${lowItems.size} article(s) sous le seuil d'alerte : réapprovisionnement conseillé.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            item {
                SearchFilterBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Rechercher référence, article, fournisseur...",
                    filters = listOf(
                        "TOUS" to "Tout le stock (${state.stockItems.size})",
                        "ALERTES" to "Alertes seuil (${lowItems.size})"
                    ),
                    selectedFilter = filterMode,
                    onFilterSelect = { filterMode = it }
                )
            }

            items(filtered, key = { it.id }) { item ->
                val isLow = item.quantity <= item.minThreshold
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isLow) 1.5.dp else 1.dp,
                        color = if (isLow) Red600 else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.sku, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            StatusBadge(
                                label = if (isLow) "Seuil critique" else "En stock",
                                statusKey = if (isLow) "IMPAYEE" else "OPERATIONNEL"
                            )
                        }
                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${item.category} • Fournisseur : ${item.supplierName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Qté : ${item.quantity} ${item.unit} (Seuil : ${item.minThreshold})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isLow) Red600 else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "PU : ${String.format(Locale.FRANCE, "%.2f €", item.unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    editingItem = item
                                    showForm = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifier article")
                                }
                                IconButton(onClick = { onDeleteStockItem(item) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer article", tint = MaterialTheme.colorScheme.error)
                                }
                                OutlinedButton(onClick = { moveTarget = item }) {
                                    Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Mouvement")
                                }
                            }
                        }
                    }
                }
            }

            if (state.stockMovements.isNotEmpty()) {
                item {
                    Text(
                        "Derniers mouvements de stock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.stockMovements.take(6), key = { "mvt_${it.id}" }) { mvt ->
                    val sign = if (mvt.quantityDelta >= 0) "+${mvt.quantityDelta}" else "${mvt.quantityDelta}"
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${mvt.itemName} ($sign)", style = MaterialTheme.typography.titleSmall)
                                Text(mvt.targetOrReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(label = mvt.type, statusKey = if (mvt.quantityDelta > 0) "PAYEE" else "A_PLANIFIER")
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingItem = null
                showForm = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouvel article")
        }
    }

    if (showForm) {
        var sku by remember(editingItem) { mutableStateOf(editingItem?.sku ?: "STK-${(100..999).random()}") }
        var name by remember(editingItem) { mutableStateOf(editingItem?.name ?: "") }
        var category by remember(editingItem) { mutableStateOf(editingItem?.category ?: "Consommables") }
        var qty by remember(editingItem) { mutableStateOf((editingItem?.quantity ?: 10).toString()) }
        var threshold by remember(editingItem) { mutableStateOf((editingItem?.minThreshold ?: 3).toString()) }
        var unitPrice by remember(editingItem) { mutableStateOf((editingItem?.unitPrice ?: 25.0).toString()) }
        var supplier by remember(editingItem) { mutableStateOf(editingItem?.supplierName ?: "") }

        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (editingItem == null) "Nouvel article en stock" else "Modifier ${editingItem!!.sku}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("Référence SKU") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Désignation *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantité") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = threshold, onValueChange = { threshold = it }, label = { Text("Seuil alerte") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it }, label = { Text("Prix unitaire (€)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Fournisseur") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveStockItem(
                                (editingItem ?: StockItemEntity(
                                    sku = sku.trim(),
                                    name = name.trim(),
                                    category = category.trim(),
                                    quantity = qty.toIntOrNull() ?: 0,
                                    minThreshold = threshold.toIntOrNull() ?: 2
                                )).copy(
                                    sku = sku.trim(),
                                    name = name.trim(),
                                    category = category.trim(),
                                    quantity = qty.toIntOrNull() ?: 0,
                                    minThreshold = threshold.toIntOrNull() ?: 2,
                                    unitPrice = unitPrice.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    supplierName = supplier.trim()
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

    moveTarget?.let { item ->
        var type by remember { mutableStateOf("SORTIE") }
        var qtyStr by remember { mutableStateOf("1") }
        var reason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { moveTarget = null },
            title = { Text("Mouvement : ${item.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = type == "ENTREE", onClick = { type = "ENTREE" }, label = { Text("Entrée") })
                        FilterChip(selected = type == "SORTIE", onClick = { type = "SORTIE" }, label = { Text("Sortie") })
                        FilterChip(selected = type == "AFFECTATION", onClick = { type = "AFFECTATION" }, label = { Text("Affectation") })
                    }
                    OutlinedTextField(value = qtyStr, onValueChange = { qtyStr = it }, label = { Text("Quantité") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Motif / Technicien destinataire") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = qtyStr.toIntOrNull() ?: 1
                        if (q > 0) {
                            onRecordMovement(item, type, q, reason.ifBlank { "Opération atelier" })
                            moveTarget = null
                        }
                    }
                ) { Text("Valider") }
            },
            dismissButton = { TextButton(onClick = { moveTarget = null }) { Text("Annuler") } }
        )
    }
}
