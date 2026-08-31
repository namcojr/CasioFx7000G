package com.retro.fx7000g

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.retro.fx7000g.ui.CalculatorScreen
import com.retro.fx7000g.ui.Fx7000gColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide the status bar (notification area) for a fully immersive look;
        // a swipe from the top temporarily reveals it.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            // Paint the whole window with the calculator's base colour so the
            // freed status-bar space blends in seamlessly.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Fx7000gColors.BodyEdge)
            ) {
                CalculatorScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        // Keep clear of the camera cutout on top and the
                        // navigation bar at the bottom, even with the status
                        // bar hidden.
                        .windowInsetsPadding(
                            WindowInsets.displayCutout.union(WindowInsets.navigationBars)
                        )
                )
            }
        }
    }
}
