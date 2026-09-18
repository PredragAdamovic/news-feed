package dev.predrag.newsfeed.ui.navigation

object ArticleListRoute {
    const val PATH = "articles"
}

object ArticleDetailRoute {
    const val ARG_ARTICLE_ID = "articleId"

    const val PATH = "article/{$ARG_ARTICLE_ID}"
    const val DEEP_LINK = "myapp://article/{$ARG_ARTICLE_ID}"

    /**
     * The shareable form. Messengers do not linkify a custom scheme, and since Android 12 an
     * unverified http(s) link opens the browser rather than the app — so sharing needs a
     * verified App Link.
     */
    private const val WEB_HOST = "predragadamovic.github.io"
    private const val WEB_PREFIX = "/news-feed/article"

    const val WEB_LINK = "https://$WEB_HOST$WEB_PREFIX/{$ARG_ARTICLE_ID}"

    fun pathFor(articleId: String): String = "article/$articleId"

    fun webUrlFor(articleId: String): String = "https://$WEB_HOST$WEB_PREFIX/$articleId"
}
