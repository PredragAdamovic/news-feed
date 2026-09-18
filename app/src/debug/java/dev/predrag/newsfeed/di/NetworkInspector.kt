package dev.predrag.newsfeed.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import okhttp3.Interceptor

/**
 * Chucker, which posts a notification per request and keeps a browsable log of them.
 * The release source set returns null instead, so nothing of it ships.
 */
fun networkInspector(context: Context): Interceptor =
    ChuckerInterceptor.Builder(context)
        .collector(
            ChuckerCollector(
                context = context,
                showNotification = true,
                retentionPeriod = RetentionManager.Period.ONE_HOUR,
            )
        )
        .alwaysReadResponseBody(true)
        .build()
