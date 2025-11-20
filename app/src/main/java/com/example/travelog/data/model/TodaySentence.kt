package com.example.travelog.data.model

enum class StudyLanguage {
    JAPANESE
}

data class TodaySentence(
    val foreign: String = "",
    val romanization: String = "",
    val translation: String = ""
)