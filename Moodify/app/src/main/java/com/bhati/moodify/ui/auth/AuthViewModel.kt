package com.bhati.moodify.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhati.moodify.data.api.RetrofitClient
import com.bhati.moodify.data.local.TokenManager
import com.bhati.moodify.data.model.LoginRequest
import com.bhati.moodify.data.model.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(tokenManager.isDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _username = MutableStateFlow(tokenManager.getUsername())
    val username: StateFlow<String?> = _username.asStateFlow()

    init {
        checkToken()
    }

    private fun checkToken() {
        if (tokenManager.isLoggedIn()) {
            _authState.value = AuthState.Success
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.login(LoginRequest("$username@moodify.com", password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    if (authResponse.token != null) {
                        tokenManager.saveToken(authResponse.token)
                        tokenManager.saveUsername(username)
                        _username.value = username
                        _authState.value = AuthState.Success
                    } else {
                        _authState.value = AuthState.Error(authResponse.error ?: "Login failed")
                    }
                } else {
                    val errorString = response.errorBody()?.string() ?: "Invalid credentials"
                    _authState.value = AuthState.Error(errorString)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Network error: ${e.message}")
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.register(RegisterRequest("$username@moodify.com", password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    if (authResponse.error == null) {
                        // After register, you could auto-login, but for now we'll just go straight to login
                        // the backend returns message: User registered successfully
                        login(username, password)
                    } else {
                        _authState.value = AuthState.Error(authResponse.error)
                    }
                } else {
                    val errorString = response.errorBody()?.string() ?: "Registration failed"
                    _authState.value = AuthState.Error(errorString)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Network error: ${e.message}")
            }
        }
    }

    fun logout() {
        tokenManager.clearToken()
        _username.value = null
        _authState.value = AuthState.Idle
    }

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        tokenManager.setDarkTheme(newTheme)
        _isDarkTheme.value = newTheme
    }
}
