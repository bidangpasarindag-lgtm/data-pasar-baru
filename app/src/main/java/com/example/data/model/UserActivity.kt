package com.example.data.model

data class UserActivity(
    val timestamp: String,
    val email: String,
    val namaPetugas: String,
    val aktivitas: String,
    val keterangan: String = ""
)
