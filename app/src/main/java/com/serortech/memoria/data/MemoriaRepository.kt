package com.serortech.memoria.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

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
            dao.insertLines(lines.map { it.copy(transactionId = txId, createdAt = now) })
        }
        return txId
    }

    companion object {
        fun from(ctx: Context): MemoriaRepository =
            MemoriaRepository(MemoriaDatabase.get(ctx).dao())
    }
}
