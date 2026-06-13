package com.serortech.memoria.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.serortech.memoria.data.MemoriaRepository
import com.serortech.memoria.data.TradeDirection
import com.serortech.memoria.data.TradeLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** État éditable d'une ligne en cours de saisie. */
private class LineDraft {
    var direction by mutableStateOf(TradeDirection.IN)
    var name by mutableStateOf("")
    var price by mutableStateOf("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { MemoriaRepository.from(ctx) }
    val scope = rememberCoroutineScope()

    val lines = remember { mutableStateListOf(LineDraft()) }
    var note by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val canSave = lines.any { it.name.isNotBlank() } && !saving

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(lines) { index, line ->
                LineEditor(
                    line = line,
                    canDelete = lines.size > 1,
                    onDelete = { lines.removeAt(index) },
                )
            }
            item {
                OutlinedButton(
                    onClick = { lines.add(LineDraft()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ajouter une carte") }
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(
                    enabled = canSave,
                    onClick = {
                        saving = true
                        val now = System.currentTimeMillis()
                        val toSave = lines.filter { it.name.isNotBlank() }.map { d ->
                            TradeLine(
                                transactionId = 0,
                                direction = d.direction,
                                name = d.name.trim(),
                                price = d.price.replace(',', '.').toDoubleOrNull(),
                                createdAt = now,
                            )
                        }
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repo.saveTransaction(
                                    note = note,
                                    validated = true,
                                    lines = toSave,
                                    now = now,
                                )
                            }
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Valider la transaction") }
            }
        }
    }
}

@Composable
private fun LineEditor(line: LineDraft, canDelete: Boolean, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        line.direction =
                            if (line.direction == TradeDirection.IN) TradeDirection.OUT
                            else TradeDirection.IN
                    },
                ) {
                    Text(if (line.direction == TradeDirection.IN) "↘ Entrant" else "↗ Sortant")
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer la ligne")
                    }
                }
            }
            OutlinedTextField(
                value = line.name,
                onValueChange = { line.name = it },
                label = { Text("Nom de la carte") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = line.price,
                onValueChange = { line.price = it },
                label = { Text(if (line.direction == TradeDirection.IN) "Prix d'achat (€)" else "Prix de vente (€)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(220.dp),
            )
        }
    }
}
