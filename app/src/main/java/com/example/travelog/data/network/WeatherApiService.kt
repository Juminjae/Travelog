package com.example.travelog.data.network

import com.example.travelog.data.model.GeoResponse
import com.example.travelog.data.model.OneCallResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    // 현재 날씨
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,           // 도시 이름 (예: "Seoul")
        @Query("appid") apiKey: String,     // OpenWeatherMap API 키
        @Query("units") units: String = "metric", // 섭씨
        @Query("lang") lang: String = "kr"        // 한국어 설명
    ): WeatherResponse

    // 도시 이름 → 위도/경도
    @GET("geo/1.0/direct")
    suspend fun getLocation(
        @Query("q") city: String,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String
    ): List<GeoResponse>

    // 시간별 + 일별 예보 (One Call 3.0)
    @GET("data/3.0/onecall")
    suspend fun getOneCallForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("exclude") exclude: String = "minutely,alerts",
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "kr",
        @Query("appid") apiKey: String
    ): OneCallResponse
}