package com.serortech.memoria.net

import android.content.Context
import com.serortech.memoria.settings.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TcgPrice(val name: String?, val price: Double?, val setName: String?)

/**
 * Prix marché via TCGPricer : GET /api/cards?search=<nom> → items triés par prix,
 * on prend le meilleur match. Best-effort : renvoie null en cas d'échec.
 */
class TcgPricer(private val ctx: Context) {

    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun lookup(query: String): TcgPrice? = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext null
        val base = ApiKeyStore(ctx).tcgPricerBaseUrl
        val url = "$base/api/cards?search=${URLEncoder.encode(query, "UTF-8")}&limit=5"
        runCatching {
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val items = JSONObject(resp.body?.string().orEmpty()).optJSONArray("items")
                    ?: return@use null
                if (items.length() == 0) return@use null
                val first = items.getJSONObject(0)
                TcgPrice(
                    name = first.optString("name").trim().ifBlank { null },
                    price = if (first.isNull("price")) null else first.optDouble("price").takeIf { !it.isNaN() },
                    setName = first.optString("set_name").trim().ifBlank { null },
                )
            }
        }.getOrNull()
    }
}
