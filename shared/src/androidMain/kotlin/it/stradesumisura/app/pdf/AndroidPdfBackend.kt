package it.stradesumisura.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import it.stradesumisura.app.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.File
import java.io.FileOutputStream

/**
 * Report PDF su Android con `android.graphics.pdf.PdfDocument`. I caratteri Roboto statici
 * vengono copiati dalle risorse in una cartella privata e caricati con Typeface.createFromFile.
 */
class AndroidPdfBackend(private val context: Context) : PdfBackend {

    private val mutex = Mutex()

    @Volatile
    private var typefaces: Map<PdfFont, Typeface> = emptyMap()

    @Volatile
    private var bitmaps: Map<PdfImage, Bitmap> = emptyMap()

    private data class PaintKey(val font: PdfFont, val size: Float, val letterSpacing: Float, val color: Int, val align: PdfAlign)

    private val paints = HashMap<PaintKey, TextPaint>()

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun prepare() {
        mutex.withLock {
            if (typefaces.isNotEmpty()) return
            // Un'immagine che non si carica non blocca il report: viene semplicemente omessa.
            bitmaps = PdfImage.entries.mapNotNull { image ->
                runCatching { Res.readBytes(image.resourcePath) }.getOrNull()
                    ?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                    ?.let { image to it }
            }.toMap()
            val dir = File(context.noBackupFilesDir, "pdf-fonts").apply { mkdirs() }
            val loaded = mutableMapOf<String, Typeface>()
            val result = mutableMapOf<PdfFont, Typeface>()
            for (font in PdfFont.entries) {
                result[font] = loaded[font.resourcePath] ?: runCatching {
                    val bytes = Res.readBytes(font.resourcePath)
                    val file = File(dir, font.resourcePath.substringAfterLast('/'))
                    if (!file.exists() || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
                    Typeface.createFromFile(file)
                }.getOrElse { systemTypeface(font) }.also { loaded[font.resourcePath] = it }
            }
            typefaces = result
        }
    }

    private fun systemTypeface(font: PdfFont): Typeface = when (font) {
        PdfFont.Regular -> Typeface.create("sans-serif", Typeface.NORMAL)
        PdfFont.Medium -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        PdfFont.Bold, PdfFont.Black -> Typeface.create("sans-serif", Typeface.BOLD)
        PdfFont.Italic -> Typeface.create("sans-serif", Typeface.ITALIC)
        PdfFont.Light -> Typeface.create("sans-serif-light", Typeface.NORMAL)
    }

    internal fun bitmap(image: PdfImage): Bitmap? = bitmaps[image]

    internal fun textPaint(font: PdfFont, size: Float, letterSpacing: Float, color: Int, align: PdfAlign): TextPaint =
        synchronized(paints) {
            paints.getOrPut(PaintKey(font, size, letterSpacing, color, align)) {
                TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                    textSize = size
                    this.color = color
                    typeface = typefaces[font] ?: systemTypeface(font)
                    this.letterSpacing = letterSpacing
                    textAlign = when (align) {
                        PdfAlign.LEFT -> Paint.Align.LEFT
                        PdfAlign.CENTER -> Paint.Align.CENTER
                        PdfAlign.RIGHT -> Paint.Align.RIGHT
                    }
                }
            }
        }

    override val metrics: PdfTextMetrics = object : PdfTextMetrics {
        override fun measure(text: String, font: PdfFont, size: Float, letterSpacing: Float): Float =
            textPaint(font, size, letterSpacing, MEASURE_COLOR, PdfAlign.LEFT).measureText(text)

        override fun ascent(font: PdfFont, size: Float): Float =
            textPaint(font, size, 0f, MEASURE_COLOR, PdfAlign.LEFT).fontMetrics.ascent

        override fun descent(font: PdfFont, size: Float): Float =
            textPaint(font, size, 0f, MEASURE_COLOR, PdfAlign.LEFT).fontMetrics.descent
    }

    override fun newDocument(): PdfDocumentWriter = object : PdfDocumentWriter {
        private val document = PdfDocument()
        private var pageNumber = 0

        override fun page(width: Float, height: Float, draw: (PdfCanvas) -> Unit) {
            val info = PdfDocument.PageInfo.Builder(width.toInt(), height.toInt(), ++pageNumber).create()
            val page = document.startPage(info)
            draw(AndroidPdfCanvas(page.canvas, this@AndroidPdfBackend))
            document.finishPage(page)
        }

        override fun writeTo(file: File) {
            FileOutputStream(file).use { document.writeTo(it) }
        }

        override fun close() = document.close()
    }

    private companion object {
        const val MEASURE_COLOR = -0x1000000
    }
}

private class AndroidPdfCanvas(private val canvas: Canvas, private val backend: AndroidPdfBackend) : PdfCanvas {

    override fun drawText(text: String, x: Float, y: Float, paint: PdfTextPaint) {
        canvas.drawText(text, x, y, backend.textPaint(paint.font, paint.textSize, paint.letterSpacing, paint.color, paint.align))
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: PdfPaint) {
        canvas.drawLine(x1, y1, x2, y2, shapePaint(paint))
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: PdfPaint) {
        canvas.drawRect(left, top, right, bottom, shapePaint(paint))
    }

    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, paint: PdfPaint) {
        canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, shapePaint(paint))
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: PdfPaint) {
        canvas.drawCircle(cx, cy, radius, shapePaint(paint))
    }

    override fun drawPath(path: PdfPath, paint: PdfPaint) {
        val androidPath = Path()
        path.points.forEachIndexed { index, (x, y) -> if (index == 0) androidPath.moveTo(x, y) else androidPath.lineTo(x, y) }
        androidPath.close()
        canvas.drawPath(androidPath, shapePaint(paint))
    }

    override fun drawImage(image: PdfImage, left: Float, top: Float, right: Float, bottom: Float) {
        val bitmap = backend.bitmap(image) ?: return
        canvas.drawBitmap(bitmap, null, RectF(left, top, right, bottom), bitmapPaint)
    }

    override fun save() {
        canvas.save()
    }

    override fun restore() {
        canvas.restore()
    }

    override fun translate(dx: Float, dy: Float) {
        canvas.translate(dx, dy)
    }

    override fun rotate(degrees: Float, px: Float, py: Float) {
        canvas.rotate(degrees, px, py)
    }

    override fun clipRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        canvas.clipPath(Path().apply { addRoundRect(left, top, right, bottom, radius, radius, Path.Direction.CW) })
    }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private fun shapePaint(paint: PdfPaint): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = if (paint.stroke) Paint.Style.STROKE else Paint.Style.FILL
        strokeWidth = paint.strokeWidth
        color = paint.color
    }
}
