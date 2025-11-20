package com.example.travelog.data

import com.example.travelog.data.model.*
import com.example.travelog.data.network.RetrofitClient
import java.text.SimpleDateFormat
import java.util.*

object WeatherRepository {

    private const val API_KEY = "450a019012f76c52e4d95ec2531fa8c7"

    suspend fun loadHourlyAndDaily(cityName: String): Pair<List<HourlyWeatherUi>, List<DailyWeatherUi>> {
        // 1) 도시 → 위도/경도
        val geoList = RetrofitClient.weatherApi.getLocation(
            city = cityName,
            apiKey = API_KEY
        )
        val geo = geoList.firstOrNull()
            ?: return emptyList<HourlyWeatherUi>() to emptyList()

        // 2) OneCall 예보 요청
        val oneCall = RetrofitClient.weatherApi.getOneCallForecast(
            lat = geo.lat,
            lon = geo.lon,
            apiKey = API_KEY
        )

        // 3) 시간별: 앞의 7개만 카드용으로 사용
        val hourlyUi = oneCall.hourly.take(7).mapIndexed { index, h ->
            val label = if (index == 0) {
                "지금"
            } else {
                val hour = Calendar.getInstance().apply {
                    timeInMillis = h.dt * 1000L
                }.get(Calendar.HOUR_OF_DAY)
                "${hour}시"
            }
            HourlyWeatherUi(
                label = label,
                tempText = "${h.temp.toInt()}°C"
            )
        }

        // 4) 일별: 오늘 포함 6개
        val sdf = SimpleDateFormat("E", Locale.KOREAN) // 요일
        val dailyUi = oneCall.daily.take(6).mapIndexed { index, d ->
            val label = if (index == 0) {
                "오늘"
            } else {
                val day = Date(d.dt * 1000L)
                // "월", "화" 처럼 앞 글자만
                sdf.format(day).first().toString()
            }
            DailyWeatherUi(
                dayLabel = label,
                minTempText = "${d.temp.min.toInt()}°C",
                maxTempText = "${d.temp.max.toInt()}°C"
            )
        }

        return hourlyUi to dailyUi
    }
}