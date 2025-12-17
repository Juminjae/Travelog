package com.example.travelog

import androidx.room.Entity
import androidx.room.PrimaryKey

//사진 entity
@Entity(tableName = "archive_photos")
data class ArchivePhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cityName: String,
    val tripId: String? = null,
    val internalPath: String,
    val createdAt: Long = System.currentTimeMillis(),
)

//댓글 entity
@Entity(tableName = "archive_comment")
data class ArchiveCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val photoId: Long,
    val authorName: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)