package pl.msg4honda

object MapsRoadEventReader {
    private val patterns = listOf(
        Regex("kontrola prędkości|fotoradar|speed (?:camera|trap)", RegexOption.IGNORE_CASE) to
            "KONTROLA PRĘDKOŚCI",
        Regex("zatrzymany pojazd|unieruchomiony pojazd|stalled vehicle|vehicle stopped", RegexOption.IGNORE_CASE) to
            "ZATRZYMANY POJAZD",
        Regex("korek|spowolnienie|traffic jam|slowdown", RegexOption.IGNORE_CASE) to "KOREK",
        Regex("policja|police", RegexOption.IGNORE_CASE) to "POLICJA",
        Regex("wypadek|kolizja|crash|accident", RegexOption.IGNORE_CASE) to "WYPADEK",
        Regex("roboty drogowe|roadworks?|construction", RegexOption.IGNORE_CASE) to "ROBOTY DROGOWE",
        Regex("zamknięty pas|lane clos", RegexOption.IGNORE_CASE) to "ZAMKNIĘTY PAS",
        Regex("przeszkoda|obiekt na drodze|object on (?:the )?road", RegexOption.IGNORE_CASE) to
            "PRZESZKODA NA DRODZE",
        Regex("zalana droga|flooded road", RegexOption.IGNORE_CASE) to "ZALANA DROGA",
        Regex("słaba widoczność|poor visibility", RegexOption.IGNORE_CASE) to "SŁABA WIDOCZNOŚĆ",
        Regex("nieodśnieżona droga|unplowed road", RegexOption.IGNORE_CASE) to
            "NIEODŚNIEŻONA DROGA",
    )

    fun find(values: List<String>): String? {
        val matches = patterns
            .mapNotNull { (pattern, label) -> label.takeIf { values.any(pattern::containsMatchIn) } }
            .distinct()
        // The Google Maps report menu contains many incident labels at once.
        // Treat only one unambiguous type as an actual road alert.
        return matches.singleOrNull()
    }

    fun isKnownLabel(value: String): Boolean = patterns.any { it.second == value }
}
