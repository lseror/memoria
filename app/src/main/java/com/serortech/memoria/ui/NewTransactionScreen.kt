package com.serortech.memoria.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.serortech.memoria.audio.VoiceRecorder
import com.serortech.memoria.data.MemoriaRepository
import com.serortech.memoria.data.TradeDirection
import com.serortech.memoria.data.TradeLine
import com.serortech.memoria.media.ImageProcessing
import com.serortech.memoria.media.PhotoFiles
import com.serortech.memoria.net.TcgPricer
import com.serortech.memoria.vision.CardRecognizer
import com.serortech.memoria.voice.OpenAiVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** État éditable d'une ligne en cours de saisie. */
private class LineDraft {
    var direction by mutableStateOf(TradeDirection.IN)
    var name by mutableStateOf("")
    var price by mutableStateOf("")
    var photoPath by mutableStateOf<String?>(null)
    var transcript by mutableStateOf<String?>(null)
    var marketPrice by mutableStateOf<Double?>(null)
    var recognized by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionScreen(editId: Long?, onBack: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { MemoriaRepository.from(ctx) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val lines = remember { mutableStateListOf(LineDraft()) }
    var note by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var createdAt by remember { mutableStateOf(0L) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != null) {
            repo.getTransaction(editId)?.let { tx ->
                createdAt = tx.transaction.createdAt
                note = tx.transaction.note.orEmpty()
                lines.clear()
                tx.lines.forEach { lines.add(draftOf(it)) }
                if (lines.isEmpty()) lines.add(LineDraft())
            }
        }
    }

    val canSave = lines.any { it.name.isNotBlank() } && !saving

    if (showDelete && editId != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Supprimer la transaction ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        withContext(Dispatchers.IO) { repo.deleteTransaction(editId) }
                        onSaved()
                    }
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Annuler") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editId == null) "Nouvelle transaction" else "Modifier la transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (editId != null) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
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
                    onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                    onTranscript = { t -> note = if (note.isBlank()) t else note + "\n" + t },
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
                                marketPrice = d.marketPrice,
                                photoPath = d.photoPath,
                                transcript = d.transcript,
                                recognized = d.recognized,
                                createdAt = now,
                            )
                        }
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                if (editId == null) {
                                    repo.saveTransaction(note, true, toSave, now)
                                } else {
                                    repo.updateTransaction(
                                        transactionId = editId,
                                        createdAt = createdAt.takeIf { it > 0 } ?: now,
                                        note = note,
                                        validated = true,
                                        lines = toSave,
                                        now = now,
                                    )
                                }
                            }
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (editId == null) "Valider la transaction" else "Enregistrer les modifications") }
            }
        }
    }
}

@Composable
private fun LineEditor(
    line: LineDraft,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onError: (String) -> Unit,
    onTranscript: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { VoiceRecorder(ctx) }
    val voice = remember { OpenAiVoice(ctx) }
    var recording by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val recognizer = remember { CardRecognizer(ctx) }
    val tcgPricer = remember { TcgPricer(ctx) }
    var recognizing by remember { mutableStateOf(false) }

    fun runRecognition() {
        val path = line.photoPath ?: return
        if (recognizing) return
        recognizing = true
        scope.launch {
            try {
                val rec = recognizer.recognize(path)
                rec.name?.let { line.name = it; line.recognized = true }
                val query = (rec.name ?: line.name).trim()
                if (query.isNotBlank()) {
                    tcgPricer.lookup(query)?.price?.let { line.marketPrice = it }
                }
                if (rec.name == null) onError("Carte non reconnue — renseigne-la à la main.")
            } catch (e: Exception) {
                onError(e.message ?: "Échec de la reconnaissance.")
            } finally {
                recognizing = false
            }
        }
    }

    var pendingFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) pendingFile?.let { f ->
            scope.launch {
                withContext(Dispatchers.IO) { ImageProcessing.stripAndNormalize(f) }
                line.photoPath = f.absolutePath
                runRecognition()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val f = PhotoFiles.newPhotoFile(ctx, System.currentTimeMillis())
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        ctx.contentResolver.openInputStream(uri)?.use { input ->
                            f.outputStream().use { input.copyTo(it) }
                        }
                        ImageProcessing.stripAndNormalize(f)
                        true
                    }.getOrDefault(false)
                }
                if (ok) { line.photoPath = f.absolutePath; runRecognition() }
                else onError("Import de l'image impossible.")
            }
        }
    }

    fun startRecording() {
        try {
            recorder.start()
            recording = true
        } catch (e: Exception) {
            onError("Micro indisponible.")
        }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startRecording() else onError("Permission micro refusée.")
    }

    fun onMicClick() {
        when {
            busy -> Unit
            recording -> {
                recording = false
                val file = recorder.stop()
                if (file == null) {
                    onError("Échec de l'enregistrement.")
                    return
                }
                busy = true
                scope.launch {
                    try {
                        val text = voice.transcribe(file)
                        line.transcript = text
                        onTranscript(text)
                        val ex = voice.extractLine(text)
                        ex.direction?.let { line.direction = it }
                        ex.name?.let { line.name = it }
                        ex.price?.let { line.price = formatPrice(it) }
                        if (ex.direction == null && ex.name == null && ex.price == null) {
                            onError("Rien compris : « $text »")
                        }
                    } catch (e: Exception) {
                        onError(e.message ?: "Échec de la saisie vocale.")
                    } finally {
                        busy = false
                        file.delete()
                    }
                }
            }
            else -> {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    startRecording()
                } else {
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        line.direction =
                            if (line.direction == TradeDirection.IN) TradeDirection.OUT
                            else TradeDirection.IN
                    },
                ) {
                    Text(if (line.direction == TradeDirection.IN) "↘ Entrant" else "↗ Sortant")
                }
                OutlinedButton(onClick = { onMicClick() }, enabled = !busy) {
                    when {
                        busy -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        recording -> Text("⏹ Stop")
                        else -> Text("🎤 Dicter")
                    }
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
            line.marketPrice?.let {
                Text(
                    "Marché ~ %.2f €".format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val f = PhotoFiles.newPhotoFile(ctx, System.currentTimeMillis())
                        pendingFile = f
                        cameraLauncher.launch(PhotoFiles.uriFor(ctx, f))
                    },
                ) {
                    Text(if (line.photoPath == null) "Photographier" else "Reprendre la photo")
                }
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) { Text("Importer") }
                line.photoPath?.let { PhotoThumb(path = it, sizeDp = 56) }
            }
            if (line.photoPath != null) {
                OutlinedButton(onClick = { runRecognition() }, enabled = !recognizing) {
                    if (recognizing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("  Reconnaissance…")
                    } else {
                        Text("Relancer la reconnaissance")
                    }
                }
            }
        }
    }
}

/** 4000.0 → "4000" ; 12.5 → "12.5". */
private fun formatPrice(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/** Construit un brouillon éditable depuis une ligne persistée (mode édition). */
private fun draftOf(line: TradeLine): LineDraft = LineDraft().apply {
    direction = line.direction
    name = line.name
    price = line.price?.let { formatPrice(it) } ?: ""
    marketPrice = line.marketPrice
    photoPath = line.photoPath
    transcript = line.transcript
    recognized = line.recognized
}
