package com.example.data.local

import androidx.room.*
import com.example.data.model.Pedagang
import kotlinx.coroutines.flow.Flow

@Dao
interface PedagangDao {
    @Query("SELECT * FROM pedagang ORDER BY id DESC")
    fun getAllPedagang(): Flow<List<Pedagang>>

    @Query("SELECT * FROM pedagang WHERE id = :id")
    suspend fun getPedagangById(id: Long): Pedagang?

    @Query("SELECT * FROM pedagang WHERE namaPedagang LIKE '%' || :query || '%' OR nik LIKE '%' || :query || '%' OR nomorKiosLos LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchPedagang(query: String): Flow<List<Pedagang>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPedagang(pedagang: Pedagang): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pedagangList: List<Pedagang>)

    @Update
    suspend fun updatePedagang(pedagang: Pedagang)

    @Delete
    suspend fun deletePedagang(pedagang: Pedagang)

    @Query("DELETE FROM pedagang WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pedagang")
    suspend fun clearAll()
}
