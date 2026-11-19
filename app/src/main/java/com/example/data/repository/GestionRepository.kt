package com.example.data.repository

import com.example.data.local.*
import com.example.data.sync.OfflineSyncManager
import com.example.data.sync.SyncExecutionReport
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GestionRepository(private val dao: GestionDao) {
    val clients: Flow<List<ClientEntity>> = dao.observeClients()
    val employees: Flow<List<EmployeeEntity>> = dao.observeEmployees()
    val interventions: Flow<List<InterventionEntity>> = dao.observeInterventions()
    val equipments: Flow<List<EquipmentEntity>> = dao.observeEquipments()
    val maintenanceRecords: Flow<List<MaintenanceRecordEntity>> = dao.observeMaintenanceRecords()
    val stockItems: Flow<List<StockItemEntity>> = dao.observeStockItems()
    val stockMovements: Flow<List<StockMovementEntity>> = dao.observeStockMovements()
    val commercialDocs: Flow<List<CommercialDocEntity>> = dao.observeCommercialDocs()
    val savTickets: Flow<List<SavTicketEntity>> = dao.observeSavTickets()
    val timeEntries: Flow<List<TimeEntryEntity>> = dao.observeTimeEntries()
    val notifications: Flow<List<NotificationEntity>> = dao.observeNotifications()
    val auditLogs: Flow<List<AuditLogEntity>> = dao.observeAuditLogs()
    val syncQueue: Flow<List<SyncQueueEntity>> = dao.observeSyncQueue()
    val pendingSyncCount: Flow<Int> = dao.observePendingSyncCount()

    private val syncManager = OfflineSyncManager(dao)

    fun observeInterventionLogs(interventionId: Long): Flow<List<InterventionLogEntity>> =
        dao.observeInterventionLogs(interventionId)

    fun todayDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date())

    fun datePlusDays(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(cal.time)
    }

    fun currentTime(): String = SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date())

    private fun computeWorkedMinutes(checkIn: String, checkOut: String): Int {
        val inParts = checkIn.split(":")
        val outParts = checkOut.split(":")
        if (inParts.size != 2 || outParts.size != 2) return 0
        val inMin = (inParts[0].toIntOrNull() ?: 0) * 60 + (inParts[1].toIntOrNull() ?: 0)
        val outMin = (outParts[0].toIntOrNull() ?: 0) * 60 + (outParts[1].toIntOrNull() ?: 0)
        val diff = outMin - inMin
        return if (diff >= 0) diff.coerceAtLeast(1) else ((24 * 60) - inMin + outMin).coerceAtLeast(1)
    }

    private suspend fun enqueueSync(
        entityType: String,
        entityId: Long,
        operation: String,
        payload: JSONObject
    ) {
        dao.insertSyncQueueItem(
            SyncQueueEntity(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payloadJson = payload.toString(),
                status = "PENDING",
                localUpdatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun logAudit(actor: EmployeeEntity?, action: String, module: String) {
        dao.insertAuditLog(
            AuditLogEntity(
                actorName = actor?.fullName ?: "Système",
                actorRole = actor?.role ?: "SYSTEME",
                action = action,
                moduleName = module
            )
        )
    }

    // --- CLIENTS ---
    suspend fun saveClient(client: ClientEntity, actor: EmployeeEntity?) {
        val isNew = client.id == 0L
        val savedId = if (isNew) dao.insertClient(client) else {
            dao.updateClient(client)
            client.id
        }
        enqueueSync(
            entityType = "CLIENT",
            entityId = savedId,
            operation = if (isNew) "CREATE" else "UPDATE",
            payload = JSONObject().apply {
                put("id", savedId)
                put("name", client.name)
                put("companyName", client.companyName)
                put("type", client.type)
                put("email", client.email)
                put("phone", client.phone)
                put("address", client.address)
                put("city", client.city)
            }
        )
        logAudit(actor, if (isNew) "Création client : ${client.name}" else "Modification client : ${client.name}", "Clients")
    }

    suspend fun deleteClient(client: ClientEntity, actor: EmployeeEntity?) {
        dao.deleteClient(client)
        enqueueSync(
            entityType = "CLIENT",
            entityId = client.id,
            operation = "DELETE",
            payload = JSONObject().apply {
                put("id", client.id)
                put("name", client.name)
            }
        )
        logAudit(actor, "Suppression client : ${client.name}", "Clients")
    }

    // --- EMPLOYEES ---
    suspend fun saveEmployee(employee: EmployeeEntity, actor: EmployeeEntity?) {
        val isNew = employee.id == 0L
        val savedId = if (isNew) dao.insertEmployee(employee) else {
            dao.updateEmployee(employee)
            employee.id
        }
        enqueueSync(
            entityType = "EMPLOYEE",
            entityId = savedId,
            operation = if (isNew) "CREATE" else "UPDATE",
            payload = JSONObject().apply {
                put("id", savedId)
                put("fullName", employee.fullName)
                put("role", employee.role)
                put("teamName", employee.teamName)
                put("availability", employee.availability)
                put("active", employee.active)
            }
        )
        logAudit(actor, if (isNew) "Ajout collaborateur : ${employee.fullName}" else "Mise à jour collaborateur : ${employee.fullName}", "Personnel")
    }

    suspend fun updateEmployeeAvailability(employee: EmployeeEntity, availability: String, leaveNote: String, actor: EmployeeEntity?) {
        val updated = employee.copy(availability = availability, leaveNote = leaveNote)
        dao.updateEmployee(updated)
        enqueueSync(
            entityType = "EMPLOYEE",
            entityId = employee.id,
            operation = "UPDATE",
            payload = JSONObject().apply {
                put("id", employee.id)
                put("availability", availability)
                put("leaveNote", leaveNote)
            }
        )
        logAudit(actor, "Disponibilité de ${employee.fullName} -> $availability", "Personnel")
    }

    // --- INTERVENTIONS & PLANNING ---
    suspend fun saveIntervention(
        intervention: InterventionEntity,
        actor: EmployeeEntity?,
        isOffline: Boolean
    ): Long {
        val isNew = intervention.id == 0L
        val toSave = intervention.copy(syncPending = true)
        val id = if (isNew) dao.insertIntervention(toSave) else {
            dao.updateIntervention(toSave)
            toSave.id
        }
        dao.insertInterventionLog(
            InterventionLogEntity(
                interventionId = id,
                action = if (isNew) "Création de l'intervention (${toSave.status})" else "Mise à jour de la fiche (${toSave.status})",
                authorName = actor?.fullName ?: "Système"
            )
        )
        enqueueSync(
            entityType = "INTERVENTION",
            entityId = id,
            operation = if (isNew) "CREATE" else "UPDATE",
            payload = JSONObject().apply {
                put("id", id)
                put("reference", toSave.reference)
                put("title", toSave.title)
                put("clientId", toSave.clientId)
                put("technicianId", toSave.technicianId ?: JSONObject.NULL)
                put("status", toSave.status)
                put("priority", toSave.priority)
                put("scheduledDate", toSave.scheduledDate)
                put("scheduledTime", toSave.scheduledTime)
            }
        )
        if (isNew) {
            dao.insertNotification(
                NotificationEntity(
                    title = "Nouvelle intervention : ${toSave.reference}",
                    message = "${toSave.title} — ${toSave.clientName}",
                    category = "INTERVENTION"
                )
            )
            if (toSave.technicianId != null) {
                dao.insertNotification(
                    NotificationEntity(
                        title = "Affectation technicien : ${toSave.technicianName}",
                        message = "Mission ${toSave.reference} le ${toSave.scheduledDate} à ${toSave.scheduledTime}",
                        category = "PLANNING"
                    )
                )
            }
        }
        logAudit(actor, "${if (isNew) "Création" else "Modification"} intervention ${toSave.reference}", "Interventions")
        return id
    }

    suspend fun updateInterventionStatus(
        intervention: InterventionEntity,
        newStatus: String,
        reportNotes: String = intervention.reportNotes,
        signatureAuthor: String = intervention.signatureAuthor,
        createDraftInvoice: Boolean = false,
        actor: EmployeeEntity?,
        isOffline: Boolean
    ) {
        val updated = intervention.copy(
            status = newStatus,
            reportNotes = reportNotes,
            signatureAuthor = signatureAuthor,
            isOverdue = if (newStatus == "TERMINEE" || newStatus == "ANNULEE") false else intervention.isOverdue,
            syncPending = true
        )
        dao.updateIntervention(updated)
        dao.insertInterventionLog(
            InterventionLogEntity(
                interventionId = intervention.id,
                action = "Statut modifié : $newStatus" + if (signatureAuthor.isNotBlank()) " (Signé par $signatureAuthor)" else "",
                authorName = actor?.fullName ?: "Système"
            )
        )
        enqueueSync(
            entityType = "INTERVENTION",
            entityId = intervention.id,
            operation = "UPDATE",
            payload = JSONObject().apply {
                put("id", intervention.id)
                put("status", newStatus)
                put("reportNotes", reportNotes)
                put("signatureAuthor", signatureAuthor)
            }
        )
        if (newStatus == "TERMINEE" && createDraftInvoice) {
            val year = SimpleDateFormat("yyyy", Locale.FRANCE).format(Date())
            val docNum = "FAC-$year-${(100..999).random()}"
            val draftDoc = CommercialDocEntity(
                docNumber = docNum,
                docType = "FACTURE",
                clientId = intervention.clientId,
                clientName = intervention.clientName,
                interventionId = intervention.id,
                title = "Prestation : ${intervention.title}",
                amountHt = (intervention.durationMinutes / 60.0) * 85.0,
                status = "BROUILLON",
                issueDate = todayDate(),
                dueDate = datePlusDays(30),
                itemsSummary = "Intervention ${intervention.reference} (${intervention.durationMinutes} min)"
            )
            val docId = dao.insertCommercialDoc(draftDoc)
            enqueueSync(
                entityType = "COMMERCIAL_DOC",
                entityId = docId,
                operation = "CREATE",
                payload = JSONObject().apply {
                    put("id", docId)
                    put("docNumber", docNum)
                    put("docType", "FACTURE")
                    put("clientId", intervention.clientId)
                    put("amountHt", draftDoc.amountHt)
                }
            )
            dao.insertNotification(
                NotificationEntity(
                    title = "Facture brouillon générée ($docNum)",
                    message = "Suite à la clôture de ${intervention.reference} pour ${intervention.clientName}",
                    category = "FACTURE"
                )
            )
        }
        logAudit(actor, "Intervention ${intervention.reference} -> $newStatus", "Interventions")
    }

    suspend fun rescheduleIntervention(
        intervention: InterventionEntity,
        newDate: String,
        newTime: String,
        newTechnician: EmployeeEntity?,
        actor: EmployeeEntity?,
        isOffline: Boolean
    ) {
        val updated = intervention.copy(
            scheduledDate = newDate,
            scheduledTime = newTime,
            technicianId = newTechnician?.id ?: intervention.technicianId,
            technicianName = newTechnician?.fullName ?: intervention.technicianName,
            status = if (intervention.status == "NOUVELLE" || intervention.status == "A_PLANIFIER") "PLANIFIEE" else intervention.status,
            syncPending = true
        )
        dao.updateIntervention(updated)
        dao.insertInterventionLog(
            InterventionLogEntity(
                interventionId = intervention.id,
                action = "Replanifiée le $newDate à $newTime (${updated.technicianName})",
                authorName = actor?.fullName ?: "Système"
            )
        )
        enqueueSync(
            entityType = "PLANNING",
            entityId = intervention.id,
            operation = "UPDATE",
            payload = JSONObject().apply {
                put("interventionId", intervention.id)
                put("scheduledDate", newDate)
                put("scheduledTime", newTime)
                put("technicianId", updated.technicianId ?: JSONObject.NULL)
            }
        )
        dao.insertNotification(
            NotificationEntity(
                title = "Modification planning : ${intervention.reference}",
                message = "Déplacée au $newDate à $newTime (${updated.technicianName})",
                category = "PLANNING"
            )
        )
        logAudit(actor, "Replanification ${intervention.reference} -> $newDate $newTime", "Planning")
    }

    suspend fun deleteIntervention(intervention: InterventionEntity, actor: EmployeeEntity?) {
        dao.deleteIntervention(intervention)
        enqueueSync(
            entityType = "INTERVENTION",
            entityId = intervention.id,
            operation = "DELETE",
            payload = JSONObject().apply {
                put("id", intervention.id)
                put("reference", intervention.reference)
            }
        )
        logAudit(actor, "Suppression intervention ${intervention.reference}", "Interventions")
    }

    // --- SYNCHRONIZATION ---
    suspend fun synchronizePending(isNetworkAvailable: Boolean, actor: EmployeeEntity?): SyncExecutionReport {
        val report = syncManager.synchronizePending(isNetworkAvailable)
        if (report.syncedCount > 0) {
            dao.clearAllPendingSync()
        }
        logAudit(actor, "Tentative synchronisation : ${report.message}", "Synchronisation")
        return report
    }

    suspend fun retrySyncItem(item: SyncQueueEntity) = syncManager.retryQueueItem(item)
    suspend fun resolveSyncConflictKeepLocal(item: SyncQueueEntity) = syncManager.resolveConflictKeepLocal(item)

    // --- EQUIPMENTS & MAINTENANCE ---
    suspend fun saveEquipment(equipment: EquipmentEntity, actor: EmployeeEntity?) {
        val isNew = equipment.id == 0L
        val normalized = if (isNew && equipment.acquisitionDate.isBlank()) {
            equipment.copy(
                acquisitionDate = todayDate(),
                nextMaintenanceDate = equipment.nextMaintenanceDate.ifBlank { datePlusDays(90) }
            )
        } else equipment
        val savedId = if (isNew) dao.insertEquipment(normalized) else {
            dao.updateEquipment(normalized)
            normalized.id
        }
        enqueueSync(
            entityType = "EQUIPMENT",
            entityId = savedId,
            operation = if (isNew) "CREATE" else "UPDATE",
            payload = JSONObject().apply {
                put("id", savedId)
                put("code", normalized.code)
                put("name", normalized.name)
                put("status", normalized.status)
                put("serialNumber", normalized.serialNumber)
            }
        )
        logAudit(actor, "${if (isNew) "Ajout" else "Mise à jour"} équipement ${normalized.code}", "Équipements")
    }

    suspend fun deleteEquipment(equipment: EquipmentEntity, actor: EmployeeEntity?) {
        dao.deleteEquipment(equipment)
        enqueueSync(
            entityType = "EQUIPMENT",
            entityId = equipment.id,
            operation = "DELETE",
            payload = JSONObject().apply {
                put("id", equipment.id)
                put("code", equipment.code)
            }
        )
        logAudit(actor, "Suppression équipement ${equipment.code}", "Équipements")
    }

    suspend fun addMaintenanceRecord(
        equipment: EquipmentEntity,
        type: String,
        description: String,
        technicianName: String,
        cost: Double,
        nextMaintenanceDate: String,
        newEquipmentStatus: String,
        actor: EmployeeEntity?
    ) {
        val recId = dao.insertMaintenanceRecord(
            MaintenanceRecordEntity(
                equipmentId = equipment.id,
                equipmentName = equipment.name,
                type = type,
                description = description,
                technicianName = technicianName,
                date = todayDate(),
                cost = cost
            )
        )
        dao.updateEquipment(
            equipment.copy(
                status = newEquipmentStatus,
                nextMaintenanceDate = nextMaintenanceDate
            )
        )
        enqueueSync(
            entityType = "EQUIPMENT_MAINTENANCE",
            entityId = recId,
            operation = "CREATE",
            payload = JSONObject().apply {
                put("equipmentId", equipment.id)
                put("type", type)
                put("description", description)
                put("cost", cost)
                put("newStatus", newEquipmentStatus)
            }
        )
        logAudit(actor, "Maintenance $type enregistrée sur ${equipment.code}", "Équipements")
    }

    // --- STOCK ---
    suspend fun saveStockItem(item: StockItemEntity, actor: EmployeeEntity?) {
        val isNew = item.id == 0L
        val savedId = if (isNew) dao.insertStockItem(item) else {
            dao.updateStockItem(item)
            item.id
        }
        enqueueSync(
            entityType = "STOCK_ITEM",
            entityId = savedId,
            operation = if (isNew) "CREATE" else "UPDATE",
            payload = JSONObject().apply {
                put("id", savedId)
                put("sku", item.sku)
                put("name", item.name)
                put("quantity", item.quantity)
                put("minThreshold", item.minThreshold)
                put("unitPrice", item.unitPrice)
            }
        )
        logAudit(actor, "${if (isNew) "Création" else "Modification"} article stock ${item.sku}", "Stocks")
    }

    suspend fun deleteStockItem(item: StockItemEntity, actor: EmployeeEntity?) {
        dao.deleteStockItem(item)
        enqueueSync(
            entityType = "STOCK_ITEM",
            entityId = item.id,
            operation = "DELETE",
            payload = JSONObject().apply {
                put("id", item.id)
                put("sku", item.sku)
            }
        )
        logAudit(actor, "Suppression article stock ${item.sku}", "Stocks")
    }

    suspend fun recordStockMovement(
        item: StockItemEntity,
        type: String,
        quantityAmount: Int,
        targetOrReason: String,
        actor: EmployeeEntity?
    ) {
        val delta = if (type == "ENTREE") quantityAmount else -quantityAmount
        val newQty = (item.quantity + delta).coerceAtLeast(0)
        dao.updateStockItem(item.copy(quantity = newQty))
        val mvtId = dao.insertStockMovement(
            StockMovementEntity(
                stockItemId = item.id,
                itemName = item.name,
                type = type,
                quantityDelta = delta,
                targetOrReason = targetOrReason
            )
        )
        enqueueSync(
            entityType = "STOCK_MOVEMENT",
            entityId = mvtId,
            operation = "CREATE",
            payload = JSONObject().apply {
                put("stockItemId", item.id)
                put("type", type)
                put("quantityDelta", delta)
                put("newQuantity", newQty)
                put("targetOrReason", targetOrReason)
            }
        )
        if (newQty <= item.minThreshold) {
            dao.insertNotification(
                NotificationEntity(
                    title = "Alerte seuil stock : ${item.name}",
                    message = "Stock restant : $newQty ${item.unit} (seuil minimal : ${item.minThreshold}).",
                    category = "STOCK"
                )
            )
        }
        logAudit(actor, "Mouvement stock $type ($delta) sur ${item.name}", "Stocks")
    }

    // --- COMMERCIAL ---
    suspend fun saveCommercialDoc(doc: CommercialDocEntity, actor: EmployeeEntity?) {
        val isNew = doc.id == 0L
        val normalized = if (isNew) {
            doc.copy(
                issueDate = doc.issueDate.ifBlank { todayDate() },
                dueDate = doc.dueDate.ifBlank { datePlusDays(30) }
            )
        } else doc
        val savedId = if (isNew) dao.insertCommercialDoc(normalized) else {
            dao.updateCommercialDoc(normalized)
            normalized.id
        }
        enqueueSync(
            entityType = "COMMERCIAL_DOC",
            entityId = savedId,
            operation = if (isNew) "CREATE" else "UPDATE",
            payload = JSONObject().apply {
                put("id", savedId)
                put("docNumber", normalized.docNumber)
                put("docType", normalized.docType)
                put("clientId", normalized.clientId)
                put("amountHt", normalized.amountHt)
                put("status", normalized.status)
            }
        )
        logAudit(actor, "${if (isNew) "Création" else "Modification"} ${normalized.docType} ${normalized.docNumber}", "Commercial")
    }

    suspend fun deleteCommercialDoc(doc: CommercialDocEntity, actor: EmployeeEntity?) {
        dao.deleteCommercialDoc(doc)
        enqueueSync(
            entityType = "COMMERCIAL_DOC",
            entityId = doc.id,
            operation = "DELETE",
            payload = JSONObject().apply {
                put("id", doc.id)
                put("docNumber", doc.docNumber)
            }
        )
        logAudit(actor, "Suppression ${doc.docType} ${doc.docNumber}", "Commercial")
    }

    suspend fun convertDevisToFacture(devis: CommercialDocEntity, actor: EmployeeEntity?) {
        val year = SimpleDateFormat("yyyy", Locale.FRANCE).format(Date())
        val invoiceNum = "FAC-$year-${(200..999).random()}"
        dao.updateCommercialDoc(devis.copy(status = "ACCEPTE"))
        val newInvoice = devis.copy(
            id = 0,
            docNumber = invoiceNum,
            docType = "FACTURE",
            status = "IMPAYEE",
            issueDate = todayDate(),
            dueDate = datePlusDays(30)
        )
        val newId = dao.insertCommercialDoc(newInvoice)
        enqueueSync(
            entityType = "COMMERCIAL_DOC",
            entityId = newId,
            operation = "CREATE",
            payload = JSONObject().apply {
                put("id", newId)
                put("docNumber", invoiceNum)
                put("docType", "FACTURE")
                put("sourceDevisId", devis.id)
                put("amountHt", newInvoice.amountHt)
            }
        )
        dao.insertNotification(
            NotificationEntity(
                title = "Devis transformé en facture : $invoiceNum",
                message = "${devis.clientName} — ${String.format(Locale.FRANCE, "%.2f", devis.totalTtc)} € TTC",
                category = "FACTURE"
            )
        )
        logAudit(actor, "Transformation ${devis.docNumber} -> $invoiceNum", "Commercial")
    }

    suspend fun recordPayment(doc: CommercialDocEntity, paymentAmount: Double, actor: EmployeeEntity?) {
        val newPaid = (doc.amountPaid + paymentAmount).coerceAtMost(doc.totalTtc)
        val newStatus = when {
            newPaid >= doc.totalTtc - 0.01 -> "PAYEE"
            newPaid > 0.0 -> "PARTIELLE"
            else -> "IMPAYEE"
        }
        dao.updateCommercialDoc(doc.copy(amountPaid = newPaid, status = newStatus))
        enqueueSync(
            entityType = "COMMERCIAL_DOC",
            entityId = doc.id,
            operation = "UPDATE",
            payload = JSONObject().apply {
                put("id", doc.id)
                put("amountPaid", newPaid)
                put("status", newStatus)
            }
        )
        logAudit(actor, "Règlement de ${String.format(Locale.FRANCE, "%.2f", paymentAmount)} € sur ${doc.docNumber}", "Commercial")
    }

    // --- SAV ---
    suspend fun saveSavTicket(ticket: SavTicketEntity, actor: EmployeeEntity?) {
        val isNew = ticket.id == 0L
        val savedId = if (isNew) dao.insertSavTicket(ticket) else {
            dao.updateSavTicket(ticket)
            ticket.id
        }
        enqueueSync(
            entityType = "SAV_TICKET",
            entityId = savedId,
            operation = if (isNew) "CREATE" else "UPDATE",
            payload = JSONObject().apply {
                put("id", savedId)
                put("ticketNumber", ticket.ticketNumber)
                put("clientId", ticket.clientId)
                put("status", ticket.status)
            }
        )
        if (isNew) {
            dao.insertNotification(
                NotificationEntity(
                    title = "Nouveau ticket SAV : ${ticket.ticketNumber}",
                    message = "${ticket.clientName} — ${ticket.equipmentName}",
                    category = "SAV"
                )
            )
        }
        logAudit(actor, "${if (isNew) "Ouverture" else "Mise à jour"} dossier SAV ${ticket.ticketNumber}", "SAV")
    }

    suspend fun appendSavExchange(ticket: SavTicketEntity, note: String, newStatus: String, actor: EmployeeEntity?) {
        val stamp = "${SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date())} ${actor?.fullName ?: "Agent"}: $note"
        val updatedLog = if (ticket.exchangeLog.isBlank()) stamp else "${ticket.exchangeLog}\n$stamp"
        dao.updateSavTicket(ticket.copy(exchangeLog = updatedLog, status = newStatus))
        enqueueSync(
            entityType = "SAV_TICKET",
            entityId = ticket.id,
            operation = "UPDATE",
            payload = JSONObject().apply {
                put("id", ticket.id)
                put("status", newStatus)
                put("lastExchange", stamp)
            }
        )
        logAudit(actor, "Échange ajouté sur ${ticket.ticketNumber} ($newStatus)", "SAV")
    }

    // --- POINTAGE ---
    suspend fun checkIn(employee: EmployeeEntity, activityNote: String, lateMinutes: Int = 0) {
        val time = currentTime()
        val entry = TimeEntryEntity(
            employeeId = employee.id,
            employeeName = employee.fullName,
            date = todayDate(),
            checkInTime = time,
            lateMinutes = lateMinutes,
            activityNote = activityNote.ifBlank { "Prise de poste" },
            status = "EN_POSTE"
        )
        val entryId = dao.insertTimeEntry(entry)
        enqueueSync(
            entityType = "TIME_ENTRY",
            entityId = entryId,
            operation = "CREATE",
            payload = JSONObject().apply {
                put("id", entryId)
                put("employeeId", employee.id)
                put("date", entry.date)
                put("checkInTime", time)
            }
        )
        logAudit(employee, "Pointage arrivée à $time", "Pointage")
    }

    suspend fun checkOut(entry: TimeEntryEntity, activityNote: String, actor: EmployeeEntity?) {
        val outTime = currentTime()
        val worked = computeWorkedMinutes(entry.checkInTime, outTime)
        val overtime = (worked - 480).coerceAtLeast(0)
        val updated = entry.copy(
            checkOutTime = outTime,
            workedMinutes = worked,
            overtimeMinutes = overtime,
            activityNote = activityNote.ifBlank { entry.activityNote },
            status = "TERMINE"
        )
        dao.updateTimeEntry(updated)
        enqueueSync(
            entityType = "TIME_ENTRY",
            entityId = entry.id,
            operation = "UPDATE",
            payload = JSONObject().apply {
                put("id", entry.id)
                put("checkOutTime", outTime)
                put("workedMinutes", worked)
                put("overtimeMinutes", overtime)
            }
        )
        logAudit(actor, "Pointage départ de ${entry.employeeName} à $outTime (${worked} min)", "Pointage")
    }

    // --- NOTIFICATIONS ---
    suspend fun markAllNotificationsRead() = dao.markAllNotificationsRead()
    suspend fun markNotificationRead(id: Long) = dao.markNotificationRead(id)
}
