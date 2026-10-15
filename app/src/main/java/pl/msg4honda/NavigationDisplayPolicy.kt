package pl.msg4honda

internal data class NavigationDisplayDecision(
    val shouldDisplay: Boolean,
    val restartTimeout: Boolean,
)

internal class NavigationDisplayPolicy {
    private var visibleKey: String? = null
    private var hiddenKey: String? = null

    fun evaluate(navigation: HondaDisplayText): NavigationDisplayDecision {
        val key = navigationKey(navigation)
        if (key == hiddenKey) {
            return NavigationDisplayDecision(
                shouldDisplay = false,
                restartTimeout = false,
            )
        }

        val restartTimeout = key != visibleKey
        if (restartTimeout) hiddenKey = null
        visibleKey = key
        return NavigationDisplayDecision(
            shouldDisplay = true,
            restartTimeout = restartTimeout,
        )
    }

    fun hideCurrent() {
        hiddenKey = visibleKey
        visibleKey = null
    }

    fun reset() {
        visibleKey = null
        hiddenKey = null
    }

    private fun navigationKey(navigation: HondaDisplayText): String {
        val maneuver = navigation.messageLine1
            .replace(TRAILING_DISTANCE_REGEX, "")
            .trim(' ', '-')
        return "$maneuver|${navigation.messageLine2.trim()}"
    }

    private companion object {
        val TRAILING_DISTANCE_REGEX = Regex(
            "-?\\d+(?:[,.]\\d+)?(?:m|km)$",
            RegexOption.IGNORE_CASE,
        )
    }
}
