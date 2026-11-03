package com.bhati.moodify.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Replace with your actual Supabase Project URL and Public Anon Key
    private const val BASE_URL = "https://ongmmrreyznqtloekppb.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_mMl-D419rtcsgV-zxu_mPA_XLb2XSen"

    // Set by MainActivity to provide user auth token dynamically
    var tokenProvider: () -> String? = { null }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val supabaseInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("apikey", SUPABASE_ANON_KEY)
            
        tokenProvider()?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(supabaseInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: MoodifyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(if (BASE_URL.endsWith("/")) BASE_URL else "$BASE_URL/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MoodifyApiService::class.java)
    }

    // Direct iTunes Search client (bypasses Supabase headers)
    private val itunesClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val itunesApiService: ItunesApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(itunesClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ItunesApiService::class.java)
    }
}
