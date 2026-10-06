package pl.msg4honda

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var startStopButton: Button
    private lateinit var permissionHint: TextView
    private lateinit var whatsAppToggle: CheckBox
    private lateinit var messengerToggle: CheckBox
    private lateinit var smsToggle: CheckBox
    private lateinit var mapsToggle: CheckBox
    private var waitingForPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startStopButton = findViewById(R.id.startStopButton)
        permissionHint = findViewById(R.id.permissionHint)
        whatsAppToggle = findViewById(R.id.whatsAppToggle)
        messengerToggle = findViewById(R.id.messengerToggle)
        smsToggle = findViewById(R.id.smsToggle)
        mapsToggle = findViewById(R.id.mapsToggle)

        bindSourceToggle(whatsAppToggle, MessageSource.WHATSAPP)
        bindSourceToggle(messengerToggle, MessageSource.MESSENGER)
        bindSourceToggle(smsToggle, MessageSource.SMS)
        bindSourceToggle(mapsToggle, MessageSource.MAPS)

        startStopButton.setOnClickListener {
            if (AppState.isEnabled(this)) {
                AppState.setEnabled(this, false)
                MessageDisplayCoordinator.stop(this)
                renderState()
            } else if (hasNotificationAccess()) {
                enableAndTest()
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
            enableAndTest()
        }
        renderState()
    }

    private fun hasNotificationAccess(): Boolean {
        val manager = getSystemService(NotificationManager::class.java)
        val listener = ComponentName(this, MessageNotificationListener::class.java)
        return manager.isNotificationListenerAccessGranted(listener)
    }

    private fun renderState() {
        val enabled = AppState.isEnabled(this)
        val lastEvent = AppState.lastEvent(this)
        statusText.text = when {
            !enabled -> getString(R.string.status_off)
            lastEvent.isBlank() -> getString(R.string.status_on)
            else -> getString(R.string.status_on_with_event, lastEvent)
        }
        startStopButton.setText(if (enabled) R.string.stop else R.string.start)
        permissionHint.visibility = if (hasNotificationAccess()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun enableAndTest() {
        AppState.setEnabled(this, true)
        AppState.setLastEvent(this, getString(R.string.test_sent))
        MessageDisplayCoordinator.enqueue(
            context = this,
            notificationKey = "test-${System.currentTimeMillis()}",
            sender = "Msg4Honda",
            message = "Test połączenia",
            source = "Test",
        )
        renderState()
    }

    private fun bindSourceToggle(toggle: CheckBox, source: MessageSource) {
        toggle.isChecked = AppState.isSourceEnabled(this, source)
        toggle.setOnCheckedChangeListener { _, enabled ->
            AppState.setSourceEnabled(this, source, enabled)
        }
    }
}
