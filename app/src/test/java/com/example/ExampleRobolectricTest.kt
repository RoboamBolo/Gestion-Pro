package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ClientEntity
import com.example.data.local.EquipmentEntity
import com.example.data.local.StockItemEntity
import com.example.data.local.TimeEntryEntity
import com.example.data.repository.GestionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `app_name matches Gestion Pro`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Gestion Pro", context.getString(R.string.app_name))
    }

    @Test
    fun `migration 1 to 2 preserves existing v1 clients and creates sync_queue`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_gestion_pro.db"
        context.deleteDatabase(dbName)

        // Create v1 SQLite database manually with clients table and 1 row
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `clients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `companyName` TEXT NOT NULL, `type` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `city` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `employees` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fullName` TEXT NOT NULL, `role` TEXT NOT NULL, `teamName` TEXT NOT NULL, `phone` TEXT NOT NULL, `email` TEXT NOT NULL, `availability` TEXT NOT NULL, `performanceScore` INTEGER NOT NULL, `pinCode` TEXT NOT NULL, `leaveNote` TEXT NOT NULL, `active` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `interventions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `reference` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `clientId` INTEGER NOT NULL, `clientName` TEXT NOT NULL, `location` TEXT NOT NULL, `technicianId` INTEGER, `technicianName` TEXT NOT NULL, `status` TEXT NOT NULL, `priority` TEXT NOT NULL, `scheduledDate` TEXT NOT NULL, `scheduledTime` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, `equipmentSummary` TEXT NOT NULL, `photoNote` TEXT NOT NULL, `reportNotes` TEXT NOT NULL, `signatureAuthor` TEXT NOT NULL, `isOverdue` INTEGER NOT NULL, `syncPending` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `intervention_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `interventionId` INTEGER NOT NULL, `action` TEXT NOT NULL, `authorName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `equipments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `brand` TEXT NOT NULL, `model` TEXT NOT NULL, `serialNumber` TEXT NOT NULL, `acquisitionDate` TEXT NOT NULL, `status` TEXT NOT NULL, `location` TEXT NOT NULL, `clientId` INTEGER, `clientName` TEXT NOT NULL, `nextMaintenanceDate` TEXT NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `maintenance_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `equipmentId` INTEGER NOT NULL, `equipmentName` TEXT NOT NULL, `type` TEXT NOT NULL, `description` TEXT NOT NULL, `technicianName` TEXT NOT NULL, `date` TEXT NOT NULL, `cost` REAL NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `stock_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sku` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `minThreshold` INTEGER NOT NULL, `unit` TEXT NOT NULL, `unitPrice` REAL NOT NULL, `supplierName` TEXT NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `stock_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `stockItemId` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `type` TEXT NOT NULL, `quantityDelta` INTEGER NOT NULL, `targetOrReason` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `commercial_docs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `docNumber` TEXT NOT NULL, `docType` TEXT NOT NULL, `clientId` INTEGER NOT NULL, `clientName` TEXT NOT NULL, `interventionId` INTEGER, `title` TEXT NOT NULL, `amountHt` REAL NOT NULL, `tvaRate` REAL NOT NULL, `depositPaid` REAL NOT NULL, `amountPaid` REAL NOT NULL, `status` TEXT NOT NULL, `issueDate` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `itemsSummary` TEXT NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `sav_tickets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ticketNumber` TEXT NOT NULL, `clientId` INTEGER NOT NULL, `clientName` TEXT NOT NULL, `equipmentName` TEXT NOT NULL, `type` TEXT NOT NULL, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `description` TEXT NOT NULL, `slaHours` INTEGER NOT NULL, `exchangeLog` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `time_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `employeeId` INTEGER NOT NULL, `employeeName` TEXT NOT NULL, `date` TEXT NOT NULL, `checkInTime` TEXT NOT NULL, `checkOutTime` TEXT NOT NULL, `workedMinutes` INTEGER NOT NULL, `overtimeMinutes` INTEGER NOT NULL, `lateMinutes` INTEGER NOT NULL, `activityNote` TEXT NOT NULL, `status` TEXT NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `message` TEXT NOT NULL, `category` TEXT NOT NULL, `isRead` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `audit_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `actorName` TEXT NOT NULL, `actorRole` TEXT NOT NULL, `action` TEXT NOT NULL, `moduleName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "INSERT INTO `clients` (`name`, `companyName`, `type`, `email`, `phone`, `address`, `city`, `notes`, `createdAt`) VALUES ('Client Historique', 'SAS Test', 'ENTREPRISE', 'a@b.fr', '0102030405', '1 rue Test', 'Paris', '', 1000)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.close()

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val dao = roomDb.gestionDao()
        val clients = dao.observeClients().first()
        assertEquals(1, clients.size)
        assertEquals("Client Historique", clients.first().name)

        val repo = GestionRepository(dao)
        repo.saveClient(ClientEntity(name = "Client Offline 2"), null)
        repo.saveEquipment(
            EquipmentEntity(
                code = "EQ-999",
                name = "Routeur 5G",
                category = "Réseau",
                brand = "Cisco",
                model = "IR1101",
                serialNumber = "SN999",
                acquisitionDate = "2026-11-18",
                location = "Dépôt"
            ),
            null
        )
        repo.saveStockItem(
            StockItemEntity(
                sku = "STK-999",
                name = "Câble Fibre",
                category = "Réseau",
                quantity = 5,
                minThreshold = 2
            ),
            null
        )

        val queue = dao.observeSyncQueue().first()
        assertEquals(3, queue.size)
        assertTrue(queue.all { it.status == "PENDING" })

        // Attempt sync without REST server: items MUST remain preserved in queue!
        val report = repo.synchronizePending(isNetworkAvailable = true, actor = null)
        assertEquals(0, report.syncedCount)
        assertEquals(3, report.remainingPendingCount)
        assertEquals(3, dao.getPendingSyncItems().size)

        // Verify real checkOut worked minutes calculation
        val entry = TimeEntryEntity(
            id = 1,
            employeeId = 1,
            employeeName = "Test Tech",
            date = "2026-11-18",
            checkInTime = "00:00",
            status = "EN_POSTE"
        )
        dao.insertTimeEntry(entry)
        repo.checkOut(entry, "Fin chantier", null)
        val updatedEntry = dao.observeTimeEntries().first().first()
        assertEquals("TERMINE", updatedEntry.status)
        assertTrue(updatedEntry.workedMinutes >= 1)

        roomDb.close()
        context.deleteDatabase(dbName)
    }
}
