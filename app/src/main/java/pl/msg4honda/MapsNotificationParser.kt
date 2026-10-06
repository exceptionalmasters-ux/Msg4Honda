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

    fun parse(rawValues: List<String>): HondaDisplayText? {
        val values = rawValues
            .flatMap { it.lines() }
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter(String::isNotBlank)
            .distinct()
        if (values.isEmpty()) return null

        val duration = values.firstNotNullOfOrNull { durationRegex.find(it)?.value }
        val maneuverText = values.firstOrNull { maneuverRegex.containsMatchIn(it) }
        val distance = maneuverText?.let { distanceRegex.find(it)?.value }
            ?: values.firstNotNullOfOrNull { distanceRegex.find(it)?.value }
        val inlineTarget = maneuverText
            ?.let { targetInInstructionRegex.find(it)?.groupValues?.getOrNull(1) }
        val separateTarget = values.firstOrNull {
            it != maneuverText && roadRegex.matches(it)
        }
        val target = inlineTarget ?: separateTarget.orEmpty()

        var action = maneuverText.orEmpty()
        duration?.let { action = action.replace(it, "", ignoreCase = true) }
        distance?.let { action = action.replace(it, "", ignoreCase = true) }
        if (target.isNotEmpty()) {
            action = action.replace(target, "", ignoreCase = true)
        }
        action = action
            .replace(Regex("(?i)^za\\s+"), "")
            .replace(Regex("(?i)\\s+(?:w kierunku|na|w)\\s*$"), "")
            .replace(Regex("(?i)\\s+za\\s*$"), "")
            .trim(' ', '-', '•', ',', '.')
            .replaceFirstChar { it.titlecase() }

        val instruction = listOfNotNull(
            action.takeIf(String::isNotBlank),
            distance,
        ).joinToString(" - ").ifBlank { "Nawigacja" }

        return HondaDisplayText(
            title = duration?.let { "Maps $it" } ?: "Maps",
            messageLine1 = instruction.take(LINE_LENGTH),
            messageLine2 = target.take(LINE_LENGTH),
        )
    }
}
