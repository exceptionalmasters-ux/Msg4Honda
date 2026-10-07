package pl.msg4honda

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

class HondaMediaSession(
    context: Context,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
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
        setCallback(
            object : MediaSession.Callback() {
                override fun onSkipToNext() = onNext()

                override fun onSkipToPrevious() = onPrevious()
            },
            mainHandler,
        )
    }

    fun showMessage(displayText: HondaDisplayText) {
        stopSilentPlayback()

        session.isActive = true
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, displayText.title)
                // The Civic IX displays album above artist, so these two fields
                // are intentionally opposite to their visual line numbers.
                .putString(
                    MediaMetadata.METADATA_KEY_ALBUM,
                    displayText.messageLine1.ifBlank { "-" },
                )
                .putString(
                    MediaMetadata.METADATA_KEY_ARTIST,
                    displayText.messageLine2.ifBlank { "-" },
                )
                .build(),
        )
        session.setPlaybackState(
            PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .setActions(
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS,
                )
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

    fun forwardMediaKey(keyCode: Int) {
        mainHandler.postDelayed({
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }, 150L)
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
