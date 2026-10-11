package org.armya.lyapunov.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.armya.lyapunov.fractal.FractalParams
import org.armya.lyapunov.fractal.LyapunovRenderer
import org.armya.lyapunov.fractal.Region
import org.armya.lyapunov.palette.Colorizer
import org.armya.lyapunov.palette.Palette
import org.armya.lyapunov.pixelsToImageBitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Exponents for one rendered image; kept so a palette change only recolors. */
private class Exponents(val width: Int, val height: Int, val values: FloatArray)

/** Larger views are rendered at reduced resolution and scaled up. */
private const val MAX_RENDER_PIXELS = 2_000_000

/** Preview first (one value per 8x8 block), then full resolution. */
private val RENDER_STEPS = intArrayOf(8, 1)

/**
 * Shows the fractal for [region], stretched to fill the view without distortion.
 * Drag to select an area to zoom into; double-click / double-tap zooms 2x.
 */
@Composable
fun FractalView(
    region: Region,
    params: FractalParams,
    palette: Palette,
    onZoom: (Region) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnZoom by rememberUpdatedState(onZoom)
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var exponents by remember { mutableStateOf<Exponents?>(null) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var rendering by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    val aspect = if (viewSize.width > 0 && viewSize.height > 0) {
        viewSize.width.toFloat() / viewSize.height
    } else {
        1f
    }
    val effective = remember(region, viewSize) {
        if (viewSize.width > 0 && viewSize.height > 0) region.fitAspect(aspect.toDouble()) else region
    }

    // Compute the exponents whenever the area, the parameters or the view size change.
    LaunchedEffect(effective, params, viewSize) {
        if (viewSize.width <= 0 || viewSize.height <= 0) return@LaunchedEffect
        delay(120) // let window resizes and typing settle before starting
        val (w, h) = renderSize(viewSize)
        rendering = true
        try {
            for (step in RENDER_STEPS) {
                val values = LyapunovRenderer.computeExponents(w, h, effective, params, step)
                exponents = Exponents(w, h, values)
            }
        } finally {
            rendering = false
        }
    }

    // Recolor whenever new exponents arrive or the palette changes.
    LaunchedEffect(exponents, palette) {
        val e = exponents ?: return@LaunchedEffect
        image = withContext(Dispatchers.Default) {
            pixelsToImageBitmap(Colorizer(palette).colorize(e.values), e.width, e.height)
        }
    }

    Box(
        modifier
            .background(Color.Black)
            .clipToBounds()
            .onSizeChanged { viewSize = it }
            .pointerInput(effective, viewSize) {
                detectDragGestures(
                    onDragStart = { dragStart = it; dragEnd = it },
                    onDrag = { change, _ ->
                        change.consume()
                        dragEnd = change.position
                    },
                    onDragEnd = {
                        val s = dragStart
                        val e = dragEnd
                        if (s != null && e != null) {
                            val r = constrainedRect(s, e, aspect)
                            if (r.width > 8f && r.height > 8f) {
                                val w = size.width.toDouble()
                                val h = size.height.toDouble()
                                currentOnZoom(effective.select(r.left / w, r.top / h, r.right / w, r.bottom / h))
                            }
                        }
                        dragStart = null
                        dragEnd = null
                    },
                    onDragCancel = {
                        dragStart = null
                        dragEnd = null
                    },
                )
            }
            .pointerInput(effective, viewSize) {
                detectTapGestures(
                    onDoubleTap = { p ->
                        currentOnZoom(
                            effective.zoomAround(p.x / size.width.toDouble(), p.y / size.height.toDouble(), 2.0)
                        )
                    },
                )
            },
    ) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = "Lyapunov fractal",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }

        val s = dragStart
        val e = dragEnd
        if (s != null && e != null) {
            val r = constrainedRect(s, e, aspect)
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color.White.copy(alpha = 0.15f), topLeft = r.topLeft, size = r.size)
                drawRect(Color.White, topLeft = r.topLeft, size = r.size, style = Stroke(width = 2.dp.toPx()))
            }
        }

        if (rendering) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

/**
 * The selection rectangle from [start] towards [end], with the view's aspect ratio so the
 * zoomed area fills the view exactly.
 */
private fun constrainedRect(start: Offset, end: Offset, aspect: Float): Rect {
    val dx = end.x - start.x
    val dy = end.y - start.y
    var w = abs(dx)
    var h = abs(dy)
    if (w / aspect > h) h = w / aspect else w = h * aspect
    val left = if (dx < 0) start.x - w else start.x
    val top = if (dy < 0) start.y - h else start.y
    return Rect(Offset(left, top), Size(w, h))
}

private fun renderSize(view: IntSize): Pair<Int, Int> {
    val total = view.width.toLong() * view.height
    if (total <= MAX_RENDER_PIXELS) return view.width to view.height
    val scale = sqrt(MAX_RENDER_PIXELS.toDouble() / total)
    return max(1, (view.width * scale).toInt()) to max(1, (view.height * scale).toInt())
}
