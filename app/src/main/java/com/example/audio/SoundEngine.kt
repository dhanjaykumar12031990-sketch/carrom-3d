package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 100% offline procedural PCM sound & ambient music synthesizer using Android AudioTrack.
 */
class SoundEngine {
  private val sampleRate = 22050
  private val executor = Executors.newFixedThreadPool(3)

  @Volatile var enabled: Boolean = true
  @Volatile var musicEnabled: Boolean = false
    private set

  @Volatile private var lastClackMs: Long = 0L
  @Volatile private var lastRailMs: Long = 0L
  @Volatile private var lastBumperMs: Long = 0L
  @Volatile private var lastPadMs: Long = 0L
  @Volatile private var musicThread: Thread? = null
  @Volatile private var musicTrack: AudioTrack? = null

  private val pocketCoinPcm: ShortArray by lazy { synthesizePocket(isStriker = false) }
  private val pocketStrikerPcm: ShortArray by lazy { synthesizePocket(isStriker = true) }
  private val foulPcm: ShortArray by lazy { synthesizeFoul() }
  private val queenCoverPcm: ShortArray by lazy { synthesizeQueenCover() }
  private val rewindPcm: ShortArray by lazy { synthesizeRewind() }
  private val rewardPcm: ShortArray by lazy { synthesizeRewardWin() }

  fun setMusicEnabled(enable: Boolean) {
    if (musicEnabled == enable) return
    musicEnabled = enable
    if (enable) startAmbientMusic() else stopAmbientMusic()
  }

  private fun startAmbientMusic() {
    stopAmbientMusic()
    val t =
      Thread {
        try {
          val durSec = 4
          val len = sampleRate * durSec
          val pcm = ShortArray(len)
          val chord = floatArrayOf(130.81f, 196.00f, 246.94f, 293.66f) // C3-G3-B3-D4 warm royal pad
          for (i in 0 until len) {
            val time = i.toFloat() / sampleRate
            val env = (0.5f - 0.5f * kotlin.math.cos((time / durSec) * 2.0 * PI).toFloat())
            var sum = 0f
            for ((idx, freq) in chord.withIndex()) {
              val detune = 1f + idx * 0.0012f
              sum += sin(2.0 * PI * freq * detune * time).toFloat()
            }
            val sample = (sum / chord.size) * (0.045f + 0.025f * env)
            pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
          }
          val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
          val track =
            AudioTrack.Builder()
              .setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_GAME)
                  .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                  .build()
              )
              .setAudioFormat(
                AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(sampleRate)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
              )
              .setBufferSizeInBytes(max(minBuf, pcm.size * 2))
              .setTransferMode(AudioTrack.MODE_STATIC)
              .build()
          track.write(pcm, 0, pcm.size)
          track.setLoopPoints(0, pcm.size, -1)
          track.play()
          musicTrack = track
        } catch (_: Throwable) {
          // Ignore in unit tests or environments without audio hardware
        }
      }
    t.isDaemon = true
    musicThread = t
    t.start()
  }

  private fun stopAmbientMusic() {
    try {
      musicTrack?.stop()
      musicTrack?.release()
    } catch (_: Throwable) {
    } finally {
      musicTrack = null
      musicThread = null
    }
  }

  fun playClack(speed: Float) {
    if (!enabled) return
    val now = System.currentTimeMillis()
    if (now - lastClackMs < 20L) return
    lastClackMs = now
    val vol = min(1.0f, max(0.08f, speed / 15f))
    executor.execute {
      val len = (sampleRate * 0.055f).toInt()
      val pcm = ShortArray(len)
      val f0 = 720f + Random.nextFloat() * 220f
      val f1 = 165f
      var phase = 0.0
      var lp = 0f
      for (i in 0 until len) {
        val t = i.toFloat() / sampleRate
        val freq = f1 + (f0 - f1) * exp(-t * 48f)
        phase += 2.0 * PI * freq / sampleRate
        val tri = (2.0 / PI) * kotlin.math.asin(sin(phase))
        val body = tri.toFloat() * exp(-t * 62f) * 0.45f
        val rawNoise = (Random.nextFloat() * 2f - 1f) * exp(-t * 145f)
        lp += 0.42f * (rawNoise - lp)
        val sample = (body + lp * 0.65f) * vol
        pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
      }
      playPcm(pcm)
    }
  }

  fun playRail(speed: Float) {
    if (!enabled) return
    val now = System.currentTimeMillis()
    if (now - lastRailMs < 28L) return
    lastRailMs = now
    val vol = min(0.9f, max(0.06f, speed / 18f))
    executor.execute {
      val len = (sampleRate * 0.075f).toInt()
      val pcm = ShortArray(len)
      val f0 = 240f + Random.nextFloat() * 40f
      var phase = 0.0
      for (i in 0 until len) {
        val t = i.toFloat() / sampleRate
        val freq = 75f + (f0 - 75f) * exp(-t * 36f)
        phase += 2.0 * PI * freq / sampleRate
        val sample = sin(phase).toFloat() * exp(-t * 48f) * vol * 0.55f
        pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
      }
      playPcm(pcm)
    }
  }

  fun playBumper(speed: Float) {
    if (!enabled) return
    val now = System.currentTimeMillis()
    if (now - lastBumperMs < 35L) return
    lastBumperMs = now
    val vol = min(0.95f, max(0.25f, speed / 14f))
    executor.execute {
      val len = (sampleRate * 0.12f).toInt()
      val pcm = ShortArray(len)
      var phase = 0.0
      for (i in 0 until len) {
        val t = i.toFloat() / sampleRate
        val freq = 420f + 540f * exp(-t * 24f)
        phase += 2.0 * PI * freq / sampleRate
        val sample = sin(phase).toFloat() * exp(-t * 22f) * vol * 0.5f
        pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
      }
      playPcm(pcm)
    }
  }

  fun playSpeedPad() {
    if (!enabled) return
    val now = System.currentTimeMillis()
    if (now - lastPadMs < 80L) return
    lastPadMs = now
    executor.execute {
      val len = (sampleRate * 0.14f).toInt()
      val pcm = ShortArray(len)
      var phase = 0.0
      for (i in 0 until len) {
        val t = i.toFloat() / sampleRate
        val freq = 380f + t * 4200f
        phase += 2.0 * PI * freq / sampleRate
        val sample = sin(phase).toFloat() * exp(-t * 14f) * 0.28f
        pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
      }
      playPcm(pcm)
    }
  }

  fun playPocket(isStriker: Boolean) {
    if (!enabled) return
    val buf = if (isStriker) pocketStrikerPcm else pocketCoinPcm
    executor.execute { playPcm(buf) }
  }

  fun playFoul() {
    if (!enabled) return
    executor.execute { playPcm(foulPcm) }
  }

  fun playQueenCover() {
    if (!enabled) return
    executor.execute { playPcm(queenCoverPcm) }
  }

  fun playRewind() {
    if (!enabled) return
    executor.execute { playPcm(rewindPcm) }
  }

  fun playRewardWin() {
    if (!enabled) return
    executor.execute { playPcm(rewardPcm) }
  }

  fun playSpinTick() {
    if (!enabled) return
    executor.execute {
      val len = (sampleRate * 0.025f).toInt()
      val pcm = ShortArray(len)
      var phase = 0.0
      for (i in 0 until len) {
        val t = i.toFloat() / sampleRate
        phase += 2.0 * PI * 980.0 / sampleRate
        val sample = sin(phase).toFloat() * exp(-t * 95f) * 0.25f
        pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
      }
      playPcm(pcm)
    }
  }

  fun playEmote() {
    if (!enabled) return
    executor.execute {
      val len = (sampleRate * 0.09f).toInt()
      val pcm = ShortArray(len)
      var phase = 0.0
      for (i in 0 until len) {
        val t = i.toFloat() / sampleRate
        val freq = 520f + sin(t * 50f) * 160f
        phase += 2.0 * PI * freq / sampleRate
        val sample = sin(phase).toFloat() * exp(-t * 25f) * 0.26f
        pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
      }
      playPcm(pcm)
    }
  }

  fun playShoot(power: Float) {
    if (!enabled) return
    val p = max(0.1f, min(1f, power))
    executor.execute {
      val len = (sampleRate * 0.07f).toInt()
      val pcm = ShortArray(len)
      val f0 = 210f + p * 230f
      var phase = 0.0
      for (i in 0 until len) {
        val t = i.toFloat() / sampleRate
        val freq = 85f + (f0 - 85f) * exp(-t * 45f)
        phase += 2.0 * PI * freq / sampleRate
        val sample = sin(phase).toFloat() * exp(-t * 58f) * 0.35f
        pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
      }
      playPcm(pcm)
    }
  }

  fun release() {
    stopAmbientMusic()
    executor.shutdownNow()
  }

  private fun synthesizePocket(isStriker: Boolean): ShortArray {
    val dur = if (isStriker) 0.16f else 0.28f
    val len = (sampleRate * dur).toInt()
    val pcm = ShortArray(len)
    var phase1 = 0.0
    var phase2 = 0.0
    val fStart = if (isStriker) 180f else 320f
    val fEnd = if (isStriker) 70f else 115f
    for (i in 0 until len) {
      val t = i.toFloat() / sampleRate
      var sample = 0f
      if (t < 0.15f) {
        val f = fEnd + (fStart - fEnd) * exp(-t * 22f)
        phase1 += 2.0 * PI * f / sampleRate
        sample += sin(phase1).toFloat() * exp(-t * 26f) * 0.45f
      }
      if (!isStriker && t >= 0.06f) {
        val ct = t - 0.06f
        val cf = if (t < 0.12f) 587.3 else 880.0
        phase2 += 2.0 * PI * cf / sampleRate
        sample += sin(phase2).toFloat() * exp(-ct * 14f) * 0.22f
      }
      pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
    }
    return pcm
  }

  private fun synthesizeFoul(): ShortArray {
    val len = (sampleRate * 0.28f).toInt()
    val pcm = ShortArray(len)
    var phase = 0.0
    for (i in 0 until len) {
      val t = i.toFloat() / sampleRate
      val freq = 195f - (80f * (t / 0.28f))
      phase += (freq / sampleRate)
      val saw = ((phase % 1.0) * 2.0 - 1.0).toFloat()
      val sample = saw * exp(-t * 7f) * 0.22f
      pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
    }
    return pcm
  }

  private fun synthesizeQueenCover(): ShortArray {
    val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f)
    val len = (sampleRate * 0.52f).toInt()
    val pcm = ShortArray(len)
    val phases = DoubleArray(notes.size)
    for (i in 0 until len) {
      val t = i.toFloat() / sampleRate
      var sample = 0f
      for (n in notes.indices) {
        val start = n * 0.07f
        if (t >= start) {
          val nt = t - start
          phases[n] += 2.0 * PI * notes[n] / sampleRate
          sample += sin(phases[n]).toFloat() * exp(-nt * 9f) * 0.22f
        }
      }
      pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
    }
    return pcm
  }

  private fun synthesizeRewind(): ShortArray {
    val len = (sampleRate * 0.36f).toInt()
    val pcm = ShortArray(len)
    var phase = 0.0
    for (i in 0 until len) {
      val t = i.toFloat() / sampleRate
      val freq = 260f + (t / 0.36f) * 680f
      phase += 2.0 * PI * freq / sampleRate
      val env = sin((t / 0.36f) * PI).toFloat()
      val sample = sin(phase).toFloat() * env * 0.3f
      pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
    }
    return pcm
  }

  private fun synthesizeRewardWin(): ShortArray {
    val notes = floatArrayOf(587.33f, 739.99f, 880.00f, 1174.66f)
    val len = (sampleRate * 0.55f).toInt()
    val pcm = ShortArray(len)
    val phases = DoubleArray(notes.size)
    for (i in 0 until len) {
      val t = i.toFloat() / sampleRate
      var sample = 0f
      for (n in notes.indices) {
        val start = n * 0.08f
        if (t >= start) {
          val nt = t - start
          phases[n] += 2.0 * PI * notes[n] / sampleRate
          sample += sin(phases[n]).toFloat() * exp(-nt * 7f) * 0.23f
        }
      }
      pcm[i] = (max(-1f, min(1f, sample)) * 32767f).toInt().toShort()
    }
    return pcm
  }

  private fun playPcm(pcm: ShortArray) {
    try {
      val minBuf =
        AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
        )
      val bytes = max(minBuf, pcm.size * 2)
      val track =
        AudioTrack.Builder()
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
          .setBufferSizeInBytes(bytes)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()
      track.write(pcm, 0, pcm.size)
      track.play()
      Thread.sleep((pcm.size * 1000L) / sampleRate + 40L)
      track.stop()
      track.release()
    } catch (_: Throwable) {
      // Ignore in headless unit test environments without audio hardware
    }
  }
}
