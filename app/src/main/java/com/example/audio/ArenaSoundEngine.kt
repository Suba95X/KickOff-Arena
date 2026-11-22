package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.UserArenaSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class ArenaSoundEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050

    @Volatile
    var settings: UserArenaSettings = UserArenaSettings()

    private fun getMasterVol(): Float {
        return if (settings.muted || settings.masterVolume == 0) 0f
        else settings.masterVolume / 100f
    }

    private fun getSfxVol(): Float = getMasterVol() * (settings.sfxVolume / 100f)
    private fun getCrowdLoopVol(): Float = getMasterVol() * (settings.crowdLoopVolume / 100f)
    private fun getWhistleVol(): Float = getMasterVol() * (settings.whistleVolume / 100f)

    fun sfx(name: String) {
        when (name) {
            "kick" -> playTone(420f, 150f, 0.12f, "triangle", 0.25f, "sfx")
            "superkick" -> {
                playTone(650f, 90f, 0.28f, "sawtooth", 0.35f, "sfx")
                playNoise(0.35f, 0.25f, 450f, "sfx")
            }
            "pu_speed" -> playTone(523f, 1046f, 0.2f, "sawtooth", 0.22f, "sfx")
            "pu_kick" -> {
                playTone(260f, 520f, 0.2f, "triangle", 0.25f, "sfx")
                playNoise(0.2f, 0.15f, 800f, "sfx")
            }
            "whiff" -> playNoise(0.12f, 0.05f, 2400f, "sfx")
            "wall" -> playTone(900f, 500f, 0.06f, "square", 0.05f, "sfx")
            "tackle" -> {
                playTone(180f, 60f, 0.18f, "sawtooth", 0.12f, "sfx")
                playNoise(0.1f, 0.1f, 400f, "sfx")
            }
            "goal" -> {
                playNoise(1.4f, 0.25f, 1200f, "crowdLoop")
                playTone(523f, 523f, 0.2f, "triangle", 0.22f, "crowd")
            }
            "crowd_preview" -> {
                playNoise(1.1f, 0.28f, 1100f, "crowdLoop")
                playTone(440f, 660f, 0.25f, "triangle", 0.18f, "crowdLoop")
            }
            "whistle" -> playTone(2300f, 2100f, 0.35f, "square", 0.14f, "crowd")
            "long" -> playTone(2300f, 2100f, 0.9f, "square", 0.14f, "crowd")
            "tick" -> playTone(880f, 880f, 0.05f, "square", 0.09f, "crowd")
            "sudden" -> playTone(300f, 600f, 0.5f, "sawtooth", 0.15f, "crowd")
        }
    }

    private fun resolveCategoryMultiplier(category: String): Float {
        return when (category) {
            "crowd" -> getWhistleVol()
            "crowdLoop" -> getCrowdLoopVol()
            else -> getSfxVol()
        }
    }

    private fun playTone(
        startFreq: Float,
        endFreq: Float,
        durationSec: Float,
        waveType: String,
        vol: Float,
        category: String
    ) {
        val mult = resolveCategoryMultiplier(category)
        val scaledVol = (vol * mult).coerceIn(0f, 1f)
        if (scaledVol <= 0.001f) return

        scope.launch {
            try {
                val numSamples = max(64, (sampleRate * durationSec).toInt())
                val pcm = ShortArray(numSamples)
                val f0 = max(20f, startFreq)
                val f1 = max(20f, if (endFreq <= 0f) startFreq else endFreq)
                var phase = 0.0

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    val freq = f0 * (f1 / f0).pow(t)
                    val gain = scaledVol * (0.001f / scaledVol).pow(t)
                    phase += (freq.toDouble() / sampleRate)
                    val p = phase - phase.toInt()
                    val sample = when (waveType) {
                        "square" -> if (p < 0.5) 1.0 else -1.0
                        "sawtooth" -> 2.0 * p - 1.0
                        "triangle" -> 1.0 - 4.0 * abs(p - 0.5)
                        else -> sin(2.0 * PI * p)
                    }
                    val v = (sample * gain * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    pcm[i] = v.toShort()
                }
                outputPcm(pcm, durationSec)
            } catch (_: Throwable) {
                // Ignore audio output failures in headless/test environments
            }
        }
    }

    private fun playNoise(
        durationSec: Float,
        vol: Float,
        centerFreq: Float,
        category: String
    ) {
        val mult = resolveCategoryMultiplier(category)
        val scaledVol = (vol * mult).coerceIn(0f, 1f)
        if (scaledVol <= 0.001f) return

        scope.launch {
            try {
                val numSamples = max(64, (sampleRate * durationSec).toInt())
                val pcm = ShortArray(numSamples)
                // Simple 1-pole resonant filter approximation around centerFreq
                val omega = 2.0 * PI * min(centerFreq.toDouble(), sampleRate * 0.4) / sampleRate
                val alpha = sin(omega) * 0.35
                var y1 = 0.0
                var y2 = 0.0
                val attackSamples = max(1, (sampleRate * min(0.12f, durationSec * 0.3f)).toInt())

                for (i in 0 until numSamples) {
                    val raw = Random.nextDouble(-1.0, 1.0)
                    val filtered = alpha * raw + (1.0 - alpha) * y1 - 0.25 * y2
                    y2 = y1
                    y1 = filtered
                    val gain = if (i < attackSamples) {
                        scaledVol * (i.toFloat() / attackSamples)
                    } else {
                        val decayT = (i - attackSamples).toFloat() / max(1, numSamples - attackSamples)
                        scaledVol * exp(-3.5f * decayT)
                    }
                    val v = (filtered * gain * 2.2 * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    pcm[i] = v.toShort()
                }
                outputPcm(pcm, durationSec)
            } catch (_: Throwable) {
                // Ignore audio output failures in headless/test environments
            }
        }
    }

    private suspend fun outputPcm(pcm: ShortArray, durationSec: Float) {
        val byteCount = pcm.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(byteCount, max(minBuf, 1024))
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.play()
        delay((durationSec * 1000L).toLong() + 80L)
        track.stop()
        track.release()
    }
}
