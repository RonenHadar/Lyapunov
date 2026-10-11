package org.armya.lyapunov.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.armya.lyapunov.fractal.FractalParams
import org.armya.lyapunov.fractal.Region
import org.armya.lyapunov.palette.Palette
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val SEQUENCE_PRESETS = listOf("AB", "AABAB", "BBBBBBAAAAAA")

@Composable
fun ControlPanel(
    region: Region,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onReset: () -> Unit,
    params: FractalParams,
    onParamsChange: (FractalParams) -> Unit,
    palettes: List<Palette>,
    selected: Palette,
    onSelectPalette: (Palette) -> Unit,
    onNewPalette: () -> Unit,
    onEditPalette: (Palette) -> Unit,
    onDeletePalette: (Palette) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Lyapunov", style = MaterialTheme.typography.headlineSmall)

        // --- View ---
        SectionTitle("View")
        val digits = decimalsFor(region.width)
        Text(
            "a: ${fmt(region.aMin, digits)} … ${fmt(region.aMax, digits)}\n" +
                "b: ${fmt(region.bMin, digits)} … ${fmt(region.bMax, digits)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, enabled = canGoBack) { Text("Back") }
            OutlinedButton(onClick = onReset) { Text("Reset") }
        }
        Text(
            "Drag across the image to zoom into that area. Double-click (or double-tap) zooms 2× on a point.",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        // --- Sequence & iterations ---
        SectionTitle("Sequence")
        var sequenceText by remember(params.sequence) { mutableStateOf(params.sequence) }
        OutlinedTextField(
            value = sequenceText,
            onValueChange = { text ->
                val clean = text.uppercase().filter { it == 'A' || it == 'B' }
                sequenceText = clean
                if (clean.isNotEmpty()) onParamsChange(params.copy(sequence = clean))
            },
            label = { Text("Letters A and B") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            SEQUENCE_PRESETS.forEach { preset ->
                TextButton(onClick = { onParamsChange(params.copy(sequence = preset)) }) { Text(preset) }
            }
        }

        var iterations by remember(params.iterations) { mutableFloatStateOf(params.iterations.toFloat()) }
        Text("Iterations: ${iterations.roundToInt()}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = iterations,
            onValueChange = { iterations = it },
            onValueChangeFinished = { onParamsChange(params.copy(iterations = iterations.roundToInt())) },
            valueRange = 100f..3000f,
        )

        HorizontalDivider()

        // --- Palettes ---
        SectionTitle("Palette")
        palettes.forEach { p ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (p.id == selected.id) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    )
                    .clickable { onSelectPalette(p) }
                    .padding(6.dp),
            ) {
                PaletteSwatch(p, Modifier.width(96.dp).height(20.dp))
                Text(p.name, Modifier.padding(start = 12.dp).weight(1f))
                if (p.builtIn) Text("built-in", style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onNewPalette) { Text("New") }
            OutlinedButton(onClick = { onEditPalette(selected) }, enabled = !selected.builtIn) { Text("Edit") }
            OutlinedButton(onClick = { onDeletePalette(selected) }, enabled = !selected.builtIn) { Text("Delete") }
        }
        Text(
            "New makes an editable copy of the selected palette. Built-in palettes can't be changed.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

/** Enough decimals to tell the edges of a region of this size apart. */
private fun decimalsFor(size: Double): Int =
    if (size <= 0.0) 6 else (ceil(-log10(size)).toInt() + 3).coerceIn(3, 15)

/** Formats [value] with [decimals] decimals (no String.format in common code). */
private fun fmt(value: Double, decimals: Int): String {
    if (decimals > 12) return value.toString()
    val factor = 10.0.pow(decimals)
    val scaled = (kotlin.math.abs(value) * factor).roundToLong()
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    val whole = scaled / factor.toLong()
    val frac = (scaled % factor.toLong()).toString().padStart(decimals, '0')
    return "$sign$whole.$frac"
}
