package com.serortech.memoria.vision

import android.content.Context
import android.util.Base64
import com.serortech.memoria.settings.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class CardRecognition(
    val name: String?,
    val setName: String?,
    val cardNumber: String?,
)

class RecognitionException(message: String) : Exception(message)

/** Reconnaissance d'une carte depuis sa photo via un LLM vision (OpenAI). */
class CardRecognizer(private val ctx: Context) {

    private val client = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun recognize(photoPath: String): CardRecognition = withContext(Dispatchers.IO) {
        val key = ApiKeyStore(ctx).openAiKey
        if (key.isBlank()) throw RecognitionException("Aucune clé OpenAI. Renseigne-la dans les Réglages.")
        val file = File(photoPath)
        if (!file.exists()) throw RecognitionException("Photo introuvable.")

        val b64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        val userContent = JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", "Identifie cette carte."))
            put(
                JSONObject().put("type", "image_url").put(
                    "image_url",
                    JSONObject().put("url", "data:image/jpeg;base64,$b64"),
                ),
            )
        }
        val payload = JSONObject().apply {
            put("model", MODEL)
            put("response_format", JSONObject().put("type", "json_object"))
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", PROMPT))
                put(JSONObject().put("role", "user").put("content", userContent))
            })
        }
        val req = Request.Builder()
            .url("$BASE/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching { JSONObject(raw).getJSONObject("error").getString("message") }
                    .getOrNull() ?: "HTTP ${resp.code}"
                throw RecognitionException("Reconnaissance : $msg")
            }
            val content = runCatching {
                JSONObject(raw).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content")
            }.getOrElse { throw RecognitionException("Réponse de reconnaissance inattendue.") }
            val o = runCatching { JSONObject(content) }.getOrElse { return@use CardRecognition(null, null, null) }
            CardRecognition(
                name = o.optString("name").trim().ifBlank { null },
                setName = o.optString("set").trim().ifBlank { null },
                cardNumber = o.optString("number").trim().ifBlank { null },
            )
        }
    }

    companion object {
        private const val BASE = "https://api.openai.com/v1"
        private const val MODEL = "gpt-4o"
        private val PROMPT = """
            Tu identifies une carte à collectionner (Pokémon en priorité) à partir d'une photo.
            Réponds uniquement en JSON avec :
            - "name" : le nom de la carte tel qu'imprimé, sinon null.
            - "set" : le nom de l'extension/série si lisible, sinon null.
            - "number" : le numéro de la carte (ex. "4/102") si lisible, sinon null.
            Ne devine pas si l'information n'est pas lisible.
        """.trimIndent()
    }
}
