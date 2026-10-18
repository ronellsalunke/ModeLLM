package dev.ronellsalunke.modellm.data.repository

sealed interface SyncState {
    data object Idle : SyncState
    data object Running : SyncState
    data class Success(val syncedAt: Long) : SyncState
    data class Failed(val message: String) : SyncState
}
