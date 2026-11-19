package com.example.data.model

enum class ClientType(val label: String) {
    PARTICULIER("Particulier"),
    ENTREPRISE("Entreprise")
}

enum class EmployeeRole(val label: String, val badgeCode: String) {
    ADMIN("Administrateur", "ADM"),
    DIRECTEUR("Directeur", "DIR"),
    RESPONSABLE("Responsable", "RSP"),
    SECRETAIRE("Secrétaire", "SEC"),
    PLANIFICATEUR("Planificateur", "PLN"),
    COMPTABLE("Comptable", "CPT"),
    CHEF_EQUIPE("Chef d'équipe", "CHF"),
    TECHNICIEN("Technicien", "TEC");

    fun canAccessBilling(): Boolean = this in setOf(ADMIN, DIRECTEUR, RESPONSABLE, COMPTABLE, SECRETAIRE)
    fun canAccessTeamAdmin(): Boolean = this in setOf(ADMIN, DIRECTEUR, RESPONSABLE)
    fun canAccessPlanning(): Boolean = this in setOf(ADMIN, DIRECTEUR, RESPONSABLE, PLANIFICATEUR, CHEF_EQUIPE, SECRETAIRE, TECHNICIEN)
    fun canPlanInterventions(): Boolean = this in setOf(ADMIN, DIRECTEUR, RESPONSABLE, PLANIFICATEUR, CHEF_EQUIPE, SECRETAIRE)
    fun canAccessReports(): Boolean = this in setOf(ADMIN, DIRECTEUR, RESPONSABLE, COMPTABLE, PLANIFICATEUR)
    fun canAccessAudit(): Boolean = this in setOf(ADMIN, DIRECTEUR)
}

enum class EmployeeAvailability(val label: String) {
    DISPONIBLE("Disponible"),
    EN_INTERVENTION("En intervention"),
    CONGE("En congé"),
    ABSENT("Absent")
}

enum class InterventionStatus(val label: String) {
    NOUVELLE("Nouvelle"),
    A_PLANIFIER("À planifier"),
    PLANIFIEE("Planifiée"),
    EN_COURS("En cours"),
    EN_ATTENTE("En attente"),
    TERMINEE("Terminée"),
    ANNULEE("Annulée")
}

enum class PriorityLevel(val label: String) {
    BASSE("Basse"),
    NORMALE("Normale"),
    HAUTE("Haute"),
    URGENTE("Urgente")
}

enum class EquipmentStatus(val label: String) {
    OPERATIONNEL("Opérationnel"),
    MAINTENANCE("En maintenance"),
    EN_PANNE("En panne"),
    HORS_SERVICE("Hors service")
}

enum class MaintenanceType(val label: String) {
    PREVENTIVE("Préventive"),
    CORRECTIVE("Corrective")
}

enum class MovementType(val label: String) {
    ENTREE("Entrée"),
    SORTIE("Sortie"),
    AFFECTATION("Affectation technicien")
}

enum class CommercialDocType(val label: String) {
    DEVIS("Devis"),
    FACTURE("Facture")
}

enum class CommercialDocStatus(val label: String) {
    BROUILLON("Brouillon"),
    ENVOYE("Envoyé"),
    ACCEPTE("Accepté"),
    PAYEE("Payée"),
    PARTIELLE("Paiement partiel"),
    IMPAYEE("Impayée"),
    REFUSE("Refusé")
}

enum class SavType(val label: String) {
    RECLAMATION("Réclamation"),
    ASSISTANCE("Assistance"),
    INCIDENT("Incident"),
    REPARATION("Réparation"),
    RETOUR("Retour client")
}

enum class SavStatus(val label: String) {
    OUVERT("Ouvert"),
    EN_TRAITEMENT("En traitement"),
    RESOLU("Résolu"),
    CLOS("Clos")
}

enum class TimeEntryStatus(val label: String) {
    EN_POSTE("En poste"),
    TERMINE("Journée terminée"),
    ABSENCE("Absence / Congé")
}

enum class NotificationCategory(val label: String) {
    INTERVENTION("Intervention"),
    PLANNING("Planning"),
    MAINTENANCE("Maintenance"),
    STOCK("Stock"),
    FACTURE("Facturation"),
    SAV("SAV")
}
