package com.example.travelog.data.model

data class HourlyWeatherUi(
    val label: String,
    val tempText: String,
    val iconCode: String?
)

data class DailyWeatherUi(
    val dayLabel: String,
    val minTempText: String,
    val maxTempText: String,
    val iconCode: String?
)