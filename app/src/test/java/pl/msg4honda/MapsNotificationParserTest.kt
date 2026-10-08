package pl.msg4honda

import org.junit.Assert.assertEquals
import org.junit.Test

class MapsNotificationParserTest {
    @Test
    fun `formats duration maneuver distance and street`() {
        val result = MapsNotificationParser.parse(
            listOf("18 min", "Za 300 m skręć w prawo w ul. Krakowska"),
        )!!

        assertEquals("18min", result.title)
        assertEquals("->Skręć w prawo-300m", result.messageLine1)
        assertEquals("ul. Krakowska", result.messageLine2)
    }

    @Test
    fun `formats motorway direction`() {
        val result = MapsNotificationParser.parse(
            listOf("1 h 5 min", "Zjedź za 2 km w kierunku A4 Rzeszów"),
        )!!

        assertEquals("1h5min", result.title)
        assertEquals("Zjedź-2km", result.messageLine1)
        assertEquals("A4 Rzeszów", result.messageLine2)
    }

    @Test
    fun `adds ascii arrows for left and straight maneuvers`() {
        val left = MapsNotificationParser.parse(
            listOf("9 min", "Za 80 m skręć w lewo w ul. Polna"),
        )!!
        val straight = MapsNotificationParser.parse(
            listOf("7 min", "Jedź prosto przez 1 km", "A4 Rzeszów"),
        )!!

        assertEquals("<-Skręć w lewo-80m", left.messageLine1)
        assertEquals("^Jedź prosto-1km", straight.messageLine1)
    }

    @Test
    fun `ignores empty terminal navigation update`() {
        val result = MapsNotificationParser.parse(listOf("0 min", "0 m", "Bez tytułu"))

        assertEquals(null, result)
    }

    @Test
    fun `distance-only update keeps previous maneuver and street`() {
        val previous = MapsNotificationParser.parse(
            listOf("8 min", "Za 300 m skręć w prawo w ul. Długa"),
        )!!

        val result = MapsNotificationParser.parse(listOf("7 min", "30 m"), previous)!!

        assertEquals("7min", result.title)
        assertEquals("->Skręć w prawo-30m", result.messageLine1)
        assertEquals("ul. Długa", result.messageLine2)
    }

    @Test
    fun `finds street without a road prefix in extra text line`() {
        val result = MapsNotificationParser.parse(
            listOf("8 min", "Skręć w lewo", "30 m", "Jana Pawła II"),
        )!!

        assertEquals("<-Skręć w lewo-30m", result.messageLine1)
        assertEquals("Jana Pawła II", result.messageLine2)
    }

    @Test
    fun `does not display distance without known maneuver`() {
        val result = MapsNotificationParser.parse(listOf("8 min", "30 m"))

        assertEquals(null, result)
    }

    @Test
    fun `terminal update clears previous maneuver`() {
        val previous = MapsNotificationParser.parse(
            listOf("8 min", "Za 300 m skręć w prawo w ul. Długa"),
        )!!

        val result = MapsNotificationParser.parse(listOf("0 min", "0 m"), previous)

        assertEquals(null, result)
    }

    @Test
    fun `uses maneuver decoded from Maps icon without changing street parsing`() {
        val result = MapsNotificationParser.parse(
            rawValues = listOf("12 min", "10 m", "Cechowa"),
            iconManeuver = "Skręć w prawo",
        )!!

        assertEquals("12min", result.title)
        assertEquals("->Skręć w prawo-10m", result.messageLine1)
        assertEquals("Cechowa", result.messageLine2)
    }

    @Test
    fun `shows remaining route distance and time in title`() {
        val result = MapsNotificationParser.parse(
            listOf(
                "10 min · 3,1 km · Będziesz o 21:30",
                "300 m",
                "Zakopiańska",
            ),
            iconManeuver = "Jedź prosto",
        )!!

        assertEquals("3.1km-10min", result.title)
        assertEquals("^Jedź prosto-300m", result.messageLine1)
        assertEquals("Zakopiańska", result.messageLine2)
    }

    @Test
    fun `adds fresh speed limit to Maps title`() {
        val result = MapsNotificationParser.parse(
            rawValues = listOf("30 min · 13 km · Będziesz o 12:36", "500 m", "DK94 Bytom"),
            iconManeuver = "Trzymaj się prawej strony",
            speedLimit = 70,
        )!!

        assertEquals("Og.70km/h-13km-30min", result.title)
    }

    @Test
    fun `compacts hour duration in Maps title`() {
        val result = MapsNotificationParser.parse(
            rawValues = listOf("1 h 10 min · 20 km · Będziesz o 14:00", "300 m", "A4 Katowice"),
            iconManeuver = "Jedź prosto",
            speedLimit = 70,
        )!!

        assertEquals("Og.70km/h-20km-1h10min", result.title)
    }
}
