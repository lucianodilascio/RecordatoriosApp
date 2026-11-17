package com.example.recordatoriosapp.data.remote.api

import com.example.recordatoriosapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object para proveer la instancia de Retrofit y el ApiService.
 * En un entorno con inyección de dependencias (ej. Hilt/Dagger), esto se reemplazaría por un @Module.
 */
object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // En un proyecto real de producción, podríamos habilitar logs solo si BuildConfig.DEBUG == true
        level = HttpLoggingInterceptor.Level.BODY 
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS) // Timeouts razonables
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiService: ReminderApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(ReminderApiService::class.java)
    }
}
