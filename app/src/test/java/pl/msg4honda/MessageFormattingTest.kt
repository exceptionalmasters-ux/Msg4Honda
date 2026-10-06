package pl.msg4honda

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageFormattingTest {
    @Test
    fun `take preserves a short message`() {
        assertEquals("Cześć!", "Cześć!".take(48))
    }

    @Test
    fun `take limits a long message`() {
        assertEquals(48, "a".repeat(80).take(48).length)
    }
}
