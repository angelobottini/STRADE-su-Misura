package it.stradesumisura.app.domain

import it.stradesumisura.app.domain.tools.RegionalAssessment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Classe della scheda regionale calcolata con i criteri generali di identificazione. */
class RegionalAssessmentTest {

    private val tool = RegionalAssessment.definition
    private val functional = listOf("d310", "d460", "d510", "d530", "d540", "d550", "d570", "d710")
    private val gravity = listOf("b1_self_harm", "b1_aggression", "b1_stereotypies", "b1_unpredictability", "b2_vital_signs", "b2_special_care")

    /**
     * Risposte con i livelli ICF indicati per le prime categorie (le altre a 0) e le voci della
     * scheda B a 1 («no») salvo quelle indicate a 3. Le risposte sono indici delle opzioni.
     */
    private fun answers(levels: List<Int>, flagged: List<String> = emptyList()): AssessmentAnswers {
        val items = mutableMapOf<String, Int>()
        functional.forEachIndexed { index, id -> items[id] = levels.getOrElse(index) { 0 } }
        gravity.forEach { id -> items[id] = if (id in flagged) 2 else 0 }
        return AssessmentAnswers(items = items)
    }

    private fun classOf(levels: List<Int>, flagged: List<String> = emptyList()): String? =
        tool.score(answers(levels, flagged)).band?.code

    @Test
    fun functionalLimitationsGiveClassesEtoB() {
        assertEquals("E", classOf(listOf(1, 1, 1, 1)))
        assertEquals("D", classOf(listOf(2, 2, 2, 2)))
        assertEquals("C", classOf(listOf(3, 3, 3, 3)))
        assertEquals("B", classOf(listOf(4, 4, 4, 4)))
    }

    @Test
    fun graverDifficultiesCountForLowerLevels() {
        // Due complete e due severe: limitazione severa in quattro categorie.
        assertEquals("C", classOf(listOf(4, 4, 3, 3)))
        // Tre medie e una completa: limitazione media in quattro categorie.
        assertEquals("D", classOf(listOf(2, 2, 2, 4)))
    }

    @Test
    fun fewerThanFourCategoriesGiveNoClass() {
        assertEquals("–", classOf(listOf(4, 4, 4)))
        assertEquals("–", tool.formatHeadline(tool.score(answers(listOf(4, 4, 4))).total))
    }

    @Test
    fun gravityItemsRaiseTheClassByOneLevel() {
        assertEquals("D", classOf(listOf(1, 1, 1, 1), flagged = listOf("b1_self_harm")))
        assertEquals("C", classOf(listOf(2, 2, 2, 2), flagged = listOf("b1_self_harm")))
        assertEquals("B", classOf(listOf(3, 3, 3, 3), flagged = listOf("b1_self_harm")))
    }

    @Test
    fun classAForSevereProblems() {
        // Due voci della sezione 1 a 3, indipendentemente dalla valutazione funzionale.
        assertEquals("A", classOf(emptyList(), flagged = listOf("b1_self_harm", "b1_aggression")))
        // Una voce della scheda B a 3 con limitazione completa in quattro categorie.
        assertEquals("A", classOf(listOf(4, 4, 4, 4), flagged = listOf("b1_unpredictability")))
        // Una voce della sezione 2 a 3 associata a disabilità completa.
        assertEquals("A", classOf(listOf(3, 3, 3, 3), flagged = listOf("b2_vital_signs")))
    }

    @Test
    fun incompleteAssessmentHasNoClass() {
        val partial = answers(listOf(4, 4, 4, 4)).let { it.copy(items = it.items - "d710") }
        val score = tool.score(partial)
        assertTrue(!score.isComplete)
        assertNull(score.band)
        assertNull(tool.computedValue(tool.headlineItem!!, partial.items))
    }

    @Test
    fun optionalOtherItemsCountWhenFilled() {
        val items = answers(emptyList(), flagged = listOf("b1_self_harm")).items + ("b1_other" to 2)
        assertEquals("A", tool.score(AssessmentAnswers(items = items)).band?.code)
    }

    @Test
    fun explanationNamesTheCriterion() {
        val explanation = tool.computedExplanation(tool.headlineItem!!, answers(listOf(4, 4, 4, 4)))
        assertTrue(explanation!!.contains("classe B"), explanation)
        assertEquals("A–E", tool.scoreRangeLabel)
    }
}
