package com.twophones.callforward.audio

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log

class AudioRouting(private val audioManager: AudioManager) {

    companion object {
        private const val TAG = "AudioRouting"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_COUNT = 1
        private const val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
        const val DEVICE_EARPIECE = 1
        const val DEVICE_SPEAKER = 2
    }

    private var mediaRecorder: MediaRecorder? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var isPlaying = false

    init {
        setupAudioMode()
    }

    private fun setupAudioMode() {
        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManager.isSpeakerphoneOn = false
    }

    fun startRecording(outputPath: String): Boolean {
        return try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputPath)
                prepare()
                start()
            }
            isRecording = true
            Log.d(TAG, "Recording started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            false
        }
    }

    fun stopRecording(): Boolean {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            Log.d(TAG, "Recording stopped")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            false
        }
    }

    fun startPlayback(audioData: ByteArray): Boolean {
        return try {
            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                AUDIO_FORMAT
            )

            audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                android.media.AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AUDIO_FORMAT)
                    .build(),
                maxOf(bufferSize, audioData.size),
                AudioTrack.MODE_STREAM,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            ).apply {
                play()
                write(audioData, 0, audioData.size)
            }

            isPlaying = true
            Log.d(TAG, "Playback started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback", e)
            false
        }
    }

    fun stopPlayback(): Boolean {
        return try {
            audioTrack?.apply {
                stop()
                release()
            }
            audioTrack = null
            isPlaying = false
            Log.d(TAG, "Playback stopped")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
            false
        }
    }

    fun setAudioDevice(device: Int) {
        when (device) {
            DEVICE_EARPIECE -> {
                audioManager.isSpeakerphoneOn = false
            }
            DEVICE_SPEAKER -> {
                audioManager.isSpeakerphoneOn = true
            }
        }
        Log.d(TAG, "Audio device set to: $device")
    }

    fun cleanup() {
        stopRecording()
        stopPlayback()
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
