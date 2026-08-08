package com.example.data.local

import androidx.room.*
import com.example.data.model.DropdownOption
import kotlinx.coroutines.flow.Flow

@Dao
interface DropdownDao {
    @Query("SELECT * FROM dropdown_options WHERE category = :category ORDER BY optionValue ASC")
    fun getAllOptionsByCategory(category: String): Flow<List<DropdownOption>>

    @Query("SELECT * FROM dropdown_options WHERE category = :category AND isVisible = 1 ORDER BY optionValue ASC")
    fun getVisibleOptionsByCategory(category: String): Flow<List<DropdownOption>>

    @Update
    suspend fun updateOption(option: DropdownOption)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOption(option: DropdownOption): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(options: List<DropdownOption>)

    @Query("DELETE FROM dropdown_options WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM dropdown_options WHERE category = :category AND optionValue = :value")
    suspend fun deleteOption(category: String, value: String)

    @Query("DELETE FROM dropdown_options")
    suspend fun clearAll()
}
