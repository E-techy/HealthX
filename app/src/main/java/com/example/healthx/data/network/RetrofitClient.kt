package com.example.healthx.data.network

import android.util.Log
import com.example.healthx.BuildConfig
import com.example.healthx.data.local.SessionManager
import com.example.healthx.shareable_data_manager.data.DelegatedAccessApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 1. Standard Logging Interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 2. Delegated Access Interceptor (WITH HEAVY LOGGING)
    private val delegatedAccessInterceptor = Interceptor { chain ->
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        // Fetch the target ID directly from the SessionManager's static companion object
        val targetId = SessionManager.currentDelegatedUserIdStatic()

        Log.d("NETWORK_INTERCEPTOR", "========================================")
        Log.d("NETWORK_INTERCEPTOR", "Intercepting Request: ${request.url}")

        if (targetId != null) {
            Log.w("NETWORK_INTERCEPTOR", "🚀 GUEST MODE ACTIVE! Injecting X-Target-User-Id: $targetId")
            requestBuilder.addHeader("X-Target-User-Id", targetId)
        } else {
            Log.d("NETWORK_INTERCEPTOR", "👤 Normal Mode Active. No Target ID injected.")
        }
        Log.d("NETWORK_INTERCEPTOR", "========================================")

        chain.proceed(requestBuilder.build())
    }

    // 3. Attach BOTH Interceptors to OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(delegatedAccessInterceptor) // <--- THIS WAS MISSING!
        .addInterceptor(loggingInterceptor)
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