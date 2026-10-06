package pl.msg4honda

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MessageNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!AppState.isEnabled(this)) return

        val source = MessageSource.fromPackage(sbn.packageName) ?: return
        if (!AppState.isSourceEnabled(this, source)) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

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
}
