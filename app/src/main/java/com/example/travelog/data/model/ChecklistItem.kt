package com.example.travelog.data.model

data class ChecklistItem(
    val id: Int,
    val label: String,
    val isChecked: Boolean = false
)