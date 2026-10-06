package pl.msg4honda

import org.junit.Assert.assertEquals
import org.junit.Test

class MapsNotificationParserTest {
    @Test
    fun `formats duration maneuver distance and street`() {
        val result = MapsNotificationParser.parse(
            listOf("18 min", "Za 300 m skręć w prawo w ul. Krakowska"),
        )!!

        assertEquals("Maps 18 min", result.title)
        assertEquals("Skręć w prawo - 300 m", result.messageLine1)
        assertEquals("ul. Krakowska", result.messageLine2)
    }

    @Test
    fun `formats motorway direction`() {
        val result = MapsNotificationParser.parse(
            listOf("1 h 5 min", "Zjedź za 2 km w kierunku A4 Rzeszów"),
        )!!

        assertEquals("Maps 1 h 5 min", result.title)
        assertEquals("Zjedź - 2 km", result.messageLine1)
        assertEquals("A4 Rzeszów", result.messageLine2)
    }
}
