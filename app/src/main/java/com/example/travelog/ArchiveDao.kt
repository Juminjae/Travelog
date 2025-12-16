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

    @Insert
    suspend fun insertAll(items: List<ArchivePhotoEntity>)

    @Query("SELECT COUNT(*) FROM archive_photos WHERE cityName = :cityName")
    suspend fun countByCity(cityName: String): Int

    @Query("SELECT * FROM archive_comment WHERE photoId = :photoID ORDER BY createdAt ASC")
    fun observeComments(photoID: Long): Flow<List<ArchiveCommentEntity>>

    @Insert
    suspend fun insertComment(item: ArchiveCommentEntity)
}