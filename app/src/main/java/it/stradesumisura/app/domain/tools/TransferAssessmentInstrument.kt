package it.stradesumisura.app.domain.tools

import it.stradesumisura.app.R
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolField
import it.stradesumisura.app.domain.ToolGroup
import it.stradesumisura.app.domain.ToolItem
import it.stradesumisura.app.domain.ToolOption

/**
 * TAI 4.0 – Transfer Assessment Instrument (Human Engineering Research Laboratories, Università di
 * Pittsburgh). Valuta la qualità e la sicurezza dei trasferimenti in carrozzina in tre fasi
 * (preparazione della carrozzina, preparazione del corpo, volo/atterraggio) più gli ausili e, nelle
 * versioni assistite, il rapporto con l'assistente. Il modulo originale (in inglese) prevede tre
 * versioni, qui tre strumenti distinti:
 * - trasferimenti autonomi (18 voci);
 * - trasferimenti assistiti senza sollevatore (21 voci);
 * - trasferimenti assistiti con sollevatore (5 voci).
 *
 * Ogni voce vale al massimo 1 punto (0,5 per le esecuzioni parziali; alcune voci a scelta multipla
 * sommano fino a 2 e non scendono sotto 0); alcune voci dipendono dal tipo di carrozzina (manuale
 * o elettrica) e altre possono essere «non applicabili», nel qual caso escono dal massimo. Il
 * punteggio di un trasferimento è la percentuale del massimo applicabile (100 × A ÷ (massimo − N/A));
 * se si valutano due trasferimenti (andata e ritorno) il totale è la media delle due percentuali.
 * I punteggi sono salvati in centesimi di punto ([ToolDefinition.scoreDivisor] = 100).
 */
object TransferAssessmentInstrument {

    const val ID_INDEPENDENT = "tai_independent"
    const val ID_ASSISTED = "tai_assisted"
    const val ID_LIFT = "tai_lift"

    private const val WHEELCHAIR_FIELD = "wheelchair"
    private const val MANUAL = "Carrozzina manuale"
    private const val POWER = "Carrozzina elettrica"

    private const val P1 = 100
    private const val P05 = 50
    private const val P0 = 0
    private const val M05 = -50
    private const val M1 = -100

    private const val SECTION_SETUP = "Fase I – Preparazione della carrozzina"
    private const val SECTION_BODY = "Fase II – Preparazione del corpo"
    private const val SECTION_FLIGHT = "Fase III – Volo e atterraggio"
    private const val SECTION_AIDS = "Ausili"
    private const val SECTION_HELPER = "Assistente"

    private fun opt(score: Int, title: String, description: String = "", image: Int? = null, group: String? = null) =
        ToolOption(score, title, description, imageRes = image, group = group)

    /** Opzione con punteggio diverso per la carrozzina elettrica (null = non applicabile con l'elettrica). */
    private fun optByChair(manual: Int, power: Int?, title: String, description: String = "") =
        ToolOption(manual, title, description, variantScores = mapOf(POWER to power))

    private fun na(title: String, description: String = "", image: Int? = null, group: String? = null) =
        ToolOption(0, title, description, notApplicable = true, imageRes = image, group = group)

    private fun pts(score: Int): String = when (score) {
        P1 -> "1 punto"
        P05 -> "0,5 punti"
        P0 -> "0 punti"
        M05 -> "−0,5 punti"
        M1 -> "−1 punto"
        else -> "$score"
    }

    private fun yesNoDesc(manual: Int, power: Int?): String =
        "Carrozzina manuale ${pts(manual)} · elettrica ${power?.let { pts(it) } ?: "non applicabile"}"

    // -----------------------------------------------------------------------
    // Posizioni della mano (voci 10 e 11)
    // -----------------------------------------------------------------------

    private const val SURFACE_WHEELCHAIR = "Carrozzina"
    private const val SURFACE_BOARD = "Tavoletta di trasferimento"
    private const val SURFACE_FIRM = "Superficie rigida"
    private const val SURFACE_SOFT = "Superficie morbida (letto, divano)"
    private const val SURFACE_CHAIR = "Sedia"
    private const val SURFACE_BATH = "Bagno"
    private const val SURFACE_NONE = "Mano non usata"

    private fun wheelchairHand(): List<ToolOption> = listOf(
        opt(P1, "Bracciolo", image = R.drawable.tai_wc_armrest, group = SURFACE_WHEELCHAIR),
        opt(P1, "Ruota", image = R.drawable.tai_wc_wheel, group = SURFACE_WHEELCHAIR),
        opt(P1, "Bordo del cuscino o telaio", image = R.drawable.tai_wc_cushion_edge, group = SURFACE_WHEELCHAIR),
        opt(P0, "Pugno sul cuscino", image = R.drawable.tai_wc_fist, group = SURFACE_WHEELCHAIR),
        opt(P0, "Mano piatta sul cuscino", image = R.drawable.tai_wc_flat, group = SURFACE_WHEELCHAIR),
        opt(P05, "Dita piegate sul cuscino", image = R.drawable.tai_wc_bent, group = SURFACE_WHEELCHAIR),
    )

    private fun boardHand(): List<ToolOption> = listOf(
        opt(P1, "Bordo della tavoletta", image = R.drawable.tai_board_edge, group = SURFACE_BOARD),
        opt(P0, "Dita arricciate sotto la tavoletta", image = R.drawable.tai_board_curled, group = SURFACE_BOARD),
        opt(P0, "Mano piatta sulla tavoletta", image = R.drawable.tai_board_flat, group = SURFACE_BOARD),
        opt(P0, "Pugno sulla tavoletta", image = R.drawable.tai_board_fist, group = SURFACE_BOARD),
        opt(P05, "Dita piegate sulla tavoletta", image = R.drawable.tai_board_bent, group = SURFACE_BOARD),
    )

    private fun firmHand(): List<ToolOption> = listOf(
        opt(P0, "Pugno sulla superficie", image = R.drawable.tai_firm_fist, group = SURFACE_FIRM),
        opt(P1, "Bordo della superficie", image = R.drawable.tai_firm_edge, group = SURFACE_FIRM),
        opt(P0, "Mano piatta sulla superficie", image = R.drawable.tai_firm_flat, group = SURFACE_FIRM),
        opt(P05, "Dita piegate sulla superficie", image = R.drawable.tai_firm_bent, group = SURFACE_FIRM),
    )

    private fun softHand(edge: Int, flat: Int, fist: Int, bent: Int): List<ToolOption> = listOf(
        opt(edge, "Bordo della superficie morbida", image = R.drawable.tai_soft_edge, group = SURFACE_SOFT),
        opt(flat, "Mano piatta sulla superficie morbida", image = R.drawable.tai_soft_flat, group = SURFACE_SOFT),
        opt(fist, "Pugno sulla superficie morbida", image = R.drawable.tai_soft_fist, group = SURFACE_SOFT),
        opt(bent, "Dita piegate sulla superficie morbida", image = R.drawable.tai_soft_bent, group = SURFACE_SOFT),
    )

    private fun chairHand(arm: Int, edge: Int, flat: Int, fist: Int, bent: Int): List<ToolOption> = listOf(
        opt(arm, "Bracciolo della sedia", image = R.drawable.tai_chair_arm, group = SURFACE_CHAIR),
        opt(edge, "Bordo della sedia", image = R.drawable.tai_chair_edge, group = SURFACE_CHAIR),
        opt(flat, "Mano piatta sulla sedia", image = R.drawable.tai_chair_flat, group = SURFACE_CHAIR),
        opt(fist, "Pugno sulla sedia", image = R.drawable.tai_chair_fist, group = SURFACE_CHAIR),
        opt(bent, "Dita piegate sulla sedia", image = R.drawable.tai_chair_bent, group = SURFACE_CHAIR),
    )

    /** Bagno con il criterio della distanza dei maniglioni dalla base d'appoggio (1 punto se entro 15 cm). */
    private fun bathHandByDistance(): List<ToolOption> = listOf(
        opt(P1, "Bordo della tazza del WC", image = R.drawable.tai_bath_toilet, group = SURFACE_BATH),
        opt(P1, "Maniglione orizzontale entro 15 cm dalla base d'appoggio", "Nel modulo: 1 punto se a meno di 6 pollici (15 cm) dalla base d'appoggio.", image = R.drawable.tai_bath_hbar, group = SURFACE_BATH),
        opt(P0, "Maniglione orizzontale a più di 15 cm dalla base d'appoggio", image = R.drawable.tai_bath_hbar, group = SURFACE_BATH),
        opt(P1, "Maniglione verticale entro 15 cm dalla base d'appoggio", "Nel modulo: 1 punto se a meno di 6 pollici (15 cm) dalla base d'appoggio.", image = R.drawable.tai_bath_vbar, group = SURFACE_BATH),
        opt(P0, "Maniglione verticale a più di 15 cm dalla base d'appoggio", image = R.drawable.tai_bath_vbar, group = SURFACE_BATH),
    )

    private fun bathHand(toilet: Int, horizontal: Int, vertical: Int): List<ToolOption> = listOf(
        opt(toilet, "Bordo della tazza del WC", image = R.drawable.tai_bath_toilet, group = SURFACE_BATH),
        opt(horizontal, "Maniglione orizzontale", image = R.drawable.tai_bath_hbar, group = SURFACE_BATH),
        opt(vertical, "Maniglione verticale", image = R.drawable.tai_bath_vbar, group = SURFACE_BATH),
    )

    private fun handNotUsed(): List<ToolOption> = listOf(
        na("Mano in grembo (non usata)", "La mano non partecipa al trasferimento: voce non applicabile.", image = R.drawable.tai_hand_on_lap, group = SURFACE_NONE),
    )

    /** Voce 10: mano di spinta sulla superficie di partenza. I punteggi seguono il modulo della versione. */
    private fun pushOffHandOptions(assisted: Boolean): List<ToolOption> = if (assisted) {
        wheelchairHand() + firmHand() + softHand(P0, P1, P0, P05) + chairHand(P0, P1, P0, P0, P05) + bathHand(P0, P1, P0) + handNotUsed()
    } else {
        wheelchairHand() + firmHand() + softHand(P1, P0, P0, P05) + chairHand(P1, P1, P0, P0, P05) + bathHandByDistance() + handNotUsed()
    }

    /** Voce 11: mano guida sulla superficie di arrivo. */
    private fun leadingHandOptions(assisted: Boolean): List<ToolOption> = if (assisted) {
        wheelchairHand() + boardHand() + firmHand() + bathHandByDistance() + softHand(P0, P1, P0, P05) + chairHand(P0, P1, P0, P0, P05) + handNotUsed()
    } else {
        wheelchairHand() + boardHand() + firmHand() + bathHandByDistance() + softHand(P1, P0, P0, P05) + chairHand(P1, P1, P0, P0, P05) + handNotUsed()
    }

    // -----------------------------------------------------------------------
    // Voci
    // -----------------------------------------------------------------------

    private class Numbering {
        private var next = 1
        fun next(): String = "${next++}"
    }

    /** Fase I e II (voci 1–9), uguali nelle versioni autonoma e assistita senza sollevatore. */
    private fun setupAndBodyItems(suffix: String, n: Numbering): List<ToolItem> = listOf(
        ToolItem(
            id = "distance$suffix",
            number = n.next(),
            section = SECTION_SETUP,
            title = "Distanza tra la carrozzina e la superficie di arrivo",
            hint = "Con un righello (se disponibile) misurare la distanza tra l'angolo anteriore della carrozzina e l'oggetto verso cui ci si trasferisce.",
            options = listOf(
                opt(P1, "Meno di 7,5 cm (3 pollici)"),
                opt(P05, "Da 7,5 a 12,5 cm (3–5 pollici)"),
                opt(P0, "Più di 12,5 cm (5 pollici)"),
            ),
        ),
        ToolItem(
            id = "angle$suffix",
            number = n.next(),
            title = "Angolo tra la carrozzina e la superficie di arrivo",
            hint = "Se possibile usare uno strumento per misurare l'angolo. Il punteggio dipende dal tipo di carrozzina.",
            imageRes = R.drawable.tai_angles,
            options = listOf(
                optByChair(P0, P1, "0–19 gradi", yesNoDesc(P0, P1)),
                optByChair(P1, P0, "20–45 gradi", yesNoDesc(P1, P0)),
                opt(P0, "46–90 gradi"),
            ),
        ),
        ToolItem(
            id = "brakes$suffix",
            number = n.next(),
            title = "Freni della carrozzina",
            hint = "Ha bloccato i freni della carrozzina?",
            options = listOf(
                opt(P1, "Sì, ho inserito i freni (carrozzina manuale)"),
                opt(P1, "Sì, ho spento la carrozzina (carrozzina elettrica)"),
                opt(P0, "No"),
                na("Non applicabile: la carrozzina non ha freni"),
            ),
        ),
        ToolItem(
            id = "armrest$suffix",
            number = n.next(),
            title = "Rimozione del bracciolo",
            hint = "Ha rimosso il bracciolo dalla carrozzina?",
            options = listOf(
                opt(P1, "Sì"),
                opt(P0, "No, ma la carrozzina ha i braccioli"),
                na("Non applicabile: la carrozzina non ha braccioli"),
                na("Non possibile: i braccioli non si possono rimuovere (imbullonati, saldati)"),
            ),
        ),
        ToolItem(
            id = "sideguards$suffix",
            number = n.next(),
            title = "Rimozione delle spondine o dei supporti posturali",
            hint = "Ha rimosso le spondine (proteggi-abiti) o i supporti posturali (guide per le cosce, supporti laterali) dalla carrozzina?",
            options = listOf(
                opt(P1, "Sì"),
                opt(P0, "No"),
                na("No, ma le spondine non superano l'altezza della ruota"),
                na("Non applicabile: la carrozzina non ha spondine"),
                na("Non possibile: le spondine non si possono rimuovere (imbullonate, saldate)"),
            ),
        ),
        ToolItem(
            id = "level$suffix",
            number = n.next(),
            title = "Trasferimento in piano (altezza del cuscino)",
            hint = "Il trasferimento era in piano (la parte superiore del cuscino a livello della superficie di arrivo)? Se possibile misurare con un righello la differenza di altezza tra la parte superiore dell'angolo anteriore del cuscino e la superficie di arrivo.",
            imageRes = R.drawable.tai_cushion_height,
            options = listOf(
                opt(P1, "Il cuscino è entro 2,5 cm (1 pollice) dalla superficie"),
                opt(P0, "Il cuscino è più alto di oltre 2,5 cm"),
                opt(P05, "Il cuscino è più basso di oltre 2,5 cm"),
                na("Non possibile regolare l'altezza della carrozzina o della superficie di arrivo"),
            ),
        ),
        ToolItem(
            id = "feet$suffix",
            number = n.next(),
            section = SECTION_BODY,
            title = "Posizione dei piedi",
            hint = "Posizionare il corpo per il trasferimento, sistemando bacino e gambe come di consueto. Dove sono i piedi? Il punteggio dipende dal tipo di carrozzina.",
            options = listOf(
                optByChair(P0, P1, "Entrambi sulla pedana", yesNoDesc(P0, P1)),
                opt(P05, "Uno sulla pedana, uno a terra"),
                opt(P1, "Entrambi sollevati sulla superficie di arrivo"),
                opt(P05, "Uno sulla superficie di arrivo e uno a terra o sulla pedana"),
                opt(P1, "Entrambi a terra"),
                opt(P1, "Uno a terra (amputazione di un arto)"),
                optByChair(P0, P1, "Uno sulla pedana (amputazione di un arto)", yesNoDesc(P0, P1)),
                na("Nessun appoggio (amputazione bilaterale)"),
            ),
        ),
        ToolItem(
            id = "scoot$suffix",
            number = n.next(),
            title = "Bacino portato avanti sulla seduta",
            hint = "Ha spostato il bacino verso il bordo anteriore della seduta, con almeno un terzo della coscia fuori dalla superficie?",
            options = listOf(
                opt(P1, "Sì"),
                opt(P0, "No"),
                na("Non possibile: non riesco a mantenere l'equilibrio in questa posizione"),
            ),
        ),
        ToolItem(
            id = "leading_arm$suffix",
            number = n.next(),
            title = "Posizione del braccio guida",
            hint = "Dove si trova il braccio guida (vedi immagini) una volta posizionato per il trasferimento?",
            imageRes = R.drawable.tai_leading_arm,
            options = listOf(
                opt(P0, "Sulla superficie di arrivo, dietro l'anca", image = R.drawable.tai_arm_behind_hip),
                opt(P1, "Sulla superficie di arrivo, tra l'anca e il ginocchio", image = R.drawable.tai_arm_between),
                opt(P1, "Sulla superficie di arrivo, oltre il ginocchio", image = R.drawable.tai_arm_past_knee),
                na("In grembo: non uso questo braccio durante il trasferimento", image = R.drawable.tai_arm_on_lap),
            ),
        ),
    )

    /** Fase III (voci 10–16) e ausili (17–18). */
    private fun flightAndAidItems(suffix: String, n: Numbering, assisted: Boolean): List<ToolItem> = listOf(
        ToolItem(
            id = "push_hand$suffix",
            number = n.next(),
            section = SECTION_FLIGHT,
            title = "Mano di spinta sulla superficie di partenza",
            hint = "Completare il trasferimento. Indicare il tipo di superficie di PARTENZA e poi tutte le posizioni che rappresentano meglio la mano di spinta mentre il bacino si spostava tra le superfici. Il punteggio è la media delle posizioni segnate.",
            optionGroupLabel = "Superficie di partenza",
            multiSelect = true,
            multiSelectMean = true,
            options = pushOffHandOptions(assisted),
        ),
        ToolItem(
            id = "lead_hand$suffix",
            number = n.next(),
            title = "Mano guida sulla superficie di arrivo",
            hint = "Indicare il tipo di superficie di ARRIVO e poi tutte le posizioni che rappresentano meglio la mano guida mentre il bacino si spostava tra le superfici. Il punteggio è la media delle posizioni segnate.",
            optionGroupLabel = "Superficie di arrivo",
            multiSelect = true,
            multiSelectMean = true,
            options = leadingHandOptions(assisted),
        ),
        ToolItem(
            id = "hand_hip$suffix",
            number = n.next(),
            title = "Mano guida al termine del trasferimento",
            hint = "Al termine del trasferimento, dove si trovava la mano guida rispetto alle anche (vedi immagine)?",
            imageRes = R.drawable.tai_hand_hip,
            options = listOf(
                opt(P1, "Vicino all'anca"),
                opt(P0, "A più di 15 cm (6 pollici) dall'anca"),
            ),
        ),
        ToolItem(
            id = "lean$suffix",
            number = n.next(),
            title = "Inclinazione del tronco durante il trasferimento",
            hint = "Durante lo spostamento tra le superfici, in che direzione era inclinato il tronco?",
            imageRes = R.drawable.tai_lean,
            options = listOf(
                opt(P0, "Inclinato indietro, verso la superficie di arrivo"),
                opt(P0, "Tronco eretto"),
                opt(P1, "Inclinato in avanti, lontano dalla superficie di arrivo"),
                opt(P0, "Non so"),
            ),
        ),
        ToolItem(
            id = "flight$suffix",
            number = n.next(),
            title = "Esecuzione del movimento",
            hint = "Durante lo spostamento tra le superfici (segnare tutte le voci che si applicano). Il punteggio è la somma, mai sotto 0.",
            multiSelect = true,
            options = listOf(
                opt(P1, "Movimento unico, fluido e continuo"),
                opt(P1, "Più spostamenti successivi («scivolamenti») per completare il trasferimento"),
                optByChair(M1, null, "Appoggio o sosta sulla ruota", yesNoDesc(M1, null)),
                opt(M05, "Movimento brusco, con cambi rapidi di direzione o di posizione del corpo per evitare una caduta"),
                opt(M1, "Quasi caduta (atterraggio involontario evitato per poco su una superficie indesiderata)"),
                opt(M1, "Caduta (atterraggio involontario su una superficie indesiderata)"),
            ),
        ),
        ToolItem(
            id = "landing$suffix",
            number = n.next(),
            title = "Atterraggio sulla superficie di arrivo",
            hint = "All'arrivo sulla superficie (segnare tutte le voci che si applicano). Il punteggio è la somma, mai sotto 0.",
            multiSelect = true,
            options = listOf(
                opt(P1, "Nessun movimento eccessivo (nessuna perdita di equilibrio)"),
                opt(M05, "Movimento eccessivo (perdita di equilibrio) senza atterrare involontariamente su una superficie indesiderata"),
                opt(M1, "Movimento eccessivo (perdita di equilibrio) con una parte del corpo atterrata involontariamente su una superficie indesiderata (caduta)"),
            ),
        ),
        ToolItem(
            id = "alternate$suffix",
            number = n.next(),
            title = "Braccio guida nei trasferimenti",
            hint = "Nei trasferimenti, con quale braccio guida?",
            options = listOf(
                opt(P0, "Sempre lo stesso braccio"),
                opt(P1, "Alterno il braccio guida"),
            ),
        ),
        ToolItem(
            id = "aids$suffix",
            number = n.next(),
            section = SECTION_AIDS,
            title = "Uso di ausili per il trasferimento",
            hint = "Uso ausili (come una tavoletta di trasferimento o un sollevatore):",
            options = listOf(
                opt(P1, "Mai: ho forza sufficiente per trasferirmi senza fatica"),
                opt(P0, "Mai, però a volte sento di faticare a completare il trasferimento"),
                opt(P1, "A volte, quando mi sento stanco o debole"),
                opt(P1, "A volte, per il dolore"),
                opt(P1, "A volte, per sicurezza o per preservare le braccia"),
                opt(P1, "Sempre, per affaticamento o limiti di forza"),
            ),
        ),
        ToolItem(
            id = "board$suffix",
            number = n.next(),
            title = "Uso della tavoletta di trasferimento",
            hint = "Se usa una tavoletta di trasferimento, come la usa?",
            options = listOf(
                opt(P1, "Più sollevamenti o spostamenti successivi, sollevando il bacino e riappoggiandolo in più passaggi"),
                opt(P0, "Faccio scivolare il bacino lungo la tavoletta"),
                na("Non uso la tavoletta di trasferimento"),
            ),
        ),
    )

    /** Rapporto con l'assistente (versioni assistite). */
    private fun helperItems(suffix: String, n: Numbering, lift: Boolean): List<ToolItem> = listOf(
        ToolItem(
            id = "feelings$suffix",
            number = n.next(),
            section = SECTION_HELPER,
            title = "Vissuto durante il trasferimento",
            hint = "Durante il trasferimento mi sono sentito (segnare tutte le voci che si applicano). Il punteggio è la somma, mai sotto 0.",
            multiSelect = true,
            options = listOf(
                opt(P1, "Fiducioso di raggiungere la destinazione senza incidenti"),
                opt(P1, "In controllo della situazione"),
                opt(M05, "Timoroso di cadere"),
                opt(M05, "Senza controllo su come veniva eseguito il trasferimento"),
                opt(M05, "Con dolore o fastidio oltre quelli che provo a riposo"),
            ),
        ),
        ToolItem(
            id = "communication$suffix",
            number = n.next(),
            title = "Comunicazione con l'assistente",
            hint = "Nella comunicazione con l'assistente durante il trasferimento (segnare tutte le voci che si applicano). Il punteggio è la somma, mai sotto 0.",
            multiSelect = true,
            options = listOf(
                opt(P1, "Mi sento a mio agio nel correggere l'assistente se qualcosa viene eseguito in modo scorretto"),
                opt(P0, "Mi sento a disagio nel far presente che qualcosa viene fatto in modo scorretto"),
                opt(P1, "Le informazioni tra me e l'assistente passano in modo chiaro, cortese ed efficace"),
                opt(M1, "La comunicazione con chi mi assiste è poco chiara e si verificano incomprensioni"),
            ),
        ),
        ToolItem(
            id = "helper_hands$suffix",
            number = n.next(),
            title = "Modalità di aiuto dell'assistente",
            hint = "Quando l'assistente mi aiuta:",
            options = listOf(
                opt(P0, "Tira il mio braccio (polso, avambraccio o braccio) per spostarmi da una posizione o superficie all'altra"),
                opt(P1, "Non tira le braccia e appoggia le mani su tronco, anche, glutei o gambe per spostarmi"),
            ),
        ),
    ) + if (lift) {
        listOf(
            ToolItem(
                id = "hands$suffix",
                number = n.next(),
                title = "Posizione delle mani durante il trasferimento",
                hint = "Durante il trasferimento le mie mani erano:",
                options = listOf(
                    opt(P1, "In grembo"),
                    opt(P1, "Aggrappate all'imbracatura o alla barra del sollevatore"),
                    opt(P1, "Lungo i fianchi"),
                ),
            ),
            ToolItem(
                id = "sling$suffix",
                number = n.next(),
                title = "Imbracatura durante il trasferimento",
                hint = "Riguardo all'imbracatura durante il trasferimento:",
                options = listOf(
                    opt(P1, "Posizionata in modo sicuro: nessuna o minime correzioni durante il volo"),
                    opt(M05, "Sono state necessarie correzioni moderate durante il volo per evitare una caduta"),
                    opt(M05, "Posizionata male: il trasferimento è stato interrotto durante il volo per evitare una caduta"),
                    opt(M1, "Posizionata male: durante il volo si è verificata una caduta"),
                ),
            ),
        )
    } else {
        emptyList()
    }

    private enum class Version { INDEPENDENT, ASSISTED, LIFT }

    private fun transferItems(version: Version, transfer: Int): List<ToolItem> {
        val suffix = "_t$transfer"
        val n = Numbering()
        return when (version) {
            Version.INDEPENDENT -> setupAndBodyItems(suffix, n) + flightAndAidItems(suffix, n, assisted = false)
            Version.ASSISTED -> setupAndBodyItems(suffix, n) + flightAndAidItems(suffix, n, assisted = true) + helperItems(suffix, n, lift = false)
            Version.LIFT -> helperItems(suffix, n, lift = true)
        }
    }

    private fun groups(version: Version): List<ToolGroup> {
        val first = transferItems(version, 1).map { it.id }
        val second = transferItems(version, 2).map { it.id }
        return listOf(
            ToolGroup(
                id = "t1",
                title = "Trasferimento 1",
                shortTitle = "Trasf. 1",
                subtitle = "Dalla carrozzina alla superficie (letto, lettino, divano, sedia, WC) o viceversa",
                itemIds = first,
            ),
            ToolGroup(
                id = "t2",
                title = "Trasferimento 2",
                shortTitle = "Trasf. 2",
                subtitle = "Facoltativo: il trasferimento di ritorno. Se compilato, il totale è la media delle percentuali dei due trasferimenti",
                itemIds = second,
                optional = true,
            ),
        )
    }

    private val wheelchairField = ToolField(
        id = WHEELCHAIR_FIELD,
        label = "Tipo di carrozzina",
        shortLabel = "Carrozzina",
        choices = listOf(MANUAL, POWER),
    )

    private val boardField = ToolField(
        id = "uses_board",
        label = "Uso della tavoletta di trasferimento",
        shortLabel = "Tavoletta",
        choices = listOf("Sì", "No"),
    )

    private val commonInstructions = listOf(
        "Indicare per prima cosa il tipo di carrozzina (manuale o elettrica): alcune voci hanno punteggi diversi " +
            "per le due carrozzine e l'app li applica da sola.",
        "Completare ogni fase prima di passare alla successiva: preparazione della carrozzina, preparazione " +
            "del corpo, volo e atterraggio. Le voci sono scritte dal punto di vista della persona (come nel " +
            "modulo originale) e possono essere compilate dal terapista, dalla persona o da chi la assiste.",
        "Le voci «non applicabili» (es. la carrozzina non ha braccioli, la mano non viene usata) escono dal " +
            "massimo: il punteggio del trasferimento è la percentuale del massimo applicabile.",
        "Nelle voci sulle posizioni della mano scegliere prima la superficie (di partenza per la mano di spinta, " +
            "di arrivo per la mano guida) e poi tutte le posizioni usate: il punteggio della voce è la media " +
            "dei punteggi delle posizioni segnate.",
        "Nelle voci «segnare tutte le voci che si applicano» i punteggi si sommano (anche quelli negativi) e " +
            "la voce non scende sotto 0.",
        "Il secondo trasferimento (il ritorno) è facoltativo: se viene compilato, il totale è la media delle " +
            "percentuali dei due trasferimenti; altrimenti vale il solo trasferimento 1.",
    )

    private const val REFERENCE = "Transfer Assessment Instrument 4.0 (TAI 4.0), Human Engineering Research Laboratories, " +
        "University of Pittsburgh / UPMC. Worobey LA, Zigler CK, Huzinec R, Rigot SK, Sung J, Rice LA. Reliability " +
        "and Validity of the Revised Transfer Assessment Instrument. Topics in Spinal Cord Injury Rehabilitation. " +
        "2018;24(3):217–226. Traduzione italiana delle voci dal modulo originale in inglese; le immagini sono " +
        "tratte dal modulo. Misure convertite: 1 pollice ≈ 2,5 cm, 3 pollici ≈ 7,5 cm, 5 pollici ≈ 12,5 cm, 6 pollici ≈ 15 cm."

    private const val CITATION = "Worobey, Rice e coll., 2018 – HERL, Università di Pittsburgh"

    private fun definition(
        version: Version,
        id: String,
        name: String,
        shortName: String,
        subtitle: String,
        description: String,
        instructions: List<String>,
        fields: List<ToolField>,
        duration: String,
        itemsPerTransfer: Int,
    ): ToolDefinition = ToolDefinition(
        id = id,
        name = name,
        shortName = shortName,
        citation = CITATION,
        subtitle = subtitle,
        domain = "Trasferimenti in carrozzina · Mobilità · Sicurezza",
        duration = duration,
        description = description,
        instructions = instructions,
        reference = REFERENCE,
        fields = fields,
        items = transferItems(version, 1) + transferItems(version, 2),
        bands = emptyList(),
        totalLabel = "Punteggio TAI",
        itemNounSingular = "voce",
        itemNounPlural = "voci",
        itemsSectionTitle = "Voci dello strumento",
        notesLabel = "Osservazioni",
        notesHint = "Superfici usate, ausili, condizioni del trasferimento, eventi rilevanti…",
        groups = groups(version),
        scoreDivisor = 100,
        variantFieldId = WHEELCHAIR_FIELD,
        percentHeadline = true,
        groupPercentMean = true,
        totalFloor = 0,
        itemsCountLabel = "$itemsPerTransfer voci per trasferimento",
        scoringNote = "Lo strumento non prevede fasce interpretative. Per ogni trasferimento: A = somma dei punteggi " +
            "delle voci, B = numero di voci non applicabili; punteggio del trasferimento = 100 × A ÷ (massimo − B). " +
            "Il totale è il punteggio del trasferimento 1 oppure, se è stato valutato anche il trasferimento 2, la " +
            "media dei due. Più alta è la percentuale, più il trasferimento è sicuro e corretto.",
    )

    /** TAI 4.0 – Trasferimenti autonomi. */
    val independent: ToolDefinition = definition(
        version = Version.INDEPENDENT,
        id = ID_INDEPENDENT,
        name = "TAI 4.0 – Trasferimenti autonomi",
        shortName = "TAI",
        subtitle = "Qualità e sicurezza dei trasferimenti autonomi in carrozzina, in tre fasi",
        description = "Il Transfer Assessment Instrument (TAI 4.0) valuta in modo oggettivo i trasferimenti eseguiti " +
            "senza aiuto da e verso la carrozzina. Scompone il trasferimento in tre fasi: preparazione della " +
            "carrozzina (distanza, angolo, freni, braccioli, spondine, altezza), preparazione del corpo (piedi, " +
            "bacino, braccio guida) e volo/atterraggio (posizioni delle mani, inclinazione del tronco, fluidità, " +
            "equilibrio all'arrivo, alternanza del braccio guida), più due voci sugli ausili. Il punteggio cambia " +
            "a seconda che la carrozzina sia manuale o elettrica. Sono previste due compilazioni (andata e " +
            "ritorno), ma basta un solo trasferimento. Il punteggio è la percentuale del massimo applicabile: le " +
            "voci non applicabili non entrano nel calcolo.",
        instructions = listOf(
            "Far preparare la carrozzina per il trasferimento verso un'altra superficie (letto, lettino, divano), " +
                "poi rispondere alle voci della fase I; far posizionare il corpo e rispondere alla fase II; far " +
                "completare il trasferimento e rispondere alla fase III e alle voci sugli ausili.",
        ) + commonInstructions + listOf(
            "Indicare se la persona usa una tavoletta di trasferimento: se non la usa, la voce sulla tavoletta " +
                "va segnata come non applicabile.",
        ),
        fields = listOf(wheelchairField, boardField),
        duration = "15–25 minuti per trasferimento",
        itemsPerTransfer = 18,
    )

    /** TAI 4.0 – Trasferimenti assistiti senza sollevatore. */
    val assisted: ToolDefinition = definition(
        version = Version.ASSISTED,
        id = ID_ASSISTED,
        name = "TAI 4.0 – Trasferimenti assistiti senza sollevatore",
        shortName = "TAI-A",
        subtitle = "Trasferimenti con l'aiuto di una persona, senza sollevatore meccanico",
        description = "Versione del Transfer Assessment Instrument (TAI 4.0) per i trasferimenti eseguiti con " +
            "l'aiuto di un'altra persona, senza sollevatore meccanico. Comprende le tre fasi dei trasferimenti " +
            "autonomi (preparazione della carrozzina, preparazione del corpo, volo e atterraggio), gli ausili e " +
            "tre voci sul rapporto con l'assistente: il vissuto della persona durante il trasferimento, la " +
            "comunicazione con l'assistente e il modo in cui l'assistente aiuta (senza tirare le braccia). " +
            "Alcuni punteggi delle posizioni della mano differiscono da quelli della versione autonoma, come nel " +
            "modulo originale. Il modulo prevede la valutazione di due trasferimenti (andata e ritorno); il " +
            "totale è la media delle due percentuali.",
        instructions = listOf(
            "Far preparare la carrozzina per il trasferimento verso un'altra superficie, poi rispondere alle " +
                "voci della fase I; far posizionare il corpo e rispondere alla fase II; far completare il " +
                "trasferimento con l'assistente e rispondere alle voci della fase III, degli ausili e " +
                "dell'assistente.",
        ) + commonInstructions,
        fields = listOf(wheelchairField),
        duration = "15–25 minuti per trasferimento",
        itemsPerTransfer = 21,
    )

    /** TAI 4.0 – Trasferimenti assistiti con sollevatore meccanico. */
    val lift: ToolDefinition = definition(
        version = Version.LIFT,
        id = ID_LIFT,
        name = "TAI 4.0 – Trasferimenti con sollevatore",
        shortName = "TAI-S",
        subtitle = "Trasferimenti assistiti con sollevatore meccanico",
        description = "Versione del Transfer Assessment Instrument (TAI 4.0) per i trasferimenti eseguiti con un " +
            "sollevatore meccanico. Cinque voci: il vissuto della persona durante il trasferimento, la " +
            "comunicazione con l'assistente, il modo in cui l'assistente aiuta, la posizione delle mani durante " +
            "il trasferimento e il posizionamento dell'imbracatura. Il punteggio di ciascun trasferimento è la " +
            "percentuale del massimo (7 punti); il modulo prevede due trasferimenti (andata e ritorno) e il " +
            "totale è la media delle due percentuali.",
        instructions = listOf(
            "Far eseguire il trasferimento con il sollevatore, poi rispondere alle cinque voci dal punto di " +
                "vista della persona trasferita.",
            "Nelle voci «segnare tutte le voci che si applicano» i punteggi si sommano (anche quelli negativi) e " +
                "la voce non scende sotto 0.",
            "Il secondo trasferimento (il ritorno) è facoltativo: se viene compilato, il totale è la media delle " +
                "percentuali dei due trasferimenti; altrimenti vale il solo trasferimento 1.",
        ),
        fields = listOf(wheelchairField),
        duration = "10 minuti per trasferimento",
        itemsPerTransfer = 5,
    )
}
