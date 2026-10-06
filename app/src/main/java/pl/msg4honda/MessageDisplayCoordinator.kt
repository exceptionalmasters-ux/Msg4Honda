package pl.msg4honda

import android.content.Context
import android.os.Handler
import android.os.Looper

object MessageDisplayCoordinator {
    private const val IDLE_TIMEOUT_MS = 5_000L
    private const val MAX_RECENT_KEYS = 100

    private val handler = Handler(Looper.getMainLooper())
    private val pages = mutableListOf<HondaDisplayText>()
    private val recentKeys = LinkedHashSet<String>()
    private var currentPage = 0
    private var mediaSession: HondaMediaSession? = null
    private var displaying = false
    private var timeout: Runnable? = null

    @Synchronized
    fun enqueue(context: Context, notificationKey: String, sender: String, message: String) {
        if (!AppState.isEnabled(context) || !recentKeys.add(notificationKey)) return

        while (recentKeys.size > MAX_RECENT_KEYS) {
            recentKeys.remove(recentKeys.first())
        }

        pages += MessageFormatter.formatPages(sender, message)
        handler.post { activate(context.applicationContext) }
    }

    @Synchronized
    fun stop(context: Context) {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        pages.clear()
        currentPage = 0
        displaying = false
        mediaSession?.hide()
        mediaSession?.release()
        mediaSession = null
    }

    @Synchronized
    private fun activate(context: Context) {
        if (pages.isEmpty() || !AppState.isEnabled(context)) return

        if (displaying) {
            restartTimeout()
            return
        }

        displaying = true
        currentPage = 0
        val session = mediaSession ?: HondaMediaSession(
            context = context,
            onNext = { moveNext() },
            onPrevious = { movePrevious() },
        ).also { mediaSession = it }
        session.showMessage(pages[currentPage])
        restartTimeout()
    }

    @Synchronized
    private fun moveNext() {
        if (!displaying || pages.isEmpty()) return

        if (currentPage == pages.lastIndex) {
            expire()
            return
        }

        currentPage += 1
        mediaSession?.showMessage(pages[currentPage])
        restartTimeout()
    }

    @Synchronized
    private fun movePrevious() {
        if (!displaying || pages.isEmpty()) return

        if (currentPage > 0) currentPage -= 1
        mediaSession?.showMessage(pages[currentPage])
        restartTimeout()
    }

    @Synchronized
    private fun restartTimeout() {
        timeout?.let(handler::removeCallbacks)
        timeout = Runnable { expire() }.also {
            handler.postDelayed(it, IDLE_TIMEOUT_MS)
        }
    }

    @Synchronized
    private fun expire() {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        pages.clear()
        currentPage = 0
        displaying = false
        mediaSession?.hide()
    }
}
