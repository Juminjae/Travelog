package com.example.travelog.data.model

import com.google.firebase.firestore.DocumentId

//지난 여행 - 도시
data class ArchivedTrip(
    @DocumentId val tripId: String = "",
    val cityName: String = "",
    val countryName: String = "",
    val isCompleted: Boolean = true,
    val startDate: String = "",
    val endDate: String = "",
)

//아카이브 사진
data class ArchivePhoto(
    @DocumentId val photoId: String = "",
    val tripId: String = "",
    val imageUrl: String = "",  // Firebase Storage 다운로드 URL
    val createdAt: Long = 0L,     // millis (간단히 Long) / Firestore Timestamp로 바꿔도 됨
    val order: Int = 0,           // 필요하면 정렬용
)

//사진 오버레이 - 댓글
data class PhotoComment(
    @DocumentId val commentId: String = "",
    val photoId: String = "",
    val authorName: String = "",
    val text: String = "",
    val createdAt: Long = 0L, // millis
)


data class ArchiveAlbum(
    val trip: ArchivedTrip = ArchivedTrip(),
    val photos: List<ArchivePhoto> = emptyList(),
)
