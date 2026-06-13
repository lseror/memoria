package com.serortech.memoria.voice

import android.content.Context
import com.serortech.memoria.data.TradeDirection
import com.serortech.memoria.settings.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Résultat de l'extraction d'une ligne depuis une phrase dictée. */
data class LineExtraction(
    val direction: TradeDirection?,
    val name: String?,
    val price: Double?,
)

class VoiceException(message: String) : Exception(message)

/**
 * Pipeline vocal : transcription (gpt-4o-transcribe) puis extraction structurée
 * (gpt-4o-mini, JSON) d'une ligne de transaction depuis une phrase en français.
 */
class OpenAiVoice(private val ctx: Context) {

    private val client = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private fun key(): String {
        val k = ApiKeyStore(ctx).openAiKey
        if (k.isBlank()) throw VoiceException("Aucune clé OpenAI. Renseigne-la dans les Réglages.")
        return k
    }

    suspend fun transcribe(file: File): String = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", TRANSCRIBE_MODEL)
            .addFormDataPart("language", "fr")
            .addFormDataPart("file", file.name, bytes.toRequestBody("audio/ogg".toMediaType()))
            .build()
        val req = Request.Builder()
            .url("$BASE/audio/transcriptions")
            .addHeader("Authorization", "Bearer ${key()}")
            .post(body)
            .build()
        client.newCall(req).execute().use { resp ->
            val payload = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw VoiceException("Transcription : ${errorMessage(payload, resp.code)}")
            runCatching { JSONObject(payload).getString("text") }
                .getOrElse { throw VoiceException("Réponse de transcription inattendue.") }
        }
    }

    suspend fun extractLine(transcript: String): LineExtraction = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("model", CHAT_MODEL)
            put("response_format", JSONObject().put("type", "json_object"))
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", ApiKeyStore(ctx).voicePrompt))
                put(JSONObject().put("role", "user").put("content", transcript))
            })
        }
        val req = Request.Builder()
            .url("$BASE/chat/completions")
            .addHeader("Authorization", "Bearer ${key()}")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw VoiceException("Analyse : ${errorMessage(raw, resp.code)}")
            val content = runCatching {
                JSONObject(raw).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content")
            }.getOrElse { throw VoiceException("Réponse d'analyse inattendue.") }
            parse(content)
        }
    }

    private fun parse(json: String): LineExtraction {
        val o = runCatching { JSONObject(json) }.getOrElse { return LineExtraction(null, null, null) }
        val dir = o.optString("direction", "").uppercase().let {
            when (it) {
                "IN" -> TradeDirection.IN
                "OUT" -> TradeDirection.OUT
                else -> null
            }
        }
        val name = o.optString("name", "").trim().ifBlank { null }
        val price = if (o.isNull("price")) null else o.optDouble("price").takeIf { !it.isNaN() }
        return LineExtraction(dir, name, price)
    }

    private fun errorMessage(payload: String, code: Int): String =
        runCatching { JSONObject(payload).getJSONObject("error").getString("message") }
            .getOrNull() ?: "HTTP $code"

    companion object {
        private const val BASE = "https://api.openai.com/v1"
        private const val TRANSCRIBE_MODEL = "gpt-4o-transcribe"
        private const val CHAT_MODEL = "gpt-4o-mini"
    }
}
