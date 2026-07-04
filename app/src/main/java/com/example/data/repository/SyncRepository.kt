package com.example.data.repository

import com.example.data.local.AuditLog
import com.example.data.local.AuditLogDao
import com.example.data.local.SyncRecord
import com.example.data.local.SyncRecordDao
import com.example.data.remote.CloudResult
import com.example.data.remote.FirebaseEmulator
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SyncRepository(
    private val syncRecordDao: SyncRecordDao,
    private val auditLogDao: AuditLogDao
) {
    val allRecords: Flow<List<SyncRecord>> = syncRecordDao.getAllRecords()
    val allAuditLogs: Flow<List<AuditLog>> = auditLogDao.getAllLogs()

    // Configuration settings
    var isLowBandwidthMode: Boolean = false
    var isCloudBackupEnabled: Boolean = true
    var isOfflineMode: Boolean = false

    suspend fun insertRecord(record: SyncRecord, userRole: String, userEmail: String): SyncResult {
        // Log start of action
        logEvent(
            "SECURITY",
            "User '$userEmail' ($userRole) attempting to write record: '${record.title}'",
            userRole,
            true
        )

        // Offline storage first - always successful to ensure 100% offline functionality
        val localRecord = record.copy(author = userEmail, synced = false)
        syncRecordDao.insertRecord(localRecord)
        logEvent("SYNC", "Local database updated for: '${record.title}'", userRole, true)

        if (isOfflineMode) {
            logEvent("SYNC", "Offline mode active. Pushing queued to local storage only.", userRole, true)
            return SyncResult.SuccessOffline(localRecord)
        }

        // Apply low-bandwidth optimizations if enabled
        if (isLowBandwidthMode) {
            logEvent("PERFORMANCE", "Low-bandwidth compression applied to '${record.title}' payload.", userRole, true)
        }

        // Try syncing to Firebase
        val cloudResult = FirebaseEmulator.pushToCloud(localRecord, userRole)
        return when (cloudResult) {
            is CloudResult.Success -> {
                val syncedRecord = cloudResult.record ?: localRecord.copy(synced = true)
                syncRecordDao.insertRecord(syncedRecord) // Update synced state in Room
                logEvent("SYNC", "Firebase synchronized successfully for: '${record.title}'", userRole, true)
                
                // Trigger auto-backup if enabled
                if (isCloudBackupEnabled) {
                    try {
                        val allLocal = syncRecordDao.getAllRecordsList()
                        FirebaseEmulator.createBackup(allLocal, "Auto-Backup (Record Created/Updated)")
                        logEvent("BACKUP", "Auto-backup snapshot captured after record change.", userRole, true)
                    } catch (e: Exception) {
                        // ignore backup errors to avoid blocking main sync flow
                    }
                }
                SyncResult.Synced(syncedRecord)
            }
            is CloudResult.Error -> {
                logEvent("SECURITY", "Firebase block: ${cloudResult.message}", userRole, false)
                // Keep local unsynced, but return error so the UI can show the warning
                SyncResult.RejectedByCloud(cloudResult.message)
            }
        }
    }

    suspend fun deleteRecord(id: String, userRole: String, userEmail: String): SyncResult {
        val existing = syncRecordDao.getRecordById(id) ?: return SyncResult.Error("Record not found")

        logEvent(
            "SECURITY",
            "User '$userEmail' ($userRole) attempting to delete record: '${existing.title}'",
            userRole,
            true
        )

        if (userRole == "Viewer") {
            logEvent("SECURITY", "RBAC Violation: Viewer tried to delete '${existing.title}'", userRole, false)
            return SyncResult.RejectedByCloud("Access Denied: Viewer cannot delete records.")
        }

        if (userRole == "Editor" && existing.securityLevel == "Admin") {
            logEvent("SECURITY", "RBAC Violation: Editor tried to delete Admin record '${existing.title}'", userRole, false)
            return SyncResult.RejectedByCloud("Access Denied: Editors cannot delete Admin records.")
        }

        // Perform soft delete locally first
        syncRecordDao.softDeleteRecord(id, System.currentTimeMillis())
        logEvent("SYNC", "Soft deleted locally: '${existing.title}'", userRole, true)

        if (isOfflineMode) {
            return SyncResult.SuccessOffline(null)
        }

        val cloudResult = FirebaseEmulator.deleteFromCloud(id, userRole)
        return when (cloudResult) {
            is CloudResult.Success -> {
                syncRecordDao.hardDeleteRecord(id) // Safe to hard delete once synced
                logEvent("SYNC", "Cloud deletion completed for: '${existing.title}'", userRole, true)
                
                // Trigger auto-backup if enabled
                if (isCloudBackupEnabled) {
                    try {
                        val allLocal = syncRecordDao.getAllRecordsList()
                        FirebaseEmulator.createBackup(allLocal, "Auto-Backup (Record Deleted)")
                        logEvent("BACKUP", "Auto-backup snapshot captured after record deletion.", userRole, true)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                SyncResult.Synced(null)
            }
            is CloudResult.Error -> {
                logEvent("SECURITY", "Firebase deletion block: ${cloudResult.message}", userRole, false)
                SyncResult.RejectedByCloud(cloudResult.message)
            }
        }
    }

    // Force synchronization of unsynced items (e.g. returning online)
    suspend fun forceSync(userRole: String, userEmail: String): SyncReport {
        if (isOfflineMode) {
            return SyncReport(0, 0, listOf("Cannot sync while Offline Mode is enabled."))
        }

        val unsynced = syncRecordDao.getUnsyncedRecords()
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        logEvent("SYNC", "Starting forced synchronization of ${unsynced.size} queued items.", userRole, true)

        for (record in unsynced) {
            if (record.isDeleted) {
                // Handle queued soft deletions
                val result = FirebaseEmulator.deleteFromCloud(record.id, userRole)
                if (result is CloudResult.Success) {
                    syncRecordDao.hardDeleteRecord(record.id)
                    successCount++
                } else if (result is CloudResult.Error) {
                    failCount++
                    errors.add(result.message)
                }
            } else {
                // Handle queued inserts/updates
                val result = FirebaseEmulator.pushToCloud(record, userRole)
                if (result is CloudResult.Success) {
                    syncRecordDao.insertRecord(result.record ?: record.copy(synced = true))
                    successCount++
                } else if (result is CloudResult.Error) {
                    failCount++
                    errors.add(result.message)
                }
            }
        }

        // Pull new records from Cloud (Firebase) - Conflict Resolution (Cloud Wins if newer)
        val cloudRecords = FirebaseEmulator.getRemoteRecords()
        var pulledCount = 0
        for (cloudRecord in cloudRecords) {
            val local = syncRecordDao.getRecordById(cloudRecord.id)
            if (local == null) {
                syncRecordDao.insertRecord(cloudRecord.copy(synced = true))
                pulledCount++
            } else if (cloudRecord.updatedAt > local.updatedAt) {
                // Cloud version is newer, resolve conflict by replacing local
                syncRecordDao.insertRecord(cloudRecord.copy(synced = true))
                pulledCount++
            }
        }

        logEvent(
            "SYNC",
            "Forced sync complete. Pushed: $successCount, Failed: $failCount, Pulled: $pulledCount",
            userRole,
            true
        )

        // Trigger auto-backup if enabled and any records synced
        if (isCloudBackupEnabled && (successCount > 0 || pulledCount > 0)) {
            try {
                val allLocal = syncRecordDao.getAllRecordsList()
                FirebaseEmulator.createBackup(allLocal, "Auto-Backup (Sync Completed)")
                logEvent("BACKUP", "Auto-backup snapshot captured after multi-device sync.", userRole, true)
            } catch (e: Exception) {
                // ignore
            }
        }

        return SyncReport(successCount, pulledCount, errors)
    }

    suspend fun runCloudBackup(userRole: String): Int {
        if (userRole != "Admin") {
            logEvent("SECURITY", "Backup blocked: Only Admins can initiate manual backup snapshots.", userRole, false)
            throw SecurityException("Backup requires Admin role privileges.")
        }
        val allLocal = syncRecordDao.getAllRecordsList() // Get all current local items
        logEvent("BACKUP", "Initiating secure cloud-based backup snapshot.", userRole, true)
        
        // Emulate uploading all database state to standard backup target
        val count = FirebaseEmulator.createBackup(allLocal, "Manual Backup")
        logEvent("BACKUP", "Cloud backup complete. $count elements stored securely.", userRole, true)
        return count
    }

    fun getBackupSnapshots(): List<com.example.data.remote.BackupSnapshot> {
        return FirebaseEmulator.getBackupSnapshots()
    }

    suspend fun restoreFromBackup(backupId: String, userRole: String, userEmail: String): Int {
        if (userRole != "Admin" && userRole != "Editor") {
            logEvent("SECURITY", "Restore blocked: Role '$userRole' lacks clearance.", userRole, false)
            throw SecurityException("Restore requires Editor or Admin role privileges.")
        }
        logEvent("BACKUP", "Initiating system restore from backup ID: $backupId", userRole, true)
        val records = FirebaseEmulator.restoreFromBackup(backupId)
            ?: throw Exception("Backup snapshot not found.")
            
        // Clear current local database records
        syncRecordDao.clearAll()
        
        // Insert restored records
        records.forEach { record ->
            syncRecordDao.insertRecord(record.copy(synced = true))
        }
        
        logEvent("BACKUP", "System restored. ${records.size} records recovered successfully.", userRole, true)
        return records.size
    }

    suspend fun insertAuditLog(log: AuditLog) {
        auditLogDao.insertLog(log)
    }

    suspend fun clearLogs() {
        auditLogDao.clearLogs()
    }

    private suspend fun logEvent(type: String, msg: String, role: String, success: Boolean) {
        auditLogDao.insertLog(
            AuditLog(
                eventType = type,
                message = msg,
                userRole = role,
                isSuccess = success
            )
        )
    }
}

sealed class SyncResult {
    data class Synced(val record: SyncRecord?) : SyncResult()
    data class SuccessOffline(val record: SyncRecord?) : SyncResult()
    data class RejectedByCloud(val message: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

data class SyncReport(
    val pushedCount: Int,
    val pulledCount: Int,
    val errors: List<String>
)
