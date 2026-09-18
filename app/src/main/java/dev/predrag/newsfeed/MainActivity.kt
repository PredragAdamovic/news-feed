package dev.predrag.newsfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.predrag.newsfeed.ui.NewsFeedApp
import dev.predrag.newsfeed.ui.theme.NewsFeedTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NewsFeedTheme {
                NewsFeedApp()
            }
        }
    }
}
