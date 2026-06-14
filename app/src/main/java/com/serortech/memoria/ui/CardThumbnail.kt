package com.serortech.memoria.ui

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.serortech.memoria.media.PhotoFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Miniature cliquable : ouvre la photo en plein écran (zoom + partage) au clic. */
@Composable
fun PhotoThumb(path: String, sizeDp: Int = 64, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    CardThumbnail(path, sizeDp, modifier.clickable { open = true })
    if (open) {
        FullScreenPhoto(path = path, onClose = { open = false })
    }
}

@Composable
private fun FullScreenPhoto(path: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val image by produceState<ImageBitmap?>(initialValue = null, path) {
            value = withContext(Dispatchers.IO) { decodeSampled(path, 2048)?.asImageBitmap() }
        }
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            image?.let { img ->
                Image(
                    bitmap = img,
                    contentDescription = "Photo de la carte",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 6f)
                                offset = if (scale > 1f) offset + pan else Offset.Zero
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        ),
                )
            }
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                IconButton(onClick = { sharePhoto(ctx, path) }) {
                    Icon(Icons.Default.Share, contentDescription = "Partager", tint = Color.White)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }
        }
    }
}

private fun sharePhoto(ctx: android.content.Context, path: String) {
    runCatching {
        val uri = PhotoFiles.uriFor(ctx, File(path))
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Partager la photo"))
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
