package it.stradesumisura.app.pdf

import java.io.File

/**
 * Superficie di disegno dei report, indipendente dalla piattaforma: su Android è realizzata con
 * `android.graphics.pdf.PdfDocument`, su Windows con Apache PDFBox. Le coordinate sono in punti
 * tipografici (1/72 di pollice) con l'origine in alto a sinistra e l'asse y verso il basso.
 */

/**
 * Caratteri dei report: Roboto statico (TrueType) incluso nelle risorse. Con i font variabili di
 * sistema PdfDocument incorpora glifi Type3 con avanzamenti arrotondati e il testo risulta con
 * spaziatura irregolare. Il nero usa il grassetto.
 */
enum class PdfFont(val resourcePath: String) {
    Regular("font/roboto_regular.ttf"),
    Medium("font/roboto_medium.ttf"),
    Bold("font/roboto_bold.ttf"),
    Black("font/roboto_bold.ttf"),
    Italic("font/roboto_italic.ttf"),
    Light("font/roboto_light.ttf"),
}

enum class PdfAlign { LEFT, CENTER, RIGHT }

/** Immagini dei report, incluse nelle risorse; [width] e [height] sono le dimensioni in pixel. */
enum class PdfImage(val resourcePath: String, val width: Int, val height: Int) {
    /** Il marchio STRADE con «Autonomia, Cittadinanza, Occupabilità», in scala di grigi. */
    Logo("files/strade_logo.png", 881, 736),
}

/** Misure del testo fornite dalla piattaforma, in punti. */
interface PdfTextMetrics {
    /** Larghezza di [text] con il carattere e il corpo indicati; [letterSpacing] è in em. */
    fun measure(text: String, font: PdfFont, size: Float, letterSpacing: Float): Float

    /** Ascendente del carattere (negativo: sopra la linea di base). */
    fun ascent(font: PdfFont, size: Float): Float

    /** Discendente del carattere (positivo: sotto la linea di base). */
    fun descent(font: PdfFont, size: Float): Float
}

/** Stile del testo; [align] indica a quale estremo del testo si riferisce la x passata a drawText. */
class PdfTextPaint(
    private val metrics: PdfTextMetrics,
    val textSize: Float,
    var color: Int,
    val font: PdfFont,
    val letterSpacing: Float = 0f,
    val align: PdfAlign = PdfAlign.LEFT,
) {
    fun measureText(text: String): Float =
        if (text.isEmpty()) 0f else metrics.measure(text, font, textSize, letterSpacing)

    val ascent: Float get() = metrics.ascent(font, textSize)
    val descent: Float get() = metrics.descent(font, textSize)
}

/** Riempimento o contorno di una forma. */
class PdfPaint(val color: Int, val stroke: Boolean = false, val strokeWidth: Float = 0f)

/** Poligono chiuso (es. il marcatore sulla barra interpretativa). */
class PdfPath {
    private val _points = mutableListOf<Pair<Float, Float>>()
    val points: List<Pair<Float, Float>> get() = _points

    fun moveTo(x: Float, y: Float) {
        _points += x to y
    }

    fun lineTo(x: Float, y: Float) {
        _points += x to y
    }

    fun close() = Unit
}

interface PdfCanvas {
    /** Disegna una riga di testo con la linea di base in [y]. */
    fun drawText(text: String, x: Float, y: Float, paint: PdfTextPaint)
    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: PdfPaint)
    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: PdfPaint)
    fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, paint: PdfPaint)
    fun drawCircle(cx: Float, cy: Float, radius: Float, paint: PdfPaint)
    fun drawPath(path: PdfPath, paint: PdfPaint)

    /** Disegna [image] adattandola al riquadro indicato. */
    fun drawImage(image: PdfImage, left: Float, top: Float, right: Float, bottom: Float)
    fun save()
    fun restore()
    fun translate(dx: Float, dy: Float)

    /** Ruota di [degrees] in senso orario attorno a ([px], [py]). */
    fun rotate(degrees: Float, px: Float, py: Float)
    fun clipRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float)
}

/** Documento in costruzione: una pagina alla volta, poi il salvataggio su file. */
interface PdfDocumentWriter {
    fun page(width: Float, height: Float, draw: (PdfCanvas) -> Unit)
    fun writeTo(file: File)
    fun close()
}

/** Implementazione della piattaforma. */
interface PdfBackend {
    /** Carica i caratteri e le immagini dei report; va chiamata prima di misurare o disegnare. */
    suspend fun prepare()
    val metrics: PdfTextMetrics
    fun newDocument(): PdfDocumentWriter
}
