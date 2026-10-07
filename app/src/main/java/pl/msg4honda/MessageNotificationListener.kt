package pl.msg4honda

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView

class MessageNotificationListener : NotificationListenerService() {
    private var lastNavigation: HondaDisplayText? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!AppState.isEnabled(this)) return

        val source = MessageSource.fromPackage(sbn.packageName) ?: return
        if (!AppState.isSourceEnabled(this, source)) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        if (source == MessageSource.MAPS) {
            handleMapsNotification(sbn)
            return
        }

        val extras = sbn.notification.extras
        val latestMessage = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.let(Notification.MessagingStyle.Message::getMessagesFromBundleArray)
            ?.lastOrNull()

        val sender = (latestMessage?.senderPerson?.name
            ?: extras.getCharSequence(Notification.EXTRA_TITLE))
            ?.toString()
            ?.trim()
            .orEmpty()
        val message = (latestMessage?.text
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString()
            ?.trim()
            .orEmpty()

        if (sender.isBlank() || message.isBlank()) return

        AppState.setLastEvent(this, "${source.displayName}: $sender")
        MessageDisplayCoordinator.enqueue(
            context = this,
            notificationKey = "${sbn.packageName}|${sbn.key}|$sender|$message",
            sender = sender,
            message = message,
            source = source.displayName,
        )
    }

    private fun handleMapsNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val values = buildList {
            // Google Maps often keeps the current manoeuvre only in its custom
            // notification layout. Read that first so a new turn replaces the
            // cached one even when the standard extras contain only a distance.
            addAll(extractRemoteViewText(sbn))
            addAll(listOfNotNull(
                extras.getCharSequence(Notification.EXTRA_TITLE),
                extras.getCharSequence(Notification.EXTRA_TEXT),
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
                extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
                extras.getCharSequence(Notification.EXTRA_INFO_TEXT),
                extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT),
                sbn.notification.tickerText,
            ).map(CharSequence::toString))
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.mapTo(this, CharSequence::toString)

            extras.keySet().forEach { key ->
                when (val value = extras.get(key)) {
                    is CharSequence -> add(value.toString())
                    is Array<*> -> value.filterIsInstance<CharSequence>()
                        .mapTo(this, CharSequence::toString)
                    is Collection<*> -> value.filterIsInstance<CharSequence>()
                        .mapTo(this, CharSequence::toString)
                }
            }
        }.map(String::trim).filter(String::isNotBlank).distinct()

        AppState.setLastEvent(this, "Maps: ${values.joinToString(" | ").take(180)}")
        val navigation = MapsNotificationParser.parse(values, lastNavigation).also {
            lastNavigation = it
        } ?: return
        MessageDisplayCoordinator.showNavigation(this, navigation)
    }

    private fun extractRemoteViewText(sbn: StatusBarNotification): List<String> {
        val sourceContext = try {
            createPackageContext(sbn.packageName, Context.CONTEXT_IGNORE_SECURITY)
        } catch (_: Exception) {
            return emptyList()
        }

        val notification = sbn.notification
        val remoteViews = buildList<RemoteViews> {
            try {
                Notification.Builder.recoverBuilder(this@MessageNotificationListener, notification)
                    .createBigContentView()
                    ?.let(::add)
            } catch (_: Exception) {
                // Some Maps versions cannot be reconstructed by recoverBuilder.
            }
            notification.bigContentView?.let(::add)
            try {
                Notification.Builder.recoverBuilder(this@MessageNotificationListener, notification)
                    .createContentView()
                    ?.let(::add)
            } catch (_: Exception) {
                // Fall back to the RemoteViews already stored on the notification.
            }
            notification.contentView?.let(::add)
        }

        return buildList {
            remoteViews.forEach { remoteView ->
                try {
                    val root = remoteView.apply(sourceContext, FrameLayout(sourceContext))
                    collectText(root, this)
                } catch (_: Exception) {
                    // A layout change must not stop notification processing.
                }
            }
        }
    }

    private fun collectText(view: View, output: MutableList<String>) {
        if (view is TextView) {
            view.text?.toString()?.trim()?.takeIf(String::isNotBlank)?.let(output::add)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collectText(view.getChildAt(index), output)
            }
        }
    }
}
