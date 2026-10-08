package pl.msg4honda

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ScrollView
import android.widget.TextView
import android.view.accessibility.AccessibilityManager

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var startStopButton: Button
    private lateinit var permissionHint: TextView
    private lateinit var whatsAppToggle: CheckBox
    private lateinit var messengerToggle: CheckBox
    private lateinit var smsToggle: CheckBox
    private lateinit var mapsToggle: CheckBox
    private lateinit var mapsDebugButton: Button
    private lateinit var speedAccessButton: Button
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
        mapsDebugButton = findViewById(R.id.mapsDebugButton)
        speedAccessButton = findViewById(R.id.speedAccessButton)

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
        mapsDebugButton.setOnClickListener { showMapsDebug() }
        speedAccessButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
        speedAccessButton.setText(
            if (hasSpeedLimitAccess()) R.string.speed_access_on else R.string.speed_access_off,
        )
    }

    private fun hasSpeedLimitAccess(): Boolean {
        val manager = getSystemService(AccessibilityManager::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val service = info.resolveInfo.serviceInfo
                service.packageName == packageName &&
                    service.name == MapsAccessibilityService::class.java.name
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

    private fun showMapsDebug() {
        val debugText = AppState.mapsDebug(this)
            .ifBlank { getString(R.string.maps_debug_empty) }
        val textView = TextView(this).apply {
            text = debugText
            setTextIsSelectable(true)
            setPadding(32, 16, 32, 16)
            textSize = 12f
        }
        val scrollView = ScrollView(this).apply { addView(textView) }

        AlertDialog.Builder(this)
            .setTitle(R.string.maps_debug_title)
            .setView(scrollView)
            .setNeutralButton(R.string.copy) { _, _ ->
                getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("Dane Maps", debugText))
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }
}
