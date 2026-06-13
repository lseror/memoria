package com.serortech.memoria.media

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

/**
 * Gestion des fichiers photo : un fichier par capture dans
 * getExternalFilesDir(Pictures), exposé à l'appareil photo via FileProvider.
 */
object PhotoFiles {

    private const val AUTHORITY = "com.serortech.memoria.fileprovider"

    /** Crée un nouveau fichier photo vide et renvoie son chemin absolu. */
    fun newPhotoFile(ctx: Context, stampMs: Long): File {
        val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: ctx.filesDir
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "card_$stampMs.jpg")
    }

    /** URI content:// exploitable par l'appareil photo pour écrire dans [file]. */
    fun uriFor(ctx: Context, file: File): Uri =
        FileProvider.getUriForFile(ctx, AUTHORITY, file)
}
