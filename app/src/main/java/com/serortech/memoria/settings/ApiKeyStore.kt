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

    fun hasOpenAiKey(): Boolean = openAiKey.isNotBlank()

    companion object {
        private const val PREFS = "memoria_secrets"
        private const val KEY_OPENAI = "openai_api_key"
        private const val KEY_TCGPRICER = "tcgpricer_base_url"
        const val DEFAULT_TCGPRICER = "https://www.d8a.fr"
    }
}
