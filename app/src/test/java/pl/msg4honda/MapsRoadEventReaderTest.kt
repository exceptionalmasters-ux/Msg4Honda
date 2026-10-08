package pl.msg4honda

import org.junit.Assert.assertEquals
import org.junit.Test

class MapsRoadEventReaderTest {
    @Test
    fun `recognizes a single road alert`() {
        assertEquals(
            "KONTROLA PRĘDKOŚCI",
            MapsRoadEventReader.find(listOf("Kontrola prędkości za 500 metrów")),
        )
        assertEquals(
            "ZATRZYMANY POJAZD",
            MapsRoadEventReader.find(listOf("Zatrzymany pojazd na poboczu")),
        )
    }

    @Test
    fun `ignores report menu containing many incident types`() {
        val result = MapsRoadEventReader.find(
            listOf("Wypadek", "Korek", "Kontrola prędkości", "Roboty drogowe"),
        )

        assertEquals(null, result)
    }
}
