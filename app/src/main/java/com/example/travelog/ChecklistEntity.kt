package com.example.travelog

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist")
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val isChecked: Boolean = false
)