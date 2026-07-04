package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.repository.SyncRepository
import com.example.ui.screens.SecureSyncApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SyncViewModel
import com.example.ui.viewmodel.SyncViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: SyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize offline persistence database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "securesync_db"
        )
        .fallbackToDestructiveMigration()
        .build()

        repository = SyncRepository(
            syncRecordDao = database.syncRecordDao(),
            auditLogDao = database.auditLogDao()
        )

        enableEdgeToEdge()

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val syncViewModel: SyncViewModel = viewModel(
                factory = SyncViewModelFactory(application, repository)
            )

            val isHighContrast by authViewModel.isHighContrast.collectAsState()

            MyApplicationTheme(isHighContrast = isHighContrast) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SecureSyncApp(
                        authViewModel = authViewModel,
                        syncViewModel = syncViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
