package it.stradesumisura.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import java.awt.Window

/**
 * Colora la barra del titolo di Windows 11 come lo sfondo dell'app (chiaro o scuro) tramite
 * DwmSetWindowAttribute. Su Windows 10 gli attributi non supportati vengono ignorati.
 */
internal object WindowsChrome {

    @Suppress("FunctionName")
    private interface DwmApi : Library {
        fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: IntByReference, size: Int): Int
    }

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_CAPTION_COLOR = 35
    private const val DWMWA_TEXT_COLOR = 36

    private val dwm: DwmApi? by lazy {
        if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) return@lazy null
        runCatching { Native.load("dwmapi", DwmApi::class.java) }.getOrNull()
    }

    fun applyColors(window: Window, caption: Color, text: Color, dark: Boolean) {
        val api = dwm ?: return
        val hwnd = runCatching { Native.getWindowPointer(window) }.getOrNull() ?: return
        set(api, hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, if (dark) 1 else 0)
        set(api, hwnd, DWMWA_CAPTION_COLOR, caption.toColorRef())
        set(api, hwnd, DWMWA_TEXT_COLOR, text.toColorRef())
    }

    private fun set(api: DwmApi, hwnd: Pointer, attribute: Int, value: Int) {
        runCatching { api.DwmSetWindowAttribute(hwnd, attribute, IntByReference(value), Int.SIZE_BYTES) }
    }

    /** COLORREF di Windows: 0x00BBGGRR. */
    private fun Color.toColorRef(): Int {
        val argb = toArgb()
        val red = (argb shr 16) and 0xFF
        val green = (argb shr 8) and 0xFF
        val blue = argb and 0xFF
        return (blue shl 16) or (green shl 8) or red
    }
}
