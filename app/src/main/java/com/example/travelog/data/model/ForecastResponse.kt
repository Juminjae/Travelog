package com.example.travelog.data.model

data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt: Long,
    val main: ForecastMain,
    val weather: List<ForecastWeather>
)

data class ForecastMain(
    val temp: Double,
    val temp_min: Double,
    val temp_max: Double
)

data class ForecastWeather(
    val icon: String?,
    val description: String?
)