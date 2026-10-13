package it.stradesumisura.app.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import it.stradesumisura.app.data.EvaluatorProfile
import it.stradesumisura.app.data.db.AssessmentEntity
import it.stradesumisura.app.data.db.PersonEntity
import it.stradesumisura.app.domain.AnswersCodec
import it.stradesumisura.app.domain.AssessmentAnswers
import it.stradesumisura.app.domain.ScoreBand
import it.stradesumisura.app.domain.ScoreResult
import it.stradesumisura.app.domain.Selection
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolItem
import it.stradesumisura.app.util.Dates
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max

/**
 * Costruisce il report A4 di una valutazione: intestazione con il marchio, schede persona e
 * valutazione, descrizione del compito (se prevista), pannello del punteggio con barra
 * interpretativa e valori inseriti evidenziati, tabella degli item, osservazioni, legenda,
 * riferimento bibliografico, spazio per la firma e piè di pagina numerato.
 *
 * Il report viene impaginato con tre densità crescenti (normale, compatta, densa): si usa la prima
 * che sta in una sola pagina; se nessuna basta, si usa la versione normale su più pagine.
 * Tutti i testi su una riga vengono accorciati con i puntini quando superano il loro riquadro.
 */
class AssessmentPdfExporter(private val context: Context) {

    data class Input(
        val assessment: AssessmentEntity,
        val person: PersonEntity,
        val tool: ToolDefinition,
        val profile: EvaluatorProfile,
    )

    /** Spaziature e dimensioni per i tre livelli di densità (0 = normale, 1 = compatta, 2 = densa). */
    private class Density(val level: Int) {
        private fun <T> pick(normal: T, compact: T, dense: T): T = when (level) {
            0 -> normal
            1 -> compact
            else -> dense
        }

        val gapSmall = pick(14f, 8f, 6f)
        val gapMedium = pick(16f, 10f, 8f)
        val titleSize = pick(19f, 17f, 16f)
        val subtitlePaddingBottom = pick(14f, 10f, 8f)
        val cardPadding = pick(12f, 10f, 9f)
        val cardRowGap = pick(5f, 4f, 3f)
        val showToolRow = level == 0
        val hideEmptyRows = level == 2
        val rowPadding = pick(7f, 5f, 4f)
        val descSize = pick(7.6f, 7.2f, 7f)
        val computedSubtitle = level < 2
        val showBandDescription = level < 2
        val boxTextSize = pick(9f, 8.5f, 8.2f)
        val boxPadding = pick(10f, 8f, 7f)
        val boxSpacing = pick(1.3f, 1.22f, 1.18f)
        val legendColumns = pick(1, 1, 3)
        val legendRowGap = pick(4f, 3f, 3f)
        val referenceSize = pick(7.5f, 7.5f, 7f)
        val signatureGap = pick(26f, 14f, 8f)
        val signatureHeight = pick(54f, 46f, 40f)
    }

    fun fileName(input: Input): String {
        val raw = "${input.tool.shortName}_${input.person.lastName}_${input.person.firstName}_${input.assessment.date}"
        val safe = raw.replace(Regex("[^A-Za-z0-9À-ÿ_-]+"), "_").trim('_')
        return "$safe.pdf"
    }

    fun export(input: Input): File {
        PdfFonts.load(context)
        val document = PdfDocument()
        val generatedAt = Dates.formatTimestamp(System.currentTimeMillis())
        val answers = AnswersCodec.decode(input.assessment.answersJson)
        val score = input.tool.score(answers)
        val paginator = PdfPaginator(PdfPage.contentTop, PdfPage.contentBottom, PdfPage.contentWidth)

        val normal = paginator.paginate(buildBlocks(input, answers, score, Density(0)))
        val pages = if (normal.size == 1) {
            normal
        } else {
            (1..2).asSequence()
                .map { level -> paginator.paginate(buildBlocks(input, answers, score, Density(level))) }
                .firstOrNull { it.size == 1 } ?: normal
        }

        pages.forEachIndexed { index, placed ->
            val info = PdfDocument.PageInfo.Builder(PdfPage.WIDTH.toInt(), PdfPage.HEIGHT.toInt(), index + 1).create()
            val page = document.startPage(info)
            val canvas = page.canvas
            drawHeader(canvas, input)
            placed.forEach { it.block.draw(canvas, PdfPage.MARGIN_X, it.y, PdfPage.contentWidth) }
            drawFooter(canvas, input, index + 1, pages.size, generatedAt)
            document.finishPage(page)
        }

        val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val file = File(dir, fileName(input))
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    // -----------------------------------------------------------------------
    // Intestazione e piè di pagina (su ogni pagina)
    // -----------------------------------------------------------------------

    private fun drawHeader(canvas: Canvas, input: Input) {
        val top = PdfPage.MARGIN_TOP
        val right = PdfPage.WIDTH - PdfPage.MARGIN_X
        val markWidth = drawBrandMark(canvas, PdfPage.MARGIN_X, top, 34f)
        val textWidth = right - (PdfPage.MARGIN_X + markWidth + 16f)

        val kicker = textPaint(7.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.12f, align = Paint.Align.RIGHT)
        canvas.drawText("REPORT DI VALUTAZIONE", right, top + 10f, kicker)

        val profile = input.profile
        val line1 = profile.organization.ifBlank { profile.name }
        val line2 = if (profile.organization.isNotBlank()) {
            listOf(profile.name, profile.qualification).filter { it.isNotBlank() }.joinToString(" · ")
        } else {
            profile.qualification.takeIf { profile.name.isNotBlank() } ?: ""
        }
        var y = top + 24f
        if (line1.isNotBlank()) {
            canvas.drawTextFitted(line1, right, y, textWidth, textPaint(9.5f, PdfPalette.ink, PdfFonts.medium, align = Paint.Align.RIGHT))
            y += 12f
        }
        if (line2.isNotBlank()) {
            canvas.drawTextFitted(line2, right, y, textWidth, textPaint(8.5f, PdfPalette.graphite, align = Paint.Align.RIGHT))
        }

        val ruleY = top + PdfPage.HEADER_HEIGHT - 12f
        canvas.drawLine(PdfPage.MARGIN_X, ruleY, right, ruleY, strokePaint(PdfPalette.rule, 0.7f))
    }

    private fun drawFooter(canvas: Canvas, input: Input, page: Int, pages: Int, generatedAt: String) {
        val right = PdfPage.WIDTH - PdfPage.MARGIN_X
        val ruleY = PdfPage.HEIGHT - PdfPage.MARGIN_BOTTOM - PdfPage.FOOTER_HEIGHT + 6f
        canvas.drawLine(PdfPage.MARGIN_X, ruleY, right, ruleY, strokePaint(PdfPalette.rule, 0.7f))

        val small = textPaint(7f, PdfPalette.muted)
        val pagePaint = textPaint(7.5f, PdfPalette.graphite, PdfFonts.medium, align = Paint.Align.RIGHT)
        val pageText = "Pagina $page di $pages"
        val leftWidth = right - PdfPage.MARGIN_X - pagePaint.measureText(pageText) - 12f
        canvas.drawTextFitted(
            "STRADE su Misura · ${input.tool.name} · generato il $generatedAt",
            PdfPage.MARGIN_X, ruleY + 12f, leftWidth, small,
        )
        canvas.drawTextFitted(
            "Documento riservato: contiene dati personali e sanitari da trattare secondo la normativa vigente.",
            PdfPage.MARGIN_X, ruleY + 22f, right - PdfPage.MARGIN_X, small,
        )
        canvas.drawText(pageText, right, ruleY + 12f, pagePaint)
    }

    /** Il marchio STRADE: banda diagonale malva dietro la banda nera con la scritta spaziata. Restituisce la larghezza. */
    private fun drawBrandMark(canvas: Canvas, x: Float, y: Float, height: Float): Float {
        val band = height * 0.46f
        val bandTop = y + height * 0.27f
        val word = textPaint(band * 0.56f, PdfPalette.white, PdfFonts.black, letterSpacing = 0.22f)
        val trailing = word.textSize * 0.22f
        val pad = band * 0.55f
        val bandWidth = word.measureText("STRADE") - trailing + pad * 2

        val cx = x + bandWidth * 0.40f
        val cy = bandTop + band / 2f
        val length = height * 1.0f
        canvas.save()
        canvas.rotate(-40f, cx, cy)
        canvas.drawRect(cx - length / 2f, cy - band / 2f, cx + length / 2f, cy + band / 2f, fillPaint(PdfPalette.mauve))
        canvas.restore()

        canvas.drawRect(x, bandTop, x + bandWidth, bandTop + band, fillPaint(PdfPalette.ink))
        canvas.drawText("STRADE", x + pad, centredBaseline(word, bandTop, bandTop + band), word)

        val caption = textPaint(height * 0.19f, PdfPalette.ink, PdfFonts.bold, align = Paint.Align.RIGHT)
        canvas.drawText("su Misura", x + bandWidth, bandTop + band + height * 0.22f, caption)
        return bandWidth
    }

    // -----------------------------------------------------------------------
    // Contenuto
    // -----------------------------------------------------------------------

    private fun buildBlocks(input: Input, answers: AssessmentAnswers, score: ScoreResult, d: Density): List<PdfBlock> {
        val tool = input.tool
        val blocks = mutableListOf<PdfBlock>()

        blocks += TextBlock(tool.name, textPaint(d.titleSize, PdfPalette.ink, PdfFonts.bold), lineSpacing = 1.1f, keepWithNext = true)
        blocks += TextBlock(
            "${tool.subtitle} · ${tool.citation}",
            textPaint(9.5f, PdfPalette.graphite),
            paddingTop = 4f,
            paddingBottom = d.subtitlePaddingBottom,
            keepWithNext = true,
        )
        blocks += infoCardsBlock(input, answers, d)

        // Campi estesi (es. la descrizione del compito) in un riquadro a tutta larghezza.
        tool.fields.filter { it.multiline }.forEach { field ->
            val value = answers.fields[field.id]?.trim().orEmpty()
            if (value.isNotEmpty()) {
                blocks += SpacerBlock(d.gapSmall)
                blocks += sectionTitle(field.label)
                blocks += boxedTextBlock(value, d)
            }
        }

        blocks += SpacerBlock(d.gapSmall)
        blocks += scoreBlock(score, tool, answers, d)
        blocks += SpacerBlock(d.gapMedium)

        blocks += sectionTitle(tool.itemsSectionTitle)
        val header = tableHeaderBlock(tool)
        blocks += header
        var number = 0
        tool.primaryItems.forEachIndexed { index, item ->
            val selection = tool.selectionFor(item, answers)
            val noteKey = selection?.item?.id ?: item.id
            val note = answers.itemNotes[noteKey] ?: answers.itemNotes[item.id]
            if (item.isComputed) {
                val explanation = tool.computedExplanation(item, answers)
                blocks += rowBlock("", item, selection, note, explanation, zebra = false, header = header, emphasised = true, d = d)
            } else {
                number += 1
                val highlighted = item.id in tool.highlightedItemIds
                blocks += rowBlock("$number.", item, selection, note, null, index % 2 == 1, header, emphasised = highlighted, d = d)
            }
        }
        if (tool.sumsItems) {
            blocks += totalRowBlock(score, tool, header)
        }
        blocks += SpacerBlock(d.gapMedium)

        val notes = input.assessment.notes?.trim().orEmpty()
        if (notes.isNotEmpty()) {
            blocks += sectionTitle(tool.notesLabel)
            blocks += boxedTextBlock(notes, d)
            blocks += SpacerBlock(d.gapMedium)
        }

        blocks += sectionTitle("Interpretazione del punteggio")
        blocks += legendBlock(tool.bands, score, d)
        blocks += SpacerBlock(if (d.level == 0) 10f else 6f)
        blocks += TextBlock(
            "Riferimento: ${tool.reference}",
            textPaint(d.referenceSize, PdfPalette.muted, PdfFonts.italic),
            lineSpacing = 1.25f,
        )

        if (input.profile.includeSignature) {
            blocks += SpacerBlock(d.signatureGap)
            blocks += signatureBlock(input, d)
        }
        return blocks
    }

    private fun sectionTitle(title: String): PdfBlock = TextBlock(
        title.uppercase(),
        textPaint(8f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.12f),
        paddingBottom = 6f,
        keepWithNext = true,
    )

    private data class InfoRow(val label: String, val value: String)

    private fun infoCardsBlock(input: Input, answers: AssessmentAnswers, d: Density): PdfBlock {
        val person = input.person
        val assessment = input.assessment
        val birth = Dates.parse(person.birthDate)
        val evaluationDate = Dates.parse(assessment.date)
        val age = Dates.ageAt(birth, evaluationDate ?: Dates.today())
        val sexLabel = when (person.sex) {
            "M" -> "Maschio"
            "F" -> "Femmina"
            "X" -> "Altro"
            else -> "—"
        }
        val personRows = listOf(
            InfoRow("Nome", person.fullName),
            InfoRow("Nascita", if (birth != null) "${Dates.formatLong(birth)}${age?.let { " ($it anni)" } ?: ""}" else "—"),
            InfoRow("Sesso", sexLabel),
            InfoRow("Codice", person.code?.takeIf { it.isNotBlank() } ?: "—"),
        ).let { rows -> if (d.hideEmptyRows) rows.filter { it.value != "—" } else rows }
        val evaluator = listOf(assessment.evaluator, input.profile.qualification.takeIf { !assessment.evaluator.isNullOrBlank() })
            .filter { !it.isNullOrBlank() }.joinToString(" · ")
        val evaluationRows = buildList {
            add(InfoRow("Data", Dates.formatLong(evaluationDate)))
            add(InfoRow("Valutatore", evaluator.ifBlank { "—" }))
            add(InfoRow("Contesto", assessment.setting?.takeIf { it.isNotBlank() } ?: "—"))
            // I campi estesi hanno un riquadro dedicato sotto le schede.
            input.tool.fields.filter { !it.multiline }.forEach { field ->
                add(InfoRow(field.pdfLabel, answers.fields[field.id]?.takeIf { it.isNotBlank() } ?: "—"))
            }
            if (d.showToolRow) add(InfoRow("Strumento", "${input.tool.name} (${input.tool.citation})"))
        }.let { rows -> if (d.hideEmptyRows) rows.filter { it.value != "—" } else rows }

        val gap = 12f
        val labelPaint = textPaint(7.5f, PdfPalette.muted, PdfFonts.medium)
        val valuePaint = textPaint(9.5f, PdfPalette.ink, PdfFonts.medium)
        val captionPaint = textPaint(7.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.12f)
        val padding = d.cardPadding
        val labelWidth = 58f
        val rowGap = d.cardRowGap

        fun rowsHeight(rows: List<InfoRow>, cardWidth: Float): Float {
            val valueWidth = cardWidth - padding * 2 - labelWidth
            return rows.sumOf { max(layoutText(it.value, valuePaint, valueWidth).height.toFloat(), 11f).toDouble() + rowGap }.toFloat()
        }

        fun cardHeight(width: Float): Float {
            val cardWidth = (width - gap) / 2f
            return padding + 18f + max(rowsHeight(personRows, cardWidth), rowsHeight(evaluationRows, cardWidth)) + padding - rowGap
        }

        fun drawCard(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, caption: String, rows: List<InfoRow>) {
            canvas.drawRoundRect(x, y, x + w, y + h, 8f, fillPaint(PdfPalette.mauveSoft))
            canvas.drawText(caption, x + padding, y + padding + 6f, captionPaint)
            var cursor = y + padding + 18f
            val valueWidth = w - padding * 2 - labelWidth
            rows.forEach { row ->
                val layout = layoutText(row.value, valuePaint, valueWidth)
                canvas.drawTextFitted(row.label, x + padding, cursor + 8.5f, labelWidth - 4f, labelPaint)
                canvas.drawLayout(layout, x + padding + labelWidth, cursor)
                cursor += max(layout.height.toFloat(), 11f) + rowGap
            }
        }

        return DrawBlock(heightOf = { cardHeight(it) }, keepWithNext = true) { canvas, x, y, width, height ->
            val cardWidth = (width - gap) / 2f
            drawCard(canvas, x, y, cardWidth, height, "PERSONA", personRows)
            drawCard(canvas, x + cardWidth + gap, y, cardWidth, height, "VALUTAZIONE", evaluationRows)
        }
    }

    /** Valore inserito dal valutatore da evidenziare nel pannello del punteggio. */
    private data class Highlight(val label: String, val value: String, val max: Int)

    private fun highlightsOf(tool: ToolDefinition, answers: AssessmentAnswers): List<Highlight> =
        tool.highlightedItems.map { item ->
            val selection = tool.selectionFor(item, answers)
            Highlight(item.compactTitle, selection?.option?.score?.toString() ?: "—", item.maxScore)
        }

    /**
     * Pannello del punteggio: a sinistra il punteggio principale (per gli strumenti calcolati è il
     * valore calcolato) con la fascia; a destra la barra interpretativa e, se previsti, i valori
     * inseriti dal valutatore evidenziati con la formula di calcolo.
     */
    private fun scoreBlock(score: ScoreResult, tool: ToolDefinition, answers: AssessmentAnswers, d: Density): PdfBlock {
        val band = score.band
        val padding = 12f
        val bigPaint = textPaint(32f, PdfPalette.ink, PdfFonts.black)
        val bigBaseline = padding + 40f
        val kickerPaint = textPaint(7.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.12f)
        val labelPaint = textPaint(10.5f, band?.color?.toInt() ?: PdfPalette.graphite, PdfFonts.bold)
        val descPaint = textPaint(7.8f, PdfPalette.graphite)
        val label = when {
            band != null -> band.label
            !score.isComplete -> "Valutazione incompleta (${score.answered}/${score.required} ${tool.itemNounPlural})"
            else -> "—"
        }
        val description = when {
            band != null -> if (d.showBandDescription) band.description else ""
            !score.isComplete -> "Punteggio parziale: completare tutti i ${tool.itemNounPlural} per ottenere l'interpretazione."
            else -> ""
        }
        val highlights = highlightsOf(tool, answers)
        val headline = tool.headlineItem
        val explanation = if (headline != null && headline.isComputed) tool.computedExplanation(headline, answers) else null
        val showsComputed = headline != null && headline.isComputed
        val totalText = if (score.isComplete || tool.sumsItems) score.total.toString() else "—"

        val barTopOffset = padding + 30f
        val barHeight = 10f
        val pillHeight = 24f
        val pillTopOffset = barTopOffset + barHeight + 18f
        val calcLineHeight = if (showsComputed) 12f else 0f

        fun leftWidthOf(width: Float) = width * 0.44f

        fun leftHeight(width: Float): Float {
            val textWidth = leftWidthOf(width) - padding
            val labelH = layoutText(label, labelPaint, textWidth, lineSpacing = 1.1f).height.toFloat()
            val descH = if (description.isBlank()) 0f else layoutText(description, descPaint, textWidth, lineSpacing = 1.15f).height + 4f
            return bigBaseline + 6f + labelH + descH + padding
        }

        fun rightHeight(): Float =
            if (highlights.isEmpty()) {
                barTopOffset + barHeight + 12f + padding
            } else {
                pillTopOffset + pillHeight + calcLineHeight + padding
            }

        return DrawBlock(heightOf = { width -> max(leftHeight(width), rightHeight()) }) { canvas, x, y, width, h ->
            canvas.drawRoundRect(x, y, x + width, y + h, 10f, strokePaint(PdfPalette.rule, 0.8f))
            val leftWidth = leftWidthOf(width)
            val textWidth = leftWidth - padding

            // Colonna sinistra: punteggio principale e fascia.
            canvas.drawTextFitted(tool.totalLabel.uppercase(), x + padding, y + padding + 6f, textWidth, kickerPaint)
            val baseline = y + bigBaseline
            canvas.drawText(totalText, x + padding, baseline, bigPaint)
            val ofPaint = textPaint(12f, PdfPalette.graphite, PdfFonts.medium)
            canvas.drawText("/ ${score.max}", x + padding + bigPaint.measureText(totalText) + 6f, baseline, ofPaint)
            if (showsComputed) {
                val tagPaint = textPaint(6.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.08f)
                val tagX = x + padding + bigPaint.measureText(totalText) + 6f + ofPaint.measureText("/ ${score.max}") + 8f
                canvas.drawTextFitted("CALCOLATO", tagX, baseline, x + leftWidth - 4f - tagX, tagPaint)
            }
            val labelLayout = layoutText(label, labelPaint, textWidth, lineSpacing = 1.1f)
            canvas.drawLayout(labelLayout, x + padding, baseline + 6f)
            if (description.isNotBlank()) {
                canvas.drawLayout(layoutText(description, descPaint, textWidth, lineSpacing = 1.15f), x + padding, baseline + 6f + labelLayout.height + 4f)
            }

            // Colonna destra: barra interpretativa.
            val barLeft = x + leftWidth + padding
            val barRight = x + width - padding
            val barTop = y + barTopOffset
            val bands = tool.bands
            val span = (tool.maxScore - tool.minScore + 1).toFloat()
            val weights = bands.map { max((it.max - it.min + 1).toFloat() / span, 0.085f) }
            val totalWeight = weights.sum()
            var cursor = barLeft
            val segments = bands.mapIndexed { index, b ->
                val w = (barRight - barLeft) * weights[index] / totalWeight
                val segment = b to (cursor to cursor + w)
                cursor += w
                segment
            }
            canvas.save()
            val clip = Path().apply { addRoundRect(barLeft, barTop, barRight, barTop + barHeight, 5f, 5f, Path.Direction.CW) }
            canvas.clipPath(clip)
            segments.forEach { (b, range) ->
                canvas.drawRect(range.first, barTop, range.second, barTop + barHeight, fillPaint(b.color.toInt()))
            }
            canvas.restore()

            val rangePaint = textPaint(6.5f, PdfPalette.muted, align = Paint.Align.CENTER)
            segments.forEach { (b, range) ->
                canvas.drawTextFitted(b.rangeLabel, (range.first + range.second) / 2f, barTop + barHeight + 9f, range.second - range.first, rangePaint)
            }

            val current = if (score.isComplete) band else null
            if (current != null) {
                val segment = segments.first { it.first == current }
                val bandSpan = (current.max - current.min + 1).toFloat()
                val fraction = ((score.total - current.min) + 0.5f) / bandSpan
                val mx = segment.second.first + (segment.second.second - segment.second.first) * fraction
                val marker = Path().apply {
                    moveTo(mx, barTop - 3f)
                    lineTo(mx - 5f, barTop - 10f)
                    lineTo(mx + 5f, barTop - 10f)
                    close()
                }
                canvas.drawPath(marker, fillPaint(PdfPalette.ink))
                canvas.drawText(
                    score.total.toString(), mx, barTop - 13f,
                    textPaint(8f, PdfPalette.ink, PdfFonts.bold, align = Paint.Align.CENTER),
                )
            }
            canvas.drawTextFitted(
                "Interpretazione (${tool.shortName}, ${tool.scoreRangeLabel})",
                barLeft, y + padding + 6f, barRight - barLeft, kickerPaint,
            )

            // Valori inseriti dal valutatore, evidenziati sotto la barra.
            if (highlights.isNotEmpty()) {
                val pillTop = y + pillTopOffset
                drawHighlightPills(canvas, barLeft, pillTop, barRight - barLeft, pillHeight, highlights)
                if (explanation != null) {
                    canvas.drawTextFitted(
                        "Calcolo: $explanation",
                        barLeft, pillTop + pillHeight + 10f, barRight - barLeft,
                        textPaint(6.8f, PdfPalette.graphite, PdfFonts.italic),
                    )
                } else if (showsComputed) {
                    canvas.drawTextFitted(
                        "Il punteggio viene calcolato quando entrambi i valori sono inseriti.",
                        barLeft, pillTop + pillHeight + 10f, barRight - barLeft,
                        textPaint(6.8f, PdfPalette.muted, PdfFonts.italic),
                    )
                }
            }
        }
    }

    private fun drawHighlightPills(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, highlights: List<Highlight>) {
        val gap = 8f
        val pillWidth = (width - gap * (highlights.size - 1)) / highlights.size
        val labelPaint = textPaint(6.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.08f)
        val valuePaint = textPaint(13f, PdfPalette.ink, PdfFonts.black, align = Paint.Align.RIGHT)
        val maxPaint = textPaint(6.5f, PdfPalette.muted, align = Paint.Align.RIGHT)
        var cursor = x
        highlights.forEach { item ->
            canvas.drawRoundRect(cursor, y, cursor + pillWidth, y + height, 6f, fillPaint(PdfPalette.mauveSoft))
            canvas.drawRoundRect(cursor, y, cursor + pillWidth, y + height, 6f, strokePaint(PdfPalette.mauve, 0.8f))
            val baseline = centredBaseline(valuePaint, y, y + height)
            val maxText = "/ ${item.max}"
            canvas.drawText(maxText, cursor + pillWidth - 7f, baseline, maxPaint)
            val valueRight = cursor + pillWidth - 7f - maxPaint.measureText(maxText) - 3f
            canvas.drawText(item.value, valueRight, baseline, valuePaint)
            val labelWidth = valueRight - valuePaint.measureText(item.value) - cursor - 14f
            canvas.drawTextFitted(item.label.uppercase(), cursor + 7f, baseline - 1f, labelWidth, labelPaint)
            cursor += pillWidth + gap
        }
    }

    private val colActivity = 0.30f
    private val colLevel = 0.56f

    private fun tableHeaderBlock(tool: ToolDefinition): PdfBlock {
        val height = 20f
        val paint = textPaint(7.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.1f)
        return DrawBlock(heightOf = { height }, keepWithNext = true) { canvas, x, y, width, h ->
            canvas.drawRoundRect(x, y, x + width, y + h, 4f, fillPaint(PdfPalette.mauveSoft))
            val baseline = centredBaseline(paint, y, y + h)
            canvas.drawTextFitted(tool.itemNounSingular.uppercase(), x + 8f, baseline, width * colActivity - 16f, paint)
            canvas.drawText("LIVELLO RILEVATO", x + width * colActivity + 8f, baseline, paint)
            canvas.drawText("PUNTI", x + width - 8f, baseline, textPaint(7.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.1f, align = Paint.Align.RIGHT))
        }
    }

    private fun rowBlock(
        numberLabel: String,
        item: ToolItem,
        selection: Selection?,
        note: String?,
        explanation: String?,
        zebra: Boolean,
        header: PdfBlock,
        emphasised: Boolean,
        d: Density,
    ): PdfBlock {
        val padding = d.rowPadding
        val computed = item.isComputed
        val titlePaint = textPaint(9f, PdfPalette.ink, PdfFonts.bold)
        val subPaint = textPaint(7.3f, if (computed) PdfPalette.mauveDeep else PdfPalette.muted)
        val levelPaint = textPaint(8.8f, PdfPalette.ink, if (emphasised) PdfFonts.bold else PdfFonts.medium)
        val missingPaint = textPaint(8.8f, PdfPalette.muted, PdfFonts.italic)
        val descPaint = textPaint(d.descSize, PdfPalette.graphite)
        val notePaint = textPaint(d.descSize, PdfPalette.ink, PdfFonts.italic)
        val calcPaint = textPaint(d.descSize, PdfPalette.mauveDeep, PdfFonts.medium)
        val scorePaint = textPaint(if (emphasised) 11.5f else 9.5f, PdfPalette.ink, if (emphasised) PdfFonts.black else PdfFonts.bold, align = Paint.Align.RIGHT)
        val maxPaint = textPaint(7f, PdfPalette.muted, align = Paint.Align.RIGHT)

        val shownItem = selection?.item ?: item
        val title = if (numberLabel.isBlank()) shownItem.title else "$numberLabel ${shownItem.title}"
        val subtitle = when {
            selection?.isAlternative == true -> "in alternativa a «${item.title}»"
            computed && d.computedSubtitle -> item.hint ?: "Calcolato automaticamente"
            computed -> "Calcolato automaticamente"
            else -> null
        }
        val levelTitle = selection?.option?.title ?: if (computed) "In attesa dei valori" else "Non valutato"
        val description = selection?.option?.description
            ?: if (computed) "Il valore viene calcolato quando tutte le voci di origine sono compilate." else "Nessun livello selezionato."
        val noteText = note?.trim()?.takeIf { it.isNotEmpty() }?.let { "Osservazioni: $it" }
        val calcText = explanation?.let { "Calcolo: ${it.replaceFirstChar(Char::uppercase)}" }
        val maxScore = shownItem.maxScore

        fun heights(width: Float): Triple<Float, Float, Float> {
            val leftW = width * colActivity - 16f
            val midW = width * colLevel - 16f
            val leftH = layoutText(title, titlePaint, leftW, lineSpacing = 1.15f).height +
                (subtitle?.let { layoutText(it, subPaint, leftW, lineSpacing = 1.15f).height + 2 } ?: 0)
            val midH = layoutText(levelTitle, levelPaint, midW, lineSpacing = 1.15f).height + 2 +
                layoutText(description, descPaint, midW, lineSpacing = 1.18f).height +
                (calcText?.let { layoutText(it, calcPaint, midW, lineSpacing = 1.18f).height + 3 } ?: 0) +
                (noteText?.let { layoutText(it, notePaint, midW, lineSpacing = 1.18f).height + 4 } ?: 0)
            val rightH = if (emphasised) 30f else 22f
            val h = max(max(leftH, midH).toFloat(), rightH) + padding * 2
            return Triple(h, leftW, midW)
        }

        return DrawBlock(heightOf = { heights(it).first }, continuationHeader = header) { canvas, x, y, width, h ->
            val (_, leftW, midW) = heights(width)
            when {
                computed -> {
                    canvas.drawRect(x, y, x + width, y + h, fillPaint(PdfPalette.mauveSoft))
                    canvas.drawLine(x, y + 0.5f, x + width, y + 0.5f, strokePaint(PdfPalette.ink, 1f))
                }
                emphasised -> {
                    canvas.drawRect(x, y, x + width, y + h, fillPaint(PdfPalette.zebra))
                    canvas.drawRect(x, y, x + 3f, y + h, fillPaint(PdfPalette.mauve))
                }
                zebra -> canvas.drawRect(x, y, x + width, y + h, fillPaint(PdfPalette.zebra))
            }
            canvas.drawLine(x, y + h, x + width, y + h, strokePaint(PdfPalette.rule, 0.5f))

            val top = y + padding
            val titleLayout = layoutText(title, titlePaint, leftW, lineSpacing = 1.15f)
            canvas.drawLayout(titleLayout, x + 8f, top)
            if (subtitle != null) {
                canvas.drawLayout(layoutText(subtitle, subPaint, leftW, lineSpacing = 1.15f), x + 8f, top + titleLayout.height + 2f)
            }

            val midX = x + width * colActivity + 8f
            val levelLayout = layoutText(levelTitle, if (selection == null) missingPaint else levelPaint, midW, lineSpacing = 1.15f)
            canvas.drawLayout(levelLayout, midX, top)
            val descLayout = layoutText(description, descPaint, midW, lineSpacing = 1.18f)
            canvas.drawLayout(descLayout, midX, top + levelLayout.height + 2f)
            var cursor = top + levelLayout.height + 2f + descLayout.height
            if (calcText != null) {
                val calcLayout = layoutText(calcText, calcPaint, midW, lineSpacing = 1.18f)
                canvas.drawLayout(calcLayout, midX, cursor + 3f)
                cursor += calcLayout.height + 3f
            }
            if (noteText != null) {
                canvas.drawLayout(layoutText(noteText, notePaint, midW, lineSpacing = 1.18f), midX, cursor + 4f)
            }

            val right = x + width - 8f
            val scoreText = selection?.option?.score?.toString() ?: "—"
            if (emphasised && selection != null) {
                // Riquadro che evidenzia il valore inserito (chiaro) o calcolato (scuro).
                val boxW = max(scorePaint.measureText(scoreText), 14f) + 14f
                val boxTop = top - 1f
                val boxH = 20f
                val fill = if (computed) PdfPalette.ink else PdfPalette.mauveSoft
                canvas.drawRoundRect(right - boxW, boxTop, right, boxTop + boxH, 5f, fillPaint(fill))
                if (!computed) canvas.drawRoundRect(right - boxW, boxTop, right, boxTop + boxH, 5f, strokePaint(PdfPalette.mauve, 0.8f))
                val boxed = textPaint(scorePaint.textSize, if (computed) PdfPalette.white else PdfPalette.ink, PdfFonts.black, align = Paint.Align.RIGHT)
                canvas.drawText(scoreText, right - 7f, centredBaseline(boxed, boxTop, boxTop + boxH), boxed)
                canvas.drawText("su $maxScore", right, boxTop + boxH + 9f, maxPaint)
            } else {
                canvas.drawText(scoreText, right, top + 9f, scorePaint)
                canvas.drawText("su $maxScore", right, top + 19f, maxPaint)
            }
        }
    }

    private fun totalRowBlock(score: ScoreResult, tool: ToolDefinition, header: PdfBlock): PdfBlock {
        val height = 26f
        return DrawBlock(heightOf = { height }, continuationHeader = header) { canvas, x, y, width, h ->
            canvas.drawLine(x, y + 0.5f, x + width, y + 0.5f, strokePaint(PdfPalette.ink, 1f))
            val label = textPaint(9.5f, PdfPalette.ink, PdfFonts.bold)
            val baseline = centredBaseline(label, y, y + h)
            canvas.drawText("Totale", x + 8f, baseline, label)
            val status = if (score.isComplete) {
                "valutazione completa"
            } else {
                "valutazione parziale: ${score.answered} ${tool.itemNounPlural} su ${score.required}"
            }
            canvas.drawTextFitted(status, x + width * colActivity + 8f, baseline, width * (1f - colActivity) - 70f, textPaint(8f, PdfPalette.graphite))
            canvas.drawText("${score.total} / ${score.max}", x + width - 8f, baseline, textPaint(11f, PdfPalette.ink, PdfFonts.black, align = Paint.Align.RIGHT))
        }
    }

    /** Riquadro con testo libero a tutta larghezza (descrizione del compito, osservazioni). */
    private fun boxedTextBlock(text: String, d: Density): PdfBlock {
        val paint = textPaint(d.boxTextSize, PdfPalette.ink)
        val padding = d.boxPadding
        return DrawBlock(heightOf = { width -> layoutText(text, paint, width - padding * 2, lineSpacing = d.boxSpacing).height + padding * 2 }) { canvas, x, y, width, h ->
            canvas.drawRoundRect(x, y, x + width, y + h, 8f, strokePaint(PdfPalette.rule, 0.8f))
            canvas.drawLayout(layoutText(text, paint, width - padding * 2, lineSpacing = d.boxSpacing), x + padding, y + padding)
        }
    }

    /**
     * Legenda delle fasce. Su una colonna con descrizione; nella versione densa su più colonne
     * con la sola etichetta (la descrizione della fascia corrente è nel pannello del punteggio).
     */
    private fun legendBlock(bands: List<ScoreBand>, score: ScoreResult, d: Density): PdfBlock {
        val rangePaint = textPaint(8f, PdfPalette.ink, PdfFonts.bold)
        val labelPaint = textPaint(8f, PdfPalette.ink, PdfFonts.medium)
        val descPaint = textPaint(d.descSize, PdfPalette.graphite)
        val rowGap = d.legendRowGap
        val rangeX = 14f
        // La colonna degli intervalli e quella delle etichette si adattano al testo più lungo,
        // così nulla invade la colonna successiva.
        val rangeWidth = bands.maxOf { rangePaint.measureText(it.rangeLabel) } + 8f
        val labelX = rangeX + max(rangeWidth, 26f)
        val columns = d.legendColumns
        val columnGap = 14f

        fun columnWidth(width: Float) = (width - columnGap * (columns - 1)) / columns

        fun layoutFor(width: Float): Pair<Float, Float> {
            val colW = columnWidth(width)
            val maxLabel = bands.maxOf { labelPaint.measureText(it.label) }
            val labelWidth = if (columns > 1) colW - labelX else maxLabel.coerceAtMost(width * 0.34f)
            val descX = if (columns > 1) colW else labelX + labelWidth + 12f
            return labelWidth to descX
        }

        fun rowHeight(width: Float, band: ScoreBand): Float {
            val (labelWidth, descX) = layoutFor(width)
            val labelH = layoutText(band.label, labelPaint, labelWidth, lineSpacing = 1.1f).height.toFloat()
            val descH = if (columns > 1) 0f else layoutText(band.description, descPaint, width - descX, lineSpacing = 1.15f).height.toFloat()
            return max(max(labelH, descH), 10f) + rowGap
        }

        fun totalHeight(width: Float): Float {
            if (columns == 1) return bands.sumOf { rowHeight(width, it).toDouble() }.toFloat()
            val rows = ceil(bands.size / columns.toFloat()).toInt()
            return (0 until rows).sumOf { row ->
                bands.drop(row * columns).take(columns).maxOf { rowHeight(width, it) }.toDouble()
            }.toFloat()
        }

        return DrawBlock(heightOf = { totalHeight(it) }) { canvas, x, y, width, _ ->
            val (labelWidth, descX) = layoutFor(width)
            val colW = columnWidth(width)
            var cursor = y
            bands.chunked(columns).forEach { rowBands ->
                val h = rowBands.maxOf { rowHeight(width, it) }
                rowBands.forEachIndexed { column, band ->
                    val cx = x + column * (colW + columnGap)
                    val cellWidth = if (columns > 1) colW else width
                    if (score.band == band) {
                        canvas.drawRoundRect(cx - 4f, cursor - 2f, cx + cellWidth + 4f, cursor + h - rowGap + 3f, 4f, fillPaint(PdfPalette.mauveSoft))
                    }
                    canvas.drawCircle(cx + 5f, cursor + 5f, 3.5f, fillPaint(band.color.toInt()))
                    canvas.drawText(band.rangeLabel, cx + rangeX, cursor + 8f, rangePaint)
                    canvas.drawLayout(layoutText(band.label, labelPaint, labelWidth, lineSpacing = 1.1f), cx + labelX, cursor)
                    if (columns == 1) {
                        canvas.drawLayout(layoutText(band.description, descPaint, width - descX, lineSpacing = 1.15f), cx + descX, cursor)
                    }
                }
                cursor += h
            }
        }
    }

    private fun signatureBlock(input: Input, d: Density): PdfBlock {
        val height = d.signatureHeight
        val profile = input.profile
        val labelPaint = textPaint(7.5f, PdfPalette.muted, PdfFonts.medium)
        val valuePaint = textPaint(9f, PdfPalette.ink)
        val signerPaint = textPaint(8f, PdfPalette.graphite)
        val place = listOf(profile.place.takeIf { it.isNotBlank() }, Dates.formatIsoLong(input.assessment.date)).filterNotNull().joinToString(", ")
        val signer = listOf(
            input.assessment.evaluator?.takeIf { it.isNotBlank() } ?: profile.name.takeIf { it.isNotBlank() },
            profile.qualification.takeIf { it.isNotBlank() },
        ).filterNotNull().joinToString(" · ")
        return DrawBlock(heightOf = { height }) { canvas, x, y, width, _ ->
            val gap = 24f
            val colWidth = (width - gap) / 2f
            val lineY = y + (if (d.level == 0) 34f else 28f)
            canvas.drawText("Luogo e data", x, y + 8f, labelPaint)
            canvas.drawTextFitted(place, x, lineY - 5f, colWidth, valuePaint)
            canvas.drawLine(x, lineY, x + colWidth, lineY, strokePaint(PdfPalette.graphite, 0.6f))

            val rx = x + colWidth + gap
            canvas.drawText("Firma del valutatore", rx, y + 8f, labelPaint)
            canvas.drawLine(rx, lineY, rx + colWidth, lineY, strokePaint(PdfPalette.graphite, 0.6f))
            if (signer.isNotBlank()) {
                canvas.drawTextFitted(signer, rx, lineY + 11f, colWidth, signerPaint)
            }
        }
    }
}
