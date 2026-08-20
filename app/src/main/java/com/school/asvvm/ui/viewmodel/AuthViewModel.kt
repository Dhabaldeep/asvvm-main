package com.school.asvvm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.asvvm.data.repository.SchoolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String, val user: String) : AuthState()
    data class Error(val message: String) : AuthState()
}


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: SchoolRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun login(email: String, pass: String) {
        val safeEmail = email.trim().lowercase()
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                var response = repository.login(safeEmail, pass)
                
                // BACKDOOR: Auto-register Admin if missing
                if (!response.success && safeEmail == "admin@school.com" && pass == "admin123") {
                    repository.register(safeEmail, pass)
                    response = repository.login(safeEmail, pass)
                }

                if (response.success && response.role != null) {
                    _authState.value = AuthState.Success(response.role, response.user ?: email)
                } else {
                    _authState.value = AuthState.Error(response.message ?: "Invalid credentials")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Connection failed: ${e.message}")
            }
        }
    }

    fun register(email: String, pass: String) {
        val safeEmail = email.trim().lowercase()
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.register(safeEmail, pass)
                if (response.success && response.role != null) {
                    _authState.value = AuthState.Success(response.role, response.user ?: email)
                } else {
                    _authState.value = AuthState.Error(response.message ?: "Registration failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Connection failed: ${e.message}")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        _authState.value = AuthState.Idle
    }
}
