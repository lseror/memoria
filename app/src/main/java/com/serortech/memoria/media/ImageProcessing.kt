package com.serortech.memoria.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Nettoie une photo juste après capture : applique l'orientation EXIF dans les
 * pixels (image droite) puis ré-encode le JPEG, ce qui supprime TOUT l'EXIF
 * d'origine (GPS/localisation, orientation, modèle d'appareil…). On garde ainsi
 * un contrôle total : seules nos métadonnées JSON sont réécrites ensuite.
 */
object ImageProcessing {

    fun stripAndNormalize(file: File) {
        runCatching {
            val path = file.absolutePath
            val orientation = ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            val src = BitmapFactory.decodeFile(path) ?: return
            val oriented = applyOrientation(src, orientation)
            // Toujours en portrait : si paysage (largeur > hauteur), pivoter de 90°.
            val out = forcePortrait(oriented)
            FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            if (oriented !== src) src.recycle()
            if (out !== oriented) oriented.recycle()
            out.recycle()
        }
    }

    private fun forcePortrait(bmp: Bitmap): Bitmap {
        if (bmp.width <= bmp.height) return bmp
        val m = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    private fun applyOrientation(bmp: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return bmp
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
