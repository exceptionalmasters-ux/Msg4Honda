package pl.msg4honda

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.ArrayDeque

data class IncomingMessage(
    val notificationKey: String,
    val sender: String,
    val text: String,
)

object MessageDisplayCoordinator {
    // Adjust after the first real test in the Honda.
    private const val DISPLAY_TIME_MS = 2_500L
    private const val BETWEEN_MESSAGES_MS = 400L
    private const val MAX_RECENT_KEYS = 100

    private val handler = Handler(Looper.getMainLooper())
    private val queue = ArrayDeque<IncomingMessage>()
    private val recentKeys = LinkedHashSet<String>()
    private var mediaSession: HondaMediaSession? = null
    private var displaying = false

    @Synchronized
    fun enqueue(context: Context, notificationKey: String, sender: String, message: String) {
        if (!AppState.isEnabled(context) || !recentKeys.add(notificationKey)) return

        while (recentKeys.size > MAX_RECENT_KEYS) {
            recentKeys.remove(recentKeys.first())
        }

        queue.addLast(
            IncomingMessage(
                notificationKey = notificationKey,
                sender = sender,
                text = message,
            ),
        )

        handler.post { showNext(context.applicationContext) }
    }

    @Synchronized
    fun stop(context: Context) {
        handler.removeCallbacksAndMessages(null)
        queue.clear()
        displaying = false
        mediaSession?.hide()
        mediaSession?.release()
        mediaSession = null
    }

    @Synchronized
    private fun showNext(context: Context) {
        if (displaying || queue.isEmpty() || !AppState.isEnabled(context)) return

        val next = queue.removeFirst()
        displaying = true
        val session = mediaSession ?: HondaMediaSession(context).also { mediaSession = it }
        session.showMessage(next.sender, next.text)

        handler.postDelayed({
            synchronized(this) {
                session.hide()
                displaying = false
            }
            handler.postDelayed({ showNext(context) }, BETWEEN_MESSAGES_MS)
        }, DISPLAY_TIME_MS)
    }
}
