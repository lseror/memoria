package com.serortech.memoria.data

import androidx.room.Embedded
import androidx.room.Relation

/** Une transaction et toutes ses lignes (lecture jointe). */
data class TransactionWithLines(
    @Embedded val transaction: Transaction,
    @Relation(parentColumn = "id", entityColumn = "transactionId")
    val lines: List<TradeLine>,
)
