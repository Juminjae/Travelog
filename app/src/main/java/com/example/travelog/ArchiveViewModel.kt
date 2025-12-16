package com.example.travelog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchiveViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).archiveDao()

    private val selectedCityFlow = MutableStateFlow("빈")
    val selectedCity: StateFlow<String> =
        selectedCityFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "빈")

    fun setSelectedCity(city: String) {
        selectedCityFlow.value = city
    }

    fun addUriPhoto(uriString: String) {
        val city = selectedCity.value
        viewModelScope.launch {
            dao.insert(
                ArchivePhotoEntity(
                    cityName = city,
                    sourceType = "uri",
                    uriString = uriString,
                    localResName = null,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    val photos: StateFlow<List<ArchivePhotoEntity>> =
        selectedCityFlow
            .flatMapLatest { city -> dao.observeByCity(city) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addDummyPhoto(city: String, localResName: String) {
        viewModelScope.launch {
            dao.insert(
                ArchivePhotoEntity(
                    cityName = city,
                    sourceType = "localRes",
                    localResName = localResName
                )
            )
        }
    }
}