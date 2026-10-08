package pl.msg4honda

import org.junit.Assert.assertEquals
import org.junit.Test

class MapsSpeedLimitReaderTest {
    @Test
    fun `finds Polish labelled speed limit`() {
        val result = MapsSpeedLimitReader.find(
            listOf(MapsSpeedLimitReader.NodeText(null, null, "Ograniczenie prędkości 70")),
        )

        assertEquals(70, result)
    }

    @Test
    fun `finds standalone number only in speed limit view`() {
        val result = MapsSpeedLimitReader.find(
            listOf(MapsSpeedLimitReader.NodeText("maps_speed_limit", "50", null)),
        )

        assertEquals(50, result)
    }

    @Test
    fun `does not mistake arbitrary navigation number for speed limit`() {
        val result = MapsSpeedLimitReader.find(
            listOf(MapsSpeedLimitReader.NodeText("distance", "130", null)),
        )

        assertEquals(null, result)
    }
}
