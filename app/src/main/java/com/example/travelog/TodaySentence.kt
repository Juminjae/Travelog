package com.example.travelog

enum class StudyLanguage {
    JAPANESE
}

data class TodaySentence(
    val foreign: String = "",
    val romanization: String = "",
    val translation: String = ""
)