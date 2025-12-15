package com.example.travelog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelog.data.model.ChecklistItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChecklistViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).checklistDao()

    // DB(Entity) -> UI모델(ChecklistItem)로 변환해서 노출
    val items: StateFlow<List<ChecklistItem>> =
        dao.observeAll()
            .map { list -> list.map { it.toUi() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // 앱 첫 실행 시 기본 항목 넣기 (이미 있으면 넣지 않음)
        viewModelScope.launch {
            if (dao.count() == 0) {
                dao.insertAll(
                    listOf(
                        ChecklistEntity(label = "여권"),
                        ChecklistEntity(label = "비행기 티켓 / e-티켓"),
                        ChecklistEntity(label = "지갑 (카드, 현금)"),
                        ChecklistEntity(label = "충전기"),
                        ChecklistEntity(label = "멀티탭"),
                        ChecklistEntity(label = "보조배터리")
                    )
                )
            }
        }
    }

    fun toggleChecked(id: Int, checked: Boolean) {
        viewModelScope.launch { dao.setChecked(id, checked) }
    }

    fun addItem(label: String) {
        val t = label.trim()
        if (t.isEmpty()) return
        viewModelScope.launch { dao.insert(ChecklistEntity(label = t)) }
    }

    fun removeItem(id: Int) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}

// Entity -> UI Model 변환
private fun ChecklistEntity.toUi(): ChecklistItem =
    ChecklistItem(id = id, label = label, isChecked = isChecked)