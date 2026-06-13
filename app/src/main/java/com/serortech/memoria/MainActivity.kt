package com.serortech.memoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.serortech.memoria.ui.HomeScreen
import com.serortech.memoria.ui.NewTransactionScreen
import com.serortech.memoria.ui.theme.MemoriaTheme

private enum class Screen { HOME, NEW_TRANSACTION }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoriaTheme {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    when (screen) {
        Screen.HOME -> HomeScreen(onNewTransaction = { screen = Screen.NEW_TRANSACTION })
        Screen.NEW_TRANSACTION -> {
            BackHandler { screen = Screen.HOME }
            NewTransactionScreen(
                onBack = { screen = Screen.HOME },
                onSaved = { screen = Screen.HOME },
            )
        }
    }
}
