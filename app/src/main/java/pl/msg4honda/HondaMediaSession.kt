package pl.msg4honda

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState

class HondaMediaSession(context: Context) {
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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

        val displayText = MessageFormatter.format(sender, message)

        session.isActive = true
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, displayText.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, displayText.messageLine1)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, displayText.messageLine2)
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
    }

    fun release() {
        stopSilentPlayback()
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
