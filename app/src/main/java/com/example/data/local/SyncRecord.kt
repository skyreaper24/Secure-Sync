package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_records")
data class SyncRecord(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val securityLevel: String = "Viewer", // "Viewer", "Editor", "Admin"
    val updatedAt: Long = System.currentTimeMillis(),
    val isLocalOnly: Boolean = false,
    val isDeleted: Boolean = false,
    val synced: Boolean = false,
    val author: String = "anonymous",
    val deviceSource: String = "Android Device"
)
