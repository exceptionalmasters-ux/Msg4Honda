package pl.msg4honda

import android.content.Context
import android.os.Handler
import android.os.Looper

object MessageDisplayCoordinator {
    private const val AUTO_ADVANCE_MS = 2_000L
    private const val LAST_PAGE_TIME_MS = 5_000L
    private const val NAVIGATION_TIME_MS = 5_000L
    private const val MAX_RECENT_KEYS = 100
    private const val MAX_MESSAGE_HISTORY = 20

    private enum class DisplayKind { MESSAGES, NAVIGATION }

    private val handler = Handler(Looper.getMainLooper())
    private val navigationPolicy = NavigationDisplayPolicy()
    private val pages = mutableListOf<HondaDisplayText>()
    private val messageHistory = mutableListOf<List<HondaDisplayText>>()
    private val recentKeys = LinkedHashSet<String>()
    private var currentPage = 0
    private var mediaSession: HondaMediaSession? = null
    private var displayKind: DisplayKind? = null
    private var currentNavigation: HondaDisplayText? = null
    private var replayingHistory = false
    private var pendingAction: Runnable? = null

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

        val newPages = MessageFormatter.formatPages(sender, message, source)
        messageHistory += newPages
        while (messageHistory.size > MAX_MESSAGE_HISTORY) {
            messageHistory.removeAt(0)
        }

        val interruptedReplay = replayingHistory
        if (interruptedReplay) {
            pages.clear()
            pages += newPages
            currentPage = 0
            replayingHistory = false
        } else {
            pages += newPages
        }
        handler.post {
            activateMessages(
                context = context.applicationContext,
                forceRefresh = interruptedReplay,
            )
        }
    }

    @Synchronized
    fun showNavigation(context: Context, navigation: HondaDisplayText) {
        if (!AppState.isEnabled(context)) return
        handler.post { activateNavigation(context.applicationContext, navigation) }
    }

    @Synchronized
    fun showWelcome(context: Context, welcome: HondaDisplayText) {
        if (!AppState.isEnabled(context)) return
        handler.post { activateWelcome(context.applicationContext, welcome) }
    }

    @Synchronized
    fun showRoadEvent(context: Context, roadEvent: String) {
        if (!AppState.isEnabled(context)) return
        handler.post { activateRoadEvent(roadEvent) }
    }

    @Synchronized
    fun stop(context: Context) {
        cancelPendingAction()
        clearActiveMessages()
        messageHistory.clear()
        displayKind = null
        currentNavigation = null
        navigationPolicy.reset()
        mediaSession?.hide()
        mediaSession?.release()
        mediaSession = null
    }

    @Synchronized
    private fun activateMessages(context: Context, forceRefresh: Boolean = false) {
        if (pages.isEmpty() || !AppState.isEnabled(context)) return

        if (displayKind != DisplayKind.MESSAGES || forceRefresh) {
            if (displayKind == DisplayKind.NAVIGATION) navigationPolicy.expireCurrent()
            if (!forceRefresh) currentPage = 0
            displayKind = DisplayKind.MESSAGES
            currentNavigation = null
            getSession(context).showMessage(pages[currentPage])
        }
        scheduleMessageAction()
    }

    @Synchronized
    private fun activateWelcome(context: Context, welcome: HondaDisplayText) {
        if (!AppState.isEnabled(context)) return
        clearActiveMessages()
        messageHistory.clear()
        pages += welcome
        currentPage = 0
        displayKind = DisplayKind.MESSAGES
        currentNavigation = null
        navigationPolicy.reset()
        getSession(context).showMessage(welcome)
        scheduleMessageAction()
    }

    @Synchronized
    private fun activateNavigation(context: Context, navigation: HondaDisplayText) {
        if (!AppState.isEnabled(context) || pages.isNotEmpty()) return

        val decision = navigationPolicy.evaluate(navigation)
        if (!decision.shouldDisplay) return

        displayKind = DisplayKind.NAVIGATION
        currentNavigation = navigation
        getSession(context).showMessage(navigation)
        if (decision.restartTimeout) scheduleNavigationExpiry()
    }

    @Synchronized
    private fun activateRoadEvent(roadEvent: String) {
        if (displayKind != DisplayKind.NAVIGATION) return
        val navigation = currentNavigation?.copy(messageLine2 = roadEvent.take(23)) ?: return
        val decision = navigationPolicy.evaluate(navigation)
        if (!decision.shouldDisplay) return

        currentNavigation = navigation
        mediaSession?.showMessage(navigation)
        if (decision.restartTimeout) scheduleNavigationExpiry()
    }

    private fun getSession(context: Context): HondaMediaSession =
        mediaSession ?: HondaMediaSession(
            context = context,
            onNext = { handleNext() },
            onPrevious = { handlePrevious() },
        ).also { mediaSession = it }

    @Synchronized
    private fun handleNext() {
        when (displayKind) {
            DisplayKind.MESSAGES -> {
                if (replayingHistory) moveTowardNewest() else moveNext()
            }
            DisplayKind.NAVIGATION -> mediaSession?.skipSpotifyNext()
            null -> Unit
        }
    }

    @Synchronized
    private fun handlePrevious() {
        when (displayKind) {
            DisplayKind.MESSAGES -> {
                if (replayingHistory) moveTowardOlder() else movePrevious()
            }
            DisplayKind.NAVIGATION -> showNewestMessageHistory()
            null -> Unit
        }
    }

    @Synchronized
    private fun showNewestMessageHistory() {
        if (displayKind != DisplayKind.NAVIGATION || messageHistory.isEmpty()) return

        cancelPendingAction()
        navigationPolicy.expireCurrent()
        pages.clear()
        messageHistory.asReversed().forEach(pages::addAll)
        currentPage = 0
        replayingHistory = true
        displayKind = DisplayKind.MESSAGES
        currentNavigation = null
        mediaSession?.showMessage(pages[currentPage])
        scheduleMessageAction()
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
    private fun moveTowardOlder() {
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
    private fun moveTowardNewest() {
        if (pages.isEmpty()) return

        if (currentPage == 0) {
            expireMessages()
            return
        }

        currentPage -= 1
        mediaSession?.showMessage(pages[currentPage])
        scheduleMessageAction()
    }

    @Synchronized
    private fun scheduleMessageAction() {
        cancelPendingAction()
        if (replayingHistory) {
            pendingAction = Runnable { expireMessages() }.also {
                handler.postDelayed(it, LAST_PAGE_TIME_MS)
            }
            return
        }

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
        val clearHistory = replayingHistory
        clearActiveMessages()
        if (clearHistory) messageHistory.clear()
        displayKind = null
        mediaSession?.hide()
    }

    @Synchronized
    private fun expireNavigation() {
        cancelPendingAction()
        if (displayKind == DisplayKind.NAVIGATION) {
            navigationPolicy.expireCurrent()
            displayKind = null
            currentNavigation = null
            mediaSession?.hide()
        }
    }

    private fun scheduleNavigationExpiry() {
        cancelPendingAction()
        pendingAction = Runnable { expireNavigation() }.also {
            handler.postDelayed(it, NAVIGATION_TIME_MS)
        }
    }

    private fun clearActiveMessages() {
        pages.clear()
        currentPage = 0
        replayingHistory = false
    }

    private fun cancelPendingAction() {
        pendingAction?.let(handler::removeCallbacks)
        pendingAction = null
    }
}
