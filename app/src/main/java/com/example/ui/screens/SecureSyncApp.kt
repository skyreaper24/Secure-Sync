@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AuditLog
import com.example.data.local.SyncRecord
import com.example.ui.localization.LocalizationManager
import com.example.ui.localization.Translations
import com.example.ui.theme.BentoBg
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoOnPrimary
import com.example.ui.theme.BentoOnSurface
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoSurface
import com.example.ui.theme.BentoSurfaceVariant
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SyncViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecureSyncApp(
    authViewModel: AuthViewModel,
    syncViewModel: SyncViewModel,
    modifier: Modifier = Modifier
) {
    val session by authViewModel.userSession.collectAsState()
    val currentLang by authViewModel.language.collectAsState()
    val isHighContrast by authViewModel.isHighContrast.collectAsState()
    val translations = LocalizationManager.getTranslations(currentLang)

    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage by syncViewModel.statusMessage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
                syncViewModel.clearStatus()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = BentoBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BentoBg)
        ) {
            val currentSession = session
            if (currentSession == null || !currentSession.isAuthenticated) {
                AuthGateScreen(
                    translations = translations,
                    isHighContrast = isHighContrast,
                    onLogin = { email, role -> authViewModel.login(email, role) }
                )
            } else {
                MainDashboardScreen(
                    translations = translations,
                    isHighContrast = isHighContrast,
                    authViewModel = authViewModel,
                    syncViewModel = syncViewModel
                )
            }
        }
    }
}

@Composable
fun AuthGateScreen(
    translations: Translations,
    isHighContrast: Boolean,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Viewer") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val loginButtonFocusRequester = remember { FocusRequester() }

    var isEmailFocused by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .border(
                    width = if (isHighContrast) 3.dp else 1.dp,
                    color = if (isHighContrast) BentoPrimary else BentoBorder,
                    shape = RoundedCornerShape(28.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = BentoSurface
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 32.dp, horizontal = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bento Security Icon Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BentoPrimary)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = BentoOnPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = translations.appTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = BentoOnSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "BENTO SECURITY GATEWAY",
                    style = MaterialTheme.typography.labelMedium,
                    color = BentoPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(translations.emailLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BentoOnSurface,
                        unfocusedTextColor = BentoOnSurface,
                        focusedBorderColor = BentoPrimary,
                        unfocusedBorderColor = BentoBorder,
                        focusedLabelColor = BentoPrimary,
                        unfocusedLabelColor = BentoOnSurface.copy(alpha = 0.6f),
                        focusedLeadingIconColor = BentoPrimary,
                        unfocusedLeadingIconColor = BentoPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(emailFocusRequester)
                        .onFocusChanged { isEmailFocused = it.isFocused }
                        .testTag("email_input"),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(translations.passwordLabel) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BentoOnSurface,
                        unfocusedTextColor = BentoOnSurface,
                        focusedBorderColor = BentoPrimary,
                        unfocusedBorderColor = BentoBorder,
                        focusedLabelColor = BentoPrimary,
                        unfocusedLabelColor = BentoOnSurface.copy(alpha = 0.6f),
                        focusedLeadingIconColor = BentoPrimary,
                        unfocusedLeadingIconColor = BentoPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .onFocusChanged { isPasswordFocused = it.isFocused }
                        .testTag("password_input"),
                    leadingIcon = {
                        Icon(Icons.Default.VpnKey, contentDescription = null)
                    },
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Modern horizontal role selector chips (Aligned to Bento Style)
                Text(
                    text = translations.selectRole,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BentoOnSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("role_selector_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val roles = listOf("Viewer", "Editor", "Admin")
                    roles.forEach { role ->
                        val isSelected = selectedRole == role
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BentoPrimary else BentoSurfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BentoPrimary else BentoBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedRole = role }
                                .testTag("role_${role.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = role,
                                color = if (isSelected) BentoOnPrimary else BentoOnSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Submit Button
                Button(
                    onClick = {
                        onLogin(email, selectedRole)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoPrimary,
                        contentColor = BentoOnPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .focusRequester(loginButtonFocusRequester)
                        .testTag("login_submit"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Login,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = translations.loginButton,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainDashboardScreen(
    translations: Translations,
    isHighContrast: Boolean,
    authViewModel: AuthViewModel,
    syncViewModel: SyncViewModel
) {
    val session by authViewModel.userSession.collectAsState()
    val isOffline by syncViewModel.isOffline.collectAsState()
    val isLowBandwidth by syncViewModel.isLowBandwidth.collectAsState()
    val isSimulationRunning by syncViewModel.isSimulationRunning.collectAsState()
    val searchQuery by syncViewModel.searchQuery.collectAsState()
    val records by syncViewModel.records.collectAsState()
    val logs by syncViewModel.auditLogs.collectAsState()
    val backupSnapshots by syncViewModel.backupSnapshots.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoBg)
    ) {
        // App header bar
        TopAppBarHeader(
            translations = translations,
            session = session,
            isOffline = isOffline,
            onLogout = { authViewModel.logout() }
        )

        // Main responsive body content
        Box(modifier = Modifier.weight(1f)) {
            if (showRestoreDialog) {
                RestoreBackupDialog(
                    translations = translations,
                    isHighContrast = isHighContrast,
                    snapshots = backupSnapshots,
                    onDismiss = { showRestoreDialog = false },
                    onRestore = { backupId ->
                        syncViewModel.restoreFromBackup(backupId, session)
                        showRestoreDialog = false
                    }
                )
            }

            when (selectedTabIndex) {
                0 -> BentoDashboardTab(
                    translations = translations,
                    isHighContrast = isHighContrast,
                    isOffline = isOffline,
                    isLowBandwidth = isLowBandwidth,
                    isSimulationRunning = isSimulationRunning,
                    session = session,
                    recordsCount = records.size,
                    onForceSync = { syncViewModel.forceSync(session) },
                    onToggleOffline = { syncViewModel.toggleOfflineMode() },
                    onToggleLowBandwidth = { syncViewModel.toggleLowBandwidth() },
                    onToggleSimulation = { syncViewModel.toggleSimulation() },
                    onBackup = { syncViewModel.runCloudBackup(session) },
                    onShowRestoreDialog = { showRestoreDialog = true },
                    authViewModel = authViewModel
                )
                1 -> VaultRecordsTab(
                    translations = translations,
                    isHighContrast = isHighContrast,
                    records = records,
                    searchQuery = searchQuery,
                    session = session,
                    onSearchQueryChange = { syncViewModel.setSearchQuery(it) },
                    onAddRecord = { title, content, securityLevel ->
                        syncViewModel.addRecord(title, content, securityLevel, session)
                    },
                    onDeleteRecord = { id ->
                        syncViewModel.deleteRecord(id, session)
                    }
                )
                2 -> AuditLogsTab(
                    translations = translations,
                    isHighContrast = isHighContrast,
                    logs = logs,
                    onClearLogs = { syncViewModel.clearLogs() }
                )
            }
        }

        // Bento navigation bar (Height 72dp / h-20 equivalent, rounded-t-3xl)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = BentoSurfaceVariant,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val menuItems = listOf(
                    Triple(0, Icons.Default.GridView, translations.dashboardTab),
                    Triple(1, Icons.Default.List, "Vault"),
                    Triple(2, Icons.Default.History, "Audits")
                )

                menuItems.forEach { (index, icon, label) ->
                    val isSelected = selectedTabIndex == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedTabIndex = index }
                            .testTag("tab_$index"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Smooth active container pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BentoPrimary else Color.Transparent)
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) BentoOnPrimary else BentoOnSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BentoPrimary else BentoOnSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopAppBarHeader(
    translations: Translations,
    session: com.example.domain.model.UserSession?,
    isOffline: Boolean,
    onLogout: () -> Unit
) {
    Surface(
        color = BentoSurface,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(width = 1.dp, color = BentoBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BentoPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = BentoOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Nexus Core",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoOnSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOffline) Color.Red else Color.Green)
                                .semantics { contentDescription = translations.altTextStatusIcon }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOffline) "Offline Queueing" else "Sync Connected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOffline) Color.Red else Color.Green
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = session?.email ?: "Guest",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoOnSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (session?.role) {
                                    "Admin" -> Color(0xFFFEE2E2)
                                    "Editor" -> Color(0xFFFEF9C3)
                                    else -> Color(0xFFDBEAFE)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = session?.role ?: "Viewer",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = when (session?.role) {
                                "Admin" -> Color(0xFF991B1B)
                                "Editor" -> Color(0xFF854D0E)
                                else -> Color(0xFF1E40AF)
                            }
                        )
                    }
                }

                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .testTag("logout_button")
                        .semantics { contentDescription = translations.logoutButton }
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = translations.logoutButton,
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun BentoDashboardTab(
    translations: Translations,
    isHighContrast: Boolean,
    isOffline: Boolean,
    isLowBandwidth: Boolean,
    isSimulationRunning: Boolean,
    session: com.example.domain.model.UserSession?,
    recordsCount: Int,
    onForceSync: () -> Unit,
    onToggleOffline: () -> Unit,
    onToggleLowBandwidth: () -> Unit,
    onToggleSimulation: () -> Unit,
    onBackup: () -> Unit,
    onShowRestoreDialog: () -> Unit,
    authViewModel: AuthViewModel
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoBg)
    ) {
        val isTablet = maxWidth >= 600.dp
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Sync Bento Card (Full Width)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isHighContrast) 2.dp else 1.dp,
                            color = if (isHighContrast) BentoPrimary else BentoBorder,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = BentoPrimary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = BentoOnPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(BentoOnPrimary)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isOffline) "Offline Local" else "Real-Time Active",
                                    color = BentoPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = translations.syncStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = BentoOnPrimary.copy(alpha = 0.8f)
                        )

                        Text(
                            text = if (isOffline) {
                                "Changes saved locally to Room Vault. ($recordsCount files ready)"
                            } else {
                                "All nodes synchronized."
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BentoOnPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onForceSync,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BentoOnPrimary,
                                contentColor = BentoPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("force_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Force Synchronize Nodes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Grid Layout (Adaptive side-by-side on tablet, or columns on mobile)
            item {
                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OfflineBentoCard(
                                isOffline = isOffline,
                                isHighContrast = isHighContrast,
                                translations = translations,
                                onToggle = onToggleOffline
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            RoleAccessBentoCard(
                                role = session?.role ?: "Viewer",
                                isHighContrast = isHighContrast,
                                translations = translations
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OfflineBentoCard(
                            isOffline = isOffline,
                            isHighContrast = isHighContrast,
                            translations = translations,
                            onToggle = onToggleOffline
                        )
                        RoleAccessBentoCard(
                            role = session?.role ?: "Viewer",
                            isHighContrast = isHighContrast,
                            translations = translations
                        )
                    }
                }
            }

            item {
                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CloudBackupBentoCard(
                                isHighContrast = isHighContrast,
                                translations = translations,
                                onBackup = onBackup,
                                onShowRestoreDialog = onShowRestoreDialog
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PerformanceBentoCard(
                                isLowBandwidth = isLowBandwidth,
                                isHighContrast = isHighContrast,
                                translations = translations,
                                onToggle = onToggleLowBandwidth
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CloudBackupBentoCard(
                            isHighContrast = isHighContrast,
                            translations = translations,
                            onBackup = onBackup,
                            onShowRestoreDialog = onShowRestoreDialog
                        )
                        PerformanceBentoCard(
                            isLowBandwidth = isLowBandwidth,
                            isHighContrast = isHighContrast,
                            translations = translations,
                            onToggle = onToggleLowBandwidth
                        )
                    }
                }
            }

            // Simulation and Preferences Bento Box (Row/Full Width)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isHighContrast) 2.dp else 1.dp,
                            color = if (isHighContrast) BentoPrimary else BentoBorder,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = BentoSurface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Settings & Simulation",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = BentoOnSurface
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isSimulationRunning) Color.Green else Color.Gray)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Simulation switch row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoSurfaceVariant)
                                .clickable { onToggleSimulation() }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Simulated Multi-Device Nodes",
                                    color = BentoOnSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Auto-creates random records from remote nodes to simulate live synchronization.",
                                    color = BentoOnSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isSimulationRunning,
                                onCheckedChange = { onToggleSimulation() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BentoPrimary,
                                    checkedTrackColor = BentoPrimary.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Multi-language localization row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val currentLang = authViewModel.language.collectAsState().value
                            val languages = listOf("EN", "ES", "DE")
                            
                            languages.forEach { lang ->
                                val isSelected = currentLang == lang
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BentoPrimary else BentoSurfaceVariant)
                                        .clickable { authViewModel.setLanguage(lang) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lang,
                                        color = if (isSelected) BentoOnPrimary else BentoOnSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Accessibility mode row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoSurfaceVariant)
                                .clickable { authViewModel.toggleHighContrast() }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = translations.accessibilityMode,
                                    color = BentoOnSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Enforces primary high-contrast borders and touch feedback optimization.",
                                    color = BentoOnSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isHighContrast,
                                onCheckedChange = { authViewModel.toggleHighContrast() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BentoPrimary,
                                    checkedTrackColor = BentoPrimary.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineBentoCard(
    isOffline: Boolean,
    isHighContrast: Boolean,
    translations: Translations,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(
                width = if (isHighContrast) 2.dp else 1.dp,
                color = if (isHighContrast) BentoPrimary else BentoBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudSync,
                contentDescription = null,
                tint = BentoPrimary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = translations.offlineModeLabel,
                    color = BentoOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Offline local queueing active. Room DB caching active.",
                    color = BentoOnSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RoleAccessBentoCard(
    role: String,
    isHighContrast: Boolean,
    translations: Translations
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(
                width = if (isHighContrast) 2.dp else 1.dp,
                color = if (isHighContrast) BentoPrimary else BentoBorder,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = when (role) {
                    "Admin" -> Icons.Default.Security
                    "Editor" -> Icons.Default.Edit
                    else -> Icons.Default.Visibility
                },
                contentDescription = null,
                tint = BentoPrimary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = translations.roleLabel,
                    color = BentoOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Signed in as $role. Authorization levels checked on database queries.",
                    color = BentoOnSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CloudBackupBentoCard(
    isHighContrast: Boolean,
    translations: Translations,
    onBackup: () -> Unit,
    onShowRestoreDialog: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(
                width = if (isHighContrast) 2.dp else 1.dp,
                color = if (isHighContrast) BentoPrimary else BentoBorder,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = BentoSurfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = BentoPrimary,
                    modifier = Modifier.size(24.dp)
                )
                PulseAnimationRing()
            }
            Column {
                Text(
                    text = "Cloud Backup & Restore",
                    color = BentoOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Auto-backup active. Snapshot backup or restore from Cloud.",
                    color = BentoOnSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBackup,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoPrimary,
                            contentColor = BentoOnPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .testTag("backup_button")
                    ) {
                        Text("Backup", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onShowRestoreDialog,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isHighContrast) BentoPrimary else BentoBorder
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BentoPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .testTag("restore_button")
                    ) {
                        Text("Restore", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PulseAnimationRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(24.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = BentoPrimary.copy(alpha = alpha),
                radius = size.minDimension / 2f * scale
            )
            drawCircle(
                color = BentoPrimary,
                radius = size.minDimension / 4f
            )
        }
    }
}

@Composable
fun PerformanceBentoCard(
    isLowBandwidth: Boolean,
    isHighContrast: Boolean,
    translations: Translations,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(
                width = if (isHighContrast) 2.dp else 1.dp,
                color = if (isHighContrast) BentoPrimary else BentoBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.NetworkWifi,
                contentDescription = null,
                tint = BentoPrimary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = translations.lowBandwidthLabel,
                    color = BentoOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isLowBandwidth) "Compression active. Delaying heavy queries." else "Full synchronization speed active.",
                    color = BentoOnSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VaultRecordsTab(
    translations: Translations,
    isHighContrast: Boolean,
    records: List<SyncRecord>,
    searchQuery: String,
    session: com.example.domain.model.UserSession?,
    onSearchQueryChange: (String) -> Unit,
    onAddRecord: (String, String, String) -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var selectedClearance by remember { mutableStateOf("Viewer") }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoBg)
    ) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Entry Form & Sync Canvas
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isHighContrast) 2.dp else 1.dp,
                                color = if (isHighContrast) BentoPrimary else BentoBorder,
                                shape = RoundedCornerShape(24.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = BentoSurface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Create Vault Record",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = BentoPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            RecordFormFields(
                                title = titleInput,
                                content = contentInput,
                                clearance = selectedClearance,
                                translations = translations,
                                session = session,
                                onTitleChange = { titleInput = it },
                                onContentChange = { contentInput = it },
                                onClearanceChange = { selectedClearance = it },
                                onSubmit = {
                                    onAddRecord(titleInput, contentInput, selectedClearance)
                                    titleInput = ""
                                    contentInput = ""
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(BentoSurfaceVariant)
                            .border(1.dp, BentoBorder, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CloudSyncCanvasDiagram(translations = translations)
                    }
                }

                // Right Column: Records Archive
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(
                            width = if (isHighContrast) 2.dp else 1.dp,
                            color = if (isHighContrast) BentoPrimary else BentoBorder,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = BentoSurface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Classified Records Archive",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = BentoOnSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text(translations.searchHint) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BentoOnSurface,
                                unfocusedTextColor = BentoOnSurface,
                                focusedBorderColor = BentoPrimary,
                                unfocusedBorderColor = BentoBorder,
                                focusedLeadingIconColor = BentoPrimary,
                                unfocusedLeadingIconColor = BentoPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_bar"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(records) { record ->
                                RecordBentoItem(
                                    record = record,
                                    isHighContrast = isHighContrast,
                                    translations = translations,
                                    session = session,
                                    onDelete = { onDeleteRecord(record.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Mobile Stack Layout
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isHighContrast) 2.dp else 1.dp,
                                color = if (isHighContrast) BentoPrimary else BentoBorder,
                                shape = RoundedCornerShape(24.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = BentoSurface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Create Vault Record",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = BentoPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            RecordFormFields(
                                title = titleInput,
                                content = contentInput,
                                clearance = selectedClearance,
                                translations = translations,
                                session = session,
                                onTitleChange = { titleInput = it },
                                onContentChange = { contentInput = it },
                                onClearanceChange = { selectedClearance = it },
                                onSubmit = {
                                    onAddRecord(titleInput, contentInput, selectedClearance)
                                    titleInput = ""
                                    contentInput = ""
                                }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Classified Archive",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = BentoOnSurface
                    )
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(translations.searchHint) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BentoOnSurface,
                            unfocusedTextColor = BentoOnSurface,
                            focusedBorderColor = BentoPrimary,
                            unfocusedBorderColor = BentoBorder,
                            focusedLeadingIconColor = BentoPrimary,
                            unfocusedLeadingIconColor = BentoPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_bar"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                items(records) { record ->
                    RecordBentoItem(
                        record = record,
                        isHighContrast = isHighContrast,
                        translations = translations,
                        session = session,
                        onDelete = { onDeleteRecord(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecordFormFields(
    title: String,
    content: String,
    clearance: String,
    translations: Translations,
    session: com.example.domain.model.UserSession?,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onClearanceChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = { Text(translations.titleHint) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = BentoOnSurface,
            unfocusedTextColor = BentoOnSurface,
            focusedBorderColor = BentoPrimary,
            unfocusedBorderColor = BentoBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_title_input"),
        shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = content,
        onValueChange = onContentChange,
        placeholder = { Text(translations.contentHint) },
        minLines = 3,
        maxLines = 4,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = BentoOnSurface,
            unfocusedTextColor = BentoOnSurface,
            focusedBorderColor = BentoPrimary,
            unfocusedBorderColor = BentoBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_content_input"),
        shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Security Level Requirements:",
        style = MaterialTheme.typography.labelMedium,
        color = BentoOnSurface.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 6.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val clearances = listOf("Viewer", "Editor", "Admin")
        clearances.forEach { level ->
            val isSelected = clearance == level
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) BentoPrimary else BentoSurfaceVariant)
                    .clickable { onClearanceChange(level) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level,
                    color = if (isSelected) BentoOnPrimary else BentoOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    val hasWriteAccess = session?.canWrite(clearance) == true

    Button(
        onClick = onSubmit,
        enabled = hasWriteAccess,
        colors = ButtonDefaults.buttonColors(
            containerColor = BentoPrimary,
            contentColor = BentoOnPrimary,
            disabledContainerColor = BentoSurfaceVariant,
            disabledContentColor = BentoOnSurface.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("save_record_button"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(translations.saveButton, fontWeight = FontWeight.Bold)
        }
    }

    if (!hasWriteAccess) {
        Text(
            text = "🔒 ${translations.rbacWarning}",
            color = Color.Red,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecordBentoItem(
    record: SyncRecord,
    isHighContrast: Boolean,
    translations: Translations,
    session: com.example.domain.model.UserSession?,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isHighContrast) 2.dp else 1.dp,
                color = if (isHighContrast) BentoPrimary else BentoBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .testTag("record_item_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = BentoSurfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = BentoOnSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (record.synced) Color.Green else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (record.synced) "Synced" else "Offline Queue",
                            fontSize = 10.sp,
                            color = BentoOnSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (record.securityLevel) {
                                "Admin" -> Color(0xFFFEE2E2)
                                "Editor" -> Color(0xFFFEF9C3)
                                else -> Color(0xFFDBEAFE)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = record.securityLevel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = when (record.securityLevel) {
                            "Admin" -> Color(0xFF991B1B)
                            "Editor" -> Color(0xFF854D0E)
                            else -> Color(0xFF1E40AF)
                        }
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BentoBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = record.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BentoOnSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Author: ${record.author}",
                            fontSize = 10.sp,
                            color = BentoOnSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Node: ${record.deviceSource}",
                            fontSize = 10.sp,
                            color = BentoOnSurface.copy(alpha = 0.5f)
                        )
                    }

                    if (session?.canDelete(record.securityLevel) == true) {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("delete_button_${record.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Purge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Red.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Lacks Purge Access",
                                color = Color.Red,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudSyncCanvasDiagram(translations: Translations) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = translations.altTextDashboardHero },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height

            val leftNodeX = width * 0.2f
            val rightNodeX = width * 0.8f
            val nodeY = height * 0.5f

            // 1. Draw connecting dotted path line
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), dotOffset)
            drawLine(
                color = BentoPrimary,
                start = Offset(leftNodeX, nodeY),
                end = Offset(rightNodeX, nodeY),
                strokeWidth = 4f,
                pathEffect = pathEffect
            )

            // 2. Draw Left Device Node (Vault/Local Cache)
            drawRoundRect(
                color = BentoPrimary,
                topLeft = Offset(leftNodeX - 35f, nodeY - 35f),
                size = Size(70f, 70f),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // 3. Draw Right Cloud Node (Firebase Server)
            drawCircle(
                color = BentoPrimary,
                center = Offset(rightNodeX, nodeY),
                radius = 35f
            )

            // 4. Draw node details inside (Visual representation of sync)
            drawCircle(
                color = BentoOnPrimary,
                center = Offset(rightNodeX, nodeY),
                radius = 12f
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Active Cloud Nodes Syncing",
            fontWeight = FontWeight.Bold,
            color = BentoOnSurface,
            fontSize = 13.sp
        )
        Text(
            text = "Room Persistence Model ⇄ Firestore Real-Time Stream",
            color = BentoOnSurface.copy(alpha = 0.5f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AuditLogsTab(
    translations: Translations,
    isHighContrast: Boolean,
    logs: List<AuditLog>,
    onClearLogs: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .border(
                width = if (isHighContrast) 2.dp else 1.dp,
                color = if (isHighContrast) BentoPrimary else BentoBorder,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = translations.logsTab,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoOnSurface
                    )
                    Text(
                        text = "Cryptographic integrity audits & operations logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoOnSurface.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = onClearLogs,
                    colors = ButtonDefaults.buttonColors(containerColor = BentoSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Clear Logs", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs.reversed()) { log ->
                    AuditLogBentoItem(log = log)
                }
            }
        }
    }
}

@Composable
fun AuditLogBentoItem(log: AuditLog) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = formatter.format(Date(log.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BentoSurfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (log.isSuccess) Color(0xFF15803D).copy(alpha = 0.2f) else Color(0xFFB91C1C).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (log.isSuccess) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (log.isSuccess) Color.Green else Color.Red,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                color = BentoOnSurface,
                lineHeight = 16.sp
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time: $formattedTime",
                    fontSize = 10.sp,
                    color = BentoOnSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "Role: ${log.userRole}",
                    fontSize = 10.sp,
                    color = BentoPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RestoreBackupDialog(
    translations: Translations,
    isHighContrast: Boolean,
    snapshots: List<com.example.data.remote.BackupSnapshot>,
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cloud Restore",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = BentoOnSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Select a backup snapshot from Firebase Cloud Storage to restore. Warning: This will overwrite your local database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BentoOnSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (snapshots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No backup snapshots found on Cloud.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BentoOnSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(snapshots) { snapshot ->
                            val dateString = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date(snapshot.timestamp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isHighContrast) 2.dp else 1.dp,
                                        color = if (isHighContrast) BentoPrimary else BentoBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onRestore(snapshot.id) },
                                colors = CardDefaults.cardColors(containerColor = BentoSurfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = snapshot.label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = BentoOnSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Created: $dateString",
                                            fontSize = 11.sp,
                                            color = BentoOnSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "${snapshot.recordCount} records",
                                            fontSize = 11.sp,
                                            color = BentoPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "Restore this backup",
                                        tint = BentoPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BentoPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = BentoSurface,
        shape = RoundedCornerShape(24.dp)
    )
}
