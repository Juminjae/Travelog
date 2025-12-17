package com.example.travelog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveDao {

    @Query("SELECT * FROM archive_photos WHERE cityName = :cityName ORDER BY id ASC")
    fun observeByCity(cityName: String): Flow<List<ArchivePhotoEntity>>

    @Insert
    suspend fun insert(item: ArchivePhotoEntity)

    @Query("SELECT * FROM archive_comment WHERE photoId = :photoId ORDER BY createdAt ASC")
    fun observeComments(photoId: Long): Flow<List<ArchiveCommentEntity>>

    @Insert
    suspend fun insertComment(item: ArchiveCommentEntity)
}