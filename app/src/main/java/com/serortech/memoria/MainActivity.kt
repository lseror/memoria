package com.serortech.memoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.serortech.memoria.ui.theme.MemoriaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoriaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    Home(modifier = Modifier.padding(inner))
                }
            }
        }
    }
}

@Composable
fun Home(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Memoria", style = MaterialTheme.typography.headlineLarge)
        Text(
            "v0.0.1 — carnet de transactions cartes",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
