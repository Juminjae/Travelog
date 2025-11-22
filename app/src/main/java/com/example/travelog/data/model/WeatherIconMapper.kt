package com.example.travelog.data.model

import com.example.travelog.R

/**
 * OpenWeather icon 코드(예: "01d", "04n")를
 * 우리 앱의 drawable 리소스 ID로 변환해 주는 함수
 */
fun mapWeatherIcon(code: String?): Int? {
    return when (code) {
        "01d", "01n" -> R.drawable.ic_weather_clear
        "02d", "02n" -> R.drawable.ic_weather_few_clouds
        "03d", "03n" -> R.drawable.ic_weather_clouds
        "04d", "04n" -> R.drawable.ic_weather_clouds
        "09d", "09n" -> R.drawable.ic_weather_rain
        "10d", "10n" -> R.drawable.ic_weather_rain
        "11d", "11n" -> R.drawable.ic_weather_rain
        "13d", "13n" -> R.drawable.ic_weather_snow
        else -> R.drawable.ic_weather_clear   // 👉 최소한 기본값 하나 넣어두면 더 안전
    }
}