package pl.msg4honda

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

class MapsAccessibilityService : AccessibilityService() {
    companion object {
        private const val MAPS_PACKAGE = "com.google.android.apps.maps"
        private const val MAX_NODES = 300
        private const val UPDATE_DELAY_MS = 250L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val update = Runnable { inspectMapsWindow() }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != MAPS_PACKAGE) return
        if (!AppState.isEnabled(this) || !AppState.isSourceEnabled(this, MessageSource.MAPS)) return
        handler.removeCallbacks(update)
        handler.postDelayed(update, UPDATE_DELAY_MS)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(update)
        super.onDestroy()
    }

    private fun inspectMapsWindow() {
        val root = rootInActiveWindow ?: return
        val nodes = readNodeTree(root)
        val speedLimit = MapsSpeedLimitReader.find(nodes)
        AppState.setMapsAccessibilityDebug(
            this,
            buildString {
                appendLine("rozpoznaneOgraniczenie=${speedLimit?.let { "$it km/h" } ?: "brak"}")
                nodes.forEach { node ->
                    append("id=${node.viewId ?: "-"}")
                    append(" text=${node.text ?: "-"}")
                    appendLine(" opis=${node.contentDescription ?: "-"}")
                }
            }.take(30_000),
        )
        if (speedLimit != null) AppState.setSpeedLimit(this, speedLimit)
    }

    private fun readNodeTree(root: AccessibilityNodeInfo): List<MapsSpeedLimitReader.NodeText> {
        val result = mutableListOf<MapsSpeedLimitReader.NodeText>()
        val pending = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }

        while (pending.isNotEmpty() && result.size < MAX_NODES) {
            val node = pending.removeFirst()
            result += MapsSpeedLimitReader.NodeText(
                viewId = node.viewIdResourceName,
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
            )
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pending::addLast)
            }
        }
        return result
    }
}
