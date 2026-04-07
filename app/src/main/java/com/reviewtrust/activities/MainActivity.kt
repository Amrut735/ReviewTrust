package com.reviewtrust.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reviewtrust.ui.HomeScreen
import com.reviewtrust.ui.ReviewViewModel
import com.reviewtrust.ui.Screen
import com.reviewtrust.ui.theme.ReviewTrustTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ReviewTrustAI"

        /**
         * Regex that matches HTTP/HTTPS URLs commonly shared from Amazon or Flipkart.
         * Also matches generic URLs so the app can still display them.
         */
        private val URL_REGEX = Regex(
            """https?://[\w\-._~:/?#\[\]@!$&'()*+,;=%]+""",
            RegexOption.IGNORE_CASE
        )
    }

    private lateinit var viewModel: ReviewViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[ReviewViewModel::class.java]

        // Handle share intent on cold start
        handleShareIntent(intent)?.let { url ->
            viewModel.onUrlReceived(url)
        }

        setContent {
            ReviewTrustTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReviewTrustNavHost(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)?.let { url ->
            Log.d(TAG, "onNewIntent: received URL -> $url")
            viewModel.onUrlReceived(url)
        }
    }

    /**
     * Extracts a product URL from a SEND intent.
     *
     * Shared text from apps like Amazon / Flipkart often contains extra text
     * around the URL, so we use a regex to pull out the first URL.
     */
    private fun handleShareIntent(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") {
            Log.d(TAG, "handleShareIntent: not a text/plain SEND intent")
            return null
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        Log.d(TAG, "handleShareIntent: raw shared text -> $sharedText")

        if (sharedText.isNullOrBlank()) {
            Log.w(TAG, "handleShareIntent: shared text is null or blank")
            return null
        }

        val matchResult = URL_REGEX.find(sharedText)
        val extractedUrl = matchResult?.value

        if (extractedUrl != null) {
            Log.i(TAG, "handleShareIntent: extracted URL -> $extractedUrl")
        } else {
            Log.w(TAG, "handleShareIntent: no URL found in shared text")
        }

        return extractedUrl
    }
}

@Composable
fun ReviewTrustNavHost(viewModel: ReviewViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(viewModel = viewModel)
        }
        composable(Screen.Analysis.route) {
            HomeScreen(viewModel = viewModel)
        }
    }
}
