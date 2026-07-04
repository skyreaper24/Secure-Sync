package com.example.domain.model

data class UserSession(
    val email: String,
    val role: String, // "Viewer", "Editor", "Admin"
    val isAuthenticated: Boolean = false
) {
    fun canWrite(securityLevel: String): Boolean {
        if (!isAuthenticated) return false
        return when (role) {
            "Admin" -> true
            "Editor" -> securityLevel != "Admin"
            "Viewer" -> false
            else -> false
        }
    }

    fun canDelete(recordLevel: String): Boolean {
        if (!isAuthenticated) return false
        return when (role) {
            "Admin" -> true
            "Editor" -> recordLevel != "Admin"
            "Viewer" -> false
            else -> false
        }
    }
}
