package com.example.tvdrive

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.tvdrive.theme.TvDriveTheme
// AppNavHost is in package com.example.tvdrive (Navigation.kt) — same package, no import needed

class MainActivity : ComponentActivity() {

    private val app get() = application as TvDriveApp

    private val viewModel: AppViewModel by viewModels {
        AppViewModel.Factory(app.container.authManager)
    }

    /** Launcher for Google Sign-In intent */
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle D-pad back / remote back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.back()) {
                    // Already at root — let system handle (minimize/exit)
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        setContent {
            TvDriveTheme {
                // Provide AppContainer & ImageLoader via CompositionLocal — no prop drilling needed
                CompositionLocalProvider(
                    LocalAppContainer provides app.container,
                    coil.compose.LocalImageLoader provides app.container.imageLoader
                ) {
                    val screen by viewModel.currentScreen.collectAsState()
                    AppNavHost(
                        screen = screen,
                        viewModel = viewModel,
                        onStartSignIn = { signInLauncher.launch(app.container.authManager.getSignInIntent()) }
                    )
                }
            }
        }
    }
}
