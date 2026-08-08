package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dropdown_options",
    indices = [Index(value = ["category", "optionValue"], unique = true)]
)
data class DropdownOption(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "JENIS_RUANG", "KOMODITI", "STATUS"
    val optionValue: String,
    val isVisible: Boolean = true
)
