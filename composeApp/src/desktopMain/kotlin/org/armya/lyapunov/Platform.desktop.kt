package org.armya.lyapunov

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.io.File

private val paletteFile = File(System.getProperty("user.home"), ".lyapunov/palettes.txt")

actual fun loadPaletteText(): String? = paletteFile.takeIf { it.exists() }?.readText()

actual fun savePaletteText(text: String) {
    paletteFile.parentFile?.mkdirs()
    paletteFile.writeText(text)
}

actual fun pixelsToImageBitmap(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    // An ARGB int stored little-endian is the byte sequence B, G, R, A.
    val bytes = ByteArray(pixels.size * 4)
    var j = 0
    for (p in pixels) {
        bytes[j++] = p.toByte()
        bytes[j++] = (p shr 8).toByte()
        bytes[j++] = (p shr 16).toByte()
        bytes[j++] = (p ushr 24).toByte()
    }
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
    return Image.makeRaster(info, bytes, width * 4).toComposeImageBitmap()
}
