package com.example.travelog.data.model

data class HourlyWeatherUi(
    val label: String,      // "지금", "12시" ...
    val tempText: String,    // "14°C"
    val iconCode: String?
)

data class DailyWeatherUi(
    val dayLabel: String,   // "오늘", "화" ...
    val minTempText: String,
    val maxTempText: String,
    val iconCode: String?
)