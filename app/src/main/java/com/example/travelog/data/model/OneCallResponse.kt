package com.example.travelog.data.model

data class OneCallResponse(
    val lat: Double,
    val lon: Double,
    val timezone: String,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>
)

data class HourlyWeather(
    val dt: Long,
    val temp: Double,
    val weather: List<WeatherDesc>
)

data class DailyWeather(
    val dt: Long,
    val temp: DailyTemp,
    val weather: List<WeatherDesc>
)

data class DailyTemp(
    val min: Double,
    val max: Double
)

data class WeatherDesc(
    val main: String,
    val description: String,
    val icon: String
)