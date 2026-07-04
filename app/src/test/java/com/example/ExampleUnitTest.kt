package com.example

import com.example.domain.model.UserSession
import com.example.ui.localization.LocalizationManager
import com.example.ui.localization.EnglishTranslations
import com.example.ui.localization.SpanishTranslations
import com.example.ui.localization.GermanTranslations
import com.example.data.remote.FirebaseEmulator
import com.example.data.remote.CloudResult
import com.example.data.local.SyncRecord
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testUserSessionPermissions() {
        // Viewer Permissions
        val viewerSession = UserSession("viewer@company.com", "Viewer", isAuthenticated = true)
        assertFalse(viewerSession.canWrite("Viewer"))
        assertFalse(viewerSession.canWrite("Admin"))
        assertFalse(viewerSession.canDelete("Viewer"))

        // Editor Permissions
        val editorSession = UserSession("editor@company.com", "Editor", isAuthenticated = true)
        assertTrue(editorSession.canWrite("Viewer"))
        assertTrue(editorSession.canWrite("Editor"))
        assertFalse(editorSession.canWrite("Admin")) // Editor cannot write Admin level
        assertTrue(editorSession.canDelete("Viewer"))
        assertFalse(editorSession.canDelete("Admin")) // Editor cannot delete Admin level

        // Admin Permissions
        val adminSession = UserSession("admin@company.com", "Admin", isAuthenticated = true)
        assertTrue(adminSession.canWrite("Viewer"))
        assertTrue(adminSession.canWrite("Admin"))
        assertTrue(adminSession.canDelete("Admin"))
    }

    @Test
    fun testLocalizationManager() {
        val en = LocalizationManager.getTranslations("EN")
        val es = LocalizationManager.getTranslations("ES")
        val de = LocalizationManager.getTranslations("DE")

        assertEquals(EnglishTranslations.appTitle, en.appTitle)
        assertEquals(SpanishTranslations.appTitle, es.appTitle)
        assertEquals(GermanTranslations.appTitle, de.appTitle)

        assertEquals("Centro SecureSync", es.appTitle)
        assertEquals("Sign In", en.loginButton)
        assertEquals("Anmelden", de.loginButton)
    }

    @Test
    fun testFirebaseEmulatorAccessControl() {
        val testRecord = SyncRecord(
            id = "test-1",
            title = "Test Doc",
            content = "Highly classified information",
            securityLevel = "Admin"
        )

        // 1. Viewer trying to push to cloud should be blocked
        val viewerResult = FirebaseEmulator.pushToCloud(testRecord, "Viewer")
        assertTrue(viewerResult is CloudResult.Error)
        assertEquals("Access Denied: Role 'Viewer' lacks write permissions.", (viewerResult as CloudResult.Error).message)

        // 2. Editor trying to push an Admin-restricted record should be blocked
        val editorAdminResult = FirebaseEmulator.pushToCloud(testRecord, "Editor")
        assertTrue(editorAdminResult is CloudResult.Error)
        assertEquals("Access Denied: Role 'Editor' cannot create or edit 'Admin' restricted records.", (editorAdminResult as CloudResult.Error).message)

        // 3. Editor trying to push an Editor-restricted record should succeed
        val editorNormalRecord = testRecord.copy(securityLevel = "Editor")
        val editorNormalResult = FirebaseEmulator.pushToCloud(editorNormalRecord, "Editor")
        assertTrue(editorNormalResult is CloudResult.Success)

        // 4. Admin trying to push Admin-restricted record should succeed
        val adminResult = FirebaseEmulator.pushToCloud(testRecord, "Admin")
        assertTrue(adminResult is CloudResult.Success)
    }
}
