package com.example.travelog.data.model

import com.google.firebase.firestore.DocumentId

//아카이브에서 쓰는 데이터 구조 정의 - data class
//@DocumentId는 파이어베이스 염두에 두고 선언했는데 파이어베이스 포기..

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
    val imageUrl: String = "",
    val createdAt: Long = 0L,
    val order: Int = 0,
)

//사진 오버레이 - 댓글
data class PhotoComment(
    @DocumentId val commentId: String = "",
    val photoId: String = "",
    val authorName: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
)


