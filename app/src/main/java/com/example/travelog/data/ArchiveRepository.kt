package com.example.travelog.data

import com.example.travelog.data.model.ArchivePhoto
import com.example.travelog.data.model.ArchivedTrip
import com.example.travelog.data.model.PhotoComment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

//파이어베이스 구조
class ArchiveRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val tripsCol = db.collection("trips")

    //지난 여행 목록(완료)
    suspend fun getArchivedTrips(): List<ArchivedTrip> {
        return tripsCol
            .whereEqualTo("isCompleted", true)
            .get()
            .await()
            .toObjects(ArchivedTrip::class.java)
    }

    //특정 여행
    suspend fun getPhotos(tripId: String): List<ArchivePhoto> {
        return tripsCol
            .document(tripId)
            .collection("photos")
            .orderBy("order")
            .get()
            .await()
            .toObjects(ArchivePhoto::class.java)
    }

    //댓글
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

    //댓글 추가
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
            .document()

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