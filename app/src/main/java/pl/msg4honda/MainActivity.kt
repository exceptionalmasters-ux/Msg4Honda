package pl.msg4honda

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var startStopButton: Button
    private lateinit var permissionHint: TextView
    private var waitingForPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startStopButton = findViewById(R.id.startStopButton)
        permissionHint = findViewById(R.id.permissionHint)

        startStopButton.setOnClickListener {
            if (AppState.isEnabled(this)) {
                AppState.setEnabled(this, false)
                MessageDisplayCoordinator.stop(this)
                renderState()
            } else if (hasNotificationAccess()) {
                AppState.setEnabled(this, true)
                renderState()
            } else {
                waitingForPermission = true
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForPermission && hasNotificationAccess()) {
            waitingForPermission = false
            AppState.setEnabled(this, true)
        }
        renderState()
    }

    private fun hasNotificationAccess(): Boolean {
        val manager = getSystemService(NotificationManager::class.java)
        val listener = ComponentName(this, WhatsAppNotificationListener::class.java)
        return manager.isNotificationListenerAccessGranted(listener)
    }

    private fun renderState() {
        val enabled = AppState.isEnabled(this)
        statusText.setText(if (enabled) R.string.status_on else R.string.status_off)
        startStopButton.setText(if (enabled) R.string.stop else R.string.start)
        permissionHint.visibility = if (hasNotificationAccess()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
}
