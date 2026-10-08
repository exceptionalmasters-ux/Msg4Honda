package pl.msg4honda

import org.junit.Assert.assertEquals
import org.junit.Test

class MapsManeuverCatalogTest {
    @Test
    fun `recognizes collected Maps maneuver icons`() {
        assertEquals("Na rondzie drugi zjazd", MapsManeuverCatalog.forHash("82b7be5f4234"))
        assertEquals("Na rondzie pierwszy zjazd na wprost", MapsManeuverCatalog.forHash("7cc5e0c621dd"))
        assertEquals("Na rondzie w lewo", MapsManeuverCatalog.forHash("f671359b7204"))
        assertEquals("Jedź prosto tą samą trasą", MapsManeuverCatalog.forHash("d9543a7b04df"))
        assertEquals("Trzymaj się prawej strony", MapsManeuverCatalog.forHash("16b3d20d3121"))
        assertEquals("Na rondzie prosto drugi zjazd", MapsManeuverCatalog.forHash("379ecb532d61"))
    }
}
