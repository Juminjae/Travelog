package com.example.travelog.data.network

data class WeatherResponse(
    val weather: List<WeatherInfo>,
    val main: MainInfo,
    val name: String // 도시 이름
)

data class WeatherInfo(
    val main: String,        // 예: "Clouds"
    val description: String  // 예: "온흐린 하늘"
)

data class MainInfo(
    val temp: Float,         // 현재 기온
    val feels_like: Float    // 체감 온도
)