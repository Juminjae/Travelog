package com.example.travelog

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel

// ✅ 여행별 예산/지출 상태
data class TripBudgetState(
    val totalBudgetText: String = "",
    val expenses: SnapshotStateList<Expense> = mutableStateListOf()
)

class TripsViewModel : ViewModel() {

    // ------------------------------
    // Trip 리스트
    // ------------------------------
    private val _trips = mutableStateListOf<Trip>()
    val trips: List<Trip> get() = _trips

    // ------------------------------
    // ✅ tripId 별 Budget 저장소
    // ------------------------------
    private val budgets = mutableStateMapOf<String, TripBudgetState>()

    fun budgetState(tripId: String): TripBudgetState =
        budgets.getOrPut(tripId) { TripBudgetState() }

    fun setTotalBudgetText(tripId: String, text: String) {
        val st = budgetState(tripId)
        budgets[tripId] = st.copy(totalBudgetText = text)
    }

    fun addExpense(tripId: String, e: Expense) {
        budgetState(tripId).expenses.add(e)
    }

    // ------------------------------
    // Trip CRUD
    // ------------------------------
    fun addTrip(country: String, dateMillis: Long): String {
        val id = System.currentTimeMillis().toString()
        _trips.add(
            Trip(
                id = id,
                countryEmoji = emojiForCountry(country),
                country = country.trim(),
                targetDateMillis = dateMillis,
                members = emptyList()
            )
        )
        // ✅ 여행 생성 시 예산 상태도 만들어두기
        budgetState(id)
        return id
    }

    fun updateDate(tripId: String, newMillis: Long) {
        val idx = _trips.indexOfFirst { it.id == tripId }
        if (idx >= 0) _trips[idx] = _trips[idx].copy(targetDateMillis = newMillis)
    }

    fun addMember(tripId: String, name: String) {
        val idx = _trips.indexOfFirst { it.id == tripId }
        if (idx >= 0) _trips[idx] = _trips[idx].copy(members = _trips[idx].members + name.trim())
    }

    fun findTrip(tripId: String?): Trip? = _trips.firstOrNull { it.id == tripId }
}
