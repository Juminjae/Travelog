package com.example.travelog.data

import com.example.travelog.data.model.*
import com.example.travelog.data.network.RetrofitClient
import java.text.SimpleDateFormat
import java.util.*
import com.example.travelog.BuildConfig


object WeatherRepository {

    val API_KEY = BuildConfig.WEATHER_API_KEY

    // 도시 이름 → (lat, lon) 변환
    suspend fun getLocation(city: String): Pair<Double, Double>? {
        val response = RetrofitClient.weatherApi.getGeoLocation(
            city = city,          // 예: "Sapporo,jp"
            apiKey = API_KEY
        )

        println("🔎 getLocation() response = $response")

        return if (response.isNotEmpty()) {
            val data = response[0]
            val result = data.lat to data.lon
            result   // (lat, lon)
        } else {
            null
        }
    }

    // 시간별 일별 UI 데이터 로드
    suspend fun loadHourlyAndDaily(city: String): Pair<List<HourlyWeatherUi>, List<DailyWeatherUi>> {

        val forecast = RetrofitClient.weatherApi.getForecast(city, API_KEY)

        // 3시간 간격 리스트 → 앞 10개를 시간별 카드용
        val hourly = forecast.list.take(10).mapIndexed { index, item ->
            val hour = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
                timeInMillis = item.dt * 1000L
            }.get(Calendar.HOUR_OF_DAY)

            HourlyWeatherUi(
                label = if (index == 0) "지금" else "${hour}시",
                tempText = "${item.main.temp.toInt()}°C",
                iconCode = item.weather.firstOrNull()?.icon
            )
        }

        // 일별은 날짜 기준으로 groupBy
        val grouped = forecast.list.groupBy { item ->
            java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(item.dt * 1000L))
        }

        val daily = grouped.entries.take(6).mapIndexed { index, entry ->
            val sdf = java.text.SimpleDateFormat("E", java.util.Locale.KOREA)
            val dayLabel = if (index == 0) "오늘" else
                sdf.format(java.util.Date(entry.value[0].dt * 1000L)).first().toString()

            val temps = entry.value.map { it.main.temp }

            DailyWeatherUi(
                dayLabel = dayLabel,
                minTempText = "${temps.minOrNull()?.toInt()}°C",
                maxTempText = "${temps.maxOrNull()?.toInt()}°C",
                iconCode = entry.value.firstOrNull()?.weather?.firstOrNull()?.icon
            )
        }

        return hourly to daily
    }
}