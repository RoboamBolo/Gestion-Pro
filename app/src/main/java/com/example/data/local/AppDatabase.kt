package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        ClientEntity::class,
        EmployeeEntity::class,
        InterventionEntity::class,
        InterventionLogEntity::class,
        EquipmentEntity::class,
        MaintenanceRecordEntity::class,
        StockItemEntity::class,
        StockMovementEntity::class,
        CommercialDocEntity::class,
        SavTicketEntity::class,
        TimeEntryEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class,
        SyncQueueEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gestionDao(): GestionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` INTEGER NOT NULL,
                        `operation` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `lastError` TEXT NOT NULL,
                        `localUpdatedAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_status` ON `sync_queue` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_entityType_entityId` ON `sync_queue` (`entityType`, `entityId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestion_pro_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                CoroutineScope(Dispatchers.IO).launch {
                    DatabaseSeeder.seedIfEmpty(instance.gestionDao())
                }
                instance
            }
        }
    }
}

object DatabaseSeeder {
    private val seedMutex = Mutex()

    suspend fun seedIfEmpty(dao: GestionDao) = seedMutex.withLock {
        if (dao.getEmployeeCount() > 0) return@withLock

        // 1. Employees (8 roles)
        val dirId = dao.insertEmployee(
            EmployeeEntity(
                fullName = "Marc Delacroix",
                role = "DIRECTEUR",
                teamName = "Direction Générale",
                phone = "06 12 45 78 90",
                email = "m.delacroix@gestionpro.fr",
                availability = "DISPONIBLE",
                performanceScore = 98,
                pinCode = "1234"
            )
        )
        val planId = dao.insertEmployee(
            EmployeeEntity(
                fullName = "Sophie Laurent",
                role = "PLANIFICATEUR",
                teamName = "Dispatch & Planning",
                phone = "06 23 56 89 01",
                email = "s.laurent@gestionpro.fr",
                availability = "DISPONIBLE",
                performanceScore = 96,
                pinCode = "1234"
            )
        )
        val tech1Id = dao.insertEmployee(
            EmployeeEntity(
                fullName = "Karim Benali",
                role = "TECHNICIEN",
                teamName = "Équipe CVC & Froid",
                phone = "06 34 67 90 12",
                email = "k.benali@gestionpro.fr",
                availability = "EN_INTERVENTION",
                performanceScore = 95,
                pinCode = "1234"
            )
        )
        val tech2Id = dao.insertEmployee(
            EmployeeEntity(
                fullName = "Lucas Moreau",
                role = "TECHNICIEN",
                teamName = "Équipe IT & Réseaux",
                phone = "06 45 78 01 23",
                email = "l.moreau@gestionpro.fr",
                availability = "DISPONIBLE",
                performanceScore = 91,
                pinCode = "1234"
            )
        )
        val chefId = dao.insertEmployee(
            EmployeeEntity(
                fullName = "Claire Girard",
                role = "CHEF_EQUIPE",
                teamName = "Équipe Maintenance Pro",
                phone = "06 56 89 12 34",
                email = "c.girard@gestionpro.fr",
                availability = "DISPONIBLE",
                performanceScore = 94,
                pinCode = "1234"
            )
        )
        dao.insertEmployee(
            EmployeeEntity(
                fullName = "Antoine Mercier",
                role = "COMPTABLE",
                teamName = "Finance & Facturation",
                phone = "06 67 90 23 45",
                email = "a.mercier@gestionpro.fr",
                availability = "DISPONIBLE",
                performanceScore = 97,
                pinCode = "1234"
            )
        )
        dao.insertEmployee(
            EmployeeEntity(
                fullName = "Nadia Haddad",
                role = "SECRETAIRE",
                teamName = "Accueil & Relation Client",
                phone = "06 78 01 34 56",
                email = "n.haddad@gestionpro.fr",
                availability = "DISPONIBLE",
                performanceScore = 93,
                pinCode = "1234"
            )
        )
        dao.insertEmployee(
            EmployeeEntity(
                fullName = "Thomas Petit",
                role = "RESPONSABLE",
                teamName = "Qualité & SAV",
                phone = "06 89 12 45 67",
                email = "t.petit@gestionpro.fr",
                availability = "CONGE",
                performanceScore = 90,
                pinCode = "1234",
                leaveNote = "Congés du 16/11 au 20/11"
            )
        )

        // 2. Clients
        val c1 = dao.insertClient(
            ClientEntity(
                name = "Hôtel Le Grand Lyon",
                companyName = "SAS Grand Lyon Hôtellerie",
                type = "ENTREPRISE",
                email = "maintenance@grandlyon-hotel.fr",
                phone = "04 72 10 22 33",
                address = "18 Place Bellecour",
                city = "Lyon",
                notes = "Contrat annuel CVC + maintenance préventive trimestrielle."
            )
        )
        val c2 = dao.insertClient(
            ClientEntity(
                name = "Clinique Saint-Jean",
                companyName = "Groupe Santé Saint-Jean",
                type = "ENTREPRISE",
                email = "technique@clinique-stjean.fr",
                phone = "04 78 45 66 10",
                address = "42 Avenue Jean Jaurès",
                city = "Villeurbanne",
                notes = "Priorité haute sur les baies serveurs et groupes électrogènes."
            )
        )
        val c3 = dao.insertClient(
            ClientEntity(
                name = "Julien Lefèvre",
                companyName = "",
                type = "PARTICULIER",
                email = "julien.lefevre@mail.fr",
                phone = "06 41 22 88 95",
                address = "9 Rue des Lilas",
                city = "Écully",
                notes = "Installation pompe à chaleur résidentielle."
            )
        )
        val c4 = dao.insertClient(
            ClientEntity(
                name = "Logistique Rhône Express",
                companyName = "Rhône Express SARL",
                type = "ENTREPRISE",
                email = "exploitation@rhone-express.fr",
                phone = "04 74 88 19 04",
                address = "Zone Industrielle Nord, Lot 14",
                city = "Saint-Priest",
                notes = "Nettoyage industriel et maintenance ponts élévateurs."
            )
        )
        val c5 = dao.insertClient(
            ClientEntity(
                name = "Cabinet Médical Lumière",
                companyName = "SCM Lumière",
                type = "ENTREPRISE",
                email = "accueil@cabinet-lumiere.fr",
                phone = "04 72 33 44 55",
                address = "105 Avenue des Frères Lumière",
                city = "Lyon",
                notes = "Maintenance réseau informatique et postes médicaux."
            )
        )

        // 3. Interventions
        val i1 = dao.insertIntervention(
            InterventionEntity(
                reference = "INT-2026-101",
                title = "Maintenance CTA & Climatisation Aile Sud",
                description = "Contrôle des filtres, pression fluide frigorigène et nettoyage condensateurs.",
                clientId = c1,
                clientName = "Hôtel Le Grand Lyon",
                location = "18 Place Bellecour, Lyon",
                technicianId = tech1Id,
                technicianName = "Karim Benali",
                status = "EN_COURS",
                priority = "HAUTE",
                scheduledDate = "2026-11-17",
                scheduledTime = "08:30",
                durationMinutes = 150,
                equipmentSummary = "CTA Daikin VRV-IV, Manomètre numérique"
            )
        )
        dao.insertInterventionLog(InterventionLogEntity(interventionId = i1, action = "Intervention créée et planifiée", authorName = "Sophie Laurent"))
        dao.insertInterventionLog(InterventionLogEntity(interventionId = i1, action = "Démarrée sur site par Karim Benali", authorName = "Karim Benali"))

        val i2 = dao.insertIntervention(
            InterventionEntity(
                reference = "INT-2026-102",
                title = "Remplacement Switch Cœur de Réseau",
                description = "Migration vers switch PoE+ 48 ports et test de redondance fibre.",
                clientId = c2,
                clientName = "Clinique Saint-Jean",
                location = "42 Avenue Jean Jaurès, Villeurbanne",
                technicianId = tech2Id,
                technicianName = "Lucas Moreau",
                status = "PLANIFIEE",
                priority = "URGENTE",
                scheduledDate = "2026-11-17",
                scheduledTime = "14:00",
                durationMinutes = 120,
                equipmentSummary = "Switch Cisco C9300, Jarretières fibre OM4"
            )
        )
        dao.insertInterventionLog(InterventionLogEntity(interventionId = i2, action = "Planifiée en urgence pour maintenance SI", authorName = "Sophie Laurent"))

        val i3 = dao.insertIntervention(
            InterventionEntity(
                reference = "INT-2026-103",
                title = "Dépannage Pompe à Chaleur Altherma",
                description = "Code erreur U4 signalé sur unité extérieure. Contrôle carte inverter.",
                clientId = c3,
                clientName = "Julien Lefèvre",
                location = "9 Rue des Lilas, Écully",
                technicianId = tech1Id,
                technicianName = "Karim Benali",
                status = "EN_ATTENTE",
                priority = "HAUTE",
                scheduledDate = "2026-11-16",
                scheduledTime = "10:00",
                durationMinutes = 90,
                equipmentSummary = "Carte inverter Daikin",
                isOverdue = true
            )
        )
        dao.insertInterventionLog(InterventionLogEntity(interventionId = i3, action = "En attente de pièce détachée (Carte Inverter)", authorName = "Karim Benali"))

        val i4 = dao.insertIntervention(
            InterventionEntity(
                reference = "INT-2026-104",
                title = "Nettoyage haute pression Quais 1 à 6",
                description = "Décapage mécanique et désinfection des quais logistiques.",
                clientId = c4,
                clientName = "Logistique Rhône Express",
                location = "ZI Nord Lot 14, Saint-Priest",
                technicianId = chefId,
                technicianName = "Claire Girard",
                status = "TERMINEE",
                priority = "NORMALE",
                scheduledDate = "2026-11-15",
                scheduledTime = "07:30",
                durationMinutes = 240,
                equipmentSummary = "Autolaveuse Kärcher B90",
                reportNotes = "Quais 1 à 6 traités et validés avec le chef de quai.",
                signatureAuthor = "P. Martin (Resp. Quai)"
            )
        )
        dao.insertInterventionLog(InterventionLogEntity(interventionId = i4, action = "Intervention clôturée et signée", authorName = "Claire Girard"))

        dao.insertIntervention(
            InterventionEntity(
                reference = "INT-2026-105",
                title = "Audit sécurité postes médicaux",
                description = "Mise à jour firmware pare-feu et sauvegarde NAS chiffrée.",
                clientId = c5,
                clientName = "Cabinet Médical Lumière",
                location = "105 Av. Frères Lumière, Lyon",
                technicianId = tech2Id,
                technicianName = "Lucas Moreau",
                status = "PLANIFIEE",
                priority = "NORMALE",
                scheduledDate = "2026-11-18",
                scheduledTime = "09:30",
                durationMinutes = 120,
                equipmentSummary = "Pare-feu Fortinet 60F"
            )
        )
        dao.insertIntervention(
            InterventionEntity(
                reference = "INT-2026-106",
                title = "Installation Borne Wi-Fi 6E Étage 3",
                description = "Tirage câble Cat6A et configuration bornes mesh.",
                clientId = c1,
                clientName = "Hôtel Le Grand Lyon",
                location = "18 Place Bellecour, Lyon",
                technicianId = null,
                technicianName = "Non affecté",
                status = "A_PLANIFIER",
                priority = "NORMALE",
                scheduledDate = "2026-11-19",
                scheduledTime = "10:00",
                durationMinutes = 180,
                equipmentSummary = "3x Bornes Ubiquiti U6-Enterprise"
            )
        )
        dao.insertIntervention(
            InterventionEntity(
                reference = "INT-2026-107",
                title = "Contrôle Groupe Électrogène Secours",
                description = "Test démarrage à froid, vidange huile et contrôle batterie.",
                clientId = c2,
                clientName = "Clinique Saint-Jean",
                location = "42 Avenue Jean Jaurès, Villeurbanne",
                technicianId = null,
                technicianName = "Non affecté",
                status = "NOUVELLE",
                priority = "HAUTE",
                scheduledDate = "2026-11-20",
                scheduledTime = "08:00",
                durationMinutes = 120
            )
        )
        dao.insertIntervention(
            InterventionEntity(
                reference = "INT-2026-108",
                title = "Remplacement Onduleur Salle Serveur",
                description = "Installation onduleur APC Smart-UPS 3000VA.",
                clientId = c5,
                clientName = "Cabinet Médical Lumière",
                location = "105 Av. Frères Lumière, Lyon",
                technicianId = tech2Id,
                technicianName = "Lucas Moreau",
                status = "TERMINEE",
                priority = "HAUTE",
                scheduledDate = "2026-11-12",
                scheduledTime = "14:00",
                durationMinutes = 90,
                reportNotes = "Tests coupure secteur validés.",
                signatureAuthor = "Dr. Chazal"
            )
        )

        // 4. Equipments & Maintenance
        val eq1 = dao.insertEquipment(
            EquipmentEntity(
                code = "EQ-CVC-014",
                name = "Centrale Traitement Air VRV-IV",
                category = "CVC / Climatisation",
                brand = "Daikin",
                model = "REYQ16U",
                serialNumber = "DK-9948120-FR",
                acquisitionDate = "2024-03-12",
                status = "OPERATIONNEL",
                location = "Toiture Aile Sud",
                clientId = c1,
                clientName = "Hôtel Le Grand Lyon",
                nextMaintenanceDate = "2026-11-22"
            )
        )
        val eq2 = dao.insertEquipment(
            EquipmentEntity(
                code = "EQ-IT-008",
                name = "Switch Cœur Redondant 48P",
                category = "Informatique & Réseau",
                brand = "Cisco",
                model = "Catalyst 9300-48P",
                serialNumber = "FCW2419L0B2",
                acquisitionDate = "2023-09-05",
                status = "MAINTENANCE",
                location = "Local Serveur B1",
                clientId = c2,
                clientName = "Clinique Saint-Jean",
                nextMaintenanceDate = "2026-11-18"
            )
        )
        val eq3 = dao.insertEquipment(
            EquipmentEntity(
                code = "EQ-NET-021",
                name = "Autolaveuse Industrielle B90",
                category = "Nettoyage Pro",
                brand = "Kärcher",
                model = "B 90 R Classic",
                serialNumber = "KR-882190-EU",
                acquisitionDate = "2025-01-20",
                status = "OPERATIONNEL",
                location = "Dépôt Saint-Priest",
                clientId = null,
                clientName = "Parc interne",
                nextMaintenanceDate = "2026-12-05"
            )
        )
        val eq4 = dao.insertEquipment(
            EquipmentEntity(
                code = "EQ-PAC-003",
                name = "Pompe à Chaleur Altherma 3",
                category = "CVC / Chauffage",
                brand = "Daikin",
                model = "ERGA08EV",
                serialNumber = "DK-771039-LY",
                acquisitionDate = "2022-11-10",
                status = "EN_PANNE",
                location = "9 Rue des Lilas, Écully",
                clientId = c3,
                clientName = "Julien Lefèvre",
                nextMaintenanceDate = "2026-11-17"
            )
        )
        dao.insertMaintenanceRecord(
            MaintenanceRecordEntity(
                equipmentId = eq1,
                equipmentName = "Centrale Traitement Air VRV-IV",
                type = "PREVENTIVE",
                description = "Contrôle étanchéité circuit frigorifique et remplacement filtres F7",
                technicianName = "Karim Benali",
                date = "2026-08-22",
                cost = 380.0
            )
        )
        dao.insertMaintenanceRecord(
            MaintenanceRecordEntity(
                equipmentId = eq4,
                equipmentName = "Pompe à Chaleur Altherma 3",
                type = "CORRECTIVE",
                description = "Diagnostic carte inverter suite code erreur U4",
                technicianName = "Karim Benali",
                date = "2026-11-16",
                cost = 145.0
            )
        )

        // 5. Stock
        val st1 = dao.insertStockItem(
            StockItemEntity(
                sku = "STK-FIL-01",
                name = "Filtres CTA Haute Efficacité F7",
                category = "Consommables CVC",
                quantity = 4,
                minThreshold = 6,
                unit = "pcs",
                unitPrice = 42.0,
                supplierName = "ClimDistrib France"
            )
        )
        val st2 = dao.insertStockItem(
            StockItemEntity(
                sku = "STK-CAB-06",
                name = "Câble RJ45 Cat6A Blindé (Bobine 100m)",
                category = "Réseaux & IT",
                quantity = 12,
                minThreshold = 4,
                unit = "bobines",
                unitPrice = 89.0,
                supplierName = "Rexel Pro"
            )
        )
        val st3 = dao.insertStockItem(
            StockItemEntity(
                sku = "STK-INV-04",
                name = "Carte Inverter Daikin ERGA08",
                category = "Pièces détachées CVC",
                quantity = 1,
                minThreshold = 3,
                unit = "pcs",
                unitPrice = 390.0,
                supplierName = "Daikin Pièces Express"
            )
        )
        dao.insertStockItem(
            StockItemEntity(
                sku = "STK-DET-12",
                name = "Détergent Désinfectant Certifié EN14476 (20L)",
                category = "Hygiène & Nettoyage",
                quantity = 18,
                minThreshold = 5,
                unit = "bidons",
                unitPrice = 54.0,
                supplierName = "Hygiène Rhône Pro"
            )
        )
        dao.insertStockMovement(
            StockMovementEntity(
                stockItemId = st1,
                itemName = "Filtres CTA Haute Efficacité F7",
                type = "AFFECTATION",
                quantityDelta = -2,
                targetOrReason = "Karim Benali (INT-2026-101)"
            )
        )
        dao.insertStockMovement(
            StockMovementEntity(
                stockItemId = st2,
                itemName = "Câble RJ45 Cat6A Blindé (Bobine 100m)",
                type = "ENTREE",
                quantityDelta = 8,
                targetOrReason = "Réception commande Rexel #RX-442"
            )
        )

        // 6. Commercial Docs (Devis & Factures)
        dao.insertCommercialDoc(
            CommercialDocEntity(
                docNumber = "FAC-2026-089",
                docType = "FACTURE",
                clientId = c4,
                clientName = "Logistique Rhône Express",
                interventionId = i4,
                title = "Nettoyage haute pression Quais 1 à 6",
                amountHt = 1850.0,
                tvaRate = 20.0,
                depositPaid = 500.0,
                amountPaid = 2220.0,
                status = "PAYEE",
                issueDate = "2026-11-15",
                dueDate = "2026-12-15",
                itemsSummary = "Prestation autolaveuse 4h + Détergent EN14476"
            )
        )
        dao.insertCommercialDoc(
            CommercialDocEntity(
                docNumber = "FAC-2026-088",
                docType = "FACTURE",
                clientId = c5,
                clientName = "Cabinet Médical Lumière",
                title = "Remplacement Onduleur Smart-UPS 3000VA",
                amountHt = 1420.0,
                tvaRate = 20.0,
                depositPaid = 0.0,
                amountPaid = 0.0,
                status = "IMPAYEE",
                issueDate = "2026-11-02",
                dueDate = "2026-11-14",
                itemsSummary = "Onduleur APC 3000VA + Main d'œuvre installation"
            )
        )
        dao.insertCommercialDoc(
            CommercialDocEntity(
                docNumber = "FAC-2026-087",
                docType = "FACTURE",
                clientId = c1,
                clientName = "Hôtel Le Grand Lyon",
                title = "Contrat Maintenance CVC Trimestre 4",
                amountHt = 3200.0,
                tvaRate = 20.0,
                depositPaid = 1200.0,
                amountPaid = 1200.0,
                status = "PARTIELLE",
                issueDate = "2026-11-05",
                dueDate = "2026-12-05",
                itemsSummary = "Forfait maintenance préventive T4 + 4 filtres F7"
            )
        )
        dao.insertCommercialDoc(
            CommercialDocEntity(
                docNumber = "DEV-2026-114",
                docType = "DEVIS",
                clientId = c1,
                clientName = "Hôtel Le Grand Lyon",
                title = "Déploiement Wi-Fi 6E Étage 3",
                amountHt = 2150.0,
                tvaRate = 20.0,
                status = "ACCEPTE",
                issueDate = "2026-11-14",
                dueDate = "2026-12-14",
                itemsSummary = "3x Bornes U6-Enterprise + Câblage Cat6A + Configuration"
            )
        )
        dao.insertCommercialDoc(
            CommercialDocEntity(
                docNumber = "DEV-2026-115",
                docType = "DEVIS",
                clientId = c2,
                clientName = "Clinique Saint-Jean",
                title = "Audit & Renouvellement Groupe Électrogène",
                amountHt = 4800.0,
                tvaRate = 20.0,
                status = "ENVOYE",
                issueDate = "2026-11-16",
                dueDate = "2026-12-16",
                itemsSummary = "Révision moteur, batteries secours et test en charge"
            )
        )

        // 7. SAV Tickets
        dao.insertSavTicket(
            SavTicketEntity(
                ticketNumber = "SAV-2026-041",
                clientId = c3,
                clientName = "Julien Lefèvre",
                equipmentName = "Pompe à Chaleur Altherma 3",
                type = "REPARATION",
                priority = "HAUTE",
                status = "EN_TRAITEMENT",
                description = "Panne récurrente code U4 après coupure secteur.",
                slaHours = 24,
                exchangeLog = "16/11 09:15 Client: Erreur U4 affichée.\n16/11 11:40 Karim: Commande carte inverter lancée."
            )
        )
        dao.insertSavTicket(
            SavTicketEntity(
                ticketNumber = "SAV-2026-042",
                clientId = c2,
                clientName = "Clinique Saint-Jean",
                equipmentName = "Switch Cœur Redondant 48P",
                type = "INCIDENT",
                priority = "URGENTE",
                status = "OUVERT",
                description = "Perte de paquets intermittente sur le port fibre SFP+ 2.",
                slaHours = 4,
                exchangeLog = "17/11 07:50 Service SI: Alerte latence sur VLAN Imagerie."
            )
        )

        // 8. Time Entries
        dao.insertTimeEntry(
            TimeEntryEntity(
                employeeId = tech1Id,
                employeeName = "Karim Benali",
                date = "2026-11-17",
                checkInTime = "08:02",
                activityNote = "Départ dépôt -> Hôtel Le Grand Lyon",
                status = "EN_POSTE"
            )
        )
        dao.insertTimeEntry(
            TimeEntryEntity(
                employeeId = tech2Id,
                employeeName = "Lucas Moreau",
                date = "2026-11-17",
                checkInTime = "08:18",
                lateMinutes = 18,
                activityNote = "Préparation switch Cisco en atelier",
                status = "EN_POSTE"
            )
        )
        dao.insertTimeEntry(
            TimeEntryEntity(
                employeeId = chefId,
                employeeName = "Claire Girard",
                date = "2026-11-16",
                checkInTime = "07:30",
                checkOutTime = "17:00",
                workedMinutes = 510,
                overtimeMinutes = 60,
                activityNote = "Supervision chantier quais logistiques",
                status = "TERMINE"
            )
        )

        // 9. Notifications
        dao.insertNotification(
            NotificationEntity(
                title = "Stock critique : Carte Inverter Daikin",
                message = "Quantité restante : 1 pcs (seuil minimal : 3).",
                category = "STOCK"
            )
        )
        dao.insertNotification(
            NotificationEntity(
                title = "Stock faible : Filtres CTA F7",
                message = "Quantité restante : 4 pcs (seuil minimal : 6).",
                category = "STOCK"
            )
        )
        dao.insertNotification(
            NotificationEntity(
                title = "Facture échue : FAC-2026-088",
                message = "Cabinet Médical Lumière — 1 704,00 € TTC impayée depuis le 14/11.",
                category = "FACTURE"
            )
        )
        dao.insertNotification(
            NotificationEntity(
                title = "Intervention en retard : INT-2026-103",
                message = "Dépannage PAC chez Julien Lefèvre en attente depuis hier.",
                category = "INTERVENTION"
            )
        )
        dao.insertNotification(
            NotificationEntity(
                title = "Maintenance prochaine : EQ-IT-008",
                message = "Switch Cœur Redondant 48P programmé le 18/11/2026.",
                category = "MAINTENANCE"
            )
        )

        // 10. Audit Log
        dao.insertAuditLog(
            AuditLogEntity(
                actorName = "Marc Delacroix",
                actorRole = "DIRECTEUR",
                action = "Initialisation de la plateforme Gestion Pro et chargement des référentiels",
                moduleName = "Système"
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                actorName = "Sophie Laurent",
                actorRole = "PLANIFICATEUR",
                action = "Planification de l'intervention urgente INT-2026-102 (Clinique Saint-Jean)",
                moduleName = "Planning"
            )
        )
    }
}
