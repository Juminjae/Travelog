package com.example.travelog.data

import com.example.travelog.data.model.StudyLanguage
import com.example.travelog.data.model.TodaySentence
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun loadSentencesFromFirestore(
    language: StudyLanguage
): List<TodaySentence> {

    val db = FirebaseFirestore.getInstance()

    // 언어에 따라 Firestore 컬렉션 이름 선택
    val collectionName = when (language) {
        StudyLanguage.JAPANESE -> "sentences_japanese"
    }

    val snapshot = db.collection(collectionName).get().await()

    return snapshot.documents.mapNotNull { doc ->
        doc.toObject(TodaySentence::class.java)
    }
}