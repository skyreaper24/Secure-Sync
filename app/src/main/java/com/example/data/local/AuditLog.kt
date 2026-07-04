package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "AUTH", "SYNC", "SECURITY", "BACKUP", "PERFORMANCE"
    val message: String,
    val userRole: String,
    val isSuccess: Boolean = true
)
