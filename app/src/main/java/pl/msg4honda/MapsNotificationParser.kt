package pl.msg4honda

object MapsNotificationParser {
    private const val LINE_LENGTH = 24

    private val durationRegex = Regex(
        "(?:\\d+\\s*h\\s*)?\\d+\\s*min|\\d+\\s*(?:h|godz\\.?)(?:\\s*\\d+\\s*min)?",
        RegexOption.IGNORE_CASE,
    )
    private val distanceRegex = Regex(
        "\\d+(?:[,.]\\d+)?\\s*(?:m|km)",
        RegexOption.IGNORE_CASE,
    )
    private val maneuverRegex = Regex(
        "skręć|jedź|zjedź|zawróć|rond|trzymaj",
        RegexOption.IGNORE_CASE,
    )
    private val roadRegex = Regex(
        "(?:ul\\.|al\\.|alej[aęy]?|A\\d+|S\\d+|DK\\d+|DW\\d+).+",
        RegexOption.IGNORE_CASE,
    )
    private val targetInInstructionRegex = Regex(
        "(?:w kierunku|na|w)\\s+((?:ul\\.|al\\.|alej[aęy]?|A\\d+|S\\d+|DK\\d+|DW\\d+).*)$",
        RegexOption.IGNORE_CASE,
    )
    private val genericMapTextRegex = Regex(
        "^(?:google )?maps$|bez tytułu|nawigacja|prowadzenie do celu|trasa",
        RegexOption.IGNORE_CASE,
    )
    private val technicalTextRegex = Regex(
        "android\\.|notification\\$|com\\.google",
        RegexOption.IGNORE_CASE,
    )
    private val oldDistanceSuffixRegex = Regex(
        "\\s+-\\s+\\d+(?:[,.]\\d+)?(?:m|km)$",
        RegexOption.IGNORE_CASE,
    )

    fun parse(
        rawValues: List<String>,
        previous: HondaDisplayText? = null,
    ): HondaDisplayText? {
        val values = rawValues
            .flatMap { it.lines() }
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter(String::isNotBlank)
            .distinct()
        if (values.isEmpty()) return null

        val isTerminalUpdate = values.any {
            durationRegex.find(it)?.value?.let(::containsPositiveNumber) == false ||
                distanceRegex.find(it)?.value?.let(::containsPositiveNumber) == false
        }
        if (isTerminalUpdate) return null

        val duration = values.firstNotNullOfOrNull { durationRegex.find(it)?.value }
            ?.takeIf(::containsPositiveNumber)
        val maneuverText = values.firstOrNull { maneuverRegex.containsMatchIn(it) }
        val distance = (maneuverText?.let { distanceRegex.find(it)?.value }
            ?: values.firstNotNullOfOrNull { distanceRegex.find(it)?.value })
            ?.takeIf(::containsPositiveNumber)
        val inlineTarget = maneuverText
            ?.let { targetInInstructionRegex.find(it)?.groupValues?.getOrNull(1) }
        val explicitTarget = values.firstOrNull {
            it != maneuverText && roadRegex.containsMatchIn(it)
        }
        val fallbackTarget = values.firstOrNull {
            it != maneuverText &&
                it.any(Char::isLetter) &&
                !durationRegex.containsMatchIn(it) &&
                !distanceRegex.containsMatchIn(it) &&
                !genericMapTextRegex.containsMatchIn(it) &&
                !technicalTextRegex.containsMatchIn(it)
        }
        val newTarget = inlineTarget ?: explicitTarget ?: fallbackTarget.orEmpty()

        var action = maneuverText.orEmpty()
        duration?.let { action = action.replace(it, "", ignoreCase = true) }
        distance?.let { action = action.replace(it, "", ignoreCase = true) }
        if (newTarget.isNotEmpty()) action = action.replace(newTarget, "", ignoreCase = true)
        action = action
            .replace(Regex("(?i)^za\\s+"), "")
            .replace(Regex("(?i)\\s+(?:w kierunku|na|w)\\s*$"), "")
            .replace(Regex("(?i)\\s+za\\s*$"), "")
            .replace(Regex("(?i)\\s+przez\\s*$"), "")
            .trim(' ', '-', '•', ',', '.')
            .replaceFirstChar { it.titlecase() }

        if (action.isBlank() && newTarget.isBlank() && previous == null) return null

        val title = duration?.let { "Maps $it" } ?: previous?.title ?: "Maps"
        val target = newTarget.ifBlank { previous?.messageLine2.orEmpty() }
        val instruction = if (action.isNotBlank()) {
            formatInstruction(withArrow(action), distance)
        } else {
            val previousAction = previous?.messageLine1
                ?.replace(oldDistanceSuffixRegex, "")
                .orEmpty()
            formatInstruction(previousAction, distance)
                .ifBlank { previous?.messageLine1.orEmpty() }
        }

        return HondaDisplayText(
            title = title,
            messageLine1 = instruction.ifBlank { "Nawigacja" }.take(LINE_LENGTH),
            messageLine2 = target.take(LINE_LENGTH),
        )
    }

    private fun withArrow(action: String): String = when {
        action.contains("w lewo", ignoreCase = true) -> "<- $action"
        action.contains("w prawo", ignoreCase = true) -> "-> $action"
        action.contains("prosto", ignoreCase = true) -> "^ $action"
        else -> action
    }

    private fun formatInstruction(action: String, distance: String?): String {
        val compactDistance = distance?.replace(" ", "") ?: return action.take(LINE_LENGTH)
        val suffix = " - $compactDistance"
        return action.take((LINE_LENGTH - suffix.length).coerceAtLeast(0)) + suffix
    }

    private fun containsPositiveNumber(value: String): Boolean = Regex("\\d+")
        .findAll(value)
        .any { match -> match.value.toIntOrNull()?.let { it > 0 } == true }
}
