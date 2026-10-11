package org.armya.lyapunov

import org.armya.lyapunov.fractal.LyapunovRenderer
import org.armya.lyapunov.fractal.Region
import org.armya.lyapunov.palette.BuiltInPalettes
import org.armya.lyapunov.palette.Colorizer
import org.armya.lyapunov.palette.Hsv
import org.armya.lyapunov.palette.PaletteCodec
import org.armya.lyapunov.palette.parseHex
import org.armya.lyapunov.palette.rgb
import org.armya.lyapunov.palette.toArgb
import org.armya.lyapunov.palette.toHex
import org.armya.lyapunov.palette.toHsv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LyapunovTest {
    private val ab = booleanArrayOf(false, true)

    private fun lambda(a: Double, b: Double) = LyapunovRenderer.exponent(a, b, ab, 200, 600)

    @Test
    fun stableAndChaoticPoints() {
        assertTrue(lambda(3.2, 3.2) < 0, "r = 3.2 has a stable 2-cycle")
        assertTrue(lambda(3.9, 3.9) > 0, "r = 3.9 is chaotic")
        assertEquals(Double.NEGATIVE_INFINITY, lambda(2.0, 2.0), "r = 2 is super-stable")
        assertEquals(Double.POSITIVE_INFINITY, lambda(4.2, 3.0), "r > 4 escapes")
    }

    @Test
    fun knownExponentForConstantR() {
        // For a stable fixed point x* = 1 - 1/r the exponent is ln|2 - r|.
        assertEquals(kotlin.math.ln(0.5), lambda(2.5, 2.5), 1e-6)
    }

    @Test
    fun fitAspectKeepsCenterAndRatio() {
        val r = Region.Default.fitAspect(2.0)
        assertEquals(2.0, r.width / r.height, 1e-12)
        assertEquals(Region.Default.centerA, r.centerA, 1e-12)
        assertEquals(Region.Default.centerB, r.centerB, 1e-12)
    }

    @Test
    fun selectTopLeftQuarter() {
        val r = Region.Default.select(0.0, 0.0, 0.5, 0.5)
        assertEquals(Region(2.0, 3.0, 3.0, 4.0), r)
    }

    @Test
    fun paletteRoundTrip() {
        val p = BuiltInPalettes.fire.copy(id = "user-1", name = "Mine", builtIn = false)
        val decoded = PaletteCodec.decode(PaletteCodec.encode(listOf(p)))
        assertEquals(listOf(p), decoded)
    }

    @Test
    fun colorizer() {
        val c = Colorizer(BuiltInPalettes.classic)
        assertEquals(BuiltInPalettes.classic.chaosColor, c.color(0.3f))
        assertEquals(BuiltInPalettes.classic.chaosColor, c.color(Float.NaN))
        assertEquals(rgb(0xFFFF80), c.color(Float.NEGATIVE_INFINITY))
    }

    @Test
    fun colorConversions() {
        assertEquals("#12AB9F", parseHex("#12ab9f")!!.toHex())
        val color = rgb(0x3366CC)
        assertEquals(color, color.toHsv().toArgb())
        assertEquals(rgb(0xFF0000), Hsv(0f, 1f, 1f).toArgb())
    }
}
