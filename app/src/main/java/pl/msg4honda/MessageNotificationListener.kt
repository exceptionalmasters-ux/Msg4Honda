package pl.msg4honda

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

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
        val diagnostics = MapsNotificationDiagnostics.create(this, sbn)
        AppState.setMapsDebug(this, diagnostics.text)
        val extras = sbn.notification.extras
        val values = buildList {
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
        val navigation = MapsNotificationParser.parse(
            rawValues = values,
            previous = lastNavigation,
            iconManeuver = diagnostics.maneuver,
            speedLimit = AppState.speedLimit(this),
            roadEvent = AppState.roadEvent(this),
        ).also {
            lastNavigation = it
        } ?: return
        MessageDisplayCoordinator.showNavigation(this, navigation)
    }
}
