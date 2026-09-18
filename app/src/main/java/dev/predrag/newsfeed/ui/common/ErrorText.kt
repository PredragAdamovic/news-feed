package dev.predrag.newsfeed.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.predrag.newsfeed.R
import dev.predrag.newsfeed.core.NewsError

data class ErrorText(val title: String, val body: String)

@Composable
fun NewsError.toErrorText(): ErrorText = when (this) {
    NewsError.MissingApiKey -> text(R.string.error_missing_key_title, R.string.error_missing_key_body)
    NewsError.NoConnection -> text(R.string.error_offline_title, R.string.error_offline_body)
    NewsError.Malformed -> text(R.string.error_malformed_title, R.string.error_malformed_body)
    is NewsError.Server -> ErrorText(
        title = stringResource(R.string.error_server_title),
        body = apiMessage ?: stringResource(R.string.error_server_body, code),
    )
    is NewsError.Unexpected -> text(R.string.error_unexpected_title, R.string.error_unexpected_body)
}

@Composable
private fun text(@StringRes title: Int, @StringRes body: Int) =
    ErrorText(stringResource(title), stringResource(body))
