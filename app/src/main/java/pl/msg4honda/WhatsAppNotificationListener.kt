package pl.msg4honda

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WhatsAppNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!AppState.isEnabled(this) || sbn.packageName !in WHATSAPP_PACKAGES) return
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

        AppState.setLastEvent(this, "WhatsApp: $sender")

        MessageDisplayCoordinator.enqueue(
            context = this,
            notificationKey = "${sbn.key}|$sender|$message",
            sender = sender,
            message = message,
        )
    }

    companion object {
        private val WHATSAPP_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
        )
    }
}
