package dev.predrag.newsfeed.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/** Sends the key as a header so it stays out of logged URLs. */
class ApiKeyInterceptor(private val apiKey: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .addHeader("X-Api-Key", apiKey)
                .build()
        )
}
