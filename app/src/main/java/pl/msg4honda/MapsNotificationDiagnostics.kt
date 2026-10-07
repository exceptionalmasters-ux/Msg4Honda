package pl.msg4honda

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object MapsNotificationDiagnostics {
    private const val MAX_VALUE_LENGTH = 1_000
    private const val MAX_DUMP_LENGTH = 30_000

    data class Result(
        val text: String,
        val maneuver: String?,
    )

    fun create(context: Context, sbn: StatusBarNotification): Result {
        val notification = sbn.notification
        val output = StringBuilder()

        output.appendLine("package=${sbn.packageName}")
        output.appendLine("key=${sbn.key}")
        output.appendLine("id=${sbn.id}, tag=${sbn.tag}")
        output.appendLine("postTime=${sbn.postTime}")
        output.appendLine("category=${notification.category}")
        output.appendLine("channelId=${notification.channelId}")
        output.appendLine("flags=${notification.flags}")
        output.appendLine("group=${notification.group}")
        output.appendLine("groupKey=${sbn.groupKey}")
        output.appendLine("sortKey=${notification.sortKey}")
        output.appendLine("ticker=${notification.tickerText}")
        output.appendLine("smallIcon=${formatIcon(notification.smallIcon)}")
        output.appendLine("largeIcon=${formatIcon(notification.getLargeIcon())}")

        output.appendLine()
        output.appendLine("=== EXTRAS ===")
        notification.extras.keySet().sorted().forEach { key ->
            @Suppress("DEPRECATION")
            val value = notification.extras.get(key)
            output.appendLine("$key = ${formatValue(value)}")
        }

        output.appendLine()
        output.appendLine("=== REMOTE VIEWS ===")
        val directionHash = appendRemoteViews(context, sbn, output)
        val maneuver = maneuverForHash(directionHash)
        output.insert(
            0,
            "rozpoznanyHash=${directionHash ?: "brak"}\n" +
                "rozpoznanyManewr=${maneuver ?: "nieznany"}\n\n",
        )

        return Result(
            text = output.toString().take(MAX_DUMP_LENGTH),
            maneuver = maneuver,
        )
    }

    private fun appendRemoteViews(
        context: Context,
        sbn: StatusBarNotification,
        output: StringBuilder,
    ): String? {
        var directionHash: String? = null
        val sourceContext = runCatching {
            context.createPackageContext(sbn.packageName, Context.CONTEXT_IGNORE_SECURITY)
        }.getOrElse {
            output.appendLine("sourceContext ERROR: ${it.javaClass.simpleName}: ${it.message}")
            return null
        }

        val notification = sbn.notification
        val views = mutableListOf<Pair<String, RemoteViews>>()
        notification.contentView?.let { views += "contentView" to it }
        notification.bigContentView?.let { views += "bigContentView" to it }
        notification.headsUpContentView?.let { views += "headsUpContentView" to it }

        runCatching {
            Notification.Builder.recoverBuilder(context, notification).createContentView()
        }.onSuccess { it?.let { view -> views += "recoveredContentView" to view } }
            .onFailure { output.appendLine("recover content ERROR: ${it.javaClass.simpleName}") }

        runCatching {
            Notification.Builder.recoverBuilder(context, notification).createBigContentView()
        }.onSuccess { it?.let { view -> views += "recoveredBigContentView" to view } }
            .onFailure { output.appendLine("recover big ERROR: ${it.javaClass.simpleName}") }

        if (views.isEmpty()) {
            output.appendLine("brak RemoteViews")
            return null
        }

        views.forEach { (name, remoteViews) ->
            output.appendLine("-- $name layoutId=${remoteViews.layoutId} --")
            runCatching {
                remoteViews.apply(sourceContext, FrameLayout(sourceContext))
            }.onSuccess { root ->
                appendViewTree(
                    context = sourceContext,
                    view = root,
                    output = output,
                    depth = 0,
                    includeRightIconPng = name == "recoveredBigContentView",
                    onRightIconHash = { hash -> directionHash = hash },
                )
            }
                .onFailure {
                    output.appendLine("apply ERROR: ${it.javaClass.simpleName}: ${it.message}")
                }
        }
        return directionHash
    }

    private fun appendViewTree(
        context: Context,
        view: View,
        output: StringBuilder,
        depth: Int,
        includeRightIconPng: Boolean,
        onRightIconHash: (String) -> Unit,
    ) {
        if (output.length >= MAX_DUMP_LENGTH) return

        val idName = if (view.id > 0) {
            runCatching { context.resources.getResourceEntryName(view.id) }
                .getOrElse { "id:${view.id}" }
        } else {
            "no-id"
        }
        val prefix = "  ".repeat(depth.coerceAtMost(8))
        when (view) {
            is TextView -> output.appendLine(
                "$prefix TextView $idName text=${view.text.toString().take(MAX_VALUE_LENGTH)}",
            )
            is ImageView -> {
                val isRightIcon = idName == "right_icon"
                if (isRightIcon) {
                    (view.drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
                        onRightIconHash(bitmapFingerprint(bitmap))
                    }
                }
                output.appendLine(
                    "$prefix ImageView $idName drawable=${formatDrawable(
                        view,
                        includePng = includeRightIconPng && isRightIcon,
                    )}",
                )
            }
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                appendViewTree(
                    context,
                    view.getChildAt(index),
                    output,
                    depth + 1,
                    includeRightIconPng,
                    onRightIconHash,
                )
            }
        }
    }

    private fun formatDrawable(view: ImageView, includePng: Boolean): String {
        val drawable = view.drawable ?: return "null"
        val bitmap = (drawable as? BitmapDrawable)?.bitmap
        val bitmapPart = bitmap?.let(::formatBitmap) ?: "bez bitmapy"
        val pngPart = if (bitmap != null && includePng) {
            val encoded = runCatching {
                Base64.encodeToString(bitmapPng(bitmap), Base64.NO_WRAP)
            }.getOrElse { "error-${it.javaClass.simpleName}" }
            ", pngBase64=$encoded"
        } else {
            ""
        }
        return "${drawable.javaClass.simpleName}, " +
            "intrinsic=${drawable.intrinsicWidth}x${drawable.intrinsicHeight}, $bitmapPart$pngPart"
    }

    private fun formatValue(value: Any?): String {
        val formatted = when (value) {
            null -> "null"
            is CharSequence -> "${value.javaClass.simpleName}: $value"
            is Bitmap -> formatBitmap(value)
            is Icon -> formatIcon(value)
            is Bundle -> value.keySet().sorted().joinToString(", ", "Bundle{", "}") { key ->
                @Suppress("DEPRECATION")
                "$key=${formatValue(value.get(key))}"
            }
            is Array<*> -> value.joinToString(", ", "${value.javaClass.simpleName}[", "]") {
                formatValue(it)
            }
            is IntArray -> "IntArray${value.contentToString()}"
            is LongArray -> "LongArray${value.contentToString()}"
            is BooleanArray -> "BooleanArray${value.contentToString()}"
            is Collection<*> -> value.joinToString(", ", "${value.javaClass.simpleName}[", "]") {
                formatValue(it)
            }
            else -> "${value.javaClass.simpleName}: $value"
        }
        return formatted.take(MAX_VALUE_LENGTH)
    }

    private fun formatIcon(icon: Icon?): String {
        if (icon == null) return "null"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return "Icon"
        return buildString {
            append("Icon(type=${icon.type}")
            if (icon.type == Icon.TYPE_RESOURCE) {
                append(", package=${icon.resPackage}, resId=${icon.resId}")
            }
            append(")")
        }
    }

    private fun formatBitmap(bitmap: Bitmap): String {
        val fingerprint = bitmapFingerprint(bitmap)
        return "Bitmap(${bitmap.width}x${bitmap.height}, hash=$fingerprint)"
    }

    private fun bitmapFingerprint(bitmap: Bitmap): String = runCatching {
        MessageDigest.getInstance("SHA-256")
            .digest(bitmapPng(bitmap))
            .take(6)
            .joinToString("") { "%02x".format(it) }
    }.getOrElse { "error-${it.javaClass.simpleName}" }

    private fun maneuverForHash(hash: String?): String? = when (hash) {
        "17db2c2d28b7" -> "Skręć w prawo"
        "e8279e455a98" -> "Skręć w lewo"
        "f21e2536ff8f" -> "Jedź prosto"
        "6f20e21aca00" -> "Lekko w prawo"
        "9b5b78d96b7f" -> "Zawróć"
        "6e73aba63816" -> "Lekko w lewo"
        else -> null
    }

    private fun bitmapPng(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { stream ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        stream.toByteArray()
    }
}
