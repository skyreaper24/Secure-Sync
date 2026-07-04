package com.example.data.remote

import com.example.data.local.SyncRecord
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

object FirebaseEmulator {
    // In-memory remote database representing Firestore
    private val remoteDatabase = mutableMapOf<String, SyncRecord>()

    // In-memory cloud storage snapshots for user backups
    private val backupSnapshots = mutableMapOf<String, List<SyncRecord>>()

    // Emits real-time data synchronization events from other simulated devices
    private val _realtimeUpdates = MutableSharedFlow<RemoteSyncEvent>(extraBufferCapacity = 10)
    val realtimeUpdates: SharedFlow<RemoteSyncEvent> = _realtimeUpdates.asSharedFlow()

    private val simulatedDeviceNames = listOf("Pixel Tablet (Kitchen)", "Galaxy Fold 5", "MacBook Cloud Console", "iPhone 15 Pro")

    init {
        // Pre-populate Firebase Firestore with some initial data
        val initialRecords = listOf(
            SyncRecord(
                id = "init-1",
                title = "Global Network Config",
                content = "Primary API gateway set to api.securesync.internal. Keep backups active.",
                securityLevel = "Admin",
                updatedAt = System.currentTimeMillis() - 100000,
                synced = true,
                author = "admin@company.com",
                deviceSource = "Cloud Console"
            ),
            SyncRecord(
                id = "init-2",
                title = "Q3 Product Milestones",
                content = "1. Edge-to-edge Compose UI\n2. Real-time offline Room sync\n3. Accessibility audit: high contrast & keyboard ready.",
                securityLevel = "Editor",
                updatedAt = System.currentTimeMillis() - 50000,
                synced = true,
                author = "editor@company.com",
                deviceSource = "Galaxy Fold 5"
            ),
            SyncRecord(
                id = "init-3",
                title = "Public Documentation Guidelines",
                content = "Ensure all images contain meaningful content descriptions for screen reader accessibility.",
                securityLevel = "Viewer",
                updatedAt = System.currentTimeMillis() - 20000,
                synced = true,
                author = "viewer@company.com",
                deviceSource = "MacBook Cloud Console"
            )
        )
        for (record in initialRecords) {
            remoteDatabase[record.id] = record
        }
        // Pre-populate some initial backups for instant restore functionality
        backupSnapshots["backup-${System.currentTimeMillis() - 86400000}-Initial Setup Snapshot"] = initialRecords.map { it.copy() }
    }

    fun getRemoteRecords(): List<SyncRecord> {
        return remoteDatabase.values.filter { !it.isDeleted }
    }

    // Save a record to Firebase Firestore. Enforces role-based rules.
    @Synchronized
    fun pushToCloud(record: SyncRecord, userRole: String): CloudResult {
        // Enforce role-based access control (Viewer cannot write anything, Editor cannot write Admin level records)
        if (userRole == "Viewer") {
            return CloudResult.Error("Access Denied: Role 'Viewer' lacks write permissions.")
        }
        if (userRole == "Editor" && record.securityLevel == "Admin") {
            return CloudResult.Error("Access Denied: Role 'Editor' cannot create or edit 'Admin' restricted records.")
        }

        // Simulating low-bandwidth payload reduction
        val processedRecord = record.copy(synced = true, updatedAt = System.currentTimeMillis())
        remoteDatabase[record.id] = processedRecord

        return CloudResult.Success(processedRecord)
    }

    // Backup entire local db to safe backup folder / cloud snapshot
    @Synchronized
    fun createBackup(records: List<SyncRecord>, label: String = "Manual Snapshot"): Int {
        val backupId = "backup-${System.currentTimeMillis()}-$label"
        val nonDeleted = records.filter { !it.isDeleted }.map { it.copy() }
        backupSnapshots[backupId] = nonDeleted
        
        // Emulate sync to cloud DB for those records as well
        nonDeleted.forEach { record ->
            remoteDatabase[record.id] = record.copy(synced = true)
        }
        return nonDeleted.size
    }

    @Synchronized
    fun getBackupSnapshots(): List<BackupSnapshot> {
        return backupSnapshots.map { (id, list) ->
            val timestamp = id.split("-").getOrNull(1)?.toLongOrNull() ?: System.currentTimeMillis()
            val label = id.split("-").getOrNull(2) ?: "Cloud Backup Snapshot"
            BackupSnapshot(id, timestamp, label, list.size)
        }.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun restoreFromBackup(backupId: String): List<SyncRecord>? {
        return backupSnapshots[backupId]?.map { it.copy(synced = true) }
    }

    @Synchronized
    fun deleteFromCloud(id: String, userRole: String): CloudResult {
        if (userRole == "Viewer") {
            return CloudResult.Error("Access Denied: Role 'Viewer' lacks delete permissions.")
        }
        val record = remoteDatabase[id]
        if (record != null && userRole == "Editor" && record.securityLevel == "Admin") {
            return CloudResult.Error("Access Denied: Role 'Editor' cannot delete 'Admin' records.")
        }

        remoteDatabase.remove(id)
        return CloudResult.Success(null)
    }

    // Triggered periodically by our simulator logic in the ViewModel to mimic real-time changes
    suspend fun simulateIncomingSyncEvent() {
        val randomDevice = simulatedDeviceNames.random()
        val randomId = UUID.randomUUID().toString()
        val actions = listOf("ADD", "UPDATE")
        val action = actions.random()

        if (action == "ADD" || remoteDatabase.isEmpty()) {
            val newRecord = SyncRecord(
                id = randomId,
                title = "Cloud Update: Sync Status ${System.currentTimeMillis() % 1000}",
                content = "This update was pushed in real-time by $randomDevice. Real-time Firebase data synchronization is live.",
                securityLevel = listOf("Viewer", "Editor", "Admin").random(),
                updatedAt = System.currentTimeMillis(),
                synced = true,
                author = "remote-colleague@company.com",
                deviceSource = randomDevice
            )
            remoteDatabase[newRecord.id] = newRecord
            _realtimeUpdates.emit(RemoteSyncEvent.AddedOrUpdated(newRecord))
        } else {
            val targetKey = remoteDatabase.keys.random()
            val existing = remoteDatabase[targetKey]!!
            val updated = existing.copy(
                title = existing.title + " (Edited)",
                content = existing.content + "\n[Synced edit from $randomDevice]",
                updatedAt = System.currentTimeMillis()
            )
            remoteDatabase[targetKey] = updated
            _realtimeUpdates.emit(RemoteSyncEvent.AddedOrUpdated(updated))
        }
    }
}

sealed class RemoteSyncEvent {
    data class AddedOrUpdated(val record: SyncRecord) : RemoteSyncEvent()
    data class Deleted(val id: String) : RemoteSyncEvent()
}

sealed class CloudResult {
    data class Success(val record: SyncRecord?) : CloudResult()
    data class Error(val message: String) : CloudResult()
}

data class BackupSnapshot(
    val id: String,
    val timestamp: Long,
    val label: String,
    val recordCount: Int
)
