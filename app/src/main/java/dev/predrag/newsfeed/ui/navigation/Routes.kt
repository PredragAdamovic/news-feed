package dev.predrag.newsfeed.ui.navigation

object ArticleListRoute {
    const val PATH = "articles"
}

object ArticleDetailRoute {
    const val ARG_ARTICLE_ID = "articleId"

    const val PATH = "article/{$ARG_ARTICLE_ID}"
    const val DEEP_LINK = "myapp://article/{$ARG_ARTICLE_ID}"

    fun pathFor(articleId: String): String = "article/$articleId"
}
