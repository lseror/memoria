package com.serortech.memoria.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Réglages chiffrés (EncryptedSharedPreferences, clé maître Android Keystore) :
 * clé OpenAI (transcription voix + vision) et base URL de TCGPricer (prix marché).
 */
class ApiKeyStore(ctx: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx.applicationContext,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var openAiKey: String
        get() = prefs.getString(KEY_OPENAI, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_OPENAI, value.trim()) }

    var tcgPricerBaseUrl: String
        get() = prefs.getString(KEY_TCGPRICER, DEFAULT_TCGPRICER).orEmpty().ifBlank { DEFAULT_TCGPRICER }
        set(value) = prefs.edit { putString(KEY_TCGPRICER, value.trim().trimEnd('/')) }

    /** Prompt système d'extraction de la saisie vocale (sens/nom/prix). */
    var voicePrompt: String
        get() = prefs.getString(KEY_VOICE_PROMPT, DEFAULT_VOICE_PROMPT).orEmpty().ifBlank { DEFAULT_VOICE_PROMPT }
        set(value) = prefs.edit { putString(KEY_VOICE_PROMPT, value) }

    /** Prompt système de reconnaissance d'image (carte). */
    var recognitionPrompt: String
        get() = prefs.getString(KEY_RECOGNITION_PROMPT, DEFAULT_RECOGNITION_PROMPT).orEmpty().ifBlank { DEFAULT_RECOGNITION_PROMPT }
        set(value) = prefs.edit { putString(KEY_RECOGNITION_PROMPT, value) }

    fun hasOpenAiKey(): Boolean = openAiKey.isNotBlank()

    companion object {
        private const val PREFS = "memoria_secrets"
        private const val KEY_OPENAI = "openai_api_key"
        private const val KEY_TCGPRICER = "tcgpricer_base_url"
        private const val KEY_VOICE_PROMPT = "voice_prompt"
        private const val KEY_RECOGNITION_PROMPT = "recognition_prompt"
        const val DEFAULT_TCGPRICER = "https://www.d8a.fr"

        val DEFAULT_VOICE_PROMPT = """
            Tu extrais les informations d'une phrase en français décrivant UNE carte échangée (cartes à collectionner, ex. Pokémon).
            Réponds uniquement en JSON avec les clés :
            - "direction" : "IN" si la carte est acquise / entrante / achetée / reçue ; "OUT" si cédée / sortante / vendue / donnée ; null si non précisé.
            - "name" : le nom de la carte tel que prononcé, sinon null.
            - "price" : le prix en euros sous forme de nombre (ex. 4000), sinon null.
            Ne déduis que ce qui est explicite. N'invente pas de valeur.
        """.trimIndent()

        val DEFAULT_RECOGNITION_PROMPT = """
            Tu identifies une carte à collectionner (Pokémon en priorité) à partir d'une photo.
            Réponds uniquement en JSON avec :
            - "name" : le nom de la carte tel qu'imprimé, sinon null.
            - "set" : le nom de l'extension/série si lisible, sinon null.
            - "number" : le numéro de la carte (ex. "4/102") si lisible, sinon null.
            Ne devine pas si l'information n'est pas lisible.
        """.trimIndent()
    }
}
