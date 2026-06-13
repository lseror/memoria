package com.serortech.memoria.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Miniature cliquable : ouvre la photo en plein écran au clic. */
@Composable
fun PhotoThumb(path: String, sizeDp: Int = 64, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    CardThumbnail(path, sizeDp, modifier.clickable { open = true })
    if (open) {
        Dialog(
            onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val image by produceState<ImageBitmap?>(initialValue = null, path) {
                value = withContext(Dispatchers.IO) { decodeSampled(path, 1440)?.asImageBitmap() }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { open = false },
                contentAlignment = Alignment.Center,
            ) {
                image?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Photo de la carte",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Miniature d'une photo de carte, décodée en sous-échantillon hors thread UI. */
@Composable
fun CardThumbnail(path: String, sizeDp: Int = 64, modifier: Modifier = Modifier) {
    val image by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { decodeSampled(path, 256)?.asImageBitmap() }
    }
    image?.let {
        Image(
            bitmap = it,
            contentDescription = "Photo de la carte",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(sizeDp.dp),
        )
    }
}

private fun decodeSampled(path: String, target: Int): android.graphics.Bitmap? {
    val file = File(path)
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    while (largest / sample > target) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(path, opts)
}
