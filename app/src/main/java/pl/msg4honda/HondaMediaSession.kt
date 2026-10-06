package pl.msg4honda

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState

class HondaMediaSession(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener { }
        .setWillPauseWhenDucked(true)
        .build()
    private var silentTrack: AudioTrack? = null

    private val session = MediaSession(context, "Msg4Honda").apply {
        setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS,
        )
        setPlaybackToLocal(audioAttributes)
    }

    fun showMessage(sender: String, message: String) {
        stopSilentPlayback()
        audioManager.requestAudioFocus(focusRequest)

        session.isActive = true
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, message)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, sender)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "WhatsApp")
                .build(),
        )
        session.setPlaybackState(
            PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .setActions(0L)
                .build(),
        )
        startSilentPlayback()
    }

    fun hide() {
        session.setPlaybackState(
            PlaybackState.Builder()
                .setState(PlaybackState.STATE_STOPPED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0f)
                .setActions(0L)
                .build(),
        )
        session.isActive = false
        stopSilentPlayback()
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    fun release() {
        stopSilentPlayback()
        audioManager.abandonAudioFocusRequest(focusRequest)
        session.release()
    }

    private fun startSilentPlayback() {
        val sampleRate = 8_000
        val channel = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minimum = AudioTrack.getMinBufferSize(sampleRate, channel, encoding)
        val bufferSize = maxOf(minimum, sampleRate * 2)

        silentTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channel)
                    .setEncoding(encoding)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .also { track ->
                track.write(ByteArray(bufferSize), 0, bufferSize)
                track.setLoopPoints(0, bufferSize / 2, -1)
                track.play()
            }
    }

    private fun stopSilentPlayback() {
        silentTrack?.let { track ->
            runCatching { track.stop() }
            track.release()
        }
        silentTrack = null
    }
}
