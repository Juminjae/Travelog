package com.example.travelog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelog.data.WeatherRepository
import com.example.travelog.data.model.DailyWeatherUi
import com.example.travelog.data.model.HourlyWeatherUi
import com.example.travelog.data.network.RetrofitClient
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    // 🔹 API 요청에 쓸 도시 (예: "Sapporo,jp")
    var apiCityName by mutableStateOf("Sapporo,jp")
        private set

    // 🔹 화면에 보여 줄 도시 이름 (예: "삿포로")
    var displayCityName by mutableStateOf("삿포로")
        private set

    var temperature by mutableStateOf<String?>(null)
        private set

    var iconCode by mutableStateOf<String?>(null)
        private set

    var hourlyList by mutableStateOf<List<HourlyWeatherUi>>(emptyList())
        private set

    var dailyList by mutableStateOf<List<DailyWeatherUi>>(emptyList())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    /**
     * 도시 변경 + 날씨 로드
     *
     * @param apiCity   OpenWeather API에 보낼 도시 (예: "Sapporo,jp")
     * @param display   화면에 보여 줄 이름 (예: "삿포로")
     */
    fun load(apiCity: String = apiCityName, display: String = displayCityName) {
        apiCityName = apiCity
        displayCityName = display

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // 1) 현재 날씨
                val current = RetrofitClient.weatherApi.getCurrentWeather(
                    city = apiCityName,
                    apiKey = BuildConfig.WEATHER_API_KEY
                )

                temperature = "${current.main.temp.toInt()}°C"
                iconCode = current.weather.firstOrNull()?.icon

                // 2) 시간별 / 일별
                val (hourly, daily) = WeatherRepository.loadHourlyAndDaily(apiCityName)
                hourlyList = hourly
                dailyList = daily

            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.message ?: "날씨 정보를 불러오지 못했습니다."
            } finally {
                isLoading = false
            }
        }
    }
}