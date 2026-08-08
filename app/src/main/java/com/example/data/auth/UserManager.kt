package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GoogleUser(
    val email: String,
    val displayName: String,
    val role: String = "Petugas Pendata Disperindag Pamekasan",
    val avatarUrl: String? = null
)

object UserManager {
    val defaultUsers = listOf(
        GoogleUser(
            email = "bidangpasar.indag@gmail.com",
            displayName = "Tim Pendata Pasar Waru (Disperindag)",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
        ),
        GoogleUser(
            email = "petugas.pasar01@pamekasankab.go.id",
            displayName = "Ahmad Zaini (Tim Pendata A)",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
        ),
        GoogleUser(
            email = "petugas.pasar02@pamekasankab.go.id",
            displayName = "Siti Rahmah (Tim Pendata B)",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"
        )
    )

    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private var sharedPrefs: SharedPreferences? = null

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences("disperindag_user_prefs", Context.MODE_PRIVATE)
        sharedPrefs = prefs
        val savedEmail = prefs.getString("email", null)
        val savedName = prefs.getString("displayName", null)
        if (savedEmail != null && savedName != null) {
            _currentUser.value = GoogleUser(savedEmail, savedName)
            _isLoggedIn.value = true
        } else {
            _currentUser.value = null
            _isLoggedIn.value = false
        }
    }

    fun loginWithUser(context: Context, user: GoogleUser) {
        val prefs = sharedPrefs ?: context.applicationContext.getSharedPreferences("disperindag_user_prefs", Context.MODE_PRIVATE)
        sharedPrefs = prefs
        prefs.edit()
            .putString("email", user.email)
            .putString("displayName", user.displayName)
            .apply()
        _currentUser.value = user
        _isLoggedIn.value = true
    }

    fun loginWithCustomEmail(context: Context, email: String, name: String) {
        val user = GoogleUser(
            email = email.trim(),
            displayName = if (name.isNotBlank()) name else email.substringBefore("@")
        )
        loginWithUser(context, user)
    }

    fun logout(context: Context) {
        val prefs = sharedPrefs ?: context.applicationContext.getSharedPreferences("disperindag_user_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _currentUser.value = null
        _isLoggedIn.value = false
    }

    // Compatibility support
    fun loginWithUser(user: GoogleUser) {
        _currentUser.value = user
        _isLoggedIn.value = true
    }

    fun loginWithCustomEmail(email: String, name: String) {
        val user = GoogleUser(
            email = email.trim(),
            displayName = if (name.isNotBlank()) name else email.substringBefore("@")
        )
        _currentUser.value = user
        _isLoggedIn.value = true
    }

    fun logout() {
        _currentUser.value = null
        _isLoggedIn.value = false
    }

    fun login() {
        if (_currentUser.value == null) {
            _currentUser.value = defaultUsers[0]
        }
        _isLoggedIn.value = true
    }
}
