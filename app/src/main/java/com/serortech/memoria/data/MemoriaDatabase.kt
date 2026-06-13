package com.serortech.memoria.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Transaction::class, TradeLine::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MemoriaDatabase : RoomDatabase() {

    abstract fun dao(): MemoriaDao

    companion object {
        @Volatile
        private var INSTANCE: MemoriaDatabase? = null

        fun get(ctx: Context): MemoriaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    MemoriaDatabase::class.java,
                    "memoria.db",
                ).build().also { INSTANCE = it }
            }
    }
}
