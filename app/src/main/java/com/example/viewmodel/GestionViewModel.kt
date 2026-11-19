package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.network.NetworkMonitor
import com.example.data.preferences.UserPreferences
import com.example.data.repository.GestionRepository
import com.example.ui.components.MonthlyChartPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DashboardMetrics(
    val totalClients: Int = 0,
    val scheduledInterventions: Int = 0,
    val inProgressInterventions: Int = 0,
    val completedInterventions: Int = 0,
    val overdueInterventions: Int = 0,
    val availableTechnicians: Int = 0,
    val totalRevenueTtc: Double = 0.0,
    val paidInvoicesCount: Int = 0,
    val unpaidInvoicesCount: Int = 0,
    val lowStockCount: Int = 0,
    val upcomingMaintenanceCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val unreadNotificationsCount: Int = 0
)

data class GestionUiState(
    val isLoading: Boolean = true,
    val isDarkMode: Boolean = false,
    val isNetworkConnected: Boolean = true,
    val isManualOfflineMode: Boolean = false,
    val isOfflineMode: Boolean = false,
    val currentUser: EmployeeEntity? = null,
    val clients: List<ClientEntity> = emptyList(),
    val employees: List<EmployeeEntity> = emptyList(),
    val interventions: List<InterventionEntity> = emptyList(),
    val equipments: List<EquipmentEntity> = emptyList(),
    val maintenanceRecords: List<MaintenanceRecordEntity> = emptyList(),
    val stockItems: List<StockItemEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val commercialDocs: List<CommercialDocEntity> = emptyList(),
    val savTickets: List<SavTicketEntity> = emptyList(),
    val timeEntries: List<TimeEntryEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val syncQueue: List<SyncQueueEntity> = emptyList(),
    val monthlyChartPoints: List<MonthlyChartPoint> = emptyList(),
    val annualChartPoints: List<MonthlyChartPoint> = emptyList(),
    val metrics: DashboardMetrics = DashboardMetrics(),
    val feedbackMessage: String? = null
)

class GestionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).gestionDao()
    private val repository = GestionRepository(dao)
    private val prefs = UserPreferences(application)
    private val networkMonitor = NetworkMonitor(application)

    private val _feedback = MutableStateFlow<String?>(null)

    private val coreBundle1 = combine(
        repository.clients,
        repository.employees,
        repository.interventions,
        repository.equipments,
        repository.maintenanceRecords
    ) { clients, employees, interventions, equipments, maintenance ->
        CoreBundle1(clients, employees, interventions, equipments, maintenance)
    }

    private val coreBundle2 = combine(
        repository.stockItems,
        repository.stockMovements,
        repository.commercialDocs,
        repository.savTickets,
        repository.timeEntries
    ) { stock, movements, docs, sav, times ->
        CoreBundle2(stock, movements, docs, sav, times)
    }

    private val syncAndNetwork = combine(
        networkMonitor.isOnline,
        repository.syncQueue,
        repository.pendingSyncCount
    ) { online, queue, pendingCount ->
        SyncAndNetwork(online, queue, pendingCount)
    }

    private val prefsAndLogs = combine(
        prefs.isDarkMode,
        prefs.isOfflineMode,
        prefs.currentUserId,
        repository.notifications,
        repository.auditLogs
    ) { dark, offline, userId, notifs, audits ->
        PrefsAndLogs(dark, offline, userId, notifs, audits)
    }

    val uiState: StateFlow<GestionUiState> = combine(
        coreBundle1,
        coreBundle2,
        prefsAndLogs,
        syncAndNetwork,
        _feedback
    ) { b1, b2, pl, sn, feedback ->
        val currentUser = pl.currentUserId?.let { id -> b1.employees.find { it.id == id } }
        val invoices = b2.docs.filter { it.docType == "FACTURE" }
        val effectiveOffline = pl.isOffline || !sn.isNetworkConnected

        val metrics = DashboardMetrics(
            totalClients = b1.clients.size,
            scheduledInterventions = b1.interventions.count { it.status == "PLANIFIEE" || it.status == "A_PLANIFIER" || it.status == "NOUVELLE" },
            inProgressInterventions = b1.interventions.count { it.status == "EN_COURS" },
            completedInterventions = b1.interventions.count { it.status == "TERMINEE" },
            overdueInterventions = b1.interventions.count { it.isOverdue || it.status == "EN_ATTENTE" },
            availableTechnicians = b1.employees.count {
                (it.role == "TECHNICIEN" || it.role == "CHEF_EQUIPE") && it.availability == "DISPONIBLE"
            },
            totalRevenueTtc = invoices.sumOf { it.totalTtc },
            paidInvoicesCount = invoices.count { it.status == "PAYEE" },
            unpaidInvoicesCount = invoices.count { it.status == "IMPAYEE" || it.status == "PARTIELLE" },
            lowStockCount = b2.stock.count { it.quantity <= it.minThreshold },
            upcomingMaintenanceCount = b1.equipments.count { it.status != "HORS_SERVICE" },
            pendingSyncCount = sn.pendingCount,
            unreadNotificationsCount = pl.notifs.count { !it.isRead }
        )

        GestionUiState(
            isLoading = false,
            isDarkMode = pl.isDark,
            isNetworkConnected = sn.isNetworkConnected,
            isManualOfflineMode = pl.isOffline,
            isOfflineMode = effectiveOffline,
            currentUser = currentUser,
            clients = b1.clients,
            employees = b1.employees,
            interventions = b1.interventions,
            equipments = b1.equipments,
            maintenanceRecords = b1.maintenance,
            stockItems = b2.stock,
            stockMovements = b2.movements,
            commercialDocs = b2.docs,
            savTickets = b2.sav,
            timeEntries = b2.times,
            notifications = pl.notifs,
            auditLogs = pl.audits,
            syncQueue = sn.syncQueue,
            monthlyChartPoints = buildMonthlyStats(b1.interventions, invoices),
            annualChartPoints = buildAnnualStats(b1.interventions, invoices),
            metrics = metrics,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GestionUiState()
    )

    private fun buildMonthlyStats(
        interventions: List<InterventionEntity>,
        invoices: List<CommercialDocEntity>
    ): List<MonthlyChartPoint> {
        val monthFormat = SimpleDateFormat("MMM", Locale.FRANCE)
        val keyFormat = SimpleDateFormat("yyyy-MM", Locale.FRANCE)
        val anchorCal = Calendar.getInstance()

        // Use latest intervention/invoice year-month if all data sits in a specific month window, else system date
        val points = mutableListOf<MonthlyChartPoint>()
        for (offset in 5 downTo 0) {
            val cal = anchorCal.clone() as Calendar
            cal.add(Calendar.MONTH, -offset)
            val ym = keyFormat.format(cal.time)
            val label = monthFormat.format(cal.time).replaceFirstChar { it.uppercase() }.trimEnd('.')
            val intCount = interventions.count { it.scheduledDate.startsWith(ym) }
            val revKe = (invoices.filter { it.issueDate.startsWith(ym) }.sumOf { it.totalTtc } / 1000.0).toFloat()
            points.add(MonthlyChartPoint(month = label, interventions = intCount, revenueKe = revKe))
        }
        return points
    }

    private fun buildAnnualStats(
        interventions: List<InterventionEntity>,
        invoices: List<CommercialDocEntity>
    ): List<MonthlyChartPoint> {
        val years = (interventions.mapNotNull { it.scheduledDate.take(4).takeIf { y -> y.length == 4 } } +
            invoices.mapNotNull { it.issueDate.take(4).takeIf { y -> y.length == 4 } } +
            listOf(SimpleDateFormat("yyyy", Locale.FRANCE).format(java.util.Date())))
            .distinct()
            .sorted()
            .takeLast(4)

        return years.map { year ->
            val intCount = interventions.count { it.scheduledDate.startsWith(year) }
            val revKe = (invoices.filter { it.issueDate.startsWith(year) }.sumOf { it.totalTtc } / 1000.0).toFloat()
            MonthlyChartPoint(month = year, interventions = intCount, revenueKe = revKe)
        }
    }

    fun observeInterventionLogs(interventionId: Long): Flow<List<InterventionLogEntity>> =
        repository.observeInterventionLogs(interventionId)

    fun clearFeedback() {
        _feedback.value = null
    }

    fun showFeedback(message: String) {
        _feedback.value = message
    }

    // --- SESSION & PREFERENCES ---
    fun signInAs(employee: EmployeeEntity) {
        viewModelScope.launch {
            prefs.setCurrentUserId(employee.id)
            repository.logAudit(employee, "Connexion à la session (${employee.role})", "Authentification")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val actor = uiState.value.currentUser
            repository.logAudit(actor, "Déconnexion de la session", "Authentification")
            prefs.setCurrentUserId(null)
        }
    }

    fun resetPin(employee: EmployeeEntity, newPin: String) {
        viewModelScope.launch {
            repository.saveEmployee(employee.copy(pinCode = newPin), uiState.value.currentUser)
            _feedback.value = "Code PIN mis à jour pour ${employee.fullName}"
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch { prefs.setDarkMode(!uiState.value.isDarkMode) }
    }

    fun toggleOfflineMode() {
        viewModelScope.launch {
            val next = !uiState.value.isManualOfflineMode
            prefs.setOfflineMode(next)
            _feedback.value = if (next) {
                "Mode hors ligne manuel activé : toutes les opérations sont mises en file locale"
            } else if (!uiState.value.isNetworkConnected) {
                "Réseau indisponible : l'application reste en mode hors ligne"
            } else {
                "Mode hors ligne manuel désactivé"
            }
        }
    }

    fun syncOfflineData() {
        viewModelScope.launch {
            val canAttemptSync = uiState.value.isNetworkConnected && !uiState.value.isManualOfflineMode
            val report = repository.synchronizePending(canAttemptSync, uiState.value.currentUser)
            _feedback.value = report.message
        }
    }

    fun retrySyncItem(item: SyncQueueEntity) {
        viewModelScope.launch {
            repository.retrySyncItem(item)
            _feedback.value = "Élément remis en file d'attente (${item.entityType})"
        }
    }

    fun resolveSyncConflictKeepLocal(item: SyncQueueEntity) {
        viewModelScope.launch {
            repository.resolveSyncConflictKeepLocal(item)
            _feedback.value = "Conflit marqué pour conservation locale"
        }
    }

    // --- CLIENTS ---
    fun saveClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.saveClient(client, uiState.value.currentUser)
            _feedback.value = "Client enregistré localement : ${client.name}"
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.deleteClient(client, uiState.value.currentUser)
            _feedback.value = "Client supprimé"
        }
    }

    // --- PERSONNEL ---
    fun saveEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            repository.saveEmployee(employee, uiState.value.currentUser)
            _feedback.value = "Collaborateur enregistré : ${employee.fullName}"
        }
    }

    fun updateEmployeeAvailability(employee: EmployeeEntity, availability: String, leaveNote: String) {
        viewModelScope.launch {
            repository.updateEmployeeAvailability(employee, availability, leaveNote, uiState.value.currentUser)
            _feedback.value = "Disponibilité mise à jour"
        }
    }

    // --- INTERVENTIONS & PLANNING ---
    fun saveIntervention(intervention: InterventionEntity) {
        viewModelScope.launch {
            repository.saveIntervention(intervention, uiState.value.currentUser, uiState.value.isOfflineMode)
            _feedback.value = "Intervention enregistrée en base locale (${intervention.reference})"
        }
    }

    fun updateInterventionStatus(
        intervention: InterventionEntity,
        newStatus: String,
        reportNotes: String = intervention.reportNotes,
        signatureAuthor: String = intervention.signatureAuthor,
        createDraftInvoice: Boolean = false
    ) {
        viewModelScope.launch {
            repository.updateInterventionStatus(
                intervention = intervention,
                newStatus = newStatus,
                reportNotes = reportNotes,
                signatureAuthor = signatureAuthor,
                createDraftInvoice = createDraftInvoice,
                actor = uiState.value.currentUser,
                isOffline = uiState.value.isOfflineMode
            )
            _feedback.value = "Intervention ${intervention.reference} -> $newStatus"
        }
    }

    fun rescheduleIntervention(
        intervention: InterventionEntity,
        newDate: String,
        newTime: String,
        newTechnician: EmployeeEntity?
    ) {
        viewModelScope.launch {
            repository.rescheduleIntervention(
                intervention = intervention,
                newDate = newDate,
                newTime = newTime,
                newTechnician = newTechnician,
                actor = uiState.value.currentUser,
                isOffline = uiState.value.isOfflineMode
            )
            _feedback.value = "Planning mis à jour : $newDate à $newTime"
        }
    }

    fun deleteIntervention(intervention: InterventionEntity) {
        viewModelScope.launch {
            repository.deleteIntervention(intervention, uiState.value.currentUser)
            _feedback.value = "Intervention supprimée"
        }
    }

    // --- EQUIPMENTS & MAINTENANCE ---
    fun saveEquipment(equipment: EquipmentEntity) {
        viewModelScope.launch {
            repository.saveEquipment(equipment, uiState.value.currentUser)
            _feedback.value = "Équipement enregistré : ${equipment.name}"
        }
    }

    fun deleteEquipment(equipment: EquipmentEntity) {
        viewModelScope.launch {
            repository.deleteEquipment(equipment, uiState.value.currentUser)
            _feedback.value = "Équipement supprimé : ${equipment.code}"
        }
    }

    fun addMaintenanceRecord(
        equipment: EquipmentEntity,
        type: String,
        description: String,
        technicianName: String,
        cost: Double,
        nextMaintenanceDate: String,
        newEquipmentStatus: String
    ) {
        viewModelScope.launch {
            repository.addMaintenanceRecord(
                equipment, type, description, technicianName, cost, nextMaintenanceDate, newEquipmentStatus, uiState.value.currentUser
            )
            _feedback.value = "Maintenance enregistrée sur ${equipment.code}"
        }
    }

    // --- STOCK ---
    fun saveStockItem(item: StockItemEntity) {
        viewModelScope.launch {
            repository.saveStockItem(item, uiState.value.currentUser)
            _feedback.value = "Article enregistré : ${item.name}"
        }
    }

    fun deleteStockItem(item: StockItemEntity) {
        viewModelScope.launch {
            repository.deleteStockItem(item, uiState.value.currentUser)
            _feedback.value = "Article supprimé : ${item.sku}"
        }
    }

    fun recordStockMovement(item: StockItemEntity, type: String, quantityAmount: Int, targetOrReason: String) {
        viewModelScope.launch {
            repository.recordStockMovement(item, type, quantityAmount, targetOrReason, uiState.value.currentUser)
            _feedback.value = "Mouvement de stock enregistré"
        }
    }

    // --- COMMERCIAL ---
    fun saveCommercialDoc(doc: CommercialDocEntity) {
        viewModelScope.launch {
            repository.saveCommercialDoc(doc, uiState.value.currentUser)
            _feedback.value = "${doc.docType} ${doc.docNumber} enregistré"
        }
    }

    fun deleteCommercialDoc(doc: CommercialDocEntity) {
        viewModelScope.launch {
            repository.deleteCommercialDoc(doc, uiState.value.currentUser)
            _feedback.value = "${doc.docType} ${doc.docNumber} supprimé"
        }
    }

    fun convertDevisToFacture(devis: CommercialDocEntity) {
        viewModelScope.launch {
            repository.convertDevisToFacture(devis, uiState.value.currentUser)
            _feedback.value = "Devis ${devis.docNumber} converti en facture"
        }
    }

    fun recordPayment(doc: CommercialDocEntity, amount: Double) {
        viewModelScope.launch {
            repository.recordPayment(doc, amount, uiState.value.currentUser)
            _feedback.value = "Règlement enregistré sur ${doc.docNumber}"
        }
    }

    // --- SAV ---
    fun saveSavTicket(ticket: SavTicketEntity) {
        viewModelScope.launch {
            repository.saveSavTicket(ticket, uiState.value.currentUser)
            _feedback.value = "Dossier SAV enregistré : ${ticket.ticketNumber}"
        }
    }

    fun appendSavExchange(ticket: SavTicketEntity, note: String, newStatus: String) {
        viewModelScope.launch {
            repository.appendSavExchange(ticket, note, newStatus, uiState.value.currentUser)
            _feedback.value = "Échange ajouté au dossier ${ticket.ticketNumber}"
        }
    }

    // --- POINTAGE ---
    fun checkInCurrent(activityNote: String) {
        val user = uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.checkIn(user, activityNote)
            _feedback.value = "Pointage d'arrivée enregistré"
        }
    }

    fun checkOutEntry(entry: TimeEntryEntity, activityNote: String) {
        viewModelScope.launch {
            repository.checkOut(entry, activityNote, uiState.value.currentUser)
            _feedback.value = "Pointage de départ enregistré"
        }
    }

    // --- NOTIFICATIONS ---
    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }
}

private data class CoreBundle1(
    val clients: List<ClientEntity>,
    val employees: List<EmployeeEntity>,
    val interventions: List<InterventionEntity>,
    val equipments: List<EquipmentEntity>,
    val maintenance: List<MaintenanceRecordEntity>
)

private data class CoreBundle2(
    val stock: List<StockItemEntity>,
    val movements: List<StockMovementEntity>,
    val docs: List<CommercialDocEntity>,
    val sav: List<SavTicketEntity>,
    val times: List<TimeEntryEntity>
)

private data class SyncAndNetwork(
    val isNetworkConnected: Boolean,
    val syncQueue: List<SyncQueueEntity>,
    val pendingCount: Int
)

private data class PrefsAndLogs(
    val isDark: Boolean,
    val isOffline: Boolean,
    val currentUserId: Long?,
    val notifs: List<NotificationEntity>,
    val audits: List<AuditLogEntity>
)
