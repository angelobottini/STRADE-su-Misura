package it.stradesumisura.app.domain.tools

import it.stradesumisura.app.domain.ComputedScore
import it.stradesumisura.app.domain.ScoreBand
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolField
import it.stradesumisura.app.domain.ToolItem
import it.stradesumisura.app.domain.ToolOption

/**
 * Scheda di valutazione regionale per le persone con disabilità.
 *
 * A) Valutazione funzionale: otto categorie ICF di attività e partecipazione, valutate con il
 * qualificatore «capacità» da 0 (nessuna difficoltà) a 4 (difficoltà completa).
 * B) Segnalazione dei problemi fisici e/o di comportamento di particolare gravità: ogni voce vale
 * 1 (no), 2 (sì, senza interventi e standard speciali) o 3 (sì, con interventi e standard speciali).
 * C) Sintesi: la classe da A (problemi di particolare gravità) a E (disabilità lieve) è calcolata
 * con i criteri generali di identificazione della scheda, dal più grave al più lieve.
 */
object RegionalAssessment {

    const val ID = "regional_assessment"

    private const val CLASS = "class"
    private const val OTHER_1 = "b1_other"
    private const val OTHER_2 = "b2_other"

    private val FUNCTIONAL = listOf("d310", "d460", "d510", "d530", "d540", "d550", "d570", "d710")
    private val SECTION_1 = listOf("b1_self_harm", "b1_aggression", "b1_stereotypies", "b1_unpredictability", OTHER_1)
    private val SECTION_2 = listOf("b2_vital_signs", "b2_special_care", OTHER_2)

    /** Voci da compilare per calcolare la classe (le voci «Altro» sono facoltative). */
    private val REQUIRED = FUNCTIONAL + (SECTION_1 + SECTION_2).filter { it != OTHER_1 && it != OTHER_2 }

    /** Punteggio della classe: più alto è, più intenso è il bisogno. */
    const val CLASS_NONE = 0
    const val CLASS_E = 1
    const val CLASS_D = 2
    const val CLASS_C = 3
    const val CLASS_B = 4
    const val CLASS_A = 5

    private val icfOptions = listOf(
        ToolOption(0, "Nessuna difficoltà", "La persona non presenta il problema."),
        ToolOption(1, "Difficoltà lieve", "Il problema è presente con un'intensità che la persona può tollerare."),
        ToolOption(2, "Difficoltà media", "Il problema è presente con un'intensità che interferisce abbastanza nella vita quotidiana della persona."),
        ToolOption(3, "Difficoltà severa", "Il problema è presente con un'intensità che altera notevolmente la vita quotidiana della persona."),
        ToolOption(4, "Difficoltà completa", "Il problema è presente con un'intensità che altera totalmente la vita quotidiana della persona."),
    )

    private val gravityOptions = listOf(
        ToolOption(1, "No", "Il problema non è presente."),
        ToolOption(2, "Sì, senza interventi speciali", "Il problema è presente, ma non tale da richiedere interventi e standard speciali."),
        ToolOption(3, "Sì, con interventi speciali", "Il problema è presente ed è tale da richiedere interventi e standard speciali."),
    )

    private val classOptions = listOf(
        ToolOption(CLASS_NONE, "Nessuna classe", "Nessuno dei criteri generali di identificazione è soddisfatto."),
        ToolOption(CLASS_E, "E · Disabilità lieve", "Discreta autosufficienza fisica e autonomia relazionale."),
        ToolOption(CLASS_D, "D · Disabilità moderata", "Non autosufficienza e/o assenza di autonomia relazionale abbastanza rilevante."),
        ToolOption(CLASS_C, "C · Disabilità severa", "Non autosufficienza e/o assenza di autonomia relazionale grave, anche se non completa."),
        ToolOption(CLASS_B, "B · Disabilità completa", "Non autosufficienza e/o assenza di autonomia relazionale particolarmente marcata."),
        ToolOption(CLASS_A, "A · Problemi di particolare gravità", "Problemi fisici e/o di comportamento che richiedono interventi e standard di personale superiori."),
    )

    /** Conteggi da cui dipende la classe. Le difficoltà più gravi contano anche per i livelli inferiori. */
    internal data class Counts(
        val complete: Int,
        val severe: Int,
        val medium: Int,
        val mild: Int,
        val section1: Int,
        val section2: Int,
    ) {
        /** Voci della scheda B con punteggio 3. */
        val flagged: Int get() = section1 + section2
    }

    internal fun counts(scores: Map<String, Int>): Counts {
        val functional = FUNCTIONAL.map { scores[it] ?: 0 }
        return Counts(
            complete = functional.count { it >= 4 },
            severe = functional.count { it >= 3 },
            medium = functional.count { it >= 2 },
            mild = functional.count { it >= 1 },
            section1 = SECTION_1.count { scores[it] == 3 },
            section2 = SECTION_2.count { scores[it] == 3 },
        )
    }

    /** Classe con il criterio che la determina: il primo soddisfatto, dalla classe A alla E. */
    internal fun classify(c: Counts): Pair<Int, String> = when {
        c.section1 >= 2 ->
            CLASS_A to "punteggio 3 in almeno due voci della sezione 1 della scheda B"
        c.flagged >= 1 && c.complete >= 4 ->
            CLASS_A to "punteggio 3 in una voce della scheda B con limitazione completa in almeno quattro categorie"
        c.section2 >= 1 && c.severe >= 4 ->
            CLASS_A to "punteggio 3 in una voce della sezione 2 della scheda B associato a disabilità completa"
        c.complete >= 4 ->
            CLASS_B to "limitazione completa in almeno quattro categorie della scheda A"
        c.flagged >= 1 && c.severe >= 4 ->
            CLASS_B to "punteggio 3 in una voce della scheda B con limitazione severa in almeno quattro categorie"
        c.severe >= 4 ->
            CLASS_C to "limitazione severa in almeno quattro categorie della scheda A"
        c.flagged >= 1 && c.medium >= 4 ->
            CLASS_C to "punteggio 3 in una voce della scheda B con limitazione media in almeno quattro categorie"
        c.medium >= 4 ->
            CLASS_D to "limitazione media in almeno quattro categorie della scheda A"
        c.flagged >= 1 && c.mild >= 4 ->
            CLASS_D to "punteggio 3 in una voce della scheda B con limitazione lieve in almeno quattro categorie"
        c.mild >= 4 ->
            CLASS_E to "limitazione lieve in almeno quattro categorie della scheda A"
        else ->
            CLASS_NONE to "meno di quattro categorie della scheda A con difficoltà"
    }

    private fun compute(scores: Map<String, Int>): Int? =
        if (REQUIRED.all { it in scores }) classify(counts(scores)).first else null

    private fun explain(scores: Map<String, Int>, value: Int): String {
        val c = counts(scores)
        val (_, criterion) = classify(c)
        val result = if (value == CLASS_NONE) "nessuna classe" else "classe ${bands.firstOrNull { it.contains(value) }?.code ?: value}"
        return "scheda A: ${c.complete} categorie con difficoltà completa, ${c.severe} almeno severa, " +
            "${c.medium} almeno media, ${c.mild} almeno lieve; scheda B: ${c.section1} voci con punteggio 3 " +
            "nella sezione 1 e ${c.section2} nella sezione 2 → $result ($criterion)"
    }

    private fun functional(id: String, code: String, title: String, section: String? = null, hint: String? = null) =
        ToolItem(id = id, title = "$code $title", options = icfOptions, hint = hint, section = section)

    private fun gravity(id: String, title: String, section: String? = null) =
        ToolItem(id = id, title = title, options = gravityOptions, section = section)

    private fun other(id: String, section: String? = null) = ToolItem(
        id = id,
        title = "Altro",
        options = gravityOptions,
        hint = "Specificare il problema nella nota della voce.",
        noteLabel = "Specificare",
        optional = true,
        section = section,
    )

    private val bands = listOf(
        ScoreBand(CLASS_NONE, CLASS_NONE, "Nessuna classe",
            "Meno di quattro categorie della scheda A con difficoltà: nessun criterio generale di identificazione è " +
                "soddisfatto e la classe va motivata nelle osservazioni.", 0xFFB9B1B5, code = "–"),
        ScoreBand(CLASS_E, CLASS_E, "Disabilità lieve",
            "Pur con disabilità anche gravi, discreta autosufficienza fisica e autonomia relazionale nelle abilità di " +
                "base della vita quotidiana, nella comunicazione, nelle relazioni, nella cura di sé e nell'orientamento.",
            0xFF2F7F5E, code = "E"),
        ScoreBand(CLASS_D, CLASS_D, "Disabilità moderata",
            "Non autosufficienza fisica e/o assenza di autonomia relazionale abbastanza rilevante. Di norma centri diurni " +
                "socio-occupazionali e residenze a minore intensità assistenziale (gruppi appartamento, comunità alloggio).",
            0xFF9BB357, code = "D"),
        ScoreBand(CLASS_C, CLASS_C, "Disabilità severa",
            "Non autosufficienza fisica e/o assenza di autonomia relazionale grave, anche se non completa. Di norma centri " +
                "diurni e residenziali socio-riabilitativi con il rapporto operatori-utenti della DGR 514/09.",
            0xFFE0A84A, code = "C"),
        ScoreBand(CLASS_B, CLASS_B, "Disabilità completa",
            "Non autosufficienza fisica e/o assenza di autonomia relazionale particolarmente marcata. Di norma centri " +
                "diurni e residenziali socio-riabilitativi con il rapporto operatori-utenti della DGR 514/09.",
            0xFFD3714F, code = "B"),
        ScoreBand(CLASS_A, CLASS_A, "Problemi di particolare gravità",
            "Problemi fisici e/o di comportamento di particolare gravità, oppure totale non autosufficienza con bisogni " +
                "sanitari complessi: richiedono interventi e standard di personale superiori a quelli previsti per la " +
                "disabilità completa.", 0xFFB4455A, code = "A"),
    )

    val definition: ToolDefinition = ToolDefinition(
        id = ID,
        name = "Scheda di valutazione regionale",
        shortName = "Regionale",
        citation = "Scheda regionale · ICF (OMS, 2001)",
        subtitle = "Valutazione funzionale ICF, problemi di particolare gravità e classe di intensità del bisogno",
        domain = "Disabilità · Accesso ai servizi diurni e residenziali",
        duration = "Dopo osservazione e colloquio",
        description = "La scheda di valutazione regionale descrive l'intensità del bisogno della persona con disabilità " +
            "per l'accesso ai servizi diurni e residenziali. La scheda A valuta otto categorie ICF di attività e " +
            "partecipazione (comunicazione, mobilità, cura della propria persona, relazioni interpersonali) con il " +
            "qualificatore «capacità»: le limitazioni intrinseche della persona, senza assistenza, da 0 (nessuna " +
            "difficoltà) a 4 (difficoltà completa). La scheda B segnala i problemi fisici e/o di comportamento di " +
            "particolare gravità: nella sezione 1 autolesionismo, aggressività, comportamenti stereotipati pericolosi e " +
            "imprevedibilità; nella sezione 2 la necessità di sorveglianza continua delle funzioni vitali e di " +
            "assistenza particolare. La sintesi C attribuisce la persona a una delle cinque classi, da A (problemi di " +
            "particolare gravità) a E (disabilità lieve), calcolata con i criteri generali di identificazione della scheda.",
        instructions = listOf(
            "Compilare i dati della persona e del servizio: codice fiscale, comune di residenza e, se la persona è già " +
                "inserita in un servizio, la tipologia codificata, il comune sede e il mese e l'anno di inserimento. " +
                "Sesso e anno di nascita sono quelli dell'anagrafica.",
            "Scheda A – Per ciascuna delle otto categorie indicare il qualificatore di capacità, cioè la difficoltà della " +
                "persona senza assistenza: 0 nessuna, 1 lieve, 2 media, 3 severa, 4 completa.",
            "Scheda B – Per ogni problema indicare: 1 = no; 2 = sì, ma tale da non richiedere interventi e standard " +
                "speciali; 3 = sì, tale da richiedere interventi psico-educativi e standard di personale superiori. Le voci " +
                "«Altro» sono facoltative: se si compilano, specificare il problema nella nota.",
            "C – Sintesi: la classe si calcola da sola con i criteri generali di identificazione. Le categorie con una " +
                "difficoltà più grave contano anche per i livelli inferiori: una difficoltà completa vale anche come severa, " +
                "media e lieve.",
            "I criteri sono di riferimento generale: le considerazioni del valutatore sulla classe vanno nelle osservazioni.",
        ),
        reference = "Scheda di valutazione regionale per persone con disabilità: A) valutazione funzionale, B) segnalazione " +
            "dei problemi fisici e/o di comportamento di particolare gravità, C) sintesi; rapporto operatori-utenti dei " +
            "centri socio-riabilitativi secondo la DGR 514/2009. Qualificatori di capacità: Organizzazione Mondiale della " +
            "Sanità, ICF – Classificazione Internazionale del Funzionamento, della Disabilità e della Salute, 2001 " +
            "(ed. it. Erickson, 2002).",
        fields = listOf(
            ToolField("fiscal_code", "Codice fiscale della persona", "16 caratteri", shortLabel = "Cod. fiscale"),
            ToolField("residence", "Comune di residenza (codice)", "codice del comune", shortLabel = "Residenza"),
            ToolField("diagnosis", "Diagnosi", "es. disabilità intellettiva grave"),
            ToolField("in_service", "Persona già inserita in un servizio", shortLabel = "In servizio", choices = listOf("Sì", "No")),
            ToolField("service_type", "Tipologia del servizio (codificata)", "se già inserita in un servizio", shortLabel = "Servizio"),
            ToolField("service_town", "Comune sede del servizio (codice)", "codice del comune", shortLabel = "Sede servizio"),
            ToolField("service_since", "Mese e anno di inserimento", "es. 09/2024", shortLabel = "Inserita dal"),
        ),
        items = listOf(
            functional("d310", "d310", "Comunicare con – ricevere – messaggi verbali", section = "A) D3 – Comunicazione"),
            functional("d460", "d460", "Spostarsi e andare in giro", section = "A) D4 – Mobilità", hint = "Mobilità e capacità di orientamento."),
            functional("d510", "d510", "Lavarsi", section = "A) D5 – Cura della propria persona"),
            functional("d530", "d530", "Bisogni corporali"),
            functional("d540", "d540", "Vestirsi"),
            functional("d550", "d550", "Mangiare"),
            functional("d570", "d570", "Prendersi cura della propria salute", hint = "Ad esempio evitare comportamenti pericolosi."),
            functional("d710", "d710", "Interagire con le persone in modo adeguato", section = "A) D7 – Relazioni interpersonali"),
            gravity("b1_self_harm", "Autolesionismo", section = "B) Sezione 1 – Problemi di comportamento"),
            gravity("b1_aggression", "Aggressività e comportamenti distruttivi verso persone e oggetti"),
            gravity("b1_stereotypies", "Comportamenti stereotipati pericolosi per sé e per gli altri"),
            gravity("b1_unpredictability", "Imprevedibilità"),
            other(OTHER_1),
            gravity(
                "b2_vital_signs",
                "Necessità di sorveglianza continua per le funzioni vitali, anche per incapacità di chiedere aiuto",
                section = "B) Sezione 2 – Funzioni vitali e assistenza",
            ),
            gravity("b2_special_care", "Necessità di assistenza particolare per funzioni quali alimentazione, respirazione, evacuazione"),
            other(OTHER_2),
            ToolItem(
                id = CLASS,
                title = "Classe",
                hint = "Calcolata automaticamente con i criteri generali di identificazione della scheda, dalla classe A alla E.",
                options = classOptions,
                computed = ComputedScore.Rule(FUNCTIONAL + SECTION_1 + SECTION_2, ::compute, ::explain),
                section = "C) Sintesi",
            ),
        ),
        bands = bands,
        headlineItemId = CLASS,
        totalLabel = "Classe",
        itemNounSingular = "voce",
        itemNounPlural = "voci",
        itemsSectionTitle = "Voci valutate",
        notesLabel = "Osservazioni",
        notesHint = "Elementi a sostegno della classe, servizi in corso, considerazioni del valutatore.",
        summaryFieldId = "diagnosis",
        scoringNote = "La classe è calcolata con i criteri generali di identificazione della scheda: si attribuisce la " +
            "prima classe i cui criteri sono soddisfatti, dalla A alla E. Le categorie con una difficoltà più grave " +
            "contano anche per i livelli inferiori. A: punteggio 3 in almeno due voci della sezione 1 della scheda B; " +
            "oppure punteggio 3 in una voce della scheda B con limitazione completa in almeno quattro categorie; oppure " +
            "punteggio 3 in una voce della sezione 2 associato a disabilità completa. B: limitazione completa in almeno " +
            "quattro categorie, oppure una voce con punteggio 3 nella scheda B con limitazione severa in almeno quattro " +
            "categorie. C: limitazione severa in almeno quattro categorie, oppure una voce con punteggio 3 con limitazione " +
            "media in almeno quattro. D: limitazione media in almeno quattro categorie, oppure una voce con punteggio 3 con " +
            "limitazione lieve in almeno quattro. E: limitazione lieve in almeno quattro categorie.",
        itemsCountLabel = "8 categorie ICF e 8 voci di gravità",
        scoreRangeText = "A–E",
    )
}
