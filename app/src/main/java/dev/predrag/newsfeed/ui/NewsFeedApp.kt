package dev.predrag.newsfeed.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.util.Consumer
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import dev.predrag.newsfeed.ui.detail.ArticleDetailScreen
import dev.predrag.newsfeed.ui.list.ArticleListScreen
import dev.predrag.newsfeed.ui.navigation.ArticleDetailRoute
import dev.predrag.newsfeed.ui.navigation.ArticleListRoute

@Composable
fun NewsFeedApp() {
    val navController = rememberNavController()

    navController.HandleDeepLinksWhileRunning()

    NavHost(navController = navController, startDestination = ArticleListRoute.PATH) {

        composable(ArticleListRoute.PATH) {
            ArticleListScreen(
                onArticleClick = { articleId ->
                    navController.navigate(ArticleDetailRoute.pathFor(articleId))
                },
            )
        }

        composable(
            route = ArticleDetailRoute.PATH,
            arguments = listOf(navArgument(ArticleDetailRoute.ARG_ARTICLE_ID) {
                type = NavType.StringType
            }),
            // Declared on the destination so a link opened from outside still gets a back
            // stack that lands on the list.
            deepLinks = listOf(
                navDeepLink { uriPattern = ArticleDetailRoute.DEEP_LINK },
                navDeepLink { uriPattern = ArticleDetailRoute.WEB_LINK },
            ),
        ) {
            ArticleDetailScreen(onBack = { navController.navigateUp() })
        }
    }
}

/**
 * Routes a link that arrives while the app is already running. The NavHost reads only the
 * intent it was created with, so a later one has to reach the NavController by hand.
 *
 * singleTop would not do: a link arrives with FLAG_ACTIVITY_NEW_TASK and an intent that does
 * not match the task's root, and the activity is rebuilt rather than reused.
 */
@Composable
private fun NavController.HandleDeepLinksWhileRunning() {
    val activity = LocalActivity.current as? ComponentActivity ?: return

    DisposableEffect(this, activity) {
        val listener = Consumer<Intent> { intent -> handleDeepLink(intent) }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
}
