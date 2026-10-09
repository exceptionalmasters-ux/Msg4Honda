package pl.msg4honda

data class HondaDisplayText(
    val title: String,
    val messageLine1: String,
    val messageLine2: String,
)

object MessageFormatter {
    private const val LINE_LENGTH = 24
    private const val TITLE_LENGTH = 32

    fun format(sender: String, message: String, source: String = "WhatsApp"): HondaDisplayText =
        formatPages(sender, message, source).first()

    fun welcome(enabledSources: List<String>): HondaDisplayText = HondaDisplayText(
        title = "Msg4Honda",
        messageLine1 = "Witaj :)",
        messageLine2 = enabledSources.joinToString(", ").ifBlank { "Brak źródeł" }.take(LINE_LENGTH),
    )

    fun formatPages(
        sender: String,
        message: String,
        source: String = "WhatsApp",
    ): List<HondaDisplayText> {
        val normalizedSender = sender.trim().replace(Regex("\\s+"), " ")
        val normalizedMessage = message.trim().replace(Regex("\\s+"), " ")
        val title = "$normalizedSender • $source".take(TITLE_LENGTH)
        val lines = wrapLines(normalizedMessage).ifEmpty { listOf("") }

        return lines.chunked(2).map { page ->
            HondaDisplayText(
                title = title,
                messageLine1 = page[0],
                messageLine2 = page.getOrElse(1) { "" },
            )
        }
    }

    private fun wrapLines(text: String): List<String> {
        val lines = mutableListOf<String>()
        var remainder = text

        while (remainder.length > LINE_LENGTH) {
            val lineBreak = remainder.lastIndexOf(' ', startIndex = LINE_LENGTH)
                .takeIf { it > 0 }
                ?: LINE_LENGTH
            lines += remainder.substring(0, lineBreak).trimEnd()
            remainder = remainder.substring(lineBreak).trimStart()
        }

        if (remainder.isNotEmpty()) lines += remainder
        return lines
    }
}
