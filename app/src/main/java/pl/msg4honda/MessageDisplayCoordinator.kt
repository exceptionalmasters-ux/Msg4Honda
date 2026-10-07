package pl.msg4honda

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

object MessageDisplayCoordinator {
    private const val AUTO_ADVANCE_MS = 2_000L
    private const val LAST_PAGE_TIME_MS = 5_000L
    private const val NAVIGATION_TIME_MS = 5_000L
    private const val SESSION_RELEASE_DELAY_MS = 1_000L
    private const val MAX_RECENT_KEYS = 100

    private enum class DisplayKind { MESSAGES, NAVIGATION }

    private val handler = Handler(Looper.getMainLooper())
    private val pages = mutableListOf<HondaDisplayText>()
    private val recentKeys = LinkedHashSet<String>()
    private var currentPage = 0
    private var mediaSession: HondaMediaSession? = null
    private var displayKind: DisplayKind? = null
    private var pendingAction: Runnable? = null
    private var pendingSessionRelease: Runnable? = null

    @Synchronized
    fun enqueue(
        context: Context,
        notificationKey: String,
        sender: String,
        message: String,
        source: String = "WhatsApp",
    ) {
        if (!AppState.isEnabled(context) || !recentKeys.add(notificationKey)) return

        while (recentKeys.size > MAX_RECENT_KEYS) {
            recentKeys.remove(recentKeys.first())
        }

        pages += MessageFormatter.formatPages(sender, message, source)
        handler.post { activateMessages(context.applicationContext) }
    }

    @Synchronized
    fun showNavigation(context: Context, navigation: HondaDisplayText) {
        if (!AppState.isEnabled(context)) return
        handler.post { activateNavigation(context.applicationContext, navigation) }
    }

    @Synchronized
    fun stop(context: Context) {
        cancelPendingAction()
        cancelPendingSessionRelease()
        pages.clear()
        currentPage = 0
        displayKind = null
        mediaSession?.hide()
        mediaSession?.release()
        mediaSession = null
    }

    @Synchronized
    private fun activateMessages(context: Context) {
        if (pages.isEmpty() || !AppState.isEnabled(context)) return

        if (displayKind != DisplayKind.MESSAGES) {
            currentPage = 0
            displayKind = DisplayKind.MESSAGES
            getSession(context).showMessage(pages[currentPage])
        }
        scheduleMessageAction()
    }

    @Synchronized
    private fun activateNavigation(context: Context, navigation: HondaDisplayText) {
        if (!AppState.isEnabled(context) || pages.isNotEmpty()) return

        displayKind = DisplayKind.NAVIGATION
        getSession(context).showMessage(navigation)
        cancelPendingAction()
        pendingAction = Runnable { expireNavigation() }.also {
            handler.postDelayed(it, NAVIGATION_TIME_MS)
        }
    }

    private fun getSession(context: Context): HondaMediaSession =
        mediaSession.also { cancelPendingSessionRelease() } ?: HondaMediaSession(
            context = context,
            onNext = { handleNext() },
            onPrevious = { handlePrevious() },
        ).also { mediaSession = it }

    @Synchronized
    private fun handleNext() {
        when (displayKind) {
            DisplayKind.MESSAGES -> moveNext()
            DisplayKind.NAVIGATION -> dismissNavigationAndForward(KeyEvent.KEYCODE_MEDIA_NEXT)
            null -> Unit
        }
    }

    @Synchronized
    private fun handlePrevious() {
        when (displayKind) {
            DisplayKind.MESSAGES -> movePrevious()
            DisplayKind.NAVIGATION -> dismissNavigationAndForward(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            null -> Unit
        }
    }

    @Synchronized
    private fun moveNext() {
        if (pages.isEmpty()) return

        if (currentPage == pages.lastIndex) {
            expireMessages()
            return
        }

        currentPage += 1
        mediaSession?.showMessage(pages[currentPage])
        scheduleMessageAction()
    }

    @Synchronized
    private fun movePrevious() {
        if (pages.isEmpty()) return

        if (currentPage > 0) currentPage -= 1
        mediaSession?.showMessage(pages[currentPage])
        scheduleMessageAction()
    }

    @Synchronized
    private fun dismissNavigationAndForward(keyCode: Int) {
        if (displayKind != DisplayKind.NAVIGATION) return

        cancelPendingAction()
        displayKind = null
        mediaSession?.hide()
        mediaSession?.forwardMediaKey(keyCode)
        scheduleSessionRelease()
    }

    @Synchronized
    private fun scheduleMessageAction() {
        cancelPendingAction()
        val delay = if (currentPage < pages.lastIndex) AUTO_ADVANCE_MS else LAST_PAGE_TIME_MS
        pendingAction = Runnable { advanceMessagesAutomatically() }.also {
            handler.postDelayed(it, delay)
        }
    }

    @Synchronized
    private fun advanceMessagesAutomatically() {
        if (displayKind != DisplayKind.MESSAGES || pages.isEmpty()) return

        if (currentPage < pages.lastIndex) {
            currentPage += 1
            mediaSession?.showMessage(pages[currentPage])
            scheduleMessageAction()
        } else {
            expireMessages()
        }
    }

    @Synchronized
    private fun expireMessages() {
        cancelPendingAction()
        pages.clear()
        currentPage = 0
        displayKind = null
        mediaSession?.hide()
        scheduleSessionRelease()
    }

    @Synchronized
    private fun expireNavigation() {
        cancelPendingAction()
        if (displayKind == DisplayKind.NAVIGATION) {
            displayKind = null
            mediaSession?.hide()
            scheduleSessionRelease()
        }
    }

    @Synchronized
    private fun scheduleSessionRelease() {
        cancelPendingSessionRelease()
        val sessionToRelease = mediaSession ?: return
        pendingSessionRelease = Runnable {
            releaseSessionIfIdle(sessionToRelease)
        }.also { handler.postDelayed(it, SESSION_RELEASE_DELAY_MS) }
    }

    @Synchronized
    private fun releaseSessionIfIdle(sessionToRelease: HondaMediaSession) {
        pendingSessionRelease = null
        if (mediaSession !== sessionToRelease || displayKind != null || pages.isNotEmpty()) return

        sessionToRelease.release()
        mediaSession = null
    }

    private fun cancelPendingAction() {
        pendingAction?.let(handler::removeCallbacks)
        pendingAction = null
    }

    private fun cancelPendingSessionRelease() {
        pendingSessionRelease?.let(handler::removeCallbacks)
        pendingSessionRelease = null
    }
}
