package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dropdown_options")
data class DropdownOption(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "JENIS_RUANG", "KOMODITI", "STATUS"
    val optionValue: String
)
