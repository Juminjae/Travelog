package com.example.travelog

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
1.	Entity: DB 테이블 모양(= 스키마)
2.	Dao: DB에 접근하는 함수들(조회/삽입/수정/삭제)
3.	Database: 어떤 테이블/DAO를 묶어서 “DB 인스턴스”를 만들어주는 관리자
 */
//사진 entity
//아카이브 화면에 보여줄 사진을 DB에 저장하기 위한 테이블
@Entity(tableName = "archive_photos")
data class ArchivePhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, //사진 ID(자동 증가)
    val cityName: String,
    val tripId: String? = null,
    val internalPath: String,//에뮬 내부 파일 경로
    val createdAt: Long = System.currentTimeMillis(),//정렬용
)

//댓글 entity
//오버레이에서 쓸 댓글 저장하기 위한 테이블
@Entity(tableName = "archive_comment")
data class ArchiveCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val photoId: Long,
    val authorName: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)