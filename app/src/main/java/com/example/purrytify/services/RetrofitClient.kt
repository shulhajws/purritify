package com.example.purrytify.services

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://34.101.226.132:3000"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Optional: If you need to access the base URL for any reason
     fun getBaseUrl(): String {
         return BASE_URL
     }
}

// Contoh Manggil:
// val call = RetrofitClient.instance.login(LoginRequest(email, password))