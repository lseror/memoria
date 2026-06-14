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
import com.serortech.memoria.ui.SettingsScreen
import com.serortech.memoria.ui.theme.MemoriaTheme

private enum class Screen { HOME, NEW_TRANSACTION, SETTINGS }

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
    var editId by remember { mutableStateOf<Long?>(null) }
    when (screen) {
        Screen.HOME -> HomeScreen(
            onNewTransaction = { editId = null; screen = Screen.NEW_TRANSACTION },
            onSettings = { screen = Screen.SETTINGS },
            onEdit = { id -> editId = id; screen = Screen.NEW_TRANSACTION },
        )
        Screen.NEW_TRANSACTION -> {
            BackHandler { screen = Screen.HOME }
            NewTransactionScreen(
                editId = editId,
                onBack = { screen = Screen.HOME },
                onSaved = { screen = Screen.HOME },
            )
        }
        Screen.SETTINGS -> {
            BackHandler { screen = Screen.HOME }
            SettingsScreen(onBack = { screen = Screen.HOME })
        }
    }
}
