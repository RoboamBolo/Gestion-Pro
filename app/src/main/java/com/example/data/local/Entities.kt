package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val companyName: String = "",
    val type: String = "ENTREPRISE",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val role: String = "TECHNICIEN",
    val teamName: String = "Équipe Terrain A",
    val phone: String = "",
    val email: String = "",
    val availability: String = "DISPONIBLE",
    val performanceScore: Int = 92,
    val pinCode: String = "1234",
    val leaveNote: String = "",
    val active: Boolean = true
)

@Entity(tableName = "interventions")
data class InterventionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val title: String,
    val description: String,
    val clientId: Long,
    val clientName: String,
    val location: String,
    val technicianId: Long? = null,
    val technicianName: String = "Non affecté",
    val status: String = "NOUVELLE",
    val priority: String = "NORMALE",
    val scheduledDate: String = "2026-11-17",
    val scheduledTime: String = "09:00",
    val durationMinutes: Int = 90,
    val equipmentSummary: String = "",
    val photoNote: String = "",
    val reportNotes: String = "",
    val signatureAuthor: String = "",
    val isOverdue: Boolean = false,
    val syncPending: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "intervention_logs")
data class InterventionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val interventionId: Long,
    val action: String,
    val authorName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "equipments")
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val category: String,
    val brand: String,
    val model: String,
    val serialNumber: String,
    val acquisitionDate: String,
    val status: String = "OPERATIONNEL",
    val location: String,
    val clientId: Long? = null,
    val clientName: String = "Parc interne",
    val nextMaintenanceDate: String = "2026-11-25"
)

@Entity(tableName = "maintenance_records")
data class MaintenanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipmentId: Long,
    val equipmentName: String,
    val type: String = "PREVENTIVE",
    val description: String,
    val technicianName: String,
    val date: String,
    val cost: Double = 0.0
)

@Entity(tableName = "stock_items")
data class StockItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val minThreshold: Int,
    val unit: String = "pcs",
    val unitPrice: Double = 0.0,
    val supplierName: String = ""
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockItemId: Long,
    val itemName: String,
    val type: String,
    val quantityDelta: Int,
    val targetOrReason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "commercial_docs")
data class CommercialDocEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val docNumber: String,
    val docType: String = "DEVIS",
    val clientId: Long,
    val clientName: String,
    val interventionId: Long? = null,
    val title: String,
    val amountHt: Double,
    val tvaRate: Double = 20.0,
    val depositPaid: Double = 0.0,
    val amountPaid: Double = 0.0,
    val status: String = "BROUILLON",
    val issueDate: String = "2026-11-17",
    val dueDate: String = "2026-12-17",
    val itemsSummary: String = ""
) {
    val totalTtc: Double get() = amountHt * (1.0 + tvaRate / 100.0)
    val remainingDue: Double get() = (totalTtc - amountPaid).coerceAtLeast(0.0)
}

@Entity(tableName = "sav_tickets")
data class SavTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketNumber: String,
    val clientId: Long,
    val clientName: String,
    val equipmentName: String,
    val type: String = "ASSISTANCE",
    val priority: String = "NORMALE",
    val status: String = "OUVERT",
    val description: String,
    val slaHours: Int = 24,
    val exchangeLog: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "time_entries")
data class TimeEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val employeeName: String,
    val date: String,
    val checkInTime: String,
    val checkOutTime: String = "",
    val workedMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val lateMinutes: Int = 0,
    val activityNote: String = "",
    val status: String = "EN_POSTE"
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actorName: String,
    val actorRole: String,
    val action: String,
    val moduleName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sync_queue",
    indices = [
        androidx.room.Index(value = ["status"]),
        androidx.room.Index(value = ["entityType", "entityId"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val operation: String,
    val payloadJson: String,
    val status: String = "PENDING",
    val retryCount: Int = 0,
    val lastError: String = "",
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

