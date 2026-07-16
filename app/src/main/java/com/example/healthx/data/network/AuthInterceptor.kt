package com.example.healthx.data.network

import com.example.healthx.data.local.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // 1. Add standard JWT Auth
        // Note: runBlocking is acceptable here because OkHttp interceptors run on background threads
        val token = runBlocking { sessionManager.activeAccountFlow.firstOrNull()?.token }
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // ADD THIS: Inject Delegated ID into headers automatically
        val targetId = sessionManager.currentDelegatedUserId()
        if (targetId != null) {
            requestBuilder.addHeader("X-Target-User-Id", targetId)
        }

        return chain.proceed(requestBuilder.build())
    }
}