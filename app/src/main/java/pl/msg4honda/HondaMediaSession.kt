package pl.msg4honda

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

class HondaMediaSession(
    context: Context,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    companion object {
        private const val CLEAR_METADATA_GRACE_MS = 750L
        private const val EMPTY_RADIO_FIELD = " "
        private const val SPOTIFY_PACKAGE = "com.spotify.music"
    }

    private val applicationContext = context.applicationContext
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mediaSessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private var silentTrack: AudioTrack? = null
    private var stateGeneration = 0

    private val session = MediaSession(context, "Msg4Honda").apply {
        setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS,
        )
        setPlaybackToLocal(audioAttributes)
        setCallback(
            object : MediaSession.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    @Suppress("DEPRECATION")
                    val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                        ?: return super.onMediaButtonEvent(mediaButtonIntent)

                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (event.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_NEXT -> onNext()
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> onPrevious()
                            else -> return super.onMediaButtonEvent(mediaButtonIntent)
                        }
                    }
                    return true
                }

                override fun onSkipToNext() = onNext()

                override fun onSkipToPrevious() = onPrevious()
            },
            mainHandler,
        )
    }

    fun showMessage(displayText: HondaDisplayText) {
        stateGeneration += 1
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
        val hideGeneration = ++stateGeneration
        val spotifySnapshot = spotifySnapshot()

        if (spotifySnapshot != null) {
            // Deactivation alone does not make Spotify resend its current
            // metadata. Mirror it once through the session already selected by
            // the car, then deactivate Msg4Honda without releasing its token.
            session.setMetadata(spotifySnapshot.metadata)
            session.setPlaybackState(spotifySnapshot.playbackState)
        } else {
            // When no Spotify session is available, clear the stale message so
            // it does not remain on the radio indefinitely.
            session.setMetadata(emptyMetadata())
            session.setPlaybackState(stoppedPlaybackState())
        }
        stopSilentPlayback()
        mainHandler.postDelayed({
            if (stateGeneration == hideGeneration) {
                session.isActive = false
            }
        }, CLEAR_METADATA_GRACE_MS)
    }

    fun release() {
        stateGeneration += 1
        stopSilentPlayback()
        session.isActive = false
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

    private fun spotifySnapshot(): SpotifySnapshot? = runCatching {
        val listener = ComponentName(applicationContext, MessageNotificationListener::class.java)
        val controller = mediaSessionManager.getActiveSessions(listener)
            .firstOrNull { it.packageName == SPOTIFY_PACKAGE }
            ?: return@runCatching null
        val source = controller.metadata ?: return@runCatching null
        val title = source.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?.takeIf(String::isNotBlank)
            ?: source.description.title?.toString()?.takeIf(String::isNotBlank)
            ?: return@runCatching null
        val artist = source.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?.takeIf(String::isNotBlank)
            ?: source.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?.takeIf(String::isNotBlank)
            ?: source.description.subtitle?.toString()?.takeIf(String::isNotBlank)
            ?: EMPTY_RADIO_FIELD
        val album = source.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?.takeIf(String::isNotBlank)
            ?: source.description.description?.toString()?.takeIf(String::isNotBlank)
            ?: EMPTY_RADIO_FIELD
        val spotifyState = controller.playbackState

        SpotifySnapshot(
            metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .build(),
            playbackState = PlaybackState.Builder()
                .setState(
                    spotifyState?.state ?: PlaybackState.STATE_PLAYING,
                    spotifyState?.position ?: PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                    spotifyState?.playbackSpeed ?: 1f,
                )
                .setActions(0L)
                .build(),
        )
    }.getOrNull()

    private fun emptyMetadata(): MediaMetadata = MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_TITLE, EMPTY_RADIO_FIELD)
        .putString(MediaMetadata.METADATA_KEY_ALBUM, EMPTY_RADIO_FIELD)
        .putString(MediaMetadata.METADATA_KEY_ARTIST, EMPTY_RADIO_FIELD)
        .build()

    private fun stoppedPlaybackState(): PlaybackState = PlaybackState.Builder()
        .setState(PlaybackState.STATE_STOPPED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0f)
        .setActions(0L)
        .build()

    private data class SpotifySnapshot(
        val metadata: MediaMetadata,
        val playbackState: PlaybackState,
    )
}
