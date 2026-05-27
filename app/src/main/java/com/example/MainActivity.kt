package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SoundManager.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (viewModel.screenState) {
                            ScreenState.SPLASH -> SplashScreen(viewModel)
                            ScreenState.MAIN_MENU -> MainMenuScreen(viewModel)
                            ScreenState.GAME_LEVEL -> {
                                GameLevelScreen(viewModel)
                            }
                            ScreenState.DEATH_SCREEN -> DeathScreen(viewModel)
                            ScreenState.LEVEL_COMPLETE -> LevelCompleteScreen(viewModel)
                            ScreenState.LEADERBOARD -> LeaderboardScreen(viewModel)
                            ScreenState.SETTINGS -> SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.screenState == ScreenState.MAIN_MENU || viewModel.screenState == ScreenState.GAME_LEVEL) {
            SoundManager.startBackgroundMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        SoundManager.stopBackgroundMusic()
    }
}
