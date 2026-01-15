package com.isratv.android

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.isratv.android.ui.TvStreamsApp
import com.isratv.android.ui.theme.TvStreamsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // A flag to control whether Picture-in-Picture (PiP) mode is currently allowed.
    // This is enabled only when the user is on the player screen.
    var isPipEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Force Left-to-Right (LTR) layout direction for the entire app.
            // This prevents UI elements from flipping in Right-to-Left (RTL) locales (like Hebrew or Arabic),
            // ensuring a consistent layout.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                TvStreamsTheme {
                    TvStreamsApp()
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Enter Picture-in-Picture mode only if the flag is enabled (i.e., we are on the player screen).
        // This is triggered when the user presses the home button.
        if (isPipEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9)) // Common aspect ratio for video.
                .build()
            enterPictureInPictureMode(params)
        }
    }
}
