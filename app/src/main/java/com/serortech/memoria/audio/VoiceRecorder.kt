package com.serortech.memoria.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Enregistre un court clip vocal en OGG Opus (API 29+) dans le cache, pour
 * transcription. Toucher pour démarrer, toucher pour arrêter.
 */
class VoiceRecorder(private val ctx: Context) {

    private var recorder: MediaRecorder? = null
    private var outFile: File? = null

    fun start() {
        val f = File(ctx.cacheDir, "voice_${System.currentTimeMillis()}.ogg")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.OGG)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
        r.setAudioEncodingBitRate(24_000)
        r.setAudioSamplingRate(16_000)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        outFile = f
    }

    /** Arrête et renvoie le fichier enregistré, ou null en cas d'échec. */
    fun stop(): File? {
        val r = recorder ?: return null
        val f = outFile
        return try {
            r.stop()
            f
        } catch (e: Exception) {
            f?.delete()
            null
        } finally {
            try { r.release() } catch (_: Exception) {}
            recorder = null
            outFile = null
        }
    }
}
