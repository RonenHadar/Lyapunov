package org.armya.lyapunov

import androidx.compose.ui.graphics.ImageBitmap

/** Reads the saved user palettes, or null if nothing has been saved yet. */
expect fun loadPaletteText(): String?

/** Saves the user palettes. */
expect fun savePaletteText(text: String)

/** Wraps ARGB pixels (row by row) in an image Compose can draw. */
expect fun pixelsToImageBitmap(pixels: IntArray, width: Int, height: Int): ImageBitmap
