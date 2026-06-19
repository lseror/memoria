package com.serortech.memoria.drive

import android.content.Context
import android.os.Environment
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.serortech.memoria.data.MemoriaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.google.api.services.drive.model.File as DriveFile

/**
 * Sauvegarde manuelle sur Google Drive : base Room + photos, dans un dossier
 * « Memoria ». Update-or-create par nom (ré-exécutable = mise à jour).
 */
class DriveBackup(private val ctx: Context, account: GoogleSignInAccount) {

    private val drive: Drive = Drive.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        DriveAuth.credential(ctx, account),
    ).setApplicationName("Memoria").build()

    suspend fun backup(onProgress: (String) -> Unit): Int = withContext(Dispatchers.IO) {
        val folder = ensureFolder()
        var count = 0

        onProgress("Base de données…")
        val db = checkpointAndGetDb()
        if (db != null && db.exists()) {
            updateOrCreate(folder, db, "application/octet-stream")
            count++
        }

        val photos = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?.listFiles { f -> f.isFile && f.name.endsWith(".jpg") }
            ?.sortedBy { it.name }
            ?: emptyList()
        photos.forEachIndexed { i, f ->
            onProgress("Photo ${i + 1}/${photos.size}…")
            updateOrCreate(folder, f, "image/jpeg")
            count++
        }
        count
    }

    private fun checkpointAndGetDb(): File? {
        runCatching {
            MemoriaDatabase.get(ctx).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        }
        return ctx.getDatabasePath("memoria.db")
    }

    private fun ensureFolder(): String {
        val query = "mimeType='application/vnd.google-apps.folder' " +
            "and name='$FOLDER_NAME' and trashed=false and 'root' in parents"
        val existing = drive.files().list()
            .setQ(query).setSpaces("drive").setFields("files(id,name)")
            .execute().files.firstOrNull()
        if (existing != null) return existing.id
        val meta = DriveFile().apply {
            name = FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
            parents = listOf("root")
        }
        return drive.files().create(meta).setFields("id").execute().id
    }

    private fun updateOrCreate(folderId: String, local: File, mime: String) {
        val q = "name='${local.name}' and '$folderId' in parents and trashed=false"
        val existing = drive.files().list()
            .setQ(q).setSpaces("drive").setFields("files(id,name)")
            .execute().files.firstOrNull()
        val content = FileContent(mime, local)
        if (existing != null) {
            drive.files().update(existing.id, DriveFile(), content).execute()
        } else {
            val meta = DriveFile().apply { name = local.name; parents = listOf(folderId) }
            drive.files().create(meta, content).setFields("id").execute()
        }
    }

    companion object {
        private const val FOLDER_NAME = "Memoria"
    }
}
