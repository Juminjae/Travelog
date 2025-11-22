package com.example.travelog.data.network

import com.example.travelog.data.model.ForecastResponse
import com.example.travelog.data.model.GeoResponse
import com.example.travelog.data.model.OneCallResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    // ➤ 현재 날씨
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,         // 예: "Sapporo,jp"
        @Query("appid") apiKey: String,   // 발급받은 API Key
        @Query("units") units: String = "metric", // 섭씨(℃)
        @Query("lang") lang: String = "kr"       // 한국어
    ): WeatherResponse


    // ➤ 시간별 & 일별 예보 (OneCall API 사용)
//    @GET("data/3.0/onecall")
//    suspend fun getOneCallForecast(
//        @Query("lat") lat: Double,
//        @Query("lon") lon: Double,
//        @Query("exclude") exclude: String = "current,minutely,alerts",
//        @Query("appid") apiKey: String,
//        @Query("units") units: String = "metric",
//        @Query("lang") lang: String = "kr"
//    ): OneCallResponse
    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric", // 섭씨
        @Query("lang") lang: String = "kr"       // 한국어
    ): ForecastResponse

    @GET("geo/1.0/direct")
    suspend fun getGeoLocation(
        @Query("q") city: String,           // "Sapporo,jp"
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String
    ): List<GeoResponse>
}

