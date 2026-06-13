package com.serortech.memoria.data

/** Sens d'une ligne de transaction : carte acquise (entrant) ou cédée (sortant). */
enum class TradeDirection {
    IN, // entrant — carte acquise (prix = prix d'achat)
    OUT, // sortant — carte cédée (prix = prix de vente)
}
