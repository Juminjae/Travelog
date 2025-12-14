package com.example.travelog.data.network

import com.example.travelog.data.model.ArchivePhoto
import com.example.travelog.data.model.ArchivedTrip
import com.example.travelog.data.model.PhotoComment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore 구조(권장)
 * - trips (collection)
 *   - {tripId} (document)
 *     - fields: cityName, countryName, isCompleted, startDate, endDate ...
 *     - photos (sub-collection)
 *       - {photoId} (document)
 *         - fields: imageUrl, createdAt, order ... (+ 필요시 tripId/photoId)
 *         - comments (sub-collection)
 *           - {commentId} (document)
 *             - fields: authorName, text, createdAt ...
 */
class ArchiveRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val tripsCol = db.collection("trips")

    /** 지난 여행(완료된 여행) 목록 */
    suspend fun getArchivedTrips(): List<ArchivedTrip> {
        return tripsCol
            .whereEqualTo("isCompleted", true)
            .get()
            .await()
            .toObjects(ArchivedTrip::class.java)
    }

    /** 특정 여행(tripId)의 사진 목록 */
    suspend fun getPhotos(tripId: String): List<ArchivePhoto> {
        // 정렬 기준은 너희가 Firestore에 넣은 필드에 맞춰서 하나만 쓰면 돼.
        // (order가 없으면 createdAt으로 바꾸기)
        return tripsCol
            .document(tripId)
            .collection("photos")
            .orderBy("order")
            .get()
            .await()
            .toObjects(ArchivePhoto::class.java)
    }

    /** 댓글 실시간 구독(오버레이에서 사용) */
    fun observeComments(tripId: String, photoId: String): Flow<List<PhotoComment>> = callbackFlow {
        val reg: ListenerRegistration = tripsCol
            .document(tripId)
            .collection("photos")
            .document(photoId)
            .collection("comments")
            .orderBy("createdAt")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val list = snap
                    ?.toObjects(PhotoComment::class.java)
                    .orEmpty()
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    /** 댓글 추가 */
    suspend fun addComment(
        tripId: String,
        photoId: String,
        authorName: String,
        text: String,
    ) {
        val commentRef = tripsCol
            .document(tripId)
            .collection("photos")
            .document(photoId)
            .collection("comments")
            .document() // 자동 ID

        val comment = PhotoComment(
            commentId = commentRef.id,
            photoId = photoId,
            authorName = authorName,
            text = text,
            createdAt = System.currentTimeMillis(),
        )

        commentRef.set(comment).await()
    }
}

