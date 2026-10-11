package org.armya.lyapunov.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import org.armya.lyapunov.palette.Palette

fun gradientBrush(palette: Palette): Brush {
    val sorted = palette.stops.sortedBy { it.position }
    return if (sorted.size < 2) {
        SolidColor(Color(sorted.first().color))
    } else {
        Brush.horizontalGradient(*sorted.map { it.position.coerceIn(0f, 1f) to Color(it.color) }.toTypedArray())
    }
}

/** The chaos color as a small block on the left, then the gradient. */
@Composable
fun PaletteSwatch(palette: Palette, modifier: Modifier = Modifier) {
    val brush = remember(palette) { gradientBrush(palette) }
    val shape = RoundedCornerShape(4.dp)
    Row(modifier.clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape)) {
        Box(Modifier.fillMaxHeight().width(16.dp).background(Color(palette.chaosColor)))
        Box(Modifier.fillMaxHeight().weight(1f).background(brush))
    }
}

@Composable
fun ColorChip(color: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(modifier.clip(shape).background(Color(color)).border(1.dp, MaterialTheme.colorScheme.outline, shape))
}
