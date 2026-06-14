package com.serortech.memoria.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.serortech.memoria.drive.DriveAuth
import com.serortech.memoria.drive.DriveBackup
import com.serortech.memoria.settings.ApiKeyStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { ApiKeyStore(ctx) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var openAiKey by remember { mutableStateOf(store.openAiKey) }
    var showKey by remember { mutableStateOf(false) }
    var tcgUrl by remember { mutableStateOf(store.tcgPricerBaseUrl) }
    var voicePrompt by remember { mutableStateOf(store.voicePrompt) }
    var recoPrompt by remember { mutableStateOf(store.recognitionPrompt) }

    var driveSignedIn by remember { mutableStateOf(DriveAuth.isSignedInWithDrive(ctx)) }
    var driveEmail by remember { mutableStateOf(DriveAuth.lastAccount(ctx)?.email ?: "") }
    var backupBusy by remember { mutableStateOf(false) }
    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        driveSignedIn = DriveAuth.isSignedInWithDrive(ctx)
        driveEmail = DriveAuth.lastAccount(ctx)?.email ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = openAiKey,
                onValueChange = { openAiKey = it },
                label = { Text("Clé OpenAI") },
                supportingText = { Text("Pour la saisie vocale et la reconnaissance de carte.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (showKey) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "Cacher" else "Voir")
                    }
                },
            )

            OutlinedTextField(
                value = tcgUrl,
                onValueChange = { tcgUrl = it },
                label = { Text("URL TCGPricer") },
                supportingText = { Text("Source des prix marché. Défaut : ${ApiKeyStore.DEFAULT_TCGPRICER}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = voicePrompt,
                onValueChange = { voicePrompt = it },
                label = { Text("Prompt — extraction vocale") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { voicePrompt = ApiKeyStore.DEFAULT_VOICE_PROMPT }) {
                Text("Réinitialiser le prompt vocal")
            }

            OutlinedTextField(
                value = recoPrompt,
                onValueChange = { recoPrompt = it },
                label = { Text("Prompt — reconnaissance image") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { recoPrompt = ApiKeyStore.DEFAULT_RECOGNITION_PROMPT }) {
                Text("Réinitialiser le prompt image")
            }

            Button(
                onClick = {
                    store.openAiKey = openAiKey
                    store.tcgPricerBaseUrl = tcgUrl
                    tcgUrl = store.tcgPricerBaseUrl
                    store.voicePrompt = voicePrompt
                    store.recognitionPrompt = recoPrompt
                    scope.launch { snackbar.showSnackbar("Réglages enregistrés") }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer") }

            HorizontalDivider()
            Text("Sauvegarde Drive", style = MaterialTheme.typography.titleMedium)
            if (driveSignedIn) {
                Text(
                    "Connecté : $driveEmail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    enabled = !backupBusy,
                    onClick = {
                        val account = DriveAuth.lastAccount(ctx)
                        if (account == null) {
                            scope.launch { snackbar.showSnackbar("Reconnecte-toi à Drive.") }
                        } else {
                            backupBusy = true
                            scope.launch {
                                try {
                                    val n = DriveBackup(ctx, account).backup { }
                                    snackbar.showSnackbar("Sauvegardé : $n fichier(s)")
                                } catch (e: Exception) {
                                    snackbar.showSnackbar(e.message ?: "Échec de la sauvegarde")
                                } finally {
                                    backupBusy = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (backupBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Sauvegarder sur Drive")
                    }
                }
                OutlinedButton(
                    onClick = {
                        DriveAuth.signInClient(ctx).signOut()
                        driveSignedIn = false
                        driveEmail = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Se déconnecter de Drive") }
            } else {
                Button(
                    onClick = { driveSignInLauncher.launch(DriveAuth.signInClient(ctx).signInIntent) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Connecter Google Drive") }
            }
        }
    }
}
