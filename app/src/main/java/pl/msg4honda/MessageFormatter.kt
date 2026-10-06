package pl.msg4honda

data class HondaDisplayText(
    val title: String,
    val messageLine1: String,
    val messageLine2: String,
)

object MessageFormatter {
    private const val LINE_LENGTH = 24
    private const val TITLE_LENGTH = 32

    fun format(sender: String, message: String): HondaDisplayText {
        val normalizedSender = sender.trim().replace(Regex("\\s+"), " ")
        val normalizedMessage = message.trim().replace(Regex("\\s+"), " ")
        val lines = splitIntoTwoLines(normalizedMessage)

        return HondaDisplayText(
            title = "$normalizedSender • WhatsApp".take(TITLE_LENGTH),
            messageLine1 = lines.first,
            messageLine2 = lines.second,
        )
    }

    private fun splitIntoTwoLines(text: String): Pair<String, String> {
        if (text.length <= LINE_LENGTH) return text to ""

        val visibleText = text.take(LINE_LENGTH * 2)
        val firstBreak = visibleText.lastIndexOf(' ', startIndex = LINE_LENGTH)
            .takeIf { it > 0 }
            ?: LINE_LENGTH
        val firstLine = visibleText.substring(0, firstBreak).trimEnd()
        val remainder = visibleText.substring(firstBreak).trimStart()

        return firstLine to remainder.take(LINE_LENGTH).trimEnd()
    }
}
