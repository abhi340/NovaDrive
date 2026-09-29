package com.example.tvdrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.tvdrive.theme.TvDriveTheme

class MainActivity : ComponentActivity() {

    private val app get() = application as TvDriveApp

    private val viewModel: AppViewModel by viewModels {
        AppViewModel.Factory(app.container.authManager)
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BackHandler { if (!viewModel.back()) finish() }
            TvDriveTheme {
                CompositionLocalProvider(LocalAppContainer provides app.container) {
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
