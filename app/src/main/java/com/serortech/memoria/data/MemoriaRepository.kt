package com.serortech.memoria.data

import android.content.Context
import com.serortech.memoria.media.ExifTagger
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

/**
 * Accès aux données Memoria : observe les transactions et enregistre une
 * transaction avec ses lignes de façon atomique (les lignes reçoivent l'id
 * de transaction généré).
 */
class MemoriaRepository(private val dao: MemoriaDao) {

    fun observeTransactions(): Flow<List<TransactionWithLines>> = dao.observeTransactions()

    suspend fun saveTransaction(
        note: String?,
        validated: Boolean,
        lines: List<TradeLine>,
        now: Long,
    ): Long {
        val txId = dao.insertTransaction(
            Transaction(createdAt = now, note = note?.ifBlank { null }, validated = validated),
        )
        if (lines.isNotEmpty()) {
            val persisted = lines.map { it.copy(transactionId = txId, createdAt = now) }
            dao.insertLines(persisted)
            // Hybride : Room = source de vérité, + on incruste les champs dans l'EXIF.
            persisted.forEach { line ->
                line.photoPath?.takeIf { it.isNotBlank() }?.let { path ->
                    ExifTagger.write(path, exifJson(line))
                }
            }
        }
        return txId
    }

    private fun exifJson(line: TradeLine): String = JSONObject().apply {
        put("app", "memoria")
        put("transactionId", line.transactionId)
        put("direction", line.direction.name)
        put("name", line.name)
        put("price", line.price ?: JSONObject.NULL)
        put("marketPrice", line.marketPrice ?: JSONObject.NULL)
        put("transcript", line.transcript ?: JSONObject.NULL)
        put("createdAt", line.createdAt)
    }.toString()

    companion object {
        fun from(ctx: Context): MemoriaRepository =
            MemoriaRepository(MemoriaDatabase.get(ctx).dao())
    }
}
