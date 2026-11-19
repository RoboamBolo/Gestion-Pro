package com.example.data.sync

import com.example.data.local.GestionDao
import com.example.data.local.SyncQueueEntity

sealed class SyncPushResult {
    data object Success : SyncPushResult()
    data class Conflict(val serverMessage: String) : SyncPushResult()
    data class NetworkError(val reason: String) : SyncPushResult()
    data class ServerNotConfigured(val reason: String) : SyncPushResult()
}

/**
 * Contrat d'abstraction prêt pour une future intégration Retrofit / Ktor REST.
 */
interface RemoteSyncApi {
    suspend fun pushOperation(item: SyncQueueEntity): SyncPushResult
}

/**
 * Implémentation par défaut tant qu'aucun endpoint REST n'est configuré :
 * ne simule jamais une synchronisation réussie et préserve la file locale.
 */
class UnconfiguredRestSyncApi : RemoteSyncApi {
    override suspend fun pushOperation(item: SyncQueueEntity): SyncPushResult {
        return SyncPushResult.ServerNotConfigured(
            "Serveur API REST non configuré : opération conservée localement dans SQLite."
        )
    }
}

data class SyncExecutionReport(
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val conflictCount: Int = 0,
    val remainingPendingCount: Int = 0,
    val message: String
)

class OfflineSyncManager(
    private val dao: GestionDao,
    private val remoteApi: RemoteSyncApi = UnconfiguredRestSyncApi()
) {
    suspend fun synchronizePending(isNetworkAvailable: Boolean): SyncExecutionReport {
        val pendingItems = dao.getPendingSyncItems()
        if (pendingItems.isEmpty()) {
            return SyncExecutionReport(
                remainingPendingCount = 0,
                message = "Aucune opération en attente de synchronisation."
            )
        }

        if (!isNetworkAvailable) {
            return SyncExecutionReport(
                remainingPendingCount = pendingItems.size,
                message = "Hors ligne : ${pendingItems.size} opération(s) conservée(s) localement."
            )
        }

        var synced = 0
        var failed = 0
        var conflicts = 0
        var serverMissingMessage: String? = null

        for (item in pendingItems) {
            when (val result = remoteApi.pushOperation(item)) {
                is SyncPushResult.Success -> {
                    dao.markSyncItemSynced(item.id)
                    synced++
                }
                is SyncPushResult.Conflict -> {
                    dao.markSyncItemError(
                        id = item.id,
                        status = "CONFLICT",
                        error = result.serverMessage,
                        retryCount = item.retryCount + 1
                    )
                    conflicts++
                }
                is SyncPushResult.NetworkError -> {
                    dao.markSyncItemError(
                        id = item.id,
                        status = "FAILED",
                        error = result.reason,
                        retryCount = item.retryCount + 1
                    )
                    failed++
                }
                is SyncPushResult.ServerNotConfigured -> {
                    // Conserve explicitement l'élément en attente (FAILED/PENDING) sans jamais simuler un succès
                    dao.markSyncItemError(
                        id = item.id,
                        status = "PENDING",
                        error = result.reason,
                        retryCount = item.retryCount
                    )
                    serverMissingMessage = result.reason
                    failed++
                }
            }
        }

        val remaining = dao.getPendingSyncItems().size
        val summary = when {
            serverMissingMessage != null ->
                "En attente d'API REST : $remaining opération(s) conservée(s) localement dans sync_queue."
            conflicts > 0 ->
                "$synced synchronisée(s), $conflicts conflit(s), $failed échec(s)."
            failed > 0 ->
                "$synced synchronisée(s), $failed en erreur (nouvelle tentative requise)."
            else ->
                "$synced opération(s) synchronisée(s) avec le serveur."
        }

        return SyncExecutionReport(
            syncedCount = synced,
            failedCount = failed,
            conflictCount = conflicts,
            remainingPendingCount = remaining,
            message = summary
        )
    }

    suspend fun retryQueueItem(item: SyncQueueEntity) {
        dao.updateSyncQueueItem(
            item.copy(status = "PENDING", lastError = "")
        )
    }

    suspend fun resolveConflictKeepLocal(item: SyncQueueEntity) {
        dao.updateSyncQueueItem(
            item.copy(
                status = "PENDING",
                lastError = "Résolution locale prioritaire demandée",
                localUpdatedAt = System.currentTimeMillis()
            )
        )
    }
}
