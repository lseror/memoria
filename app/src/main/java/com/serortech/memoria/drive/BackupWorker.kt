package com.serortech.memoria.drive

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Sauvegarde Drive quotidienne en arrière-plan (~4h). */
class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val account = DriveAuth.lastAccount(applicationContext)
            ?: return Result.success() // pas connecté à Drive → rien à faire
        return try {
            val n = DriveBackup(applicationContext, account).backup { }
            Log.i(TAG, "auto-backup OK: $n fichier(s)")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "auto-backup failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "MemoriaBackupWorker"
        private const val WORK_NAME = "daily_drive_backup"
        private const val TARGET_HOUR = 4

        /** Planifie la sauvegarde quotidienne autour de 4h (best-effort WorkManager). */
        fun schedule(ctx: Context) {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, TARGET_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelay = next.timeInMillis - now.timeInMillis
            val request = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
