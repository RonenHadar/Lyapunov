package org.armya.lyapunov.palette

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Colors are stored as opaque ARGB ints (0xFFRRGGBB).

fun argb(r: Int, g: Int, b: Int): Int =
    (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

/** Builds a color from a 0xRRGGBB literal. */
fun rgb(hex: Int): Int = (0xFF shl 24) or (hex and 0xFFFFFF)

val Int.red: Int get() = (this shr 16) and 0xFF
val Int.green: Int get() = (this shr 8) and 0xFF
val Int.blue: Int get() = this and 0xFF

fun lerpColor(from: Int, to: Int, t: Float): Int {
    val f = t.coerceIn(0f, 1f)
    fun mix(x: Int, y: Int) = (x + (y - x) * f).roundToInt()
    return argb(mix(from.red, to.red), mix(from.green, to.green), mix(from.blue, to.blue))
}

fun Int.toHex(): String = "#" + (this and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

/** Parses "#RRGGBB" or "RRGGBB"; returns null if the text isn't a valid color. */
fun parseHex(text: String): Int? {
    val s = text.trim().removePrefix("#")
    if (s.length != 6) return null
    val v = s.toIntOrNull(16) ?: return null
    return rgb(v)
}

/** Hue in degrees 0..360, saturation and value 0..1. */
data class Hsv(val h: Float, val s: Float, val v: Float)

fun Int.toHsv(): Hsv {
    val r = red / 255f
    val g = green / 255f
    val b = blue / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val d = maxC - minC
    val h = when {
        d == 0f -> 0f
        maxC == r -> 60f * (((g - b) / d) % 6f)
        maxC == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val s = if (maxC == 0f) 0f else d / maxC
    return Hsv(h, s, maxC)
}

fun Hsv.toArgb(): Int {
    val c = v * s
    val hp = (h % 360f) / 60f
    val x = c * (1 - abs(hp % 2f - 1))
    val (r1, g1, b1) = when {
        hp < 1 -> Triple(c, x, 0f)
        hp < 2 -> Triple(x, c, 0f)
        hp < 3 -> Triple(0f, c, x)
        hp < 4 -> Triple(0f, x, c)
        hp < 5 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = v - c
    return argb(((r1 + m) * 255).roundToInt(), ((g1 + m) * 255).roundToInt(), ((b1 + m) * 255).roundToInt())
}
