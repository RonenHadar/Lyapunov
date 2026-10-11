package org.armya.lyapunov.palette

import androidx.compose.runtime.mutableStateListOf
import org.armya.lyapunov.loadPaletteText
import org.armya.lyapunov.savePaletteText
import kotlin.random.Random

object BuiltInPalettes {
    val classic = Palette(
        id = "builtin-classic", name = "Classic", builtIn = true,
        chaosColor = rgb(0x000000),
        stops = listOf(ColorStop(0f, rgb(0x1A0F00)), ColorStop(0.4f, rgb(0xC08000)), ColorStop(1f, rgb(0xFFFF80))),
    )
    val ocean = Palette(
        id = "builtin-ocean", name = "Ocean", builtIn = true,
        chaosColor = rgb(0x000814),
        stops = listOf(ColorStop(0f, rgb(0x001D3D)), ColorStop(0.5f, rgb(0x0077B6)), ColorStop(1f, rgb(0xCAF0F8))),
    )
    val fire = Palette(
        id = "builtin-fire", name = "Fire", builtIn = true,
        chaosColor = rgb(0x000000),
        stops = listOf(
            ColorStop(0f, rgb(0x200000)), ColorStop(0.35f, rgb(0xA01000)),
            ColorStop(0.7f, rgb(0xFF8000)), ColorStop(1f, rgb(0xFFFFC0)),
        ),
    )
    val zircon = Palette(
        id = "builtin-zircon", name = "Zircon", builtIn = true,
        chaosColor = rgb(0x0A1A60),
        stops = listOf(ColorStop(0f, rgb(0x3A2A00)), ColorStop(0.6f, rgb(0xE0B030)), ColorStop(1f, rgb(0xFFF4C0))),
    )

    val all = listOf(classic, ocean, fire, zircon)
}

/**
 * Plain-text format, one palette per line:
 * `id|name|#CHAOS|pos:#COLOR,pos:#COLOR,...`
 */
object PaletteCodec {
    fun encode(palettes: List<Palette>): String = palettes.joinToString("\n") { p ->
        val name = p.name.replace('|', ' ').replace('\n', ' ')
        val stops = p.stops.joinToString(",") { "${it.position}:${it.color.toHex()}" }
        "${p.id}|$name|${p.chaosColor.toHex()}|$stops"
    }

    fun decode(text: String): List<Palette> = text.lines().mapNotNull { line ->
        runCatching {
            val parts = line.split('|')
            require(parts.size == 4)
            val stops = parts[3].split(',').map { s ->
                val (pos, hex) = s.split(':')
                ColorStop(pos.toFloat(), parseHex(hex)!!)
            }
            Palette(id = parts[0], name = parts[1], stops = stops, chaosColor = parseHex(parts[2])!!)
        }.getOrNull()
    }
}

/** Built-in palettes plus the user's own, which are saved to disk. */
class PaletteRepository {
    val palettes = mutableStateListOf<Palette>()

    init {
        palettes.addAll(BuiltInPalettes.all)
        val saved = runCatching { loadPaletteText()?.let(PaletteCodec::decode) }.getOrNull().orEmpty()
        palettes.addAll(saved)
    }

    fun byId(id: String): Palette? = palettes.firstOrNull { it.id == id }

    fun upsert(palette: Palette) {
        val index = palettes.indexOfFirst { it.id == palette.id }
        if (index >= 0) palettes[index] = palette else palettes.add(palette)
        persist()
    }

    fun delete(id: String) {
        palettes.removeAll { it.id == id && !it.builtIn }
        persist()
    }

    /** A new, editable copy of [source]. Not saved until [upsert] is called. */
    fun newFrom(source: Palette): Palette {
        val taken = palettes.map { it.name }.toSet()
        var n = 1
        while ("My palette $n" in taken) n++
        return source.copy(id = "user-" + Random.nextLong().toULong().toString(16), name = "My palette $n", builtIn = false)
    }

    private fun persist() {
        runCatching { savePaletteText(PaletteCodec.encode(palettes.filter { !it.builtIn })) }
    }
}
