package pl.msg4honda

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageFormattingTest {
    @Test
    fun `puts sender and WhatsApp in title`() {
        val result = MessageFormatter.format("Jan", "Cześć!")

        assertEquals("Jan • WhatsApp", result.title)
    }

    @Test
    fun `keeps short message on one line`() {
        val result = MessageFormatter.format("Jan", "Cześć!")

        assertEquals("Cześć!", result.messageLine1)
        assertEquals("", result.messageLine2)
    }

    @Test
    fun `splits message into two lines at word boundary`() {
        val result = MessageFormatter.format(
            "Jan",
            "To jest trochę dłuższa wiadomość podzielona na dwie linie",
        )

        assertEquals("To jest trochę dłuższa", result.messageLine1)
        assertEquals("wiadomość podzielona na", result.messageLine2)
    }

    @Test
    fun `limits message to two display lines`() {
        val result = MessageFormatter.format("Jan", "a".repeat(80))

        assertEquals(24, result.messageLine1.length)
        assertEquals(24, result.messageLine2.length)
    }
}
