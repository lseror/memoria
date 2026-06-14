package com.serortech.memoria.media

import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Incruste (et relit) un blob JSON dans l'EXIF d'une photo JPEG, via le champ
 * UserComment (et ImageDescription en doublon lisible). Room reste la source de
 * vérité ; l'EXIF rend la photo auto-descriptive et portable.
 */
object ExifTagger {

    fun write(path: String, json: String) {
        val file = File(path)
        if (!file.exists()) return
        runCatching {
            val exif = ExifInterface(path)
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, json)
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, json)
            exif.saveAttributes()
        }
    }

    fun read(path: String): String? =
        runCatching { ExifInterface(path).getAttribute(ExifInterface.TAG_USER_COMMENT) }.getOrNull()
}
