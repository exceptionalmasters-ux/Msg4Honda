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
    private val recentEvents = ArrayDeque<String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != MAPS_PACKAGE) return
        if (!AppState.isEnabled(this) || !AppState.isSourceEnabled(this, MessageSource.MAPS)) return
        inspectAccessibilityEvent(event)
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
        val roadEvent = MapsRoadEventReader.find(nodes.flatMap(::nodeValues))
        AppState.setMapsAccessibilityDebug(
            this,
            buildString {
                appendLine("rozpoznaneOgraniczenie=${speedLimit?.let { "$it km/h" } ?: "brak"}")
                appendLine("rozpoznaneZdarzenie=${roadEvent ?: "brak"}")
                nodes.forEach { node ->
                    append("id=${node.viewId ?: "-"}")
                    append(" text=${node.text ?: "-"}")
                    appendLine(" opis=${node.contentDescription ?: "-"}")
                }
            }.take(30_000),
        )
        if (speedLimit != null) AppState.setSpeedLimit(this, speedLimit)
        roadEvent?.let(::handleRoadEvent)
    }

    private fun inspectAccessibilityEvent(event: AccessibilityEvent) {
        val source = runCatching { event.source }.getOrNull()
        val values = buildList {
            event.text.mapNotNullTo(this) { it?.toString()?.takeIf(String::isNotBlank) }
            event.contentDescription?.toString()?.takeIf(String::isNotBlank)?.let(::add)
            source?.text?.toString()?.takeIf(String::isNotBlank)?.let(::add)
            source?.contentDescription?.toString()?.takeIf(String::isNotBlank)?.let(::add)
        }.distinct()
        val roadEvent = MapsRoadEventReader.find(values)
        recentEvents += buildString {
            append(AccessibilityEvent.eventTypeToString(event.eventType))
            append(" | ")
            append(values.joinToString(" | ").ifBlank { "bez tekstu" })
            append(" | rozpoznane=")
            append(roadEvent ?: "brak")
        }
        while (recentEvents.size > 40) recentEvents.removeFirst()
        AppState.setMapsAccessibilityEventDebug(this, recentEvents.joinToString("\n"))
        roadEvent?.let(::handleRoadEvent)
    }

    private fun handleRoadEvent(roadEvent: String) {
        if (AppState.recordRoadEvent(this, roadEvent)) {
            MessageDisplayCoordinator.showRoadEvent(this, roadEvent)
        }
    }

    private fun nodeValues(node: MapsSpeedLimitReader.NodeText): List<String> =
        listOfNotNull(node.text, node.contentDescription)

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
