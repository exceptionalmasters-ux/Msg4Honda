package pl.msg4honda

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState

class HondaMediaSession(context: Context) {
    private val session = MediaSession(context, "Msg4Honda").apply {
        setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS,
        )
    }

    fun showMessage(sender: String, message: String) {
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
        // Do not request audio focus: Spotify should keep playing.
        session.isActive = true
    }

    fun hide() {
        session.isActive = false
    }

    fun release() {
        session.release()
    }
}
