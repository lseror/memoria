package com.serortech.memoria.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Une carte au sein d'une transaction : son sens (entrant/sortant), son nom,
 * le prix convenu (EUR), un éventuel prix de marché suggéré, et la photo.
 */
@Entity(
    tableName = "trade_lines",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("transactionId")],
)
data class TradeLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val direction: TradeDirection,
    val name: String,
    val setName: String? = null,
    val cardNumber: String? = null,
    /** Prix convenu pour cette carte dans cette transaction, en euros. */
    val price: Double? = null,
    /** Prix de marché suggéré (TCGPricer), en euros. */
    val marketPrice: Double? = null,
    val photoPath: String? = null,
    /** Phrase dictée ayant servi à remplir la ligne (saisie vocale), si présente. */
    val transcript: String? = null,
    /** true si le nom vient de la reconnaissance auto (et non d'une saisie manuelle). */
    val recognized: Boolean = false,
    val createdAt: Long,
)
