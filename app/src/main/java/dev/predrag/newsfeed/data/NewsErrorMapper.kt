package dev.predrag.newsfeed.data

import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.predrag.newsfeed.core.NewsError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsErrorMapper @Inject constructor(private val json: Json) {

    fun map(throwable: Throwable): NewsError = classify(throwable).also { it.reportIfUnexpected(throwable) }

    private fun classify(throwable: Throwable): NewsError = when (throwable) {
        is UnknownHostException,
        is SocketTimeoutException,
        -> NewsError.NoConnection

        is HttpException -> NewsError.Server(throwable.code(), throwable.newsApiMessage())

        is SerializationException -> NewsError.Malformed

        // Dropped connection, TLS failure — all "the network did not work" to the user.
        is IOException -> NewsError.NoConnection

        else -> NewsError.Unexpected(throwable)
    }

    /**
     * Only the failures that mean a bug are reported. Being offline or getting a 429 is a
     * condition the app already handles; sending those would bury real problems in noise.
     */
    private fun NewsError.reportIfUnexpected(throwable: Throwable) {
        if (this is NewsError.Unexpected || this is NewsError.Malformed) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }

    /** NewsAPI explains refusals in the body, which beats showing a bare status code. */
    private fun HttpException.newsApiMessage(): String? = runCatching {
        val body = response()?.errorBody()?.string().orEmpty()
        json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content
    }.getOrNull()
}
