package com.example.travelog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/*
1.	Entity: DB 테이블 모양(= 스키마)
2.	Dao: DB에 접근하는 함수들(조회/삽입/수정/삭제)
3.	Database: 어떤 테이블/DAO를 묶어서 “DB 인스턴스”를 만들어주는 관리자
 */

// 1. 도시(cityName)별 사진 목록을 Flow로 관찰
// 2. 사진 1장 저장
// 3. 사진(photoId)별 댓글 목록을 Flow로 관찰
// 4. 댓글 1개 저장

@Dao
interface ArchiveDao {
     //도시별 사진 목록을 조회
     //화면(Compose)에서 collectAsState()로 구독하면 DB 변경 시 자동 갱신
    @Query("SELECT * FROM archive_photos WHERE cityName = :cityName ORDER BY id ASC")
    fun observeByCity(cityName: String): Flow<List<ArchivePhotoEntity>>

     //사진 1장을 DB에 저장한다.
     //suspend: 백그라운드 실행 목적
    @Insert
    suspend fun insert(item: ArchivePhotoEntity)

     //특정 사진(photoId)에 달린 댓글 목록 조회(createdat으로 오름차순 정렬)
    @Query("SELECT * FROM archive_comment WHERE photoId = :photoId ORDER BY createdAt ASC")
    fun observeComments(photoId: Long): Flow<List<ArchiveCommentEntity>>

    //댓글 1개를 DB에 저장한다.
    @Insert
    suspend fun insertComment(item: ArchiveCommentEntity)
}