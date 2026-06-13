package com.serortech.memoria.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoriaDao {

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Insert
    suspend fun insertLine(line: TradeLine): Long

    @Insert
    suspend fun insertLines(lines: List<TradeLine>)

    @Query("DELETE FROM trade_lines WHERE transactionId = :transactionId")
    suspend fun deleteLinesFor(transactionId: Long)

    @Update
    suspend fun updateLine(line: TradeLine)

    @Delete
    suspend fun deleteLine(line: TradeLine)

    /** Transactions les plus récentes en premier, avec leurs lignes. */
    @RoomTransaction
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeTransactions(): Flow<List<TransactionWithLines>>

    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: Long): TransactionWithLines?
}
