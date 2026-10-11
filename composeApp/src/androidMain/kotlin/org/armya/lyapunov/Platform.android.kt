package org.armya.lyapunov

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/** Set by [MainActivity] before the UI starts. */
internal object AndroidStorage {
    var filesDir: File? = null
}

private fun paletteFile(): File? = AndroidStorage.filesDir?.let { File(it, "palettes.txt") }

actual fun loadPaletteText(): String? = paletteFile()?.takeIf { it.exists() }?.readText()

actual fun savePaletteText(text: String) {
    paletteFile()?.writeText(text)
}

actual fun pixelsToImageBitmap(pixels: IntArray, width: Int, height: Int): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
