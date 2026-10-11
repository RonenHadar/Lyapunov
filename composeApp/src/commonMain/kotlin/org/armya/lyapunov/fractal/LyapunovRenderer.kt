package org.armya.lyapunov.fractal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min

/**
 * Parameters of the fractal.
 *
 * @param sequence the forcing sequence of A's and B's; at step n the logistic map uses
 *   r = a when the n-th letter is A, and r = b when it is B (the sequence repeats).
 * @param warmup iterations discarded before measuring, so transients die out.
 * @param iterations iterations used to estimate the Lyapunov exponent.
 */
data class FractalParams(
    val sequence: String = "AB",
    val warmup: Int = 200,
    val iterations: Int = 600,
) {
    /** `true` where the sequence has a B. */
    fun pattern(): BooleanArray {
        val s = sequence.ifEmpty { "AB" }
        return BooleanArray(s.length) { s[it] == 'B' }
    }
}

object LyapunovRenderer {

    /**
     * Lyapunov exponent of the logistic map x -> r x (1 - x) where r alternates between
     * [a] and [b] following [pattern].
     *
     * Negative values mean the orbit settles into a stable cycle, positive values mean chaos.
     * Returns -Infinity at super-stable points and +Infinity when the orbit escapes [0, 1].
     */
    fun exponent(
        a: Double,
        b: Double,
        pattern: BooleanArray,
        warmup: Int,
        iterations: Int,
        x0: Double = 0.5,
    ): Double {
        val n = pattern.size
        var x = x0
        var i = 0

        repeat(warmup) {
            val r = if (pattern[i]) b else a
            x = r * x * (1 - x)
            if (!x.isFinite()) return Double.POSITIVE_INFINITY
            i++
            if (i == n) i = 0
        }

        // Multiply derivatives and only take a logarithm when the product gets very large
        // or very small: much faster than calling ln() every step.
        var sum = 0.0
        var product = 1.0
        repeat(iterations) {
            val r = if (pattern[i]) b else a
            product *= abs(r * (1 - 2 * x))
            x = r * x * (1 - x)
            if (!x.isFinite()) return Double.POSITIVE_INFINITY
            if (product == 0.0) return Double.NEGATIVE_INFINITY
            if (product > 1e150 || product < 1e-150) {
                sum += ln(product)
                product = 1.0
            }
            i++
            if (i == n) i = 0
        }
        sum += ln(product)
        return sum / iterations
    }

    /**
     * Computes the exponent for every pixel of a [width] x [height] image of [region],
     * in parallel on all cores. With [step] > 1 only every step-th pixel is computed and
     * the value fills a step x step block: a quick low-resolution preview.
     */
    suspend fun computeExponents(
        width: Int,
        height: Int,
        region: Region,
        params: FractalParams,
        step: Int = 1,
    ): FloatArray = coroutineScope {
        val out = FloatArray(width * height)
        val pattern = params.pattern()
        val rows = (0 until height step step).toList()

        rows.chunked(ROWS_PER_TASK).forEach { chunk ->
            launch(Dispatchers.Default) {
                for (y in chunk) {
                    ensureActive()
                    val b = region.bMax - (y + step / 2.0) / height * region.height
                    val yEnd = min(y + step, height)
                    for (x in 0 until width step step) {
                        val a = region.aMin + (x + step / 2.0) / width * region.width
                        val value = exponent(a, b, pattern, params.warmup, params.iterations).toFloat()
                        val xEnd = min(x + step, width)
                        for (yy in y until yEnd) {
                            val row = yy * width
                            for (xx in x until xEnd) out[row + xx] = value
                        }
                    }
                }
            }
        }
        out
    }

    private const val ROWS_PER_TASK = 4
}
