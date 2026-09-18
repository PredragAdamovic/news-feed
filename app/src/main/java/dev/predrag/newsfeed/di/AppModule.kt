package dev.predrag.newsfeed.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.predrag.newsfeed.BuildConfig
import dev.predrag.newsfeed.data.NewsRepositoryImpl
import dev.predrag.newsfeed.data.local.ArticleCache
import dev.predrag.newsfeed.data.remote.ApiKeyInterceptor
import dev.predrag.newsfeed.data.remote.NewsApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @Named(NewsRepositoryImpl.API_KEY)
    fun provideApiKey(): String = BuildConfig.NEWS_API_KEY

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // The endpoint sends more fields than the DTOs declare, and adds new ones over time.
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        @Named(NewsRepositoryImpl.API_KEY) apiKey: String,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor(apiKey))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                )
            }
            networkInspector(context)?.let(::addInterceptor)
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.NEWS_API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideNewsApi(retrofit: Retrofit): NewsApi = retrofit.create(NewsApi::class.java)

    @Provides
    @Singleton
    fun provideArticleCache(
        @ApplicationContext context: Context,
        json: Json,
    ): ArticleCache = ArticleCache(context, json)
}
