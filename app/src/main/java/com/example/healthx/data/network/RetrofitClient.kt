package com.example.healthx.data.network

import com.example.healthx.BuildConfig
import com.example.healthx.shareable_data_manager.data.DelegatedAccessApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 1. Create the Interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Level.BODY logs headers, URLs, and the full JSON payloads for both requests and responses.
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 2. Attach it to your OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // <-- Attached here
        .connectTimeout(400, TimeUnit.SECONDS)
        .writeTimeout(400, TimeUnit.SECONDS)
        .readTimeout(400, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Your exposed APIs
    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val remindersApi: RemindersApi by lazy { retrofit.create(RemindersApi::class.java) }
    val subscriptionApi: SubscriptionApi by lazy { retrofit.create(SubscriptionApi::class.java) }
    val homeApi: HomeApi by lazy { retrofit.create(HomeApi::class.java) }
    val settingsApi: SettingsApi by lazy { retrofit.create(SettingsApi::class.java) }
    val nutritionApi: NutritionApi by lazy { retrofit.create(NutritionApi::class.java) }

    val delegatedAccessApi: DelegatedAccessApi by lazy { retrofit.create(DelegatedAccessApi::class.java) }
}