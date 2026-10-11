package org.armya.lyapunov

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.armya.lyapunov.fractal.FractalParams
import org.armya.lyapunov.fractal.Region
import org.armya.lyapunov.palette.BuiltInPalettes
import org.armya.lyapunov.palette.Palette
import org.armya.lyapunov.palette.PaletteRepository
import org.armya.lyapunov.ui.ControlPanel
import org.armya.lyapunov.ui.FractalView
import org.armya.lyapunov.ui.PaletteEditorDialog

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val repository = remember { PaletteRepository() }
        // Every zoom pushes a region; Back pops it.
        val history = remember { mutableStateListOf(Region.Default) }
        var params by remember { mutableStateOf(FractalParams()) }
        var paletteId by remember { mutableStateOf(BuiltInPalettes.classic.id) }
        var editing by remember { mutableStateOf<Palette?>(null) }

        val palette = repository.byId(paletteId) ?: repository.palettes.first()

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                val wide = maxWidth > 720.dp
                val panelHeight = maxHeight * 0.42f

                val fractal: @Composable (Modifier) -> Unit = { m ->
                    FractalView(
                        region = history.last(),
                        params = params,
                        palette = palette,
                        onZoom = { history.add(it) },
                        modifier = m,
                    )
                }
                val controls: @Composable (Modifier) -> Unit = { m ->
                    ControlPanel(
                        region = history.last(),
                        canGoBack = history.size > 1,
                        onBack = { if (history.size > 1) history.removeAt(history.lastIndex) },
                        onReset = {
                            history.clear()
                            history.add(Region.Default)
                        },
                        params = params,
                        onParamsChange = { params = it },
                        palettes = repository.palettes,
                        selected = palette,
                        onSelectPalette = { paletteId = it.id },
                        onNewPalette = { editing = repository.newFrom(palette) },
                        onEditPalette = { editing = it },
                        onDeletePalette = {
                            repository.delete(it.id)
                            paletteId = BuiltInPalettes.classic.id
                        },
                        modifier = m,
                    )
                }

                if (wide) {
                    Row(Modifier.fillMaxSize()) {
                        fractal(Modifier.weight(1f).fillMaxHeight())
                        controls(Modifier.width(320.dp).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        fractal(Modifier.fillMaxWidth().weight(1f))
                        controls(Modifier.fillMaxWidth().height(panelHeight))
                    }
                }
            }
        }

        editing?.let { p ->
            PaletteEditorDialog(
                initial = p,
                onDismiss = { editing = null },
                onSave = {
                    repository.upsert(it)
                    paletteId = it.id
                    editing = null
                },
            )
        }
    }
}
