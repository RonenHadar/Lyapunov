package org.armya.lyapunov.fractal

/**
 * A rectangle in parameter space. `a` runs along the horizontal axis (left to right),
 * `b` along the vertical axis (bottom to top), as in the usual Lyapunov fractal plots.
 */
data class Region(
    val aMin: Double,
    val aMax: Double,
    val bMin: Double,
    val bMax: Double,
) {
    val width: Double get() = aMax - aMin
    val height: Double get() = bMax - bMin
    val centerA: Double get() = (aMin + aMax) / 2
    val centerB: Double get() = (bMin + bMax) / 2

    /**
     * Expands the shorter side around the center so that width / height equals [aspect]
     * (pixel width / pixel height of the view). The fractal is never stretched.
     */
    fun fitAspect(aspect: Double): Region {
        if (aspect <= 0.0 || width <= 0.0 || height <= 0.0) return this
        val current = width / height
        return if (current < aspect) {
            val w = height * aspect
            Region(centerA - w / 2, centerA + w / 2, bMin, bMax)
        } else {
            val h = width / aspect
            Region(aMin, aMax, centerB - h / 2, centerB + h / 2)
        }
    }

    /**
     * The sub-region covered by a rectangle on the image, given as fractions of the image
     * size (0..1). `top`/`bottom` are measured from the top of the image.
     */
    fun select(left: Double, top: Double, right: Double, bottom: Double): Region = Region(
        aMin = aMin + minOf(left, right) * width,
        aMax = aMin + maxOf(left, right) * width,
        bMin = bMax - maxOf(top, bottom) * height,
        bMax = bMax - minOf(top, bottom) * height,
    )

    /** Zooms by [factor] around the point at image fractions ([fx], [fy]). */
    fun zoomAround(fx: Double, fy: Double, factor: Double): Region {
        val ca = aMin + fx * width
        val cb = bMax - fy * height
        val w = width / factor
        val h = height / factor
        return Region(ca - w / 2, ca + w / 2, cb - h / 2, cb + h / 2)
    }

    companion object {
        /** The classic view of the fractal: a and b both in [2, 4]. */
        val Default = Region(2.0, 4.0, 2.0, 4.0)
    }
}
