package com.example.healthx.data.network

import com.example.healthx.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * A Singleton object that manages the Retrofit instance and HTTP client for the entire app.
 */
object RetrofitClient {

    // Configure the OkHttpClient with basic timeouts
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Lazily initialize Retrofit so it's only created when first needed
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL) // Pulls the URL from your local.properties/gradle setup
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Expose the Subscription API
    val subscriptionApi: SubscriptionApi by lazy {
        retrofit.create(SubscriptionApi::class.java)
    }

    // Note: As you build out Auth or Profile features, you can expose those APIs here too.
    // example: val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
}