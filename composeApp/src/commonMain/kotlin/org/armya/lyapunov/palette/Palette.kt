package org.armya.lyapunov.palette

import kotlin.math.exp
import kotlin.math.roundToInt

/** A color at a position (0..1) along the gradient. */
data class ColorStop(val position: Float, val color: Int)

/**
 * A color palette for the fractal.
 *
 * Stable points (Lyapunov exponent < 0) are colored from the gradient [stops]: position 0
 * is the edge of chaos, position 1 is the most stable. Chaotic points (exponent >= 0) are
 * painted [chaosColor].
 */
data class Palette(
    val id: String,
    val name: String,
    val stops: List<ColorStop>,
    val chaosColor: Int,
    val builtIn: Boolean = false,
) {
    init {
        require(stops.isNotEmpty()) { "A palette needs at least one color stop" }
    }

    fun colorAt(t: Float): Int {
        val sorted = stops.sortedBy { it.position }
        if (t <= sorted.first().position) return sorted.first().color
        if (t >= sorted.last().position) return sorted.last().color
        for (i in 0 until sorted.size - 1) {
            val from = sorted[i]
            val to = sorted[i + 1]
            if (t <= to.position) {
                val span = to.position - from.position
                val f = if (span <= 0f) 0f else (t - from.position) / span
                return lerpColor(from.color, to.color, f)
            }
        }
        return sorted.last().color
    }

    fun lookupTable(size: Int): IntArray = IntArray(size) { colorAt(it / (size - 1f)) }
}

/** Turns Lyapunov exponents into pixel colors using a palette. */
class Colorizer(palette: Palette) {
    private val lut = palette.lookupTable(LUT_SIZE)
    private val chaos = palette.chaosColor

    fun color(lambda: Float): Int {
        if (lambda.isNaN() || lambda >= 0f) return chaos
        // Maps (-inf, 0) onto (1, 0): 0 at the edge of chaos, 1 for super-stable points.
        val t = 1f - exp(lambda)
        val index = (t * (LUT_SIZE - 1)).roundToInt().coerceIn(0, LUT_SIZE - 1)
        return lut[index]
    }

    fun colorize(exponents: FloatArray): IntArray = IntArray(exponents.size) { color(exponents[it]) }

    private companion object {
        const val LUT_SIZE = 1024
    }
}
