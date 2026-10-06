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
    fun `puts selected source in title`() {
        val result = MessageFormatter.format("Anna", "Hej", "Messenger")

        assertEquals("Anna • Messenger", result.title)
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
    fun `splits a very long message into multiple pages`() {
        val result = MessageFormatter.formatPages("Jan", "a".repeat(80))

        assertEquals(2, result.size)
        assertEquals(24, result[0].messageLine1.length)
        assertEquals(24, result[0].messageLine2.length)
        assertEquals(24, result[1].messageLine1.length)
        assertEquals(8, result[1].messageLine2.length)
    }
}
