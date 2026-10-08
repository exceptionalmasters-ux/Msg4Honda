package pl.msg4honda

object MapsSpeedLimitReader {
    data class NodeText(
        val viewId: String?,
        val text: String?,
        val contentDescription: String?,
    )

    private val labelledLimitRegex = Regex(
        "(?:ograniczenie(?: prędkości)?|dozwolona prędkość|speed limit)" +
            "[^0-9]{0,24}(\\d{1,3})",
        RegexOption.IGNORE_CASE,
    )
    private val standaloneNumberRegex = Regex("^\\s*(\\d{1,3})\\s*(?:km/?h)?\\s*$", RegexOption.IGNORE_CASE)
    private val speedIdRegex = Regex("speed.*limit|limit.*speed|speed_limit", RegexOption.IGNORE_CASE)

    fun find(nodes: List<NodeText>): Int? {
        nodes.forEach { node ->
            listOfNotNull(node.text, node.contentDescription).forEach { value ->
                labelledLimitRegex.find(value)?.groupValues?.getOrNull(1)
                    ?.toIntOrNull()
                    ?.takeIf(::isPlausibleLimit)
                    ?.let { return it }
            }
        }

        nodes.forEach { node ->
            if (!speedIdRegex.containsMatchIn(node.viewId.orEmpty())) return@forEach
            listOfNotNull(node.text, node.contentDescription).forEach { value ->
                standaloneNumberRegex.matchEntire(value)?.groupValues?.getOrNull(1)
                    ?.toIntOrNull()
                    ?.takeIf(::isPlausibleLimit)
                    ?.let { return it }
            }
        }
        return null
    }

    private fun isPlausibleLimit(value: Int): Boolean = value in 5..160
}
