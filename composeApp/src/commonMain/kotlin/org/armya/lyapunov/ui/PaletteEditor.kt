package org.armya.lyapunov.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.armya.lyapunov.palette.ColorStop
import org.armya.lyapunov.palette.Palette
import org.armya.lyapunov.palette.parseHex
import org.armya.lyapunov.palette.toArgb
import org.armya.lyapunov.palette.toHex
import org.armya.lyapunov.palette.toHsv
import kotlin.math.roundToInt

private const val CHAOS = -1

/**
 * Edits a palette: its name, gradient stops (position + color) and chaos color.
 * Changes are applied only when the user presses Save.
 */
@Composable
fun PaletteEditorDialog(
    initial: Palette,
    onDismiss: () -> Unit,
    onSave: (Palette) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    val stops = remember { mutableStateListOf<ColorStop>().apply { addAll(initial.stops) } }
    var chaos by remember { mutableStateOf(initial.chaosColor) }
    var selected by remember { mutableStateOf(0) }

    val sel = if (selected == CHAOS) CHAOS else selected.coerceIn(0, stops.lastIndex)
    val current = initial.copy(name = name, stops = stops.toList(), chaosColor = chaos)
    val selectedColor = if (sel == CHAOS) chaos else stops[sel].color

    fun setSelectedColor(c: Int) {
        if (sel == CHAOS) chaos = c else stops[sel] = stops[sel].copy(color = c)
    }

    fun addStop() {
        val sorted = stops.sortedBy { it.position }
        var best = 0
        var gap = -1f
        for (i in 0 until sorted.size - 1) {
            val g = sorted[i + 1].position - sorted[i].position
            if (g > gap) {
                gap = g
                best = i
            }
        }
        val pos = if (sorted.size < 2) 0.5f else (sorted[best].position + sorted[best + 1].position) / 2
        stops.add(ColorStop(pos, current.colorAt(pos)))
        selected = stops.lastIndex
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit palette") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PaletteSwatch(current, Modifier.fillMaxWidth().height(32.dp))
                Text(
                    "Gradient for stable areas: left is the edge of chaos, right is the most stable.",
                    style = MaterialTheme.typography.bodySmall,
                )

                stops.forEachIndexed { i, stop ->
                    SelectableRow(selected = sel == i, onClick = { selected = i }) {
                        ColorChip(stop.color, Modifier.size(28.dp))
                        Slider(
                            value = stop.position,
                            onValueChange = { stops[i] = stops[i].copy(position = it) },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        if (stops.size > 2) {
                            TextButton(onClick = {
                                stops.removeAt(i)
                                selected = 0
                            }) { Text("✕") }
                        } else {
                            Spacer(Modifier.width(48.dp))
                        }
                    }
                }
                TextButton(onClick = { addStop() }) { Text("+ Add color stop") }

                SelectableRow(selected = sel == CHAOS, onClick = { selected = CHAOS }) {
                    ColorChip(chaos, Modifier.size(28.dp))
                    Text("Chaos color (exponent ≥ 0)", Modifier.padding(start = 12.dp))
                }

                HorizontalDivider()
                Text(
                    if (sel == CHAOS) "Editing: chaos color" else "Editing: stop ${sel + 1}",
                    style = MaterialTheme.typography.labelLarge,
                )
                ColorEditor(selectedColor, onChange = { setSelectedColor(it) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(current.copy(name = name.ifBlank { "Untitled" }, stops = stops.sortedBy { it.position }))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SelectableRow(selected: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        content()
    }
}

/** Hue / saturation / brightness sliders plus a hex field. */
@Composable
fun ColorEditor(color: Int, onChange: (Int) -> Unit) {
    // Keep our own HSV so the hue isn't lost when saturation or brightness hit zero;
    // fall back to the color's HSV whenever the color was changed from elsewhere.
    val hsvState = remember { mutableStateOf(color.toHsv()) }
    val hsv = hsvState.value.let { if (it.toArgb() == color) it else color.toHsv() }
    var hexText by remember(color) { mutableStateOf(color.toHex()) }

    fun update(h: Float = hsv.h, s: Float = hsv.s, v: Float = hsv.v) {
        val next = hsv.copy(h = h, s = s, v = v)
        hsvState.value = next
        onChange(next.toArgb())
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorChip(color, Modifier.size(40.dp))
            OutlinedTextField(
                value = hexText,
                onValueChange = { text ->
                    hexText = text
                    parseHex(text)?.let(onChange)
                },
                label = { Text("Hex") },
                singleLine = true,
                modifier = Modifier.padding(start = 12.dp).width(140.dp),
            )
        }
        Text("Hue ${hsv.h.roundToInt()}°", style = MaterialTheme.typography.bodySmall)
        Slider(value = hsv.h, onValueChange = { update(h = it) }, valueRange = 0f..359.9f)
        Text("Saturation ${(hsv.s * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall)
        Slider(value = hsv.s, onValueChange = { update(s = it) })
        Text("Brightness ${(hsv.v * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall)
        Slider(value = hsv.v, onValueChange = { update(v = it) })
    }
}
