package dev.predrag.newsfeed.core

sealed interface NewsError {

    /** No NEWS_API_KEY in local.properties — a setup problem, not a runtime failure. */
    data object MissingApiKey : NewsError

    /** Device is offline, DNS failed, or the request timed out. Retrying may help. */
    data object NoConnection : NewsError

    /** The API answered, but with an error. [apiMessage] is NewsAPI's own explanation. */
    data class Server(val code: Int, val apiMessage: String?) : NewsError

    /** A 200 whose body did not match the contract. Retrying the same call will not help. */
    data object Malformed : NewsError

    data class Unexpected(val cause: Throwable) : NewsError
}

sealed interface NewsResult<out T> {
    data class Success<T>(val data: T) : NewsResult<T>
    data class Failure(val error: NewsError) : NewsResult<Nothing>
}
