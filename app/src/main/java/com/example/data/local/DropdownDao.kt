package com.example.data.local

import androidx.room.*
import com.example.data.model.DropdownOption
import kotlinx.coroutines.flow.Flow

@Dao
interface DropdownDao {
    @Query("SELECT * FROM dropdown_options WHERE category = :category ORDER BY optionValue ASC")
    fun getOptionsByCategory(category: String): Flow<List<DropdownOption>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOption(option: DropdownOption): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(options: List<DropdownOption>)

    @Query("DELETE FROM dropdown_options WHERE category = :category AND optionValue = :value")
    suspend fun deleteOption(category: String, value: String)

    @Query("DELETE FROM dropdown_options")
    suspend fun clearAll()
}
