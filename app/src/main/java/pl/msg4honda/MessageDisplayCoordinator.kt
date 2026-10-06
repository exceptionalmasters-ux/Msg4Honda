package pl.msg4honda

import android.content.Context
import android.os.Handler
import android.os.Looper

object MessageDisplayCoordinator {
    private const val AUTO_ADVANCE_MS = 2_000L
    private const val LAST_PAGE_TIME_MS = 5_000L
    private const val MAX_RECENT_KEYS = 100

    private val handler = Handler(Looper.getMainLooper())
    private val pages = mutableListOf<HondaDisplayText>()
    private val recentKeys = LinkedHashSet<String>()
    private var currentPage = 0
    private var mediaSession: HondaMediaSession? = null
    private var displaying = false
    private var pendingAction: Runnable? = null

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
        pendingAction?.let(handler::removeCallbacks)
        pendingAction = null
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
            scheduleNextAction()
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
        scheduleNextAction()
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
        scheduleNextAction()
    }

    @Synchronized
    private fun movePrevious() {
        if (!displaying || pages.isEmpty()) return

        if (currentPage > 0) currentPage -= 1
        mediaSession?.showMessage(pages[currentPage])
        scheduleNextAction()
    }

    @Synchronized
    private fun scheduleNextAction() {
        pendingAction?.let(handler::removeCallbacks)
        val delay = if (currentPage < pages.lastIndex) AUTO_ADVANCE_MS else LAST_PAGE_TIME_MS
        pendingAction = Runnable { advanceAutomatically() }.also {
            handler.postDelayed(it, delay)
        }
    }

    @Synchronized
    private fun advanceAutomatically() {
        if (!displaying || pages.isEmpty()) return

        if (currentPage < pages.lastIndex) {
            currentPage += 1
            mediaSession?.showMessage(pages[currentPage])
            scheduleNextAction()
        } else {
            expire()
        }
    }

    @Synchronized
    private fun expire() {
        pendingAction?.let(handler::removeCallbacks)
        pendingAction = null
        pages.clear()
        currentPage = 0
        displaying = false
        mediaSession?.hide()
    }
}
