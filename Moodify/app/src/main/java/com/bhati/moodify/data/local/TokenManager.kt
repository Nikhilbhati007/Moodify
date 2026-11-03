package com.bhati.moodify.data.local

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("moodify_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun saveUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

    fun getUsername(): String? {
        return prefs.getString("username", null)
    }

    fun clearToken() {
        prefs.edit().remove("auth_token").remove("username").apply()
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
    }

    fun isDarkTheme(): Boolean {
        return prefs.getBoolean("is_dark_theme", true) // Default true
    }
}
