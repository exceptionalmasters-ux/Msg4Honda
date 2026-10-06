package pl.msg4honda

enum class MessageSource(
    val displayName: String,
    val preferenceKey: String,
    val packageNames: Set<String>,
) {
    WHATSAPP(
        displayName = "WhatsApp",
        preferenceKey = "source_whatsapp",
        packageNames = setOf("com.whatsapp", "com.whatsapp.w4b"),
    ),
    MESSENGER(
        displayName = "Messenger",
        preferenceKey = "source_messenger",
        packageNames = setOf("com.facebook.orca"),
    ),
    SMS(
        displayName = "SMS",
        preferenceKey = "source_sms",
        packageNames = setOf(
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging",
            "com.android.mms",
        ),
    ),
    MAPS(
        displayName = "Maps",
        preferenceKey = "source_maps",
        packageNames = setOf("com.google.android.apps.maps"),
    ),
    ;

    companion object {
        fun fromPackage(packageName: String): MessageSource? =
            entries.firstOrNull { packageName in it.packageNames }
    }
}
