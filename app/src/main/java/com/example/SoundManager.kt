package com.example

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

object SoundManager {
    private const val TAG = "SoundManager"
    private const val SAMPLE_RATE = 44100
    private val scope = CoroutineScope(Dispatchers.Default)
    private var isMuted = false
    private var bgmJob: Job? = null
    private var appContext: android.content.Context? = null

    fun initialize(context: android.content.Context) {
        appContext = context.applicationContext
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stopBackgroundMusic()
        } else {
            startBackgroundMusic()
        }
    }

    fun isMuted(): Boolean = isMuted

    private fun playBuffer(buffer: ShortArray) {
        if (isMuted) return
        scope.launch {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(buffer.size, minBufferSize)
                
                val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val builder = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                    builder.build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize * 2,
                        AudioTrack.MODE_STATIC
                    )
                }

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                
                // Track release timer based on sound length plus buffer cushion
                val durationMs = (buffer.size * 1000L) / SAMPLE_RATE
                delay(durationMs + 200)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed playing synth sound: ${e.message}")
            }
        }
    }

    private fun generateTone(
        durationMs: Int,
        freqStart: Float,
        freqEnd: Float,
        waveType: String = "sine",
        volume: Float = 0.5f
    ): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        var phase = 0f
        val random = Random()

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val currentFreq = freqStart + (freqEnd - freqStart) * progress
            val deltaPhase = (2 * PI * currentFreq / SAMPLE_RATE).toFloat()
            phase += deltaPhase
            if (phase > 2 * PI) {
                phase -= (2 * PI).toFloat()
            }

            val amplitude = when (waveType) {
                "sine" -> sin(phase)
                "square" -> if (sin(phase) >= 0) 1.0f else -1.0f
                "triangle" -> {
                    val normPhase = phase / (2 * PI).toFloat()
                    if (normPhase < 0.25f) normPhase * 4f
                    else if (normPhase < 0.75f) 2.0f - normPhase * 4f
                    else (normPhase - 1.0f) * 4f
                }
                "noise" -> (random.nextFloat() * 2f - 1f)
                else -> sin(phase)
            }

            // Apply fade-out window to prevent sudden pops
            val fadeWindow = if (progress > 0.85f) {
                (1f - progress) / 0.15f
            } else if (progress < 0.05f) {
                progress / 0.05f
            } else {
                1.0f
            }

            samples[i] = (amplitude * Short.MAX_VALUE * volume * fadeWindow).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun concatenate(vararg arrays: ShortArray): ShortArray {
        val totalLength = arrays.sumOf { it.size }
        val result = ShortArray(totalLength)
        var offset = 0
        for (array in arrays) {
            System.arraycopy(array, 0, result, offset, array.size)
            offset += array.size
        }
        return result
    }

    // --- SOUND EFFECTS ---

    fun playJump() {
        // High retro triangle sweep
        val sound = generateTone(120, 200f, 650f, "triangle", 0.4f)
        playBuffer(sound)
    }

    fun playFloorCrumble() {
        // Cracking low static noise
        val crackle = generateTone(200, 150f, 50f, "noise", 0.4f)
        val rumble = generateTone(250, 90f, 40f, "square", 0.5f)
        playBuffer(concatenate(crackle, rumble))
    }

    fun playButtonTrap() {
        // Sharp beep + instant doom explosion
        val click = generateTone(40, 1000f, 1000f, "square", 0.3f)
        val boom = generateTone(400, 250f, 30f, "noise", 0.6f)
        playBuffer(concatenate(click, boom))
    }

    fun playDoorEscape() {
        // Slide sliding downwards in funny melody
        val zip = generateTone(250, 480f, 120f, "sine", 0.4f)
        playBuffer(zip)
    }

    fun playCheckpointWrong() {
        // Chime chime, then sad buzzer sliding down!
        val chime1 = generateTone(120, 523.25f, 523.25f, "sine", 0.5f) // C5
        val chime2 = generateTone(120, 659.25f, 659.25f, "sine", 0.5f) // E5
        val buzz = generateTone(500, 220f, 75f, "square", 0.6f)       // Comedic decline
        playBuffer(concatenate(chime1, chime2, buzz))
    }

    fun playGravityFlip() {
        // Cool sci-fi laser ascending with slight modulation
        val sound = generateTone(300, 250f, 850f, "sine", 0.4f)
        playBuffer(sound)
    }

    fun playEvilLaugh() {
        // Multi-segment deep raspy robotic laugh
        val laughSegment1 = generateTone(180, 180f, 90f, "square", 0.6f)
        val sil = ShortArray(1100)
        val laughSegment2 = generateTone(180, 160f, 80f, "square", 0.6f)
        val laughSegment3 = generateTone(180, 140f, 70f, "square", 0.6f)
        val laughSegment4 = generateTone(350, 120f, 50f, "square", 0.7f)
        playBuffer(concatenate(laughSegment1, sil, laughSegment2, sil, laughSegment3, sil, laughSegment4))
    }

    fun playFakeWin() {
        // Glorious arpeggio that abruptly breaks into flatline fail buzzer!
        val chime1 = generateTone(100, 261.63f, 261.63f, "sine", 0.5f) // C
        val chime2 = generateTone(100, 329.63f, 329.63f, "sine", 0.5f) // E
        val chime3 = generateTone(100, 392.00f, 392.00f, "sine", 0.5f) // G
        val chime4 = generateTone(200, 523.25f, 523.25f, "sine", 0.5f) // High C
        val click = generateTone(50, 800f, 400f, "noise", 0.5f) // Crack!
        val buzz = generateTone(400, 110f, 110f, "square", 0.6f) // Sad fail drone
        playBuffer(concatenate(chime1, chime2, chime3, chime4, click, buzz))
    }

    fun playRealWin() {
        // Full, beautiful hyper retro victory fanfare
        val t1 = generateTone(100, 261.63f, 261.63f, "triangle", 0.5f) // C4
        val t2 = generateTone(100, 329.63f, 329.63f, "triangle", 0.5f) // E4
        val t3 = generateTone(100, 392.00f, 392.00f, "triangle", 0.5f) // G4
        val t4 = generateTone(150, 523.25f, 523.25f, "triangle", 0.5f) // C5
        val t5 = generateTone(150, 392.00f, 392.00f, "triangle", 0.5f) // G4
        val t6 = generateTone(300, 523.25f, 523.25f, "sine", 0.6f)     // Sustained final C5
        playBuffer(concatenate(t1, t2, t3, t4, t5, t6))
    }

    // --- BACKGROUND MUSIC ---

    fun startBackgroundMusic() {
        if (isMuted) return
        stopBackgroundMusic()
        bgmJob = scope.launch {
            try {
                // Generates a constant low-pitch spooky dark drone and bass rhythm
                val drone = generateTone(1500, 75f, 75f, "sine", 0.2f)
                val space = ShortArray(2200) // Small space
                val highNote = generateTone(400, 120f, 110f, "sine", 0.15f)
                val spacer = ShortArray(11000)
                val track = concatenate(drone, space, highNote, spacer)
                
                val minSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(track.size * 2, minSize)
                
                val bgmTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val builder = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                    builder.build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                bgmTrack.play()
                
                while (isActive) {
                    bgmTrack.write(track, 0, track.size)
                    delay(50) // Tiny delay to allow buffering
                }
                
                try {
                    bgmTrack.stop()
                    bgmTrack.release()
                } catch (e: Exception) {}
            } catch (e: Exception) {
                Log.e(TAG, "BGM failed: ${e.message}")
            }
        }
    }

    fun stopBackgroundMusic() {
        bgmJob?.cancel()
        bgmJob = null
    }
}
