package com.example.travelog



import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "archive_photos")
data class ArchivePhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cityName: String,
    val tripId: String? = null,

    // 더미/로컬용(지금 단계)
    // sourceType: "localRes" or "uri" 같은 식으로 구분해도 됨
    val sourceType: String = "localRes",
    val localResName: String? = null, // 예: "vin_1"
    val uriString: String? = null,    // 나중에 갤러리/스토리지 연결 시

    val createdAt: Long = System.currentTimeMillis(),
    val order: Int = 0
)