package pl.msg4honda

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationDisplayPolicyTest {
    @Test
    fun `distance updates do not restart timeout`() {
        val policy = NavigationDisplayPolicy()

        val first = policy.evaluate(navigation("->Skręć w prawo-300m", "Cechowa"))
        val update = policy.evaluate(navigation("->Skręć w prawo-120m", "Cechowa"))

        assertTrue(first.shouldDisplay)
        assertTrue(first.restartTimeout)
        assertTrue(update.shouldDisplay)
        assertFalse(update.restartTimeout)
    }

    @Test
    fun `new maneuver starts a fresh timeout`() {
        val policy = NavigationDisplayPolicy()
        policy.evaluate(navigation("->Skręć w prawo-300m", "Cechowa"))

        val next = policy.evaluate(navigation("<-Skręć w lewo-500m", "Łużycka"))

        assertTrue(next.shouldDisplay)
        assertTrue(next.restartTimeout)
    }

    @Test
    fun `hidden maneuver stays hidden until instruction changes`() {
        val policy = NavigationDisplayPolicy()
        policy.evaluate(navigation("->Skręć w prawo-300m", "Cechowa"))
        policy.hideCurrent()

        val same = policy.evaluate(navigation("->Skręć w prawo-120m", "Cechowa"))
        val next = policy.evaluate(navigation("^Jedź prosto-1km", "Zakopiańska"))

        assertFalse(same.shouldDisplay)
        assertTrue(next.shouldDisplay)
        assertTrue(next.restartTimeout)
    }

    @Test
    fun `unknown maneuver uses road as stable identity`() {
        val policy = NavigationDisplayPolicy()
        val first = policy.evaluate(navigation("300m", "Cechowa"))
        val update = policy.evaluate(navigation("120m", "Cechowa"))

        assertTrue(first.restartTimeout)
        assertTrue(update.shouldDisplay)
        assertFalse(update.restartTimeout)
    }

    private fun navigation(line1: String, line2: String) = HondaDisplayText(
        title = "10km-12min",
        messageLine1 = line1,
        messageLine2 = line2,
    )
}
