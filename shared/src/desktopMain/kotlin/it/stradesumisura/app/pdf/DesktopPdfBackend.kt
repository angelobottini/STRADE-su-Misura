package it.stradesumisura.app.pdf

import it.stradesumisura.app.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.fontbox.ttf.CmapLookup
import org.apache.fontbox.ttf.TTFParser
import org.apache.fontbox.ttf.TrueTypeFont
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.util.Matrix
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Calendar
import kotlin.math.PI

/**
 * Report PDF su Windows con Apache PDFBox. I caratteri Roboto vengono incorporati (solo i glifi
 * usati); i simboli che Roboto non contiene (es. «→») vengono presi da Segoe UI o Segoe UI Symbol,
 * presenti in Windows, e in loro assenza sostituiti con «?».
 */
class DesktopPdfBackend : PdfBackend {

    private val mutex = Mutex()

    @Volatile
    private var faces: Map<PdfFont, FontFace> = emptyMap()

    /** Caratteri di riserva di Windows, caricati solo se servono. */
    private val fallbacks: List<Lazy<FontFace?>> = listOf("segoeui.ttf", "seguisym.ttf").map { name ->
        lazy {
            val windows = System.getenv("WINDIR") ?: "C:\\Windows"
            val file = File(File(windows, "Fonts"), name)
            if (file.isFile) runCatching { FontFace(name, file.readBytes()) }.getOrNull()?.takeIf { it.embeddable } else null
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun prepare() {
        mutex.withLock {
            if (faces.isNotEmpty()) return
            val byPath = mutableMapOf<String, FontFace>()
            faces = PdfFont.entries.associateWith { font ->
                byPath[font.resourcePath] ?: FontFace(font.name, Res.readBytes(font.resourcePath)).also { byPath[font.resourcePath] = it }
            }
        }
    }

    private fun face(font: PdfFont): FontFace = faces[font] ?: error("Caratteri del PDF non caricati")

    /** Divide il testo in tratti da disegnare ciascuno con un carattere che ne contiene i glifi. */
    internal fun runs(text: String, font: PdfFont): List<TextRun> {
        val primary = face(font)
        if (text.codePoints().allMatch { primary.hasGlyph(it) }) return listOf(TextRun(text, primary))
        val runs = mutableListOf<TextRun>()
        val current = StringBuilder()
        var currentFace = primary
        text.codePoints().forEach { codePoint ->
            val (face, value) = when {
                primary.hasGlyph(codePoint) -> primary to codePoint
                Character.isISOControl(codePoint) -> primary to ' '.code
                else -> fallbacks.firstNotNullOfOrNull { it.value?.takeIf { f -> f.hasGlyph(codePoint) } }
                    ?.let { it to codePoint } ?: (primary to '?'.code)
            }
            if (face !== currentFace && current.isNotEmpty()) {
                runs += TextRun(current.toString(), currentFace)
                current.clear()
            }
            currentFace = face
            current.appendCodePoint(value)
        }
        if (current.isNotEmpty()) runs += TextRun(current.toString(), currentFace)
        return runs
    }

    override val metrics: PdfTextMetrics = object : PdfTextMetrics {
        override fun measure(text: String, font: PdfFont, size: Float, letterSpacing: Float): Float {
            val runs = runs(text, font)
            val glyphs = runs.sumOf { it.text.codePointCount(0, it.text.length) }
            return runs.sumOf { it.face.advance(it.text).toDouble() }.toFloat() * size + letterSpacing * size * glyphs
        }

        override fun ascent(font: PdfFont, size: Float): Float = -face(font).ascender * size

        override fun descent(font: PdfFont, size: Float): Float = -face(font).descender * size
    }

    override fun newDocument(): PdfDocumentWriter = PdfBoxDocument(this)
}

internal class TextRun(val text: String, val face: FontFace)

/** Carattere TrueType: misure dei glifi e dati per incorporarlo nei documenti. */
internal class FontFace(val name: String, val bytes: ByteArray) {
    private val font: TrueTypeFont = TTFParser().parse(RandomAccessReadBuffer(bytes))
    private val cmap: CmapLookup = font.unicodeCmapLookup
    private val unitsPerEm = font.unitsPerEm.toFloat()
    private val glyphCache = HashMap<Int, Int>()
    private val advanceCache = HashMap<Int, Float>()

    /** Ascendente e discendente in em (il discendente è negativo). */
    val ascender: Float = font.horizontalHeader.ascender / unitsPerEm
    val descender: Float = font.horizontalHeader.descender / unitsPerEm

    /** La licenza del carattere ne consente l'incorporazione nei documenti (campo fsType di OS/2). */
    val embeddable: Boolean = run {
        val fsType = font.getOS2Windows()?.fsType?.toInt() ?: 0
        (fsType and FS_TYPE_RESTRICTED) != FS_TYPE_RESTRICTED && (fsType and FS_TYPE_BITMAP_ONLY) == 0
    }

    @Synchronized
    fun hasGlyph(codePoint: Int): Boolean = glyphId(codePoint) != 0

    private fun glyphId(codePoint: Int): Int = glyphCache.getOrPut(codePoint) { cmap.getGlyphId(codePoint) }

    /** Larghezza di [text] in em. */
    @Synchronized
    fun advance(text: String): Float {
        var total = 0f
        text.codePoints().forEach { codePoint ->
            total += advanceCache.getOrPut(codePoint) { font.getAdvanceWidth(glyphId(codePoint)) / unitsPerEm }
        }
        return total
    }

    private companion object {
        const val FS_TYPE_RESTRICTED = 0x0002
        const val FS_TYPE_BITMAP_ONLY = 0x0200
    }
}

private class PdfBoxDocument(private val backend: DesktopPdfBackend) : PdfDocumentWriter {
    private val document = PDDocument()
    private val embedded = HashMap<FontFace, PDFont>()

    init {
        document.documentInformation.apply {
            creator = "STRADE su Misura"
            producer = "STRADE su Misura"
            creationDate = Calendar.getInstance()
        }
    }

    fun pdfFont(face: FontFace): PDFont = embedded.getOrPut(face) {
        PDType0Font.load(document, ByteArrayInputStream(face.bytes), true)
    }

    override fun page(width: Float, height: Float, draw: (PdfCanvas) -> Unit) {
        val page = PDPage(PDRectangle(width, height))
        document.addPage(page)
        PDPageContentStream(document, page).use { stream ->
            // Origine in alto a sinistra con l'asse y verso il basso, come su Android.
            stream.transform(Matrix(1f, 0f, 0f, -1f, 0f, height))
            draw(PdfBoxCanvas(stream, backend, this))
        }
    }

    override fun writeTo(file: File) {
        document.save(file)
    }

    override fun close() {
        document.close()
    }
}

private class PdfBoxCanvas(
    private val stream: PDPageContentStream,
    private val backend: DesktopPdfBackend,
    private val document: PdfBoxDocument,
) : PdfCanvas {

    override fun drawText(text: String, x: Float, y: Float, paint: PdfTextPaint) {
        if (text.isEmpty()) return
        val width = paint.measureText(text)
        val left = when (paint.align) {
            PdfAlign.LEFT -> x
            PdfAlign.CENTER -> x - width / 2f
            PdfAlign.RIGHT -> x - width
        }
        fill(paint.color)
        stream.beginText()
        stream.setCharacterSpacing(paint.letterSpacing * paint.textSize)
        // La matrice del testo raddrizza i glifi rispetto all'asse y rovesciato della pagina.
        stream.setTextMatrix(Matrix(1f, 0f, 0f, -1f, left, y))
        backend.runs(text, paint.font).forEach { run ->
            stream.setFont(document.pdfFont(run.face), paint.textSize)
            stream.showText(run.text)
        }
        stream.endText()
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: PdfPaint) {
        stroke(paint)
        stream.moveTo(x1, y1)
        stream.lineTo(x2, y2)
        stream.stroke()
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: PdfPaint) {
        stream.addRect(left, top, right - left, bottom - top)
        paintShape(paint)
    }

    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, paint: PdfPaint) {
        roundRectPath(left, top, right, bottom, radius)
        paintShape(paint)
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: PdfPaint) {
        roundRectPath(cx - radius, cy - radius, cx + radius, cy + radius, radius)
        paintShape(paint)
    }

    override fun drawPath(path: PdfPath, paint: PdfPaint) {
        val points = path.points
        if (points.isEmpty()) return
        stream.moveTo(points[0].first, points[0].second)
        points.drop(1).forEach { (x, y) -> stream.lineTo(x, y) }
        stream.closePath()
        paintShape(paint)
    }

    override fun save() {
        stream.saveGraphicsState()
    }

    override fun restore() {
        stream.restoreGraphicsState()
    }

    override fun translate(dx: Float, dy: Float) {
        stream.transform(Matrix.getTranslateInstance(dx, dy))
    }

    override fun rotate(degrees: Float, px: Float, py: Float) {
        stream.transform(Matrix.getTranslateInstance(px, py))
        stream.transform(Matrix.getRotateInstance(degrees * PI / 180.0, 0f, 0f))
        stream.transform(Matrix.getTranslateInstance(-px, -py))
    }

    override fun clipRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        roundRectPath(left, top, right, bottom, radius)
        stream.clip()
    }

    private fun paintShape(paint: PdfPaint) {
        if (paint.stroke) {
            stroke(paint)
            stream.stroke()
        } else {
            fill(paint.color)
            stream.fill()
        }
    }

    private fun fill(color: Int) {
        stream.setNonStrokingColor(red(color), green(color), blue(color))
    }

    private fun stroke(paint: PdfPaint) {
        stream.setStrokingColor(red(paint.color), green(paint.color), blue(paint.color))
        stream.setLineWidth(paint.strokeWidth)
    }

    /** Rettangolo con angoli arrotondati approssimati da curve di Bézier. */
    private fun roundRectPath(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        val r = minOf(radius, (right - left) / 2f, (bottom - top) / 2f).coerceAtLeast(0f)
        val k = r * KAPPA
        stream.moveTo(left + r, top)
        stream.lineTo(right - r, top)
        stream.curveTo(right - r + k, top, right, top + r - k, right, top + r)
        stream.lineTo(right, bottom - r)
        stream.curveTo(right, bottom - r + k, right - r + k, bottom, right - r, bottom)
        stream.lineTo(left + r, bottom)
        stream.curveTo(left + r - k, bottom, left, bottom - r + k, left, bottom - r)
        stream.lineTo(left, top + r)
        stream.curveTo(left, top + r - k, left + r - k, top, left + r, top)
        stream.closePath()
    }

    private fun red(color: Int): Float = ((color shr 16) and 0xFF) / 255f
    private fun green(color: Int): Float = ((color shr 8) and 0xFF) / 255f
    private fun blue(color: Int): Float = (color and 0xFF) / 255f

    private companion object {
        const val KAPPA = 0.5522848f
    }
}
