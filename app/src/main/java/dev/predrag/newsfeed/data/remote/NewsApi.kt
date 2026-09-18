package dev.predrag.newsfeed.data.remote

import dev.predrag.newsfeed.data.remote.dto.TopHeadlinesResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {

    @GET("top-headlines")
    suspend fun topHeadlines(
        @Query("country") country: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): TopHeadlinesResponseDto
}
