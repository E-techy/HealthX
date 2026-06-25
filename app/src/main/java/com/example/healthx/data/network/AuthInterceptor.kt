package com.example.healthx.data.network

import android.content.Context
import com.example.healthx.data.local.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // Ensure every request sends JSON
        requestBuilder.addHeader("Content-Type", "application/json")

        // Synchronously fetch the current active token from DataStore
        val activeAccount = runBlocking {
            SessionManager(context).activeAccountFlow.first()
        }

        if (activeAccount?.token != null) {
            requestBuilder.addHeader("Authorization", "Bearer ${activeAccount.token}")
        }

        return chain.proceed(requestBuilder.build())
    }
}