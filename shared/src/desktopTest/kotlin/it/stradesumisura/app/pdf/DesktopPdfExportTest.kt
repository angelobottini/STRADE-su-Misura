package it.stradesumisura.app.pdf

import it.stradesumisura.app.data.EvaluatorProfile
import it.stradesumisura.app.data.db.AssessmentEntity
import it.stradesumisura.app.data.db.PersonEntity
import it.stradesumisura.app.domain.AnswersCodec
import it.stradesumisura.app.domain.AssessmentAnswers
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolRegistry
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Genera con PDFBox il report di ogni strumento registrato, compilato con risposte di prova.
 * I PDF restano in `build/report-di-prova` per poterli controllare.
 */
class DesktopPdfExportTest {

    private val person = PersonEntity(
        id = "persona-prova",
        firstName = "Maria",
        lastName = "Rossi",
        birthDate = "1950-03-12",
        sex = "F",
        code = "CR-0042",
        createdAt = 0,
        updatedAt = 0,
    )

    private val profile = EvaluatorProfile(
        name = "Giulia Bianchi",
        organization = "Studio di terapia occupazionale",
        place = "Verona",
    )

    @Test
    fun generatesReportForEveryTool() = runBlocking {
        val outputDir = File("build/report-di-prova").apply { mkdirs() }
        val exporter = AssessmentPdfExporter(DesktopPdfBackend())
        ToolRegistry.tools.forEach { tool ->
            val answers = sampleAnswers(tool)
            val score = tool.score(answers)
            val assessment = AssessmentEntity(
                id = "valutazione-${tool.id}",
                personId = person.id,
                toolId = tool.id,
                date = "2026-10-15",
                evaluator = profile.name,
                setting = "Ambulatorio",
                answersJson = AnswersCodec.encode(answers),
                totalScore = score.total,
                maxScore = score.max,
                isComplete = score.isComplete,
                notes = "Osservazioni di prova: buona collaborazione → tolleranza allo sforzo nella norma.",
                createdAt = 0,
                updatedAt = 0,
            )
            val file = exporter.export(AssessmentPdfExporter.Input(assessment, person, tool, profile), outputDir)
            Loader.loadPDF(file).use { document ->
                assertTrue(document.numberOfPages >= 1, "${tool.id}: nessuna pagina")
                val text = PDFTextStripper().getText(document)
                assertTrue(text.contains("Maria Rossi"), "${tool.id}: manca il nome della persona")
                assertTrue(text.contains(tool.name.take(12)), "${tool.id}: manca il nome dello strumento")
            }
        }
    }

    /** Risposte varie per esercitare livelli diversi, conteggi, scelte multiple, campi e note. */
    private fun sampleAnswers(tool: ToolDefinition): AssessmentAnswers {
        val items = mutableMapOf<String, Int>()
        val subtasks = mutableMapOf<String, Map<Int, Boolean>>()
        tool.primaryItems.filter { !it.isComputed }.forEachIndexed { index, item ->
            when {
                item.multiSelect -> items[item.id] = 1
                item.hasSubtasks -> {
                    val results = item.subtasks.indices.associateWith { it != item.subtasks.lastIndex }
                    subtasks[item.id] = results
                    items[item.id] = results.values.count { it }
                }
                item.isCount -> items[item.id] = (item.countMax ?: 0) - 1
                item.options.isNotEmpty() -> items[item.id] = (index + 1) % item.options.size
            }
        }
        val fields = tool.fields.associate { field ->
            field.id to when {
                field.isChoice -> field.choices.first()
                field.multiline -> "Preparare un caffè con la moka, dalla scelta degli ingredienti al servizio in tazza."
                else -> "Valore di prova"
            }
        }
        val firstItem = tool.primaryItems.firstOrNull { !it.isComputed }
        val notes = firstItem?.let { mapOf(it.id to "Esegue con lentezza ma senza errori.") }.orEmpty()
        return AssessmentAnswers(items = items, itemNotes = notes, fields = fields, subtaskResults = subtasks)
    }
}
