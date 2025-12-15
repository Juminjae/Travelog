package com.example.travelog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist ORDER BY id ASC")
    fun observeAll(): Flow<List<ChecklistEntity>>

    @Insert
    suspend fun insert(item: ChecklistEntity)

    @Query("UPDATE checklist SET isChecked = :checked WHERE id = :id")
    suspend fun setChecked(id: Int, checked: Boolean)

    @Query("DELETE FROM checklist WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM checklist")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(items: List<ChecklistEntity>)
}