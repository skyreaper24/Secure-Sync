package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AuditLog
import com.example.data.local.SyncRecord
import com.example.data.remote.FirebaseEmulator
import com.example.data.remote.RemoteSyncEvent
import com.example.data.repository.SyncRepository
import com.example.data.repository.SyncResult
import com.example.domain.model.UserSession
import com.example.data.remote.BackupSnapshot
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class SyncViewModel(
    application: Application,
    private val repository: SyncRepository
) : AndroidViewModel(application) {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _isLowBandwidth = MutableStateFlow(false)
    val isLowBandwidth: StateFlow<Boolean> = _isLowBandwidth.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSimulationRunning = MutableStateFlow(true)
    val isSimulationRunning: StateFlow<Boolean> = _isSimulationRunning.asStateFlow()

    private val _backupSnapshots = MutableStateFlow<List<BackupSnapshot>>(emptyList())
    val backupSnapshots: StateFlow<List<BackupSnapshot>> = _backupSnapshots.asStateFlow()

    // Expose filtered list of sync records using combine with search query
    val records: StateFlow<List<SyncRecord>> = repository.allRecords
        .combine(_searchQuery) { records, query ->
            if (query.isBlank()) {
                records
            } else {
                records.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true) ||
                    it.securityLevel.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val auditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // 1. Observe real-time Firebase remote events (Simulating real-time synchronization)
        viewModelScope.launch {
            FirebaseEmulator.realtimeUpdates.collect { event ->
                if (_isOffline.value) return@collect // Ignore network updates when offline

                when (event) {
                    is RemoteSyncEvent.AddedOrUpdated -> {
                        // Insert into Room offline database instantly
                        repository.insertAuditLog(
                            AuditLog(
                                eventType = "SYNC",
                                message = "Real-time sync: Received record update '${event.record.title}' from ${event.record.deviceSource}",
                                userRole = "System",
                                isSuccess = true
                            )
                        )
                        // Update local Room database
                        val updatedLocal = event.record.copy(synced = true)
                        // Trigger a local Room persistence write
                        viewModelScope.launch {
                            // Directly save local representation
                            repository.insertRecord(updatedLocal, "Admin", "system-sync@google.com")
                        }

                        // Trigger visual Android push notification!
                        NotificationHelper.triggerNotification(
                            getApplication(),
                            "Sync: ${event.record.title}",
                            "Real-time update from ${event.record.deviceSource} synced."
                        )
                        _statusMessage.value = "Real-time Update: '${event.record.title}' synced."
                    }
                    is RemoteSyncEvent.Deleted -> {
                        // Handle remote deletion
                    }
                }
            }
        }

        // 2. Periodic background cloud update simulator (Simulating collaborative multi-device environment)
        viewModelScope.launch {
            while (true) {
                delay(20000) // Trigger every 20 seconds
                if (_isSimulationRunning.value && !_isOffline.value) {
                    try {
                        FirebaseEmulator.simulateIncomingSyncEvent()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fetchBackupSnapshots()
    }

    fun addRecord(title: String, content: String, securityLevel: String, session: UserSession?) {
        val userRole = session?.role ?: "Viewer"
        val userEmail = session?.email ?: "anonymous@company.com"

        if (title.isBlank()) {
            _statusMessage.value = "Title cannot be blank."
            return
        }

        viewModelScope.launch {
            val newRecord = SyncRecord(
                id = UUID.randomUUID().toString(),
                title = title,
                content = content,
                securityLevel = securityLevel,
                updatedAt = System.currentTimeMillis(),
                synced = false,
                author = userEmail,
                deviceSource = "My Device"
            )

            when (val result = repository.insertRecord(newRecord, userRole, userEmail)) {
                is SyncResult.Synced -> {
                    _statusMessage.value = "Successfully saved and synced to Firebase."
                }
                is SyncResult.SuccessOffline -> {
                    _statusMessage.value = "Offline persistence active: Saved locally. Queued for synchronization."
                }
                is SyncResult.RejectedByCloud -> {
                    _statusMessage.value = "RBAC Restriction: ${result.message}"
                }
                is SyncResult.Error -> {
                    _statusMessage.value = "Error saving record: ${result.message}"
                }
            }
        }
    }

    fun deleteRecord(id: String, session: UserSession?) {
        val userRole = session?.role ?: "Viewer"
        val userEmail = session?.email ?: "anonymous@company.com"

        viewModelScope.launch {
            when (val result = repository.deleteRecord(id, userRole, userEmail)) {
                is SyncResult.Synced -> {
                    _statusMessage.value = "Record purged and synced globally."
                }
                is SyncResult.SuccessOffline -> {
                    _statusMessage.value = "Offline cache: Soft-deleted locally."
                }
                is SyncResult.RejectedByCloud -> {
                    _statusMessage.value = "RBAC Restriction: ${result.message}"
                }
                is SyncResult.Error -> {
                    _statusMessage.value = "Error: ${result.message}"
                }
            }
        }
    }

    fun forceSync(session: UserSession?) {
        val userRole = session?.role ?: "Viewer"
        val userEmail = session?.email ?: "anonymous@company.com"

        viewModelScope.launch {
            _statusMessage.value = "Syncing..."
            val report = repository.forceSync(userRole, userEmail)
            if (report.errors.isNotEmpty()) {
                _statusMessage.value = "Sync partially successful. Errors: ${report.errors.first()}"
            } else {
                _statusMessage.value = "Sync complete. Uploaded ${report.pushedCount}, downloaded ${report.pulledCount} records."
            }
        }
    }

    fun runCloudBackup(session: UserSession?) {
        val userRole = session?.role ?: "Viewer"

        viewModelScope.launch {
            try {
                val count = repository.runCloudBackup(userRole)
                _statusMessage.value = "Cloud backup complete. $count elements saved securely."
                fetchBackupSnapshots() // Refresh backups list
            } catch (e: SecurityException) {
                _statusMessage.value = "Access Denied: Backup requires 'Admin' role clearance."
            } catch (e: Exception) {
                _statusMessage.value = "Backup failed: ${e.message}"
            }
        }
    }

    fun fetchBackupSnapshots() {
        viewModelScope.launch {
            try {
                _backupSnapshots.value = repository.getBackupSnapshots()
            } catch (e: Exception) {
                // ignore or log
            }
        }
    }

    fun restoreFromBackup(backupId: String, session: UserSession?, onRestoreSuccess: () -> Unit = {}) {
        val userRole = session?.role ?: "Viewer"
        val userEmail = session?.email ?: "anonymous@company.com"

        viewModelScope.launch {
            try {
                val count = repository.restoreFromBackup(backupId, userRole, userEmail)
                _statusMessage.value = "Restore complete. $count elements recovered successfully."
                fetchBackupSnapshots() // Refresh backups list
                onRestoreSuccess()
            } catch (e: SecurityException) {
                _statusMessage.value = "Access Denied: Restore requires Editor or Admin role privileges."
            } catch (e: Exception) {
                _statusMessage.value = "Restore failed: ${e.message}"
            }
        }
    }

    fun toggleOfflineMode() {
        val next = !_isOffline.value
        _isOffline.value = next
        repository.isOfflineMode = next
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLog(
                    eventType = "SYNC",
                    message = if (next) "Offline mode forced. Cloud sync suspended." else "Online sync resumed. Connected to Firebase emulator.",
                    userRole = "User",
                    isSuccess = true
                )
            )
        }
        _statusMessage.value = if (next) "Offline Mode Enabled." else "Online Mode Enabled."
    }

    fun toggleLowBandwidth() {
        val next = !_isLowBandwidth.value
        _isLowBandwidth.value = next
        repository.isLowBandwidthMode = next
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLog(
                    eventType = "PERFORMANCE",
                    message = if (next) "Low-bandwidth throttling active. Compression on." else "Low-bandwidth mode deactivated. Normal syncing.",
                    userRole = "User",
                    isSuccess = true
                )
            )
        }
        _statusMessage.value = if (next) "Low-Bandwidth Compression On." else "Standard Mode Enabled."
    }

    fun toggleSimulation() {
        _isSimulationRunning.value = !_isSimulationRunning.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _statusMessage.value = "Audit logs cleared."
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}
