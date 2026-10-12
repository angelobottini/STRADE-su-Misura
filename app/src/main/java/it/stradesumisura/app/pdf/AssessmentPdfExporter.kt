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
import kotlin.math.max

/**
 * Costruisce il report A4 di una valutazione: intestazione con il marchio, schede persona e
 * valutazione, pannello del punteggio con barra interpretativa, tabella degli item, osservazioni,
 * legenda, riferimento bibliografico, spazio per la firma e piè di pagina numerato.
 */
class AssessmentPdfExporter(private val context: Context) {

    data class Input(
        val assessment: AssessmentEntity,
        val person: PersonEntity,
        val tool: ToolDefinition,
        val profile: EvaluatorProfile,
    )

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
        val blocks = buildBlocks(input, answers, score)
        val pages = PdfPaginator(PdfPage.contentTop, PdfPage.contentBottom, PdfPage.contentWidth).paginate(blocks)

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
        drawBrandMark(canvas, PdfPage.MARGIN_X, top, 34f)

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
            canvas.drawText(line1, right, y, textPaint(9.5f, PdfPalette.ink, PdfFonts.medium, align = Paint.Align.RIGHT))
            y += 12f
        }
        if (line2.isNotBlank()) {
            canvas.drawText(line2, right, y, textPaint(8.5f, PdfPalette.graphite, align = Paint.Align.RIGHT))
        }

        val ruleY = top + PdfPage.HEADER_HEIGHT - 12f
        canvas.drawLine(PdfPage.MARGIN_X, ruleY, right, ruleY, strokePaint(PdfPalette.rule, 0.7f))
    }

    private fun drawFooter(canvas: Canvas, input: Input, page: Int, pages: Int, generatedAt: String) {
        val right = PdfPage.WIDTH - PdfPage.MARGIN_X
        val ruleY = PdfPage.HEIGHT - PdfPage.MARGIN_BOTTOM - PdfPage.FOOTER_HEIGHT + 6f
        canvas.drawLine(PdfPage.MARGIN_X, ruleY, right, ruleY, strokePaint(PdfPalette.rule, 0.7f))

        val small = textPaint(7f, PdfPalette.muted)
        canvas.drawText(
            "STRADE su Misura · ${input.tool.name} · generato il $generatedAt",
            PdfPage.MARGIN_X, ruleY + 12f, small,
        )
        canvas.drawText(
            "Documento riservato: contiene dati personali e sanitari da trattare secondo la normativa vigente.",
            PdfPage.MARGIN_X, ruleY + 22f, small,
        )
        canvas.drawText(
            "Pagina $page di $pages",
            right, ruleY + 12f, textPaint(7.5f, PdfPalette.graphite, PdfFonts.medium, align = Paint.Align.RIGHT),
        )
    }

    /** Il marchio STRADE: banda diagonale malva dietro la banda nera con la scritta spaziata. */
    private fun drawBrandMark(canvas: Canvas, x: Float, y: Float, height: Float) {
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
    }

    // -----------------------------------------------------------------------
    // Contenuto
    // -----------------------------------------------------------------------

    private fun buildBlocks(input: Input, answers: AssessmentAnswers, score: ScoreResult): List<PdfBlock> {
        val tool = input.tool
        val blocks = mutableListOf<PdfBlock>()

        blocks += TextBlock(tool.name, textPaint(19f, PdfPalette.ink, PdfFonts.bold), lineSpacing = 1.1f, keepWithNext = true)
        blocks += TextBlock(
            "${tool.subtitle} · ${tool.citation}",
            textPaint(9.5f, PdfPalette.graphite),
            paddingTop = 4f,
            paddingBottom = 14f,
            keepWithNext = true,
        )
        blocks += infoCardsBlock(input, answers)
        blocks += SpacerBlock(14f)
        blocks += scoreBlock(score, tool, input.assessment)
        blocks += SpacerBlock(16f)

        blocks += sectionTitle(tool.itemsSectionTitle)
        val header = tableHeaderBlock(tool)
        blocks += header
        var number = 0
        tool.primaryItems.forEachIndexed { index, item ->
            val selection = tool.selectionFor(item, answers)
            val noteKey = selection?.item?.id ?: item.id
            val note = answers.itemNotes[noteKey] ?: answers.itemNotes[item.id]
            if (item.isComputed) {
                blocks += rowBlock("", item, selection, note, zebra = false, header = header, emphasised = true)
            } else {
                number += 1
                blocks += rowBlock("$number.", item, selection, note, index % 2 == 1, header, emphasised = false)
            }
        }
        if (tool.sumsItems) {
            blocks += totalRowBlock(score, tool, header)
        }
        blocks += SpacerBlock(16f)

        val notes = input.assessment.notes?.trim().orEmpty()
        if (notes.isNotEmpty()) {
            blocks += sectionTitle(tool.notesLabel)
            blocks += notesBlock(notes)
            blocks += SpacerBlock(16f)
        }

        blocks += sectionTitle("Interpretazione del punteggio")
        blocks += legendBlock(tool.bands, score)
        blocks += SpacerBlock(10f)
        blocks += TextBlock(
            "Riferimento: ${tool.reference}",
            textPaint(7.5f, PdfPalette.muted, PdfFonts.italic),
            lineSpacing = 1.25f,
        )

        if (input.profile.includeSignature) {
            blocks += SpacerBlock(26f)
            blocks += signatureBlock(input)
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

    private fun infoCardsBlock(input: Input, answers: AssessmentAnswers): PdfBlock {
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
        )
        val evaluator = listOf(assessment.evaluator, input.profile.qualification.takeIf { !assessment.evaluator.isNullOrBlank() })
            .filter { !it.isNullOrBlank() }.joinToString(" · ")
        val evaluationRows = buildList {
            add(InfoRow("Data", Dates.formatLong(evaluationDate)))
            add(InfoRow("Valutatore", evaluator.ifBlank { "—" }))
            add(InfoRow("Contesto", assessment.setting?.takeIf { it.isNotBlank() } ?: "—"))
            input.tool.fields.forEach { field ->
                add(InfoRow(field.pdfLabel, answers.fields[field.id]?.takeIf { it.isNotBlank() } ?: "—"))
            }
            add(InfoRow("Strumento", "${input.tool.name} (${input.tool.citation})"))
        }

        val gap = 12f
        val labelPaint = textPaint(7.5f, PdfPalette.muted, PdfFonts.medium)
        val valuePaint = textPaint(9.5f, PdfPalette.ink, PdfFonts.medium)
        val captionPaint = textPaint(7.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.12f)
        val padding = 12f
        val labelWidth = 58f

        fun rowsHeight(rows: List<InfoRow>, cardWidth: Float): Float {
            val valueWidth = cardWidth - padding * 2 - labelWidth
            return rows.sumOf { max(layoutText(it.value, valuePaint, valueWidth).height.toFloat(), 11f).toDouble() + 5.0 }.toFloat()
        }

        fun cardHeight(width: Float): Float {
            val cardWidth = (width - gap) / 2f
            return padding + 18f + max(rowsHeight(personRows, cardWidth), rowsHeight(evaluationRows, cardWidth)) + padding - 5f
        }

        fun drawCard(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, caption: String, rows: List<InfoRow>) {
            canvas.drawRoundRect(x, y, x + w, y + h, 8f, fillPaint(PdfPalette.mauveSoft))
            canvas.drawText(caption, x + padding, y + padding + 6f, captionPaint)
            var cursor = y + padding + 18f
            val valueWidth = w - padding * 2 - labelWidth
            rows.forEach { row ->
                val layout = layoutText(row.value, valuePaint, valueWidth)
                canvas.drawText(row.label, x + padding, cursor + 8.5f, labelPaint)
                canvas.drawLayout(layout, x + padding + labelWidth, cursor)
                cursor += max(layout.height.toFloat(), 11f) + 5f
            }
        }

        return DrawBlock(heightOf = { cardHeight(it) }, keepWithNext = true) { canvas, x, y, width, height ->
            val cardWidth = (width - gap) / 2f
            drawCard(canvas, x, y, cardWidth, height, "PERSONA", personRows)
            drawCard(canvas, x + cardWidth + gap, y, cardWidth, height, "VALUTAZIONE", evaluationRows)
        }
    }

    private fun scoreBlock(score: ScoreResult, tool: ToolDefinition, assessment: AssessmentEntity): PdfBlock {
        val height = 96f
        val band = score.band
        return DrawBlock(heightOf = { height }) { canvas, x, y, width, h ->
            canvas.drawRoundRect(x, y, x + width, y + h, 10f, strokePaint(PdfPalette.rule, 0.8f))
            val padding = 14f
            val leftWidth = width * 0.44f

            canvas.drawText(
                tool.totalLabel.uppercase(),
                x + padding, y + padding + 6f,
                textPaint(7.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.12f),
            )
            val big = textPaint(36f, PdfPalette.ink, PdfFonts.black)
            val totalText = score.total.toString()
            val baseline = y + padding + 44f
            canvas.drawText(totalText, x + padding, baseline, big)
            val ofPaint = textPaint(12f, PdfPalette.graphite, PdfFonts.medium)
            canvas.drawText("/ ${score.max}", x + padding + big.measureText(totalText) + 6f, baseline, ofPaint)

            val labelPaint = textPaint(11f, band?.color?.toInt() ?: PdfPalette.graphite, PdfFonts.bold)
            val label = when {
                band != null -> band.label
                !score.isComplete -> "Valutazione incompleta (${score.answered}/${score.required} ${tool.itemNounPlural})"
                else -> "—"
            }
            canvas.drawText(label, x + padding, baseline + 17f, labelPaint)
            val description = band?.description
                ?: if (!score.isComplete) "Il punteggio è parziale: completare tutti gli ${tool.itemNounPlural} per ottenere l'interpretazione." else ""
            if (description.isNotBlank()) {
                val layout = layoutText(description, textPaint(7.8f, PdfPalette.graphite), leftWidth - padding, lineSpacing = 1.15f)
                canvas.drawLayout(layout, x + padding, baseline + 23f)
            }

            // Barra interpretativa a destra
            val barLeft = x + leftWidth + padding
            val barRight = x + width - padding
            val barTop = y + padding + 30f
            val barHeight = 11f
            val bands = tool.bands
            val weights = bands.map { max((it.max - it.min + 1).toFloat() / (tool.maxScore + 1).toFloat(), 0.085f) }
            val totalWeight = weights.sum()
            var cursor = barLeft
            val segments = bands.mapIndexed { index, b ->
                val w = (barRight - barLeft) * weights[index] / totalWeight
                val segment = b to (cursor to cursor + w)
                cursor += w
                segment
            }
            canvas.save()
            val clip = Path().apply { addRoundRect(barLeft, barTop, barRight, barTop + barHeight, 5.5f, 5.5f, Path.Direction.CW) }
            canvas.clipPath(clip)
            segments.forEach { (b, range) ->
                canvas.drawRect(range.first, barTop, range.second, barTop + barHeight, fillPaint(b.color.toInt()))
            }
            canvas.restore()

            val rangePaint = textPaint(6.5f, PdfPalette.muted, align = Paint.Align.CENTER)
            segments.forEach { (b, range) ->
                canvas.drawText(b.rangeLabel, (range.first + range.second) / 2f, barTop + barHeight + 10f, rangePaint)
            }

            // Indicatore del punteggio
            val current = if (score.isComplete) band else tool.bandFor(score.total)
            if (current != null) {
                val segment = segments.first { it.first == current }
                val span = (current.max - current.min + 1).toFloat()
                val fraction = ((score.total - current.min) + 0.5f) / span
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
            canvas.drawText(
                "Interpretazione (${tool.shortName}, 0–${tool.maxScore})",
                barLeft, y + padding + 6f,
                textPaint(7.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.12f),
            )
            val stamp = "Valutazione del ${Dates.formatIsoShort(assessment.date)}"
            canvas.drawText(stamp, barRight, y + h - padding + 2f, textPaint(7.5f, PdfPalette.muted, align = Paint.Align.RIGHT))
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
            canvas.drawText(tool.itemNounSingular.uppercase(), x + 8f, baseline, paint)
            canvas.drawText("LIVELLO RILEVATO", x + width * colActivity + 8f, baseline, paint)
            canvas.drawText("PUNTI", x + width - 8f, baseline, textPaint(7.5f, PdfPalette.mauveDeep, PdfFonts.bold, letterSpacing = 0.1f, align = Paint.Align.RIGHT))
        }
    }

    private fun rowBlock(
        numberLabel: String,
        item: ToolItem,
        selection: Selection?,
        note: String?,
        zebra: Boolean,
        header: PdfBlock,
        emphasised: Boolean,
    ): PdfBlock {
        val padding = 7f
        val titlePaint = textPaint(9f, PdfPalette.ink, PdfFonts.bold)
        val subPaint = textPaint(7.3f, if (emphasised) PdfPalette.mauveDeep else PdfPalette.muted)
        val levelPaint = textPaint(8.8f, PdfPalette.ink, if (emphasised) PdfFonts.bold else PdfFonts.medium)
        val missingPaint = textPaint(8.8f, PdfPalette.muted, PdfFonts.italic)
        val descPaint = textPaint(7.6f, PdfPalette.graphite)
        val notePaint = textPaint(7.6f, PdfPalette.ink, PdfFonts.italic)
        val scorePaint = textPaint(if (emphasised) 11f else 9.5f, PdfPalette.ink, if (emphasised) PdfFonts.black else PdfFonts.bold, align = Paint.Align.RIGHT)
        val maxPaint = textPaint(7f, PdfPalette.muted, align = Paint.Align.RIGHT)

        val shownItem = selection?.item ?: item
        val title = if (numberLabel.isBlank()) shownItem.title else "$numberLabel ${shownItem.title}"
        val subtitle = when {
            selection?.isAlternative == true -> "in alternativa a «${item.title}»"
            item.isComputed -> item.hint ?: "Calcolato automaticamente"
            else -> null
        }
        val levelTitle = selection?.option?.title ?: if (item.isComputed) "In attesa delle fasi" else "Non valutato"
        val description = selection?.option?.description
            ?: if (item.isComputed) "Il valore viene calcolato quando tutte le voci di origine sono compilate." else "Nessun livello selezionato."
        val noteText = note?.trim()?.takeIf { it.isNotEmpty() }?.let { "Osservazioni: $it" }
        val maxScore = shownItem.maxScore

        fun heights(width: Float): Triple<Float, Float, Float> {
            val leftW = width * colActivity - 16f
            val midW = width * colLevel - 16f
            val leftH = layoutText(title, titlePaint, leftW, lineSpacing = 1.15f).height +
                (subtitle?.let { layoutText(it, subPaint, leftW, lineSpacing = 1.15f).height + 2 } ?: 0)
            val midH = layoutText(levelTitle, levelPaint, midW, lineSpacing = 1.15f).height + 2 +
                layoutText(description, descPaint, midW, lineSpacing = 1.18f).height +
                (noteText?.let { layoutText(it, notePaint, midW, lineSpacing = 1.18f).height + 4 } ?: 0)
            val h = max(max(leftH, midH).toFloat(), 22f) + padding * 2
            return Triple(h, leftW, midW)
        }

        return DrawBlock(heightOf = { heights(it).first }, continuationHeader = header) { canvas, x, y, width, h ->
            val (_, leftW, midW) = heights(width)
            if (emphasised) {
                canvas.drawRect(x, y, x + width, y + h, fillPaint(PdfPalette.mauveSoft))
                canvas.drawLine(x, y + 0.5f, x + width, y + 0.5f, strokePaint(PdfPalette.ink, 1f))
            } else if (zebra) {
                canvas.drawRect(x, y, x + width, y + h, fillPaint(PdfPalette.zebra))
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
            if (noteText != null) {
                canvas.drawLayout(
                    layoutText(noteText, notePaint, midW, lineSpacing = 1.18f),
                    midX, top + levelLayout.height + 2f + descLayout.height + 4f,
                )
            }

            val right = x + width - 8f
            val scoreText = selection?.option?.score?.toString() ?: "—"
            canvas.drawText(scoreText, right, top + 9f, scorePaint)
            canvas.drawText("su $maxScore", right, top + 19f, maxPaint)
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
            canvas.drawText(status, x + width * colActivity + 8f, baseline, textPaint(8f, PdfPalette.graphite))
            canvas.drawText("${score.total} / ${score.max}", x + width - 8f, baseline, textPaint(11f, PdfPalette.ink, PdfFonts.black, align = Paint.Align.RIGHT))
        }
    }

    private fun notesBlock(notes: String): PdfBlock {
        val paint = textPaint(9f, PdfPalette.ink)
        val padding = 10f
        return DrawBlock(heightOf = { width -> layoutText(notes, paint, width - padding * 2, lineSpacing = 1.3f).height + padding * 2 }) { canvas, x, y, width, h ->
            canvas.drawRoundRect(x, y, x + width, y + h, 8f, strokePaint(PdfPalette.rule, 0.8f))
            canvas.drawLayout(layoutText(notes, paint, width - padding * 2, lineSpacing = 1.3f), x + padding, y + padding)
        }
    }

    private fun legendBlock(bands: List<ScoreBand>, score: ScoreResult): PdfBlock {
        val rangePaint = textPaint(8f, PdfPalette.ink, PdfFonts.bold)
        val labelPaint = textPaint(8f, PdfPalette.ink, PdfFonts.medium)
        val descPaint = textPaint(7.6f, PdfPalette.graphite)
        val rowGap = 4f
        fun rowHeight(width: Float, band: ScoreBand): Float =
            max(layoutText(band.description, descPaint, width - 150f, lineSpacing = 1.15f).height.toFloat(), 10f) + rowGap
        return DrawBlock(heightOf = { width -> bands.sumOf { rowHeight(width, it).toDouble() }.toFloat() }) { canvas, x, y, width, _ ->
            var cursor = y
            bands.forEach { band ->
                val isCurrent = score.band == band
                val h = rowHeight(width, band)
                if (isCurrent) {
                    canvas.drawRoundRect(x - 4f, cursor - 2f, x + width + 4f, cursor + h - rowGap + 3f, 4f, fillPaint(PdfPalette.mauveSoft))
                }
                canvas.drawCircle(x + 5f, cursor + 5f, 3.5f, fillPaint(band.color.toInt()))
                canvas.drawText(band.rangeLabel, x + 14f, cursor + 8f, rangePaint)
                canvas.drawText(band.label, x + 48f, cursor + 8f, labelPaint)
                canvas.drawLayout(layoutText(band.description, descPaint, width - 150f, lineSpacing = 1.15f), x + 150f, cursor)
                cursor += h
            }
        }
    }

    private fun signatureBlock(input: Input): PdfBlock {
        val height = 54f
        val profile = input.profile
        val labelPaint = textPaint(7.5f, PdfPalette.muted, PdfFonts.medium)
        val valuePaint = textPaint(9f, PdfPalette.ink)
        val place = listOf(profile.place.takeIf { it.isNotBlank() }, Dates.formatIsoLong(input.assessment.date)).filterNotNull().joinToString(", ")
        val signer = listOf(
            input.assessment.evaluator?.takeIf { it.isNotBlank() } ?: profile.name.takeIf { it.isNotBlank() },
            profile.qualification.takeIf { it.isNotBlank() },
        ).filterNotNull().joinToString(" · ")
        return DrawBlock(heightOf = { height }) { canvas, x, y, width, _ ->
            val gap = 24f
            val colWidth = (width - gap) / 2f
            val lineY = y + 34f
            canvas.drawText("Luogo e data", x, y + 8f, labelPaint)
            canvas.drawText(place, x, lineY - 5f, valuePaint)
            canvas.drawLine(x, lineY, x + colWidth, lineY, strokePaint(PdfPalette.graphite, 0.6f))

            val rx = x + colWidth + gap
            canvas.drawText("Firma del valutatore", rx, y + 8f, labelPaint)
            canvas.drawLine(rx, lineY, rx + colWidth, lineY, strokePaint(PdfPalette.graphite, 0.6f))
            if (signer.isNotBlank()) {
                canvas.drawText(signer, rx, lineY + 11f, textPaint(8f, PdfPalette.graphite))
            }
        }
    }
}
