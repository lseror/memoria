package com.serortech.memoria.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, TradeLine::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MemoriaDatabase : RoomDatabase() {

    abstract fun dao(): MemoriaDao

    companion object {
        @Volatile
        private var INSTANCE: MemoriaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trade_lines ADD COLUMN transcript TEXT")
            }
        }

        fun get(ctx: Context): MemoriaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    MemoriaDatabase::class.java,
                    "memoria.db",
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
