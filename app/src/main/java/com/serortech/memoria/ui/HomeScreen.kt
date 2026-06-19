package com.serortech.memoria.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.serortech.memoria.data.MemoriaRepository
import com.serortech.memoria.data.TradeDirection
import com.serortech.memoria.data.TransactionWithLines
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNewTransaction: () -> Unit, onSettings: () -> Unit, onEdit: (Long) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { MemoriaRepository.from(ctx) }
    val transactions by repo.observeTransactions().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memoria") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Réglages")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle transaction")
            }
        },
    ) { inner ->
        if (transactions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(inner).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Aucune transaction.", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Touche + pour en créer une.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        val spent = transactions.sumOf { tx ->
            tx.lines.filter { it.direction == TradeDirection.IN }.sumOf { it.price ?: 0.0 }
        }
        val received = transactions.sumOf { tx ->
            tx.lines.filter { it.direction == TradeDirection.OUT }.sumOf { it.price ?: 0.0 }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { BalanceCard(spent = spent, received = received) }
            items(transactions, key = { it.transaction.id }) { tx ->
                TransactionCard(tx, onClick = { onEdit(tx.transaction.id) })
            }
        }
    }
}

@Composable
private fun BalanceCard(spent: Double, received: Double) {
    val balance = received - spent
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Dépensé", style = MaterialTheme.typography.bodyMedium)
                Text("%.2f €".format(spent), style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Encaissé", style = MaterialTheme.typography.bodyMedium)
                Text("%.2f €".format(received), style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Balance", style = MaterialTheme.typography.titleMedium)
                Text(
                    "%+.2f €".format(balance),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (balance >= 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun TransactionCard(tx: TransactionWithLines, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                formatDate(tx.transaction.createdAt) +
                    if (tx.transaction.validated) " · validée" else " · brouillon",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            tx.lines.forEach { line ->
                val arrow = if (line.direction == TradeDirection.IN) "↘ entrant" else "↗ sortant"
                val price = line.price?.let { " — %.2f €".format(it) } ?: ""
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    line.photoPath?.let { PhotoThumb(path = it, sizeDp = 40) }
                    Text(
                        "$arrow  ${line.name}$price",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            tx.transaction.note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
private fun formatDate(epochMs: Long): String = dateFormat.format(Date(epochMs))
