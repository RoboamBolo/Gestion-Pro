package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GestionDao {
    // Clients
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun observeClients(): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    // Employees
    @Query("SELECT * FROM employees ORDER BY fullName ASC")
    fun observeEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeEntity)

    // Interventions
    @Query("SELECT * FROM interventions ORDER BY scheduledDate ASC, scheduledTime ASC")
    fun observeInterventions(): Flow<List<InterventionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: InterventionEntity): Long

    @Update
    suspend fun updateIntervention(intervention: InterventionEntity)

    @Delete
    suspend fun deleteIntervention(intervention: InterventionEntity)

    @Query("SELECT * FROM intervention_logs WHERE interventionId = :interventionId ORDER BY timestamp DESC")
    fun observeInterventionLogs(interventionId: Long): Flow<List<InterventionLogEntity>>

    @Insert
    suspend fun insertInterventionLog(log: InterventionLogEntity)

    @Query("UPDATE interventions SET syncPending = 0 WHERE syncPending = 1")
    suspend fun clearAllPendingSync()

    // Equipments & Maintenance
    @Query("SELECT * FROM equipments ORDER BY name ASC")
    fun observeEquipments(): Flow<List<EquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: EquipmentEntity): Long

    @Update
    suspend fun updateEquipment(equipment: EquipmentEntity)

    @Delete
    suspend fun deleteEquipment(equipment: EquipmentEntity)

    @Query("SELECT * FROM maintenance_records ORDER BY date DESC")
    fun observeMaintenanceRecords(): Flow<List<MaintenanceRecordEntity>>

    @Insert
    suspend fun insertMaintenanceRecord(record: MaintenanceRecordEntity): Long

    // Stock
    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    fun observeStockItems(): Flow<List<StockItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockItem(item: StockItemEntity): Long

    @Update
    suspend fun updateStockItem(item: StockItemEntity)

    @Delete
    suspend fun deleteStockItem(item: StockItemEntity)

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun observeStockMovements(): Flow<List<StockMovementEntity>>

    @Insert
    suspend fun insertStockMovement(movement: StockMovementEntity): Long

    // Commercial
    @Query("SELECT * FROM commercial_docs ORDER BY issueDate DESC, id DESC")
    fun observeCommercialDocs(): Flow<List<CommercialDocEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommercialDoc(doc: CommercialDocEntity): Long

    @Update
    suspend fun updateCommercialDoc(doc: CommercialDocEntity)

    @Delete
    suspend fun deleteCommercialDoc(doc: CommercialDocEntity)

    // SAV
    @Query("SELECT * FROM sav_tickets ORDER BY createdAt DESC")
    fun observeSavTickets(): Flow<List<SavTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavTicket(ticket: SavTicketEntity): Long

    @Update
    suspend fun updateSavTicket(ticket: SavTicketEntity)

    @Delete
    suspend fun deleteSavTicket(ticket: SavTicketEntity)

    // Time Tracking
    @Query("SELECT * FROM time_entries ORDER BY date DESC, id DESC")
    fun observeTimeEntries(): Flow<List<TimeEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeEntry(entry: TimeEntryEntity): Long

    @Update
    suspend fun updateTimeEntry(entry: TimeEntryEntity)

    // Notifications & Audit
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Insert
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: Long)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT COUNT(*) FROM employees")
    suspend fun getEmployeeCount(): Int

    // Sync Queue (Offline-first REST synchronization)
    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC")
    fun observeSyncQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'FAILED', 'CONFLICT')")
    fun observePendingSyncCount(): Flow<Int>

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingSyncItems(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueueItem(item: SyncQueueEntity): Long

    @Update
    suspend fun updateSyncQueueItem(item: SyncQueueEntity)

    @Delete
    suspend fun deleteSyncQueueItem(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status, lastError = :error, retryCount = :retryCount WHERE id = :id")
    suspend fun markSyncItemError(id: Long, status: String, error: String, retryCount: Int)

    @Query("UPDATE sync_queue SET status = 'SYNCED', lastError = '' WHERE id = :id")
    suspend fun markSyncItemSynced(id: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun deleteSyncedItems()
}
