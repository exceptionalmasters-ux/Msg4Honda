package pl.msg4honda

object MapsManeuverCatalog {
    private val maneuvers = mapOf(
        "17db2c2d28b7" to "Skręć w prawo",
        "e8279e455a98" to "Skręć w lewo",
        "f21e2536ff8f" to "Jedź prosto",
        "6f20e21aca00" to "Lekko w prawo",
        "9b5b78d96b7f" to "Zawróć",
        "6e73aba63816" to "Lekko w lewo",
        "82b7be5f4234" to "Na rondzie drugi zjazd",
        "7cc5e0c621dd" to "Na rondzie pierwszy zjazd na wprost",
        "f671359b7204" to "Na rondzie w lewo",
        "d9543a7b04df" to "Jedź prosto tą samą trasą",
        "16b3d20d3121" to "Trzymaj się prawej strony",
        "379ecb532d61" to "Na rondzie prosto drugi zjazd",
    )

    fun forHash(hash: String?): String? = hash?.let(maneuvers::get)
}
