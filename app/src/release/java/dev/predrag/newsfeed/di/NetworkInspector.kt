package dev.predrag.newsfeed.di

import android.content.Context
import okhttp3.Interceptor

/** No inspector in release — see the debug source set. */
fun networkInspector(context: Context): Interceptor? = null
