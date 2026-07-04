package com.example.ui.localization

interface Translations {
    val appTitle: String
    val emailLabel: String
    val passwordLabel: String
    val loginButton: String
    val registerButton: String
    val logoutButton: String
    val dashboardTab: String
    val logsTab: String
    val settingsTab: String
    val syncStatus: String
    val synced: String
    val unsynced: String
    val roleLabel: String
    val selectRole: String
    val addRecordTitle: String
    val titleHint: String
    val contentHint: String
    val saveButton: String
    val deleteButton: String
    val rbacWarning: String
    val lowBandwidthLabel: String
    val lowBandwidthDesc: String
    val offlineModeLabel: String
    val offlineModeDesc: String
    val backupButton: String
    val backupSuccess: String
    val backupAdminOnly: String
    val accessibilityMode: String
    val altTextDashboardHero: String
    val altTextStatusIcon: String
    val searchHint: String
    val simulatedEventReceived: String
    val forcedSyncSuccess: String
}

object EnglishTranslations : Translations {
    override val appTitle = "SecureSync Hub"
    override val emailLabel = "Email Address"
    override val passwordLabel = "Password (any to login)"
    override val loginButton = "Sign In"
    override val registerButton = "Create Account"
    override val logoutButton = "Sign Out"
    override val dashboardTab = "Hub"
    override val logsTab = "Security Audits"
    override val settingsTab = "System Settings"
    override val syncStatus = "Cloud Sync"
    override val synced = "Synchronized with Firestore"
    override val unsynced = "Pending Sync (Offline Cache)"
    override val roleLabel = "Security Clearance"
    override val selectRole = "Select Assigned Role"
    override val addRecordTitle = "Secure Data Record"
    override val titleHint = "Enter classification or title..."
    override val contentHint = "Enter secure data payload..."
    override val saveButton = "Save & Synchronize"
    override val deleteButton = "Purge Record"
    override val rbacWarning = "Role lacks permissions to execute this operation."
    override val lowBandwidthLabel = "Low Bandwidth Compression"
    override val lowBandwidthDesc = "Compress sync payload and delay background checks to save cellular data."
    override val offlineModeLabel = "Force Offline Cache"
    override val offlineModeDesc = "Stop live cloud sync and queue all edits inside Room offline persistence."
    override val backupButton = "Initiate Cloud Backup"
    override val backupSuccess = "Cloud-based snapshot backup complete!"
    override val backupAdminOnly = "Backup requires Administrator level clearance."
    override val accessibilityMode = "High Contrast Contrast Mode"
    override val altTextDashboardHero = "SecureSync Cloud connection diagram showing active bi-directional synchronization nodes."
    override val altTextStatusIcon = "Security status indicator circle."
    override val searchHint = "Search secure records..."
    override val simulatedEventReceived = "Real-time sync update received from remote device."
    override val forcedSyncSuccess = "Manual force-sync successful! Synchronized items."
}

object SpanishTranslations : Translations {
    override val appTitle = "Centro SecureSync"
    override val emailLabel = "Correo Electrónico"
    override val passwordLabel = "Contraseña"
    override val loginButton = "Iniciar Sesión"
    override val registerButton = "Crear Cuenta"
    override val logoutButton = "Cerrar Sesión"
    override val dashboardTab = "Centro"
    override val logsTab = "Auditorías de Seguridad"
    override val settingsTab = "Ajustes de Sistema"
    override val syncStatus = "Sincronización en la Nube"
    override val synced = "Sincronizado con Firestore"
    override val unsynced = "Pendiente de Sincronización"
    override val roleLabel = "Autorización de Seguridad"
    override val selectRole = "Seleccionar Rol Asignado"
    override val addRecordTitle = "Registro de Datos Seguro"
    override val titleHint = "Ingrese título o clasificación..."
    override val contentHint = "Ingrese datos de seguridad..."
    override val saveButton = "Guardar y Sincronizar"
    override val deleteButton = "Eliminar Registro"
    override val rbacWarning = "El rol carece de permisos para realizar esta operación."
    override val lowBandwidthLabel = "Compresión de Bajo Ancho de Banda"
    override val lowBandwidthDesc = "Comprime la carga útil de sincronización y retrasa revisiones de red."
    override val offlineModeLabel = "Forzar Caché Fuera de Línea"
    override val offlineModeDesc = "Detiene la sincronización activa y pone en cola los cambios locales en Room."
    override val backupButton = "Iniciar Copia de Seguridad"
    override val backupSuccess = "¡Copia de seguridad en la nube completada!"
    override val backupAdminOnly = "La copia de seguridad requiere credenciales de Administrador."
    override val accessibilityMode = "Modo de Alto Contraste"
    override val altTextDashboardHero = "Diagrama de conexión de SecureSync que muestra nodos activos de sincronización bidireccional."
    override val altTextStatusIcon = "Círculo indicador de estado de seguridad."
    override val searchHint = "Buscar registros seguros..."
    override val simulatedEventReceived = "Actualización en tiempo real recibida de un dispositivo remoto."
    override val forcedSyncSuccess = "¡Sincronización manual exitosa! Elementos actualizados."
}

object GermanTranslations : Translations {
    override val appTitle = "SecureSync Hub"
    override val emailLabel = "E-Mail-Adresse"
    override val passwordLabel = "Passwort"
    override val loginButton = "Anmelden"
    override val registerButton = "Konto Erstellen"
    override val logoutButton = "Abmelden"
    override val dashboardTab = "Zentrale"
    override val logsTab = "Sicherheits-Audits"
    override val settingsTab = "Systemeinstellungen"
    override val syncStatus = "Cloud-Synchronisierung"
    override val synced = "Mit Firestore synchronisiert"
    override val unsynced = "Ausstehende Synchronisierung"
    override val roleLabel = "Sicherheitsfreigabe"
    override val selectRole = "Zugewiesene Rolle Auswählen"
    override val addRecordTitle = "Sicherer Datensatz"
    override val titleHint = "Titel oder Klassifizierung eingeben..."
    override val contentHint = "Sichere Nutzdaten eingeben..."
    override val saveButton = "Speichern & Synchronisieren"
    override val deleteButton = "Datensatz Löschen"
    override val rbacWarning = "Rolle verfügt nicht über die erforderlichen Rechte."
    override val lowBandwidthLabel = "Kompression für niedrige Bandbreite"
    override val lowBandwidthDesc = "Komprimiert Nutzdaten und verzögert Netzwerkprüfungen zur Datensparung."
    override val offlineModeLabel = "Offline-Cache erzwingen"
    override val offlineModeDesc = "Stoppt die Live-Synchronisierung und speichert Änderungen lokal in Room."
    override val backupButton = "Cloud-Backup starten"
    override val backupSuccess = "Cloud-Backup erfolgreich abgeschlossen!"
    override val backupAdminOnly = "Backup erfordert Administrator-Freigabe."
    override val accessibilityMode = "Hoher Kontrastmodus"
    override val altTextDashboardHero = "SecureSync-Verbindungsdiagramm mit aktiven bidirektionalen Synchronisationsknoten."
    override val altTextStatusIcon = "Sicherheitsstatus-Anzeigekreis."
    override val searchHint = "Sichere Datensätze durchsuchen..."
    override val simulatedEventReceived = "Echtzeit-Synchronisationsupdate vom Remote-Gerät empfangen."
    override val forcedSyncSuccess = "Manuelle Synchronisierung erfolgreich! Elemente aktualisiert."
}

object LocalizationManager {
    fun getTranslations(lang: String): Translations {
        return when (lang) {
            "ES" -> SpanishTranslations
            "DE" -> GermanTranslations
            else -> EnglishTranslations
        }
    }
}
