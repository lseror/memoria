package com.serortech.memoria.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Une transaction = un échange daté regroupant une ou plusieurs lignes
 * (cartes entrantes/sortantes). V1 : pas de liaison d'exemplaire entre
 * transactions (cf. épic AOO-11, P&L par exemplaire = V2).
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val note: String? = null,
    val validated: Boolean = false,
)
