package com.serortech.memoria.data

import androidx.room.TypeConverter

/** Conversions Room (Room ne persiste pas les enums nativement). */
class Converters {
    @TypeConverter
    fun directionToString(d: TradeDirection): String = d.name

    @TypeConverter
    fun stringToDirection(s: String): TradeDirection = TradeDirection.valueOf(s)
}
