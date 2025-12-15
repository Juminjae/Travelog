package com.example.travelog

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.example.travelog.data.model.ChecklistItem

class ChecklistViewModel : ViewModel() {

    private val _items = mutableStateListOf<ChecklistItem>()
    val items: SnapshotStateList<ChecklistItem> get() = _items

    init {
        // 기본 항목들
        _items.addAll(
            listOf(
                ChecklistItem(1, "여권"),
                ChecklistItem(2, "비행기 티켓 / e-티켓"),
                ChecklistItem(3, "지갑 (카드, 현금)"),
                ChecklistItem(4, "충전기"),
                ChecklistItem(5, "멀티탭"),
                ChecklistItem(6, "보조배터리")
            )
        )
    }

    fun toggleChecked(id: Int, checked: Boolean) {
        val index = _items.indexOfFirst { it.id == id }
        if (index != -1) {
            _items[index] = _items[index].copy(isChecked = checked)
        }
    }

    fun addItem(label: String) {
        if (label.isBlank()) return
        val newId = (_items.maxOfOrNull { it.id } ?: 0) + 1
        _items.add(ChecklistItem(newId, label.trim()))
    }

    fun removeItem(id: Int) {
        _items.removeAll { it.id == id }
    }
}