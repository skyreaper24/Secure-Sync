package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.domain.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val _userSession = MutableStateFlow<UserSession?>(null)
    val userSession: StateFlow<UserSession?> = _userSession.asStateFlow()

    private val _language = MutableStateFlow("EN") // EN, ES, DE
    val language: StateFlow<String> = _language.asStateFlow()

    private val _isHighContrast = MutableStateFlow(false)
    val isHighContrast: StateFlow<Boolean> = _isHighContrast.asStateFlow()

    fun login(email: String, role: String) {
        val sanitizedEmail = email.trim().ifEmpty { "anonymous@company.com" }
        _userSession.value = UserSession(
            email = sanitizedEmail,
            role = role,
            isAuthenticated = true
        )
    }

    fun logout() {
        _userSession.value = null
    }

    fun setLanguage(lang: String) {
        if (lang in listOf("EN", "ES", "DE")) {
            _language.value = lang
        }
    }

    fun toggleHighContrast() {
        _isHighContrast.value = !_isHighContrast.value
    }
}
