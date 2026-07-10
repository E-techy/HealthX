package com.example.healthx.data.network

import com.example.healthx.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(400, TimeUnit.SECONDS)
        .writeTimeout(400, TimeUnit.SECONDS) // Important for large image uploads
        .readTimeout(400, TimeUnit.SECONDS)  // Important for waiting on Gemini AI
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Expose ALL APIs through the singleton
    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val remindersApi: RemindersApi by lazy { retrofit.create(RemindersApi::class.java) }
    val subscriptionApi: SubscriptionApi by lazy { retrofit.create(SubscriptionApi::class.java) }

    val homeApi: HomeApi by lazy { retrofit.create(HomeApi::class.java) }


    val settingsApi: SettingsApi by lazy { retrofit.create(SettingsApi::class.java) }
}