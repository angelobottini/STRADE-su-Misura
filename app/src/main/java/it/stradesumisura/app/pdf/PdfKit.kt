package it.stradesumisura.app.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

/**
 * Small layout kit on top of android.graphics for building paginated A4 reports with
 * PdfDocument. Everything is measured in PostScript points (1/72 inch).
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

object PdfPalette {
    val ink: Int = Color.rgb(0x14, 0x12, 0x14)
    val graphite: Int = Color.rgb(0x5F, 0x58, 0x5C)
    val muted: Int = Color.rgb(0x8A, 0x82, 0x86)
    val mauve: Int = Color.rgb(0xC7, 0xAD, 0xBF)
    val mauveDeep: Int = Color.rgb(0x7F, 0x60, 0x74)
    val mauveSoft: Int = Color.rgb(0xF4, 0xED, 0xF1)
    val zebra: Int = Color.rgb(0xFA, 0xF7, 0xF9)
    val rule: Int = Color.rgb(0xE2, 0xD9, 0xDE)
    val white: Int = Color.WHITE
}

/**
 * Caratteri dei report. Vengono usati i file Roboto statici (TrueType) inclusi negli asset:
 * con i font variabili di sistema PdfDocument incorpora glifi Type3 con avanzamenti arrotondati,
 * e il testo risulta con spaziatura irregolare. Chiamare [load] prima di creare i Paint.
 */
object PdfFonts {
    @Volatile
    private var loaded = false

    var regular: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        private set
    var medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        private set
    var bold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
        private set
    var black: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        private set
    var italic: Typeface = Typeface.create("sans-serif", Typeface.ITALIC)
        private set
    var light: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        private set

    @Synchronized
    fun load(context: Context) {
        if (loaded) return
        val assets = context.applicationContext.assets
        fun asset(name: String, fallback: Typeface): Typeface =
            runCatching { Typeface.createFromAsset(assets, "fonts/$name") }.getOrDefault(fallback)
        regular = asset("Roboto-Regular.ttf", regular)
        medium = asset("Roboto-Medium.ttf", medium)
        bold = asset("Roboto-Bold.ttf", bold)
        black = bold
        italic = asset("Roboto-Italic.ttf", italic)
        light = asset("Roboto-Light.ttf", light)
        loaded = true
    }
}

fun textPaint(
    size: Float,
    color: Int = PdfPalette.ink,
    typeface: Typeface = PdfFonts.regular,
    letterSpacing: Float = 0f,
    align: Paint.Align = Paint.Align.LEFT,
): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    textSize = size
    this.color = color
    this.typeface = typeface
    this.letterSpacing = letterSpacing
    textAlign = align
}

fun fillPaint(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    this.color = color
}

fun strokePaint(color: Int, width: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = width
    this.color = color
}

fun layoutText(
    text: CharSequence,
    paint: TextPaint,
    width: Float,
    align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    lineSpacing: Float = 1.2f,
): StaticLayout = StaticLayout.Builder
    .obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
    .setAlignment(align)
    .setLineSpacing(0f, lineSpacing)
    .setIncludePad(false)
    .build()

/** Accorcia [text] con i puntini di sospensione in modo che stia in [width] punti con [paint]. */
fun ellipsize(text: String, paint: TextPaint, width: Float): String =
    if (width <= 0f) "" else TextUtils.ellipsize(text, paint, width, TextUtils.TruncateAt.END).toString()

/** Disegna una sola riga di testo accorciandola se supera [maxWidth]. */
fun Canvas.drawTextFitted(text: String, x: Float, y: Float, maxWidth: Float, paint: TextPaint) {
    drawText(ellipsize(text, paint, maxWidth), x, y, paint)
}

fun Canvas.drawLayout(layout: StaticLayout, x: Float, y: Float) {
    save()
    translate(x, y)
    layout.draw(this)
    restore()
}

fun Canvas.drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, paint: Paint) {
    drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint)
}

/** Baseline that vertically centres [paint]'s text inside a box of [top]..[bottom]. */
fun centredBaseline(paint: Paint, top: Float, bottom: Float): Float {
    val fm = paint.fontMetrics
    return (top + bottom) / 2f - (fm.ascent + fm.descent) / 2f
}

// ---------------------------------------------------------------------------
// Flow blocks and pagination
// ---------------------------------------------------------------------------

abstract class PdfBlock {
    abstract fun measure(width: Float): Float
    abstract fun draw(canvas: Canvas, x: Float, y: Float, width: Float)

    /** Drawn at the top of a new page before this block when the block opens that page. */
    open val continuationHeader: PdfBlock? = null

    /** Keep on the same page as the block that follows. */
    open val keepWithNext: Boolean = false
}

class SpacerBlock(private val height: Float) : PdfBlock() {
    override fun measure(width: Float): Float = height
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) = Unit
}

class TextBlock(
    private val text: CharSequence,
    private val paint: TextPaint,
    private val align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    private val lineSpacing: Float = 1.2f,
    private val paddingTop: Float = 0f,
    private val paddingBottom: Float = 0f,
    private val indent: Float = 0f,
    override val keepWithNext: Boolean = false,
) : PdfBlock() {
    private var cachedWidth = -1f
    private var cached: StaticLayout? = null

    private fun layout(width: Float): StaticLayout {
        val w = width - indent
        if (cached == null || cachedWidth != w) {
            cached = layoutText(text, paint, w, align, lineSpacing)
            cachedWidth = w
        }
        return cached!!
    }

    override fun measure(width: Float): Float = paddingTop + layout(width).height + paddingBottom

    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) {
        canvas.drawLayout(layout(width), x + indent, y + paddingTop)
    }
}

/** A block drawn by a lambda; height can depend on the width. */
class DrawBlock(
    private val heightOf: (width: Float) -> Float,
    override val keepWithNext: Boolean = false,
    override val continuationHeader: PdfBlock? = null,
    private val onDraw: (canvas: Canvas, x: Float, y: Float, width: Float, height: Float) -> Unit,
) : PdfBlock() {
    override fun measure(width: Float): Float = heightOf(width)
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) {
        onDraw(canvas, x, y, width, heightOf(width))
    }
}

/** Several blocks kept together on one page. */
class GroupBlock(private val children: List<PdfBlock>) : PdfBlock() {
    override fun measure(width: Float): Float = children.sumOf { it.measure(width).toDouble() }.toFloat()
    override fun draw(canvas: Canvas, x: Float, y: Float, width: Float) {
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
