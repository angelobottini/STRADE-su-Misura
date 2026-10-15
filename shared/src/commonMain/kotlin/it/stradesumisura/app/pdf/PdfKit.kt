package it.stradesumisura.app.pdf

/**
 * Small layout kit on top of [PdfCanvas] for building paginated A4 reports.
 * Everything is measured in PostScript points (1/72 inch).
 */
object PdfPage {
    const val WIDTH = 595f
    const val HEIGHT = 842f
    const val MARGIN_X = 46f
    const val MARGIN_TOP = 36f
    const val MARGIN_BOTTOM = 40f
    const val HEADER_HEIGHT = 62f
    const val FOOTER_HEIGHT = 30f

    val contentWidth: Float get() = WIDTH - 2 * MARGIN_X
    val contentTop: Float get() = MARGIN_TOP + HEADER_HEIGHT
    val contentBottom: Float get() = HEIGHT - MARGIN_BOTTOM - FOOTER_HEIGHT
}

private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

object PdfPalette {
    val ink: Int = rgb(0x14, 0x12, 0x14)
    val graphite: Int = rgb(0x5F, 0x58, 0x5C)
    val muted: Int = rgb(0x8A, 0x82, 0x86)
    val mauve: Int = rgb(0xC7, 0xAD, 0xBF)
    val mauveDeep: Int = rgb(0x7F, 0x60, 0x74)
    val mauveSoft: Int = rgb(0xF4, 0xED, 0xF1)
    val zebra: Int = rgb(0xFA, 0xF7, 0xF9)
    val rule: Int = rgb(0xE2, 0xD9, 0xDE)
    val white: Int = rgb(0xFF, 0xFF, 0xFF)
}

fun fillPaint(color: Int): PdfPaint = PdfPaint(color)

fun strokePaint(color: Int, width: Float): PdfPaint = PdfPaint(color, stroke = true, strokeWidth = width)

/**
 * Testo impaginato su più righe entro una larghezza: a capo tra le parole (e dopo trattini e
 * barre), con le parole più lunghe della riga spezzate tra i caratteri, come StaticLayout.
 */
class TextLayout(
    val lines: List<String>,
    private val paint: PdfTextPaint,
    private val width: Float,
    private val align: PdfAlign,
    private val lineHeight: Float,
) {
    private val glyphHeight: Float get() = paint.descent - paint.ascent

    /** L'interlinea si applica tra le righe, non dopo l'ultima. */
    val height: Float
        get() = if (lines.isEmpty()) 0f else (lines.size - 1) * lineHeight + glyphHeight

    fun draw(canvas: PdfCanvas, x: Float, y: Float) {
        lines.forEachIndexed { index, line ->
            if (line.isEmpty()) return@forEachIndexed
            val baseline = y + index * lineHeight - paint.ascent
            val lineWidth = paint.measureText(line)
            val left = when (align) {
                PdfAlign.LEFT -> x
                PdfAlign.CENTER -> x + (width - lineWidth) / 2f
                PdfAlign.RIGHT -> x + width - lineWidth
            }
            // drawText interpreta la x secondo l'allineamento del paint.
            val anchor = when (paint.align) {
                PdfAlign.LEFT -> left
                PdfAlign.CENTER -> left + lineWidth / 2f
                PdfAlign.RIGHT -> left + lineWidth
            }
            canvas.drawText(line, anchor, baseline, paint)
        }
    }
}

fun layoutText(
    text: CharSequence,
    paint: PdfTextPaint,
    width: Float,
    align: PdfAlign = PdfAlign.LEFT,
    lineSpacing: Float = 1.2f,
): TextLayout {
    val available = width.coerceAtLeast(1f)
    val lines = wrapText(text.toString(), paint, available)
    return TextLayout(lines, paint, available, align, (paint.descent - paint.ascent) * lineSpacing)
}

private fun wrapText(text: String, paint: PdfTextPaint, width: Float): List<String> {
    val lines = mutableListOf<String>()
    text.split('\n').forEach { paragraph ->
        if (paragraph.isBlank()) {
            lines += ""
            return@forEach
        }
        val line = StringBuilder()
        var lineWidth = 0f
        fun flush() {
            lines += line.toString().trimEnd()
            line.clear()
            lineWidth = 0f
        }
        breakTokens(paragraph).forEach { token ->
            val tokenWidth = paint.measureText(token.trimEnd())
            if (line.isNotEmpty() && lineWidth + tokenWidth > width) flush()
            if (line.isEmpty() && tokenWidth > width) {
                // Parola più lunga della riga: si spezza tra i caratteri.
                var piece = StringBuilder()
                token.forEach { ch ->
                    if (piece.isNotEmpty() && paint.measureText(piece.toString() + ch) > width) {
                        lines += piece.toString()
                        piece = StringBuilder()
                    }
                    piece.append(ch)
                }
                line.append(piece)
                lineWidth = paint.measureText(piece.toString())
            } else {
                line.append(token)
                lineWidth += paint.measureText(token)
            }
        }
        if (line.isNotEmpty()) flush()
    }
    return lines
}

/** Divide il testo nei punti in cui si può andare a capo: dopo gli spazi, i trattini e le barre. */
private fun breakTokens(text: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    text.forEachIndexed { index, ch ->
        current.append(ch)
        val next = text.getOrNull(index + 1)
        val breakAfter = when {
            ch == ' ' -> next != ' '
            ch == '-' || ch == '/' || ch == '–' -> next != null && next != ' ' && current.length > 1
            else -> false
        }
        if (breakAfter) {
            tokens += current.toString()
            current.clear()
        }
    }
    if (current.isNotEmpty()) tokens += current.toString()
    return tokens
}

/** Accorcia [text] con i puntini di sospensione in modo che stia in [width] punti con [paint]. */
fun ellipsize(text: String, paint: PdfTextPaint, width: Float): String {
    if (width <= 0f) return ""
    if (paint.measureText(text) <= width) return text
    val ellipsis = "…"
    val available = width - paint.measureText(ellipsis)
    if (available <= 0f) return ""
    var low = 0
    var high = text.length
    while (low < high) {
        val mid = (low + high + 1) / 2
        if (paint.measureText(text.substring(0, mid)) <= available) low = mid else high = mid - 1
    }
    if (low > 0 && text[low - 1].isHighSurrogate()) low--
    return text.substring(0, low).trimEnd() + ellipsis
}

/** Disegna una sola riga di testo accorciandola se supera [maxWidth]. */
fun PdfCanvas.drawTextFitted(text: String, x: Float, y: Float, maxWidth: Float, paint: PdfTextPaint) {
    drawText(ellipsize(text, paint, maxWidth), x, y, paint)
}

fun PdfCanvas.drawLayout(layout: TextLayout, x: Float, y: Float) {
    layout.draw(this, x, y)
}

/** Baseline that vertically centres [paint]'s text inside a box of [top]..[bottom]. */
fun centredBaseline(paint: PdfTextPaint, top: Float, bottom: Float): Float =
    (top + bottom) / 2f - (paint.ascent + paint.descent) / 2f

// ---------------------------------------------------------------------------
// Flow blocks and pagination
// ---------------------------------------------------------------------------

abstract class PdfBlock {
    abstract fun measure(width: Float): Float
    abstract fun draw(canvas: PdfCanvas, x: Float, y: Float, width: Float)

    /** Drawn at the top of a new page before this block when the block opens that page. */
    open val continuationHeader: PdfBlock? = null

    /** Keep on the same page as the block that follows. */
    open val keepWithNext: Boolean = false
}

class SpacerBlock(private val height: Float) : PdfBlock() {
    override fun measure(width: Float): Float = height
    override fun draw(canvas: PdfCanvas, x: Float, y: Float, width: Float) = Unit
}

class TextBlock(
    private val text: CharSequence,
    private val paint: PdfTextPaint,
    private val align: PdfAlign = PdfAlign.LEFT,
    private val lineSpacing: Float = 1.2f,
    private val paddingTop: Float = 0f,
    private val paddingBottom: Float = 0f,
    private val indent: Float = 0f,
    override val keepWithNext: Boolean = false,
) : PdfBlock() {
    private var cachedWidth = -1f
    private var cached: TextLayout? = null

    private fun layout(width: Float): TextLayout {
        val w = width - indent
        if (cached == null || cachedWidth != w) {
            cached = layoutText(text, paint, w, align, lineSpacing)
            cachedWidth = w
        }
        return cached!!
    }

    override fun measure(width: Float): Float = paddingTop + layout(width).height + paddingBottom

    override fun draw(canvas: PdfCanvas, x: Float, y: Float, width: Float) {
        canvas.drawLayout(layout(width), x + indent, y + paddingTop)
    }
}

/** A block drawn by a lambda; height can depend on the width. */
class DrawBlock(
    private val heightOf: (width: Float) -> Float,
    override val keepWithNext: Boolean = false,
    override val continuationHeader: PdfBlock? = null,
    private val onDraw: (canvas: PdfCanvas, x: Float, y: Float, width: Float, height: Float) -> Unit,
) : PdfBlock() {
    override fun measure(width: Float): Float = heightOf(width)
    override fun draw(canvas: PdfCanvas, x: Float, y: Float, width: Float) {
        onDraw(canvas, x, y, width, heightOf(width))
    }
}

/** Several blocks kept together on one page. */
class GroupBlock(private val children: List<PdfBlock>) : PdfBlock() {
    override fun measure(width: Float): Float = children.sumOf { it.measure(width).toDouble() }.toFloat()
    override fun draw(canvas: PdfCanvas, x: Float, y: Float, width: Float) {
        var cursor = y
        children.forEach { child ->
            child.draw(canvas, x, cursor, width)
            cursor += child.measure(width)
        }
    }
}

data class PlacedBlock(val block: PdfBlock, val y: Float)

class PdfPaginator(private val top: Float, private val bottom: Float, private val width: Float) {

    fun paginate(blocks: List<PdfBlock>): List<List<PlacedBlock>> {
        val pages = mutableListOf<List<PlacedBlock>>()
        var page = mutableListOf<PlacedBlock>()
        var cursor = top

        fun newPage() {
            pages += page
            page = mutableListOf()
            cursor = top
        }

        blocks.forEachIndexed { index, block ->
            val height = block.measure(width)
            var needed = height
            if (block.keepWithNext && index + 1 < blocks.size) {
                needed += blocks[index + 1].measure(width)
            }
            if (cursor + needed > bottom && page.isNotEmpty()) {
                newPage()
                block.continuationHeader?.let { header ->
                    page += PlacedBlock(header, cursor)
                    cursor += header.measure(width)
                }
            }
            page += PlacedBlock(block, cursor)
            cursor += height
        }
        if (page.isNotEmpty() || pages.isEmpty()) pages += page
        return pages
    }
}
