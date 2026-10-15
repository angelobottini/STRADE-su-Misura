package it.stradesumisura.app.domain.tools

import it.stradesumisura.app.domain.DecisionNode
import it.stradesumisura.app.domain.DecisionNode.Companion.choice
import it.stradesumisura.app.domain.DecisionNode.Companion.level
import it.stradesumisura.app.domain.DecisionNode.Companion.yesNo
import it.stradesumisura.app.domain.DecisionPart
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolField
import it.stradesumisura.app.domain.ToolGroup
import it.stradesumisura.app.domain.ToolItem
import it.stradesumisura.app.domain.ToolOption

/**
 * FIM – Functional Independence Measure (versione italiana).
 * Diciotto voci (13 motorie e 5 cognitive) valutate su sette livelli, da 1 (assistenza totale) a
 * 7 (autosufficienza completa): punteggio totale 18–126, area motoria 13–91, area cognitiva 5–35.
 *
 * Per ogni voce lo strumento riporta le descrizioni dei livelli del manuale italiano e il relativo
 * albero decisionale (flow-chart): il valutatore può rispondere alle domande Sì/No e l'app
 * seleziona il livello raggiunto. Le voci Vescica e Alvo hanno due alberi (livello di assistenza e
 * frequenza degli episodi di incontinenza): il livello della voce è il più basso tra i due.
 */
object FunctionalIndependenceMeasure {

    const val ID = "fim"

    private class LevelNames(
        val l7: String,
        val l6: String,
        val l5: String,
        val l4: String,
        val l3: String,
        val l2: String,
        val l1: String,
    )

    private val motorNames = LevelNames(
        "Autosufficienza completa",
        "Autosufficienza con adattamenti",
        "Supervisione o predisposizione/adattamenti",
        "Assistenza con minimo contatto fisico",
        "Assistenza moderata",
        "Assistenza intensa",
        "Assistenza totale",
    )

    private val facilitationNames = LevelNames(
        "Autosufficienza completa",
        "Autosufficienza con adattamenti",
        "Facilitazione occasionale",
        "Facilitazione minima",
        "Facilitazione moderata",
        "Facilitazione intensa",
        "Assistenza totale",
    )

    private val socialNames = LevelNames(
        "Autosufficienza completa",
        "Autosufficienza con adattamenti",
        "Supervisione",
        "Controllo minimo",
        "Controllo moderato",
        "Controllo intenso",
        "Assistenza totale",
    )

    private val problemNames = LevelNames(
        "Autosufficienza completa",
        "Autosufficienza con adattamenti",
        "Supervisione",
        "Risolve da solo, raramente aiutato",
        "Risolve da solo, aiutato ogni tanto",
        "Risolve da solo, aiutato spesso",
        "Assistenza totale",
    )

    private val memoryNames = LevelNames(
        "Autosufficienza completa",
        "Autosufficienza con adattamenti",
        "Supervisione",
        "Facilitazione minima",
        "Facilitazione moderata",
        "Facilitazione intensa",
        "Assistenza totale",
    )

    /** Le sette opzioni di una voce, dal livello 7 al livello 1 come nel manuale. */
    private fun levels(
        names: LevelNames,
        l7: String,
        l6: String,
        l5: String,
        l4: String,
        l3: String,
        l2: String,
        l1: String,
    ): List<ToolOption> = listOf(
        ToolOption(7, names.l7, l7),
        ToolOption(6, names.l6, l6),
        ToolOption(5, names.l5, l5),
        ToolOption(4, names.l4, l4),
        ToolOption(3, names.l3, l3),
        ToolOption(2, names.l2, l2),
        ToolOption(1, names.l1, l1),
    )

    /**
     * Albero decisionale standard (figura 1 del manuale): assistenza necessaria? → metà dello
     * sforzo? → supervisione / aiuto occasionale / assistenza totale.
     */
    private fun standardTree(
        names: LevelNames,
        needsHelp: String,
        adaptations: String,
        halfEffort: String,
        supervisionOnly: String,
        occasionalHelp: String,
        totalAssistance: String,
    ): DecisionNode = yesNo(
        needsHelp,
        no = yesNo(adaptations, yes = level(6, names.l6), no = level(7, names.l7)),
        yes = yesNo(
            halfEffort,
            yes = yesNo(
                supervisionOnly,
                yes = level(5, names.l5),
                no = yesNo(occasionalHelp, yes = level(4, names.l4), no = level(3, names.l3)),
            ),
            no = yesNo(totalAssistance, yes = level(1, names.l1), no = level(2, names.l2)),
        ),
    )

    /** Albero della frequenza degli episodi di incontinenza (figure 9 e 11). */
    private fun frequencyTree(hadEpisodes: String, device: String, howMany: String): DecisionNode = yesNo(
        hadEpisodes,
        no = yesNo(
            device,
            yes = level(6, "Nessun episodio; usa un dispositivo particolare"),
            no = level(7, "Nessun episodio"),
        ),
        yes = choice(
            howMany,
            "Un (1) episodio" to level(5, "Un episodio negli ultimi 3 giorni"),
            "Due (2) episodi" to level(4, "Due episodi negli ultimi 3 giorni"),
            "Tre (3) episodi" to level(3, "Tre episodi negli ultimi 3 giorni"),
            "Quattro (4) episodi" to level(2, "Quattro episodi negli ultimi 3 giorni"),
            "Cinque (5) episodi o più" to level(1, "Cinque o più episodi negli ultimi 3 giorni"),
        ),
    )

    /** Albero della locomozione e delle scale (figure 15 e 16), con le distanze al posto dello sforzo. */
    private fun locomotionTree(
        needsPerson: String,
        fullDistance: String,
        aidOrTime: String,
        shortDistance: String,
        shortDistanceLabel: String,
        assistedFullDistance: String,
        supervisionOnly: String,
        occasionalHelp: String,
        minimalDistance: String,
    ): DecisionNode = yesNo(
        needsPerson,
        no = yesNo(
            fullDistance,
            yes = yesNo(aidOrTime, yes = level(6, motorNames.l6), no = level(7, motorNames.l7)),
            no = yesNo(shortDistance, yes = level(5, shortDistanceLabel), no = level(1, motorNames.l1)),
        ),
        yes = yesNo(
            assistedFullDistance,
            yes = yesNo(
                supervisionOnly,
                yes = level(5, "Supervisione"),
                no = yesNo(occasionalHelp, yes = level(4, motorNames.l4), no = level(3, motorNames.l3)),
            ),
            no = yesNo(minimalDistance, yes = level(1, motorNames.l1), no = level(2, motorNames.l2)),
        ),
    )

    private const val NOTE = "Note"

    val definition: ToolDefinition = ToolDefinition(
        id = ID,
        name = "FIM – Misura dell'indipendenza funzionale",
        shortName = "FIM",
        citation = "Granger e coll., 1987; versione italiana SO.GE.COM.",
        subtitle = "Diciotto voci motorie e cognitive su sette livelli, con gli alberi decisionali",
        domain = "Attività di base della vita quotidiana · Comunicazione · Capacità cognitive",
        duration = "30–45 minuti (osservazione e colloquio)",
        description = "La FIM (Functional Independence Measure) misura la disabilità come quantità di assistenza " +
            "necessaria a svolgere le attività quotidiane: tredici voci motorie (cura della persona, controllo " +
            "sfinterico, trasferimenti, locomozione) e cinque cognitive (comunicazione, capacità relazionali e " +
            "cognitive), ciascuna valutata su sette livelli, da 1 (assistenza totale) a 7 (autosufficienza " +
            "completa). I livelli 7 e 6 indicano autosufficienza (nessun'altra persona è necessaria); i livelli " +
            "da 5 a 1 indicano non autosufficienza, parziale (5–3: la persona compie almeno la metà dello sforzo) " +
            "o completa (2–1). Il punteggio totale va da 18 a 126; l'area motoria da 13 a 91 e l'area cognitiva " +
            "da 5 a 35. Per ogni voce l'app propone l'albero decisionale del manuale: rispondendo alle domande " +
            "(Sì/No, oppure il numero di episodi di incontinenza) si arriva al livello da registrare, che resta " +
            "comunque modificabile a mano.",
        instructions = listOf(
            "Valutare ciò che la persona fa effettivamente, non ciò che potrebbe fare, nel periodo di osservazione " +
                "(di regola i primi tre giorni di ricovero o, per il controllo sfinterico, gli ultimi tre giorni). " +
                "Se i livelli variano durante la giornata registrare il più basso.",
            "Per ogni voce aprire l'albero decisionale e rispondere alle domande: seguire il percorso Sì quando " +
                "la risposta è positiva, il percorso No quando è negativa a tutte le domande del riquadro. Il " +
                "livello raggiunto viene selezionato automaticamente; in alternativa scegliere il livello " +
                "direttamente dalle descrizioni.",
            "Autosufficienza (7–6): non è richiesta un'altra persona. Non autosufficienza parziale (5–3): la " +
                "persona compie almeno il 50% dello sforzo. Non autosufficienza completa (2–1): meno del 50% " +
                "dello sforzo, con assistenza intensa o totale. I riferimenti percentuali sono indicativi.",
            "Vescica e Alvo hanno due alberi: il livello di assistenza e la frequenza degli episodi di " +
                "incontinenza negli ultimi tre giorni. Il livello da registrare è il più basso tra i due.",
            "Locomozione, Comprensione ed Espressione: indicare nei campi dedicati la modalità più frequente " +
                "(cammino o carrozzina; uditiva o visiva; verbale o non verbale), oppure entrambe se usate in " +
                "egual misura.",
            "Quando l'attività non ha luogo (es. la persona non viene vestita né si veste, o non si trasferisce " +
                "in vasca) registrare il livello 1. Non lasciare voci non valutate: il totale ha senso solo con " +
                "tutte le diciotto voci.",
        ),
        reference = "Keith RA, Granger CV, Hamilton BB, Sherwin FS. The Functional Independence Measure: a new tool " +
            "for rehabilitation. Advances in Clinical Rehabilitation. 1987;1:6–18. Versione italiana della scala " +
            "FIM® a cura di SO.GE.COM. (Milano): descrizioni dei livelli e alberi decisionali dal manuale italiano.",
        fields = listOf(
            ToolField("locomotion_mode", "Modalità di locomozione", "Cammino, carrozzina o entrambe", shortLabel = "Locomozione"),
            ToolField("comprehension_mode", "Modalità di comprensione", "Uditiva, visiva o entrambe", shortLabel = "Comprensione"),
            ToolField("expression_mode", "Modalità di espressione", "Verbale, non verbale o entrambe", shortLabel = "Espressione"),
        ),
        totalLabel = "Punteggio FIM",
        itemNounSingular = "voce",
        itemNounPlural = "voci",
        itemsSectionTitle = "Voci della scala",
        groups = listOf(
            ToolGroup(
                id = "motor",
                title = "Area motoria",
                shortTitle = "Motoria",
                subtitle = "Cura della persona (A–F), controllo sfinterico (G–H), trasferimenti (I–K), locomozione (L–M)",
                itemIds = listOf(
                    "feeding", "grooming", "bathing", "dressing_upper", "dressing_lower", "toileting",
                    "bladder", "bowel", "transfer_bed", "transfer_toilet", "transfer_tub", "locomotion", "stairs",
                ),
            ),
            ToolGroup(
                id = "cognitive",
                title = "Area cognitiva",
                shortTitle = "Cognitiva",
                subtitle = "Comunicazione (N–O), capacità relazionali e cognitive (P–R)",
                itemIds = listOf("comprehension", "expression", "social", "problem_solving", "memory"),
            ),
        ),
        items = listOf(
            // ------------------------------------------------------------------
            // Cura della persona
            // ------------------------------------------------------------------
            ToolItem(
                id = "feeding",
                number = "A",
                title = "Nutrirsi",
                hint = "Uso di dispositivi idonei a portare il cibo alla bocca, masticazione e deglutizione, con il pasto già preparato e presentato.",
                description = "Comprende l'utilizzo di dispositivi idonei a portare il cibo alla bocca, la masticazione e la " +
                    "deglutizione, una volta che il pasto sia stato preparato e presentato su tavolo o vassoio " +
                    "(presentare il pasto nel modo usuale include la consegna del vassoio e la rimozione del coperchio). " +
                    "Se la persona presenta livelli diversi durante il giorno, registrare il più basso. Se si alimenta in " +
                    "parte per bocca e in parte tramite sondino, considerare ogni pasto e ogni somministrazione via " +
                    "sondino come episodi separati e registrare la valutazione più bassa (es. predisposizione per bocca e " +
                    "sondino gestito da un assistente = livello 1). Un sondino non usato per idratazione o nutrizione " +
                    "non va considerato.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Mangia da un piatto e si nutre con qualsiasi tipo di cibo; beve da una tazza o da un bicchiere " +
                        "con il pasto predisposto in maniera abituale su tavolo o vassoio. Usa cucchiaio o forchetta per " +
                        "portare il cibo alla bocca, poi mastica e deglutisce.",
                    l6 = "Uno o più punti: sono necessari dispositivi sostitutivi o ausili (cannuccia, cucchiaio " +
                        "modificato con punte a forchetta, coltello a lama ricurva); per il pasto è richiesto un tempo " +
                        "superiore al ragionevole, oppure cibi di consistenza modificata (tritati/omogeneizzati) o " +
                        "precauzioni; se si alimenta in parte per altre vie (parenterale, sondino) gestisce da solo anche " +
                        "questi tipi di alimentazione; c'è preoccupazione per la sicurezza quando mangia.",
                    l5 = "Richiede supervisione (presenza di una persona vicina, suggerimenti, indicazioni, esortazioni) " +
                        "oppure predisposizioni (applicazione di ortesi o dispositivi di supporto, adattamenti per " +
                        "mangiare); oppure serve una persona per aprire contenitori, tagliare la carne, spalmare alimenti " +
                        "sul pane o versare le bevande; oppure si nutre con bevande di consistenza modificata e un " +
                        "assistente utilizza un addensante o modifica la consistenza del cibo.",
                    l4 = "Esegue il 75% o più delle operazioni richieste per nutrirsi.",
                    l3 = "Esegue dal 50 al 74% delle operazioni necessarie per alimentarsi.",
                    l2 = "Esegue dal 25 al 49% delle operazioni necessarie per alimentarsi.",
                    l1 = "Uno o più punti: esegue meno del 25% delle operazioni; non mangia o beve pasti completi per via " +
                        "orale ma si basa, almeno in parte, su altri metodi (parenterale, gastrostomia) con manovre " +
                        "compiute da personale di assistenza; necessita di due persone per mangiare; necessita di " +
                        "sondino per liquidi o idratazione (o ambedue) somministrati da una persona.",
                ),
                decisionTree = standardTree(
                    motorNames,
                    needsHelp = "Il paziente richiede la presenza di un assistente quando assume i pasti?",
                    adaptations = "Il paziente ha bisogno di dispositivi sostitutivi o ausili per mangiare? Impiega più di un " +
                        "tempo ragionevole per mangiare? C'è preoccupazione per la sua sicurezza quando mangia? Ha bisogno " +
                        "di cibo con consistenza modificata? Si nutre per via parenterale o con sondino naso-gastrico in " +
                        "modo autonomo?",
                    halfEffort = "Il paziente compie almeno metà dello sforzo necessario per nutrirsi?",
                    supervisionOnly = "Al paziente serve solo supervisione, suggerimenti, incoraggiamento o aiuto per " +
                        "applicare un'ortesi, per tagliare il cibo, aprire i contenitori, versare o ispessire bevande o " +
                        "imburrare il pane?",
                    occasionalHelp = "Il paziente ha solo bisogno di assistenza occasionale, come per impugnare una posata " +
                        "o per raccogliere il cibo con una forchetta o un cucchiaio?",
                    totalAssistance = "Il paziente necessita di assistenza per tenere in mano le posate, raccogliere il " +
                        "cibo e portarlo alla bocca, compresi i liquidi, prima di masticare e deglutire? Ha bisogno di " +
                        "assistenza totale con il sondino gastrico o l'idratazione endovenosa?",
                ),
            ),
            ToolItem(
                id = "grooming",
                number = "B",
                title = "Rassettarsi",
                hint = "Igiene orale, pettinarsi, lavare e asciugare mani e faccia, radersi o truccarsi.",
                description = "Comprende l'igiene del cavo orale (lavarsi i denti); pettinarsi o spazzolarsi i capelli; lavare, " +
                    "sciacquare e asciugare le mani; lavare, sciacquare e asciugare la faccia; radersi o truccarsi. Non " +
                    "include il filo interdentale, lo shampoo o le acconciature. Comprende quattro o cinque operazioni " +
                    "(se la persona non si rade o non si trucca, solo le prime quattro, ognuna pari al 25% del totale; se " +
                    "è calva, non valutare la cura dei capelli). Include il procurarsi gli strumenti necessari e le " +
                    "operazioni preparatorie (es. il dentifricio sullo spazzolino). La carrozzina non è un dispositivo " +
                    "di assistenza. Basare la valutazione sul numero di compiti eseguiti e sulla percentuale di sforzo.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Esegue in sicurezza tutte le operazioni di cura personale senza assistenza di una persona, " +
                        "senza ausili e in un ragionevole lasso di tempo.",
                    l6 = "Esegue tutte le operazioni senza assistenza di una persona e uno o più punti: ha bisogno di un " +
                        "ausilio (ortesi, protesi, guanto spugnato, spazzolino, pettine o spazzola modificati) e lo usa in " +
                        "modo autonomo; ha bisogno di tempo supplementare; c'è preoccupazione per la sicurezza.",
                    l5 = "Esegue tutte le operazioni ma richiede supervisione (stare vicino, suggerire o incoraggiare) o " +
                        "predisposizione (applicazione di un'ortesi, disposizione degli oggetti, assistenza nelle " +
                        "operazioni preparatorie come il dentifricio sullo spazzolino).",
                    l4 = "Esegue il 75% o più delle operazioni di cura personale.",
                    l3 = "Esegue dal 50 al 74% delle operazioni di cura personale.",
                    l2 = "Esegue dal 25 al 49% delle operazioni di cura personale.",
                    l1 = "Esegue meno del 25% delle operazioni di cura personale, oppure necessita dell'assistenza di " +
                        "due persone.",
                ),
                decisionTree = standardTree(
                    motorNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per lavarsi i denti, pettinarsi o spazzolarsi i capelli, " +
                        "lavarsi le mani e la faccia, radersi o truccarsi?",
                    adaptations = "Il paziente ha bisogno di dispositivi particolari, come un pettine modificato, per " +
                        "rassettarsi? È richiesto un tempo superiore a quanto sia ragionevole? Si devono prendere " +
                        "precauzioni?",
                    halfEffort = "Il paziente compie almeno metà dello sforzo per rassettarsi?",
                    supervisionOnly = "Il paziente ha solo bisogno di supervisione o di indicazioni per la predisposizione " +
                        "degli strumenti per la cura della persona, oppure di aiuto per l'applicazione di ortesi?",
                    occasionalHelp = "Il paziente compie il 75% o più delle operazioni richieste e ha solo bisogno di aiuto " +
                        "occasionale, come porgergli un asciugamano?",
                    totalAssistance = "Il paziente necessita di assistenza totale per rassettarsi, ad esempio un assistente " +
                        "che procuri gli strumenti necessari ed esegua in linea di massima tutte le attività?",
                ),
            ),
            ToolItem(
                id = "bathing",
                number = "C",
                title = "Lavarsi",
                hint = "Lavarsi, sciacquarsi e asciugarsi il corpo dal collo in giù, in vasca, doccia o con spugnature a letto.",
                description = "Include lavarsi, sciacquarsi e asciugarsi il corpo dal collo in giù (escluso il collo e la " +
                    "schiena) in una vasca o in una doccia, oppure con spugnature a letto; l'attività è eseguita in " +
                    "sicurezza. Dividere il corpo in dieci aree, ciascuna pari al 10%: torace; braccio sinistro; braccio " +
                    "destro; addome; area perineale anteriore; area perineale posteriore; coscia sinistra; coscia destra; " +
                    "gamba sinistra e piede; gamba destra e piede.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Si lava in sicurezza (lava, sciacqua e asciuga il corpo).",
                    l6 = "Ha bisogno di dispositivi particolari (guanto da lavaggio, spugna con impugnatura lunga) per " +
                        "lavarsi, oppure impiega più del tempo ragionevole, oppure si devono prendere precauzioni per " +
                        "ragioni di rischio.",
                    l5 = "È richiesta supervisione (presenza di una persona vicina, indicazioni, suggerimenti, esortazioni) " +
                        "oppure predisposizioni (attrezzi speciali per il bagno, preparazione dell'acqua o degli accessori " +
                        "per il lavaggio).",
                    l4 = "Esegue il 75% o più delle operazioni richieste per lavarsi.",
                    l3 = "Esegue dal 50 al 74% delle operazioni richieste per lavarsi.",
                    l2 = "Esegue dal 25 al 49% delle operazioni richieste per lavarsi.",
                    l1 = "Esegue meno del 25% delle operazioni richieste per lavarsi; necessita dell'assistenza di due " +
                        "persone; oppure l'attività non ha luogo (la persona non si lava da sola né viene lavata).",
                ),
                decisionTree = standardTree(
                    motorNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per lavarsi, sciacquarsi o asciugarsi il corpo?",
                    adaptations = "Il paziente ha bisogno di un ausilio per lavarsi (come una spugna con impugnatura lunga o " +
                        "un guanto da lavaggio), oppure impiega più del tempo ragionevole per lavarsi, oppure c'è " +
                        "preoccupazione per la sua sicurezza, ad esempio per la regolazione della temperatura dell'acqua?",
                    halfEffort = "Il paziente compie almeno metà dello sforzo necessario per lavarsi?",
                    supervisionOnly = "Il paziente ha solo bisogno di supervisione, suggerimenti o incoraggiamento, oppure " +
                        "di aiuto per preparare l'occorrente per il bagno o regolare la temperatura dell'acqua, oppure di " +
                        "aiuto per applicare un'ortesi?",
                    occasionalHelp = "Il paziente ha solo bisogno di aiuto occasionale, come porgergli qualche volta una " +
                        "spugna mentre si lava, o aiutarlo a lavare solo una o due aree del corpo (un braccio, i piedi o " +
                        "i glutei)?",
                    totalAssistance = "Il paziente ha bisogno di assistenza totale per lavarsi, come un assistente che " +
                        "utilizzi spugna e asciugamano ed esegua in linea di massima tutte le attività?",
                ),
            ),
            ToolItem(
                id = "dressing_upper",
                number = "D",
                title = "Vestirsi, dalla vita in su",
                hint = "Vestirsi e svestirsi sopra la vita, incluse protesi od ortesi quando necessarie.",
                description = "Include il vestirsi e lo svestirsi al di sopra della vita, e l'applicazione e rimozione di " +
                    "protesi od ortesi quando necessarie; l'attività è eseguita in sicurezza. Non valutare se la persona " +
                    "non indossa vestiti idonei da portare in pubblico: con il solo camice da ospedale o il pigiama la " +
                    "valutazione è livello 1. Il personale deve procurare vestiti adatti; una volta ottenuti, aggiornare " +
                    "la valutazione osservando la persona in un momento in cui è sveglia e vigile.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Si veste e si sveste da solo ed è in grado di prelevare gli abiti dalle loro sedi abituali " +
                        "(cassetti o armadi); si mette e si toglie reggiseno, maglioni, indumenti con allacciatura " +
                        "anteriore; apre e chiude cerniere-lampo, bottoni con asola e a pressione; applica e toglie da " +
                        "solo protesi od ortesi.",
                    l6 = "Sono richiesti speciali dispositivi di allacciatura (es. in velcro) o particolari ausili " +
                        "(incluse protesi od ortesi) per completare il gesto, oppure un tempo superiore al ragionevole. " +
                        "Se usa una protesi o un'ortesi come ausilio, la applica da solo.",
                    l5 = "È richiesta supervisione (presenza di una persona vicina, indicazioni, suggerimenti, esortazioni) " +
                        "oppure predisposizioni (applicazione di ortesi, disposizione di attrezzi, predisposizione di " +
                        "vestiti o indumenti speciali).",
                    l4 = "Esegue il 75% o più delle operazioni necessarie a vestirsi.",
                    l3 = "Esegue dal 50 al 74% delle operazioni necessarie a vestirsi.",
                    l2 = "Esegue dal 25 al 49% delle operazioni necessarie a vestirsi.",
                    l1 = "Esegue meno del 25% delle operazioni; viene completamente vestito da un assistente; " +
                        "necessita di due persone; oppure l'attività non ha luogo (non si veste né viene vestito).",
                ),
                decisionTree = standardTree(
                    motorNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per vestire la parte superiore del corpo?",
                    adaptations = "Il paziente ha bisogno di un ausilio o dispositivo particolare per vestire la parte " +
                        "superiore del corpo, come un allaccia-bottoni o una chiusura in velcro? Impiega più di un tempo " +
                        "ragionevole? C'è preoccupazione per la sicurezza quando veste la parte superiore del corpo?",
                    halfEffort = "Il paziente compie almeno metà dello sforzo necessario per vestire la parte superiore " +
                        "del corpo?",
                    supervisionOnly = "Il paziente ha solo bisogno di supervisione, suggerimenti o incoraggiamento, oppure " +
                        "di aiuto per preparare i vestiti e i relativi accessori, oppure di aiuto per applicare " +
                        "un'ortesi o una protesi?",
                    occasionalHelp = "Il paziente ha solo bisogno di aiuto occasionale ed esiguo per iniziare a vestirsi " +
                        "sopra la cintola, o solo di assistenza con bottoni, cerniere o ganci?",
                    totalAssistance = "Il paziente ha bisogno di assistenza totale per vestire la parte superiore del " +
                        "corpo, come una persona che gli porga i vestiti e che esegua sostanzialmente tutte le attività?",
                ),
            ),
            ToolItem(
                id = "dressing_lower",
                number = "E",
                title = "Vestirsi, dalla vita in giù",
                hint = "Vestirsi e svestirsi dalla cintola in giù, incluse eventuali protesi od ortesi.",
                description = "Include il vestirsi e lo svestirsi dalla cintola in giù, nonché applicare e rimuovere " +
                    "un'eventuale protesi od ortesi; l'attività è eseguita in sicurezza. Non valutare se la persona non " +
                    "indossa vestiti idonei da portare in pubblico: con il solo camice da ospedale o il pigiama la " +
                    "valutazione è livello 1. Il personale deve procurare vestiti adatti, incluse le calzature; una " +
                    "volta ottenuti, aggiornare la valutazione osservando la persona sveglia e vigile.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Si veste e si sveste autonomamente ed è in grado di prendere gli indumenti dalle loro sedi " +
                        "abituali; si mette e si toglie mutande, calzoni, gonne, cinture, calze e scarpe; usa cerniere " +
                        "lampo, bottoni ad asola e a pressione; si mette e si toglie eventuali protesi od ortesi.",
                    l6 = "Necessita di speciali dispositivi di allacciatura (es. in velcro) o di ausili particolari " +
                        "(incluse protesi od ortesi), oppure di un tempo superiore al ragionevole. Se usa una protesi o " +
                        "un'ortesi come ausilio, è in grado di applicarla da solo.",
                    l5 = "Necessita di supervisione (stare vicino, suggerimenti o incoraggiamento) o predisposizione " +
                        "(applicazione di un'ortesi/protesi per la parte inferiore del corpo o gli arti, applicazione di " +
                        "un dispositivo di supporto/adattamento, preparazione di vestiti o accessori).",
                    l4 = "Esegue il 75% o più delle operazioni necessarie a vestirsi.",
                    l3 = "Esegue dal 50 al 74% delle operazioni necessarie a vestirsi.",
                    l2 = "Esegue dal 25 al 49% delle operazioni necessarie a vestirsi.",
                    l1 = "Esegue meno del 25% delle operazioni; viene completamente vestito da un assistente; " +
                        "necessita di due persone; oppure l'attività non ha luogo (non si veste né viene vestito).",
                ),
                decisionTree = standardTree(
                    motorNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per vestire la parte inferiore del corpo?",
                    adaptations = "Il paziente ha bisogno di un ausilio per vestire la parte inferiore del corpo, come un " +
                        "infila-bottoni? Ci mette più di un tempo ragionevole? C'è preoccupazione per la sicurezza " +
                        "quando veste la parte inferiore del corpo?",
                    halfEffort = "Il paziente compie almeno metà dello sforzo necessario per vestire la parte inferiore " +
                        "del corpo?",
                    supervisionOnly = "Il paziente ha solo bisogno di supervisione o incoraggiamento, oppure di aiuto per " +
                        "preparare i vestiti e i relativi accessori, oppure di aiuto per applicare un'ortesi o una " +
                        "protesi?",
                    occasionalHelp = "Il paziente ha solo bisogno di aiuto occasionale, come per iniziare a vestirsi dalla " +
                        "cintola in giù, o solo di assistenza con bottoni, cerniere o ganci?",
                    totalAssistance = "Il paziente ha bisogno di assistenza totale per vestire la parte inferiore del " +
                        "corpo, ad esempio un assistente che gli procuri i vestiti ed esegua sostanzialmente tutte le " +
                        "attività?",
                ),
            ),
            ToolItem(
                id = "toileting",
                number = "F",
                title = "Igiene perineale",
                hint = "Igiene perineale e vestirsi/svestirsi prima e dopo l'uso dei servizi o della padella.",
                description = "Comprende il mantenimento dell'igiene perineale e l'attività di vestirsi e svestirsi dopo " +
                    "l'utilizzo dei servizi o di una padella. Non vi devono essere problemi di sicurezza.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Si pulisce da solo dopo minzione ed evacuazione, è in grado di applicarsi tamponi esterni o " +
                        "interni o pannoloni, e compie le necessarie operazioni di vestirsi/svestirsi prima e dopo l'uso " +
                        "dei servizi.",
                    l6 = "Richiede dispositivi specifici (comprese ortesi o protesi) o un tempo superiore al ragionevole, " +
                        "oppure si devono prendere precauzioni per evitare rischi.",
                    l5 = "Necessita di supervisione (una persona presente vicina, indicazioni, suggerimenti o esortazioni) " +
                        "oppure predisposizioni (applicazione di ausili, apertura di contenitori) o assistenza per " +
                        "l'applicazione di assorbenti igienici o tamponi (solitamente per 3–5 giorni al mese).",
                    l4 = "Esegue il 75% o più delle operazioni necessarie per l'igiene perineale.",
                    l3 = "Esegue dal 50 al 74% delle operazioni necessarie per l'igiene perineale.",
                    l2 = "Esegue dal 25 al 49% delle operazioni necessarie per l'igiene perineale.",
                    l1 = "Esegue meno del 25% delle operazioni per l'igiene perineale o richiede l'assistenza di due " +
                        "persone.",
                ),
                decisionTree = standardTree(
                    motorNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per aggiustarsi i vestiti prima o dopo l'utilizzo dei " +
                        "servizi o per eseguire l'igiene perineale?",
                    adaptations = "Il paziente ha bisogno di un dispositivo specifico per l'igiene perineale? Impiega più " +
                        "del tempo ragionevole per eseguire l'attività? C'è preoccupazione per la sicurezza durante le " +
                        "attività di igiene perineale?",
                    halfEffort = "Il paziente fa almeno metà dello sforzo necessario per eseguire le attività di igiene " +
                        "perineale?",
                    supervisionOnly = "Il paziente ha solo bisogno di supervisione, suggerimenti o incoraggiamento, oppure " +
                        "di aiuto per preparare l'occorrente per l'igiene perineale?",
                    occasionalHelp = "Il paziente ha solo bisogno di aiuto occasionale, come un aiuto per reggersi e " +
                        "mantenere l'equilibrio mentre si aggiusta i vestiti o effettua l'igiene perineale?",
                    totalAssistance = "Il paziente necessita di assistenza totale per le attività di igiene perineale, " +
                        "come un assistente che gli sistemi i vestiti prima e dopo l'uso della toilette ed esegua " +
                        "l'igiene dell'area perineale?",
                ),
            ),
            // ------------------------------------------------------------------
            // Controllo sfinterico
            // ------------------------------------------------------------------
            ToolItem(
                id = "bladder",
                number = "G",
                title = "Vescica",
                hint = "Controllo vescico-sfinterico e uso di dispositivi e farmaci; livello = il più basso tra assistenza richiesta e frequenza degli episodi.",
                description = "Include il completo controllo volontario vescico-sfinterico e l'utilizzo di dispositivi e " +
                    "sostanze necessarie per il controllo stesso. La voce rispecchia due fattori: il livello di efficacia " +
                    "del controllo (frequenza degli episodi di incontinenza: bagnare con urina lenzuola o vestiti, incluso " +
                    "rovesciare padella o pappagallo, negli ultimi tre giorni) e il livello di assistenza richiesto. Di " +
                    "solito variano di pari passo; se non sono alterati in egual misura, registrare il minore tra i due " +
                    "punteggi. L'albero decisionale percorre entrambi e sceglie il più basso.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Controllo completo della minzione senza episodi di incontinenza; nessun dispositivo " +
                        "particolare, farmaco, assistenza o tempo supplementare; oppure la persona non produce urina " +
                        "(es. insufficienza renale in dialisi).",
                    l6 = "Nessun episodio di incontinenza, ma richiede dispositivi particolari (pappagallo o padella, " +
                        "sedia con comoda, assorbenti, pannoloni, collettori urinari, collettori per ureterostomia o " +
                        "cistostomia) o farmaci specifici; gestisce da solo catetere, irrigazioni, pulizia e " +
                        "preparazione dei dispositivi, collettori e sacche raccogli-urina, senza fuoriuscite.",
                    l5 = "Uno o più punti: ha bisogno di supervisione o preparazione dei dispositivi per una minzione " +
                        "igienicamente soddisfacente o per gestire una sacca per urostomia; un assistente fornisce " +
                        "assistenza occasionale, suggerimenti o incoraggiamento con un dispositivo esterno (es. " +
                        "auto-cateterismo, cura dell'urostomia); un assistente procura padella, pappagallo o comoda, " +
                        "svuota il dispositivo o esegue entrambi i compiti; si pulisce da solo dopo un episodio di " +
                        "incontinenza. Oppure un (1) episodio di incontinenza negli ultimi 3 giorni.",
                    l4 = "Esegue il 75% o più delle operazioni necessarie al controllo vescico-sfinterico, oppure " +
                        "necessita di minima assistenza per mantenere una minzione soddisfacente con un dispositivo " +
                        "esterno. Oppure due (2) episodi di incontinenza negli ultimi 3 giorni.",
                    l3 = "Esegue dal 50 al 74% delle operazioni necessarie al controllo vescico-sfinterico, oppure " +
                        "necessita di assistenza moderata per mantenere una minzione soddisfacente con un dispositivo " +
                        "esterno. Oppure tre (3) episodi di incontinenza negli ultimi 3 giorni.",
                    l2 = "Esegue dal 25 al 49% delle operazioni necessarie al controllo vescico-sfinterico, oppure " +
                        "necessita di assistenza intensa per mantenere una minzione soddisfacente con un dispositivo " +
                        "esterno. Oppure quattro (4) episodi di incontinenza negli ultimi 3 giorni.",
                    l1 = "Esegue meno del 25% delle operazioni; un assistente cambia l'assorbente o il pannolone; ha " +
                        "avuto un episodio di incontinenza e un assistente ha cambiato biancheria o vestiti; necessita " +
                        "di due persone per la cura della vescica. Oppure cinque (5) o più episodi di incontinenza " +
                        "negli ultimi 3 giorni.",
                ),
                decisionTree = DecisionNode.LowerOf(
                    listOf(
                        DecisionPart(
                            "Livello di assistenza",
                            standardTree(
                                motorNames,
                                needsHelp = "Il paziente ha bisogno di aiuto per gestire la vescica?",
                                adaptations = "Il paziente ha bisogno di un dispositivo particolare per la gestione della " +
                                    "vescica, come un catetere, un pappagallo, una padella o un assorbente? Di solito usa " +
                                    "farmaci per il controllo della vescica?",
                                halfEffort = "Il paziente compie almeno metà delle operazioni necessarie per il controllo " +
                                    "vescico-sfinterico?",
                                supervisionOnly = "Il paziente ha solo bisogno di supervisione, suggerimenti o " +
                                    "incoraggiamento, oppure di aiuto per predisporre l'attrezzatura per la gestione " +
                                    "della vescica?",
                                occasionalHelp = "Il paziente ha solo bisogno di aiuto occasionale, come sistemare nelle " +
                                    "sue mani i dispositivi necessari, o aiuto per eseguire uno dei diversi compiti " +
                                    "inclusi nella gestione della vescica?",
                                totalAssistance = "Il paziente ha bisogno di assistenza totale per la gestione della " +
                                    "vescica, con un assistente che esegue sostanzialmente tutta la gestione delle " +
                                    "attrezzature?",
                            ),
                        ),
                        DecisionPart(
                            "Frequenza degli episodi di incontinenza",
                            frequencyTree(
                                hadEpisodes = "Il paziente ha avuto episodi di incontinenza urinaria negli ultimi 3 giorni " +
                                    "(biancheria o vestiti bagnati, incluso il rovesciare padella o pappagallo)?",
                                device = "Il paziente ha bisogno di un dispositivo di assistenza per la gestione della " +
                                    "vescica (come un catetere, un pappagallo, una padella o un pannolone)? Usa " +
                                    "solitamente farmaci specifici?",
                                howMany = "Quanti episodi di incontinenza ha avuto il paziente negli ultimi 3 giorni?",
                            ),
                        ),
                    ),
                ),
            ),
            ToolItem(
                id = "bowel",
                number = "H",
                title = "Alvo",
                hint = "Controllo sfinterico-intestinale e uso di dispositivi e farmaci; livello = il più basso tra assistenza richiesta e frequenza degli episodi.",
                description = "Include il completo controllo volontario dell'alvo e, se necessario, l'utilizzo di " +
                    "dispositivi o sostanze necessarie per il controllo dello stesso (attrezzatura e farmaci). La voce " +
                    "rispecchia due fattori: il successo nel controllo (frequenza degli episodi di incontinenza fecale: " +
                    "sporcare con feci biancheria o vestiti, incluso rovesciare la padella, negli ultimi tre giorni) e il " +
                    "livello di assistenza richiesto. Se i due non sono alterati in egual misura, registrare il minore " +
                    "tra i due punteggi. L'albero decisionale percorre entrambi e sceglie il più basso.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Completo controllo volontario sfinterico-intestinale senza assistenza, senza dispositivi " +
                        "particolari o farmaci e in un tempo ragionevole; nessun episodio di incontinenza.",
                    l6 = "Controlla completamente e volontariamente l'intestino (nessun episodio) ma utilizza dispositivi " +
                        "particolari (colostomia, padella o comoda, assorbente o pannolone che cambia da solo), oppure " +
                        "necessita di supposte, lassativi, clisteri periodici o farmaci gestendo da solo tutti gli " +
                        "aspetti (preparazione e igiene), oppure ha bisogno di tempo supplementare o di precauzioni.",
                    l5 = "Uno o più punti: richiede supervisione o predisposizione dei dispositivi per un'evacuazione " +
                        "soddisfacente o per gestire una stomia; un assistente fornisce indicazioni, suggerimenti o " +
                        "esortazioni nell'uso di un dispositivo (inserimento di supposta, cura della stomia); un " +
                        "assistente procura padella o comoda, la vuota o esegue entrambi i compiti; si pulisce da solo " +
                        "dopo una perdita fecale. Oppure un (1) episodio di incontinenza intestinale negli ultimi 3 giorni.",
                    l4 = "Esegue il 75% o più delle operazioni di gestione intestinale, oppure necessita di assistenza " +
                        "minima per un'evacuazione soddisfacente con supposte, clisteri o un dispositivo esterno. " +
                        "Oppure due (2) episodi negli ultimi 3 giorni.",
                    l3 = "Esegue dal 50 al 74% delle operazioni di gestione intestinale, oppure necessita di assistenza " +
                        "moderata per un'evacuazione soddisfacente con supposte, clisteri o un dispositivo esterno. " +
                        "Oppure tre (3) episodi negli ultimi 3 giorni.",
                    l2 = "Esegue dal 25 al 49% delle operazioni di gestione intestinale, oppure necessita di assistenza " +
                        "intensa per un'evacuazione soddisfacente con supposte, clisteri o un dispositivo esterno. " +
                        "Oppure quattro (4) episodi negli ultimi 3 giorni.",
                    l1 = "Esegue meno del 25% delle operazioni; un assistente cambia l'assorbente o il pannolone; ha " +
                        "avuto una perdita fecale e un assistente ha cambiato biancheria o vestiti; necessita di due " +
                        "persone. Oppure cinque (5) o più episodi negli ultimi 3 giorni.",
                ),
                decisionTree = DecisionNode.LowerOf(
                    listOf(
                        DecisionPart(
                            "Livello di assistenza",
                            standardTree(
                                motorNames,
                                needsHelp = "Il paziente ha bisogno di aiuto per la gestione dell'alvo?",
                                adaptations = "Il paziente ha bisogno di un dispositivo di assistenza per la gestione " +
                                    "dell'alvo, come una colostomia o una padella? Utilizza solitamente farmaci per il " +
                                    "controllo sfinterico-intestinale?",
                                halfEffort = "Il paziente fa almeno metà dello sforzo per la gestione dell'alvo?",
                                supervisionOnly = "Il paziente ha solo bisogno di supervisione o suggerimenti, oppure " +
                                    "della predisposizione dei dispositivi necessari per la gestione dell'alvo?",
                                occasionalHelp = "Il paziente ha solo bisogno di aiuto occasionale, come posizionare il " +
                                    "dispositivo necessario (padella) oppure aiuto per eseguire solo uno dei tanti " +
                                    "compiti inclusi nella gestione dell'alvo?",
                                totalAssistance = "Il paziente ha bisogno di assistenza totale per la gestione dell'alvo, " +
                                    "con un assistente che esegue in linea di massima tutte le operazioni necessarie al " +
                                    "controllo sfinterico-intestinale?",
                            ),
                        ),
                        DecisionPart(
                            "Frequenza degli episodi di incontinenza",
                            frequencyTree(
                                hadEpisodes = "Il paziente ha avuto episodi di incontinenza intestinale negli ultimi 3 " +
                                    "giorni (biancheria o vestiti imbrattati, incluso il rovesciare la padella)?",
                                device = "Il paziente ha bisogno di un dispositivo particolare per la gestione " +
                                    "dell'intestino, come una colostomia o una padella? Usa di solito farmaci per il " +
                                    "controllo?",
                                howMany = "Quanti episodi di incontinenza intestinale ha avuto il paziente negli ultimi " +
                                    "3 giorni?",
                            ),
                        ),
                    ),
                ),
            ),
            // ------------------------------------------------------------------
            // Trasferimenti
            // ------------------------------------------------------------------
            ToolItem(
                id = "transfer_bed",
                number = "I",
                title = "Trasferimenti: letto, sedia, carrozzina",
                hint = "Trasferimenti da e verso letto, sedia e carrozzina, e portarsi in posizione eretta se cammina.",
                description = "Comprende ogni aspetto dei trasferimenti da e verso il letto, la sedia e la carrozzina, " +
                    "nonché il portarsi in posizione eretta se il modo normale di locomozione è il cammino. Si assume " +
                    "che la persona possa iniziare e terminare la manovra in posizione supina.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Se cammina: si dispone davanti a una sedia normale, si siede e poi si alza; si trasferisce dal " +
                        "letto alla sedia. Se si muove solo in carrozzina: si dispone vicino a letto o sedia, blocca i " +
                        "freni, sblocca le pedane, rimuove i braccioli se necessario e si trasferisce alzandosi o " +
                        "facendo perno sugli arti inferiori, oppure con manovre di scivolamento. Tutto senza rischi.",
                    l6 = "Sono richiesti dispositivi adattativi o ausili (incluse protesi od ortesi) come uno scivolo, un " +
                        "elevatore, sedie modificate o speciali, tutori o bastoni; oppure un tempo superiore al " +
                        "ragionevole, oppure le manovre comportano rischi che impongono precauzioni.",
                    l5 = "È richiesta supervisione (una persona presente vicina, indicazioni, suggerimenti, esortazioni) " +
                        "o predisposizioni (posizionamento di uno scivolo, sbloccare le pedane della carrozzina ecc.).",
                    l4 = "Esegue il 75% o più delle manovre; necessita solo di minimo contatto e guida per completare il " +
                        "trasferimento; oppure ha bisogno di assistenza per sollevare un arto fuori e dentro il letto.",
                    l3 = "Esegue dal 50 al 74% delle operazioni; ha bisogno di minimo aiuto con contatto fisico per " +
                        "avvicinarsi alla sedia o al letto ma deve essere parzialmente sorretto per sedersi o alzarsi; " +
                        "oppure ha bisogno di assistenza per sollevare i due arti inferiori fuori e dentro il letto.",
                    l2 = "Esegue dal 25 al 49% delle manovre; oppure ha bisogno di minima assistenza per avvicinarsi " +
                        "alla sedia o al letto ma deve essere sostenuto pesantemente per sedersi e alzarsi.",
                    l1 = "Esegue meno del 25% delle manovre; necessita di due persone; richiede più dell'assistenza con " +
                        "contatto fisico per avvicinarsi, o dev'essere sollevato di peso per sedersi e/o alzarsi; " +
                        "oppure l'attività non ha luogo.",
                ),
                decisionTree = standardTree(
                    motorNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per salire o scendere dal letto, dalla sedia o dalla " +
                        "carrozzina?",
                    adaptations = "Il paziente ha bisogno di un ausilio per i trasferimenti da e verso il letto, la sedia o " +
                        "la carrozzina, come una tavola di trasferimento, una maniglia di sicurezza o un tutore? Ci " +
                        "mette più di un tempo ragionevole? C'è preoccupazione per la sua sicurezza quando effettua i " +
                        "trasferimenti?",
                    halfEffort = "Il paziente fa almeno metà dello sforzo necessario per i trasferimenti da e verso il " +
                        "letto, la sedia o la carrozzina?",
                    supervisionOnly = "Il paziente ha solo bisogno di supervisione o incoraggiamento per approntare i " +
                        "dispositivi necessari al trasferimento, oppure di aiuto per bloccare i freni o sollevare le " +
                        "pedane (o ambedue)?",
                    occasionalHelp = "Il paziente ha solo bisogno di aiuto come un minimo contatto per mantenere " +
                        "l'equilibrio durante i trasferimenti, oppure di aiuto per sollevare un solo arto inferiore?",
                    totalAssistance = "Il paziente necessita di assistenza totale per il trasferimento, come un " +
                        "assistente che effettui in linea di massima tutti gli sforzi? Usa un sollevatore meccanico?",
                ),
            ),
            ToolItem(
                id = "transfer_toilet",
                number = "J",
                title = "Trasferimenti: WC",
                hint = "Avvicinamento in sicurezza, sedersi e alzarsi da un WC standard.",
                description = "Include tutti gli aspetti relativi al trasferimento sul e dal WC: l'avvicinamento in " +
                    "sicurezza, il sedersi e l'alzarsi dal WC. Analizzare i trasferimenti su un WC standard. Protesi e " +
                    "ortesi sono da considerare dispositivi di supporto eventualmente necessari per il trasferimento.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Se cammina: si dispone vicino a un WC standard, si siede e si alza senza rischi. Se si muove " +
                        "solo in carrozzina: si avvicina a un WC standard, blocca i freni, rimuove pedane e braccioli se " +
                        "necessario e si trasferisce alzandosi in piedi e facendo perno sugli arti inferiori o con " +
                        "manovre di scivolamento, senza rischi.",
                    l6 = "Si trasferisce senza assistenza di una persona e uno o più punti: necessita di un dispositivo " +
                        "di supporto/adattamento (tavola di trasferimento, maniglie di sicurezza, sedile speciale); ha " +
                        "bisogno di un tempo superiore al ragionevole; c'è preoccupazione per la sicurezza.",
                    l5 = "Si trasferisce sul e dal WC ma necessita di supervisione (stare vicino o incoraggiamento) o di " +
                        "preparazione (posizionamento della tavola di trasferimento, spostamento di un bracciolo).",
                    l4 = "Esegue il 75% o più delle manovre, oppure necessita solo di contatto, guida o assistenza con " +
                        "minimo contatto fisico per completare il trasferimento.",
                    l3 = "Esegue dal 50 al 74% delle manovre, oppure ha solo bisogno di assistenza con contatto per " +
                        "avvicinarsi al WC o alla comoda ma deve essere sorretto parzialmente per sedersi o alzarsi.",
                    l2 = "Esegue dal 25 al 49% delle manovre, oppure ha bisogno di assistenza con contatto per " +
                        "avvicinarsi al WC o alla comoda ma deve essere sostenuto pesantemente per sedersi e alzarsi.",
                    l1 = "Esegue meno del 25% delle manovre; necessita di due persone; non basta l'assistenza con " +
                        "contatto per avvicinarsi e deve essere sollevato di peso per sedersi e alzarsi; oppure " +
                        "l'attività non ha luogo.",
                ),
                decisionTree = standardTree(
                    motorNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per sedersi e alzarsi dal WC?",
                    adaptations = "Il paziente ha bisogno di un ausilio per il trasferimento al/dal WC, come una maniglia " +
                        "di sicurezza o un sedile speciale? Ci mette più di un tempo ragionevole per trasferirsi? C'è " +
                        "preoccupazione per la sua sicurezza quando si trasferisce?",
                    halfEffort = "Il paziente compie almeno metà dello sforzo per il trasferimento sul WC?",
                    supervisionOnly = "Il paziente ha solo bisogno di supervisione, suggerimenti o incoraggiamento, oppure " +
                        "di aiuto per predisporre l'attrezzatura per il trasferimento?",
                    occasionalHelp = "Il paziente ha solo bisogno di aiuto occasionale, come la sorveglianza con contatto " +
                        "o un minimo aiuto durante i trasferimenti sul WC?",
                    totalAssistance = "Il paziente ha bisogno di assistenza totale per il trasferimento, come un " +
                        "assistente che in linea di massima compie tutto lo sforzo per avvicinarsi, sedersi e alzarsi?",
                ),
            ),
            ToolItem(
                id = "transfer_tub",
                number = "K",
                title = "Trasferimenti: vasca o doccia",
                hint = "Entrare e uscire da una vasca o da una cabina-doccia, in sicurezza.",
                description = "Comprende l'entrare e uscire da una vasca o da una cabina-doccia; l'attività è eseguita in " +
                    "sicurezza. Valutare durante l'effettiva esecuzione, non in una simulazione. Se la persona si " +
                    "trasferisce sia in vasca che in doccia, basare la valutazione sulla modalità più frequente.",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Se cammina: si accosta a una vasca o a una cabina-doccia, vi entra ed esce senza rischi. Se si " +
                        "muove solo in carrozzina: si accosta, blocca i freni, sblocca e rimuove pedane e braccioli se " +
                        "necessario e compie le manovre di trasferimento alzandosi in piedi e facendo perno sugli arti " +
                        "inferiori oppure con manovre di scivolamento, senza rischi.",
                    l6 = "Sono richiesti adattamenti e ausili (comprese protesi od ortesi) come uno scivolo, un " +
                        "sollevatore, appigli speciali o sedili particolari; oppure un tempo superiore al ragionevole, " +
                        "oppure si devono prendere precauzioni.",
                    l5 = "È richiesta supervisione (una persona presente vicina, indicazioni, suggerimenti, esortazioni) " +
                        "oppure predisposizioni (posizionamento di uno scivolo, sblocco e rimozione delle pedane ecc.).",
                    l4 = "Esegue il 75% o più delle manovre; necessita di minimo aiuto con contatto fisico per " +
                        "completare il trasferimento; oppure ha bisogno di assistenza per sollevare una gamba fuori e " +
                        "dentro la vasca o la doccia.",
                    l3 = "Esegue dal 50 al 74% delle operazioni; ha solo bisogno di una minima assistenza per avvicinarsi " +
                        "ma necessita di essere sorretto parzialmente per entrare e uscire; oppure ha bisogno di " +
                        "assistenza per sollevare due estremità fuori e dentro la vasca o la doccia.",
                    l2 = "Esegue dal 25 al 49% delle operazioni, oppure ha solo bisogno di essere accompagnato per " +
                        "avvicinarsi ma necessita di essere sostenuto per entrare e uscire.",
                    l1 = "Esegue meno del 25% delle operazioni; necessita di due persone; dev'essere sollevato di peso " +
                        "anche se accenna a collaborare; oppure l'attività non ha luogo.",
                ),
                decisionTree = standardTree(
                    motorNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per entrare e uscire dalla vasca o dalla doccia?",
                    adaptations = "Il paziente ha bisogno di adattamenti per i trasferimenti nella doccia, come una " +
                        "maniglia di sicurezza o un sedile speciale? Ci mette più di un tempo ragionevole per il " +
                        "trasferimento? C'è preoccupazione per la sua sicurezza quando si trasferisce?",
                    halfEffort = "Il paziente fa almeno metà dello sforzo per il trasferimento?",
                    supervisionOnly = "Il paziente ha solo bisogno di supervisione, suggerimenti o incoraggiamento, oppure " +
                        "di aiuto per approntare l'apparecchiatura di trasferimento, oppure di aiuto per bloccare i " +
                        "freni o sollevare le pedane (o ambedue)?",
                    occasionalHelp = "Il paziente ha solo bisogno di aiuto occasionale, come la sorveglianza con contatto " +
                        "o un minimo aiuto durante i trasferimenti in vasca o doccia, o di aiuto per sollevare una " +
                        "gamba per entrare e uscire dalla vasca?",
                    totalAssistance = "Il paziente ha bisogno di assistenza totale per entrare o uscire dalla vasca o " +
                        "dalla doccia, come un assistente che esegua sostanzialmente tutte le operazioni di " +
                        "sollevamento?",
                ),
            ),
            // ------------------------------------------------------------------
            // Locomozione
            // ------------------------------------------------------------------
            ToolItem(
                id = "locomotion",
                number = "L",
                title = "Locomozione: cammino o carrozzina",
                hint = "Muoversi su una superficie piana camminando o in carrozzina; indicare la modalità più frequente nel campo dedicato.",
                description = "Include il muoversi su una superficie piana, sia che la persona cammini in posizione eretta, " +
                    "sia che utilizzi una carrozzina e vi sia seduta sopra. Indicare la modalità di locomozione più " +
                    "frequentemente utilizzata (cammino o carrozzina), o entrambe se usate in egual misura. Il " +
                    "riferimento è la distanza di 50 metri; il livello 5 corrisponde anche alla locomozione occasionale " +
                    "domiciliare (almeno 15 metri senza assistenza).",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Cammina per almeno 50 metri senza alcun dispositivo ortopedico, non necessita di carrozzina e " +
                        "non vi sono rischi.",
                    l6 = "Cammina per almeno 50 metri ma usa tutori o protesi, calzature ortopediche, bastone o " +
                        "deambulatore, oppure impiega un tempo superiore al ragionevole, oppure la deambulazione " +
                        "comporta rischi. Se non cammina: manovra autonomamente una carrozzina autospinta o elettrica " +
                        "per almeno 50 metri, sterza, si accosta a tavolo, letto e WC, supera pendenze di almeno il 3%, " +
                        "terreni non lisci e soglie di accesso.",
                    l5 = "Locomozione occasionale domiciliare: cammina soltanto per brevi tratti (comunque almeno 15 " +
                        "metri) con o senza dispositivi ortopedici, con tempo superiore o precauzioni, oppure usa " +
                        "autonomamente una carrozzina solo per brevi distanze (comunque superiori a 15 metri). " +
                        "Oppure supervisione: è richiesta la presenza di una persona vicina, indicazioni, suggerimenti o " +
                        "esortazioni per percorrere almeno 50 metri a piedi o in carrozzina.",
                    l4 = "Compie almeno il 75% dello sforzo locomotorio per percorrere almeno 50 metri.",
                    l3 = "Compie dal 50 al 74% dello sforzo locomotorio per percorrere almeno 50 metri.",
                    l2 = "Compie dal 25 al 49% dello sforzo locomotorio per percorrere almeno 15 metri; è richiesta " +
                        "l'assistenza di una sola persona.",
                    l1 = "Compie meno del 25% dello sforzo, oppure richiede l'assistenza di due persone, oppure non " +
                        "riesce a coprire la distanza minima di 15 metri né camminando né in carrozzina.",
                ),
                decisionTree = locomotionTree(
                    needsPerson = "Il paziente ha bisogno dell'assistenza di una persona per percorrere qualsiasi distanza?",
                    fullDistance = "Il paziente percorre 50 metri o più, con o senza ausili?",
                    aidOrTime = "Il paziente ha bisogno di un ausilio (come un'ortesi, una protesi, stampelle, carrozzina, " +
                        "deambulatore) per percorrere almeno 50 metri? Ci mette più del tempo ragionevole? C'è " +
                        "preoccupazione per la sua sicurezza?",
                    shortDistance = "Il paziente percorre almeno 15 metri con o senza un ausilio?",
                    shortDistanceLabel = "Locomozione occasionale: domiciliare",
                    assistedFullDistance = "Il paziente percorre almeno 50 metri a piedi o in carrozzina?",
                    supervisionOnly = "Il paziente ha solo bisogno di supervisione, suggerimenti ed esortazioni per " +
                        "percorrere almeno 50 metri?",
                    occasionalHelp = "Il paziente ha bisogno di aiuto occasionale, come una sorveglianza con contatto o " +
                        "l'essere tenuto in equilibrio mentre percorre 50 metri, o di assistenza nel girare agli angoli " +
                        "o superare eventuali soglie di accesso se è in carrozzina?",
                    minimalDistance = "Il paziente percorre meno di 15 metri o ha bisogno dell'aiuto di uno o due " +
                        "assistenti?",
                ),
            ),
            ToolItem(
                id = "stairs",
                number = "M",
                title = "Locomozione: scale",
                hint = "Salire e scendere una rampa di 12–14 gradini, in ambiente chiuso, senza rischi.",
                description = "La persona riesce a salire e scendere per una rampa di 12–14 gradini, in ambiente chiuso, " +
                    "senza rischi. Il livello 5 corrisponde anche all'autonomia domiciliare (4–11 gradini senza " +
                    "assistenza).",
                noteLabel = NOTE,
                options = levels(
                    motorNames,
                    l7 = "Sale e scende per una rampa di scale senza corrimano né appoggi. Non vi sono rischi.",
                    l6 = "Sale e scende per almeno una rampa di scale con un appoggio (corrimano, bastone o qualsiasi " +
                        "appoggio mobile); è richiesto un tempo superiore al ragionevole oppure le manovre comportano " +
                        "rischi.",
                    l5 = "Autonomia domiciliare: è autosufficiente nel salire e scendere 4–11 gradini, con o senza " +
                        "dispositivi ortopedici, con tempo superiore o precauzioni. Oppure supervisione: è richiesta una " +
                        "persona presente vicina, indicazioni, suggerimenti o esortazioni per una rampa di 12–14 gradini.",
                    l4 = "Compie almeno il 75% dello sforzo richiesto per salire o scendere una rampa di scale.",
                    l3 = "Compie almeno il 50% dello sforzo richiesto per salire o scendere una rampa di scale.",
                    l2 = "Compie dal 25 al 49% dello sforzo richiesto per salire e scendere 4–11 gradini; è richiesta " +
                        "l'assistenza di una sola persona.",
                    l1 = "Compie meno del 25% dello sforzo, oppure richiede due persone, oppure sale e scende meno di 4 " +
                        "gradini, oppure deve essere trasportato di peso o usa un sollevatore meccanico azionato da un " +
                        "assistente.",
                ),
                decisionTree = locomotionTree(
                    needsPerson = "Il paziente ha bisogno dell'assistenza di una persona per salire e scendere le scale?",
                    fullDistance = "Il paziente sale e scende 12–14 gradini (una rampa) con o senza un ausilio?",
                    aidOrTime = "Il paziente ha bisogno di un ausilio (come un corrimano o un bastone) per salire e " +
                        "scendere le scale? Ci mette più di un tempo ragionevole? C'è preoccupazione per la sua " +
                        "sicurezza?",
                    shortDistance = "Il paziente sale e scende 4–11 gradini con o senza un ausilio?",
                    shortDistanceLabel = "Salita/discesa delle scale (autonomia domiciliare)",
                    assistedFullDistance = "Il paziente sale e scende almeno 12–14 gradini solo con l'assistenza di una " +
                        "persona?",
                    supervisionOnly = "Il paziente ha solo bisogno di supervisione o incoraggiamento per salire e " +
                        "scendere almeno 12–14 gradini?",
                    occasionalHelp = "Il paziente necessita di aiuto occasionale, con minimo contatto fisico per ragioni " +
                        "di sicurezza o per mantenere l'equilibrio, per salire e scendere almeno 12–14 gradini?",
                    minimalDistance = "Il paziente sale e scende meno di 4 gradini? Necessita dell'assistenza di due o " +
                        "più persone?",
                ),
            ),
            // ------------------------------------------------------------------
            // Comunicazione
            // ------------------------------------------------------------------
            ToolItem(
                id = "comprehension",
                number = "N",
                title = "Comprensione",
                hint = "Comprensione della comunicazione uditiva o visiva; indicare la modalità più frequente nel campo dedicato.",
                description = "Include la comprensione della comunicazione sia uditiva sia visiva (scrittura, linguaggio dei " +
                    "segni, linguaggio gestuale). Indicare la modalità di comprensione più frequentemente utilizzata " +
                    "(uditiva o visiva) o entrambe. Le informazioni complesse o astratte includono conversazioni di " +
                    "gruppo, notizie di attualità, argomenti religiosi, battute umoristiche, discorsi matematici e " +
                    "finanziari; le necessità elementari sono conversazioni, istruzioni, domande o affermazioni relative " +
                    "a nutrirsi, minzione e defecazione, igiene personale, sonno.",
                noteLabel = NOTE,
                options = levels(
                    facilitationNames,
                    l7 = "Comprende istruzioni e colloqui di tipo complesso o astratto; comprende la propria lingua madre " +
                        "sia parlata sia scritta.",
                    l6 = "Comprende istruzioni e colloqui di tipo complesso e astratto nella maggior parte (ma non nella " +
                        "totalità) delle circostanze, oppure con modesta difficoltà; non è necessario facilitarlo. Può " +
                        "richiedere un ausilio per l'udito o per la vista o un tempo eccessivo.",
                    l5 = "Comprende istruzioni e colloqui relativi alle necessità elementari quotidiane per almeno il 90% " +
                        "del tempo; è richiesta facilitazione (parlare lentamente, ripetere, enfatizzare parole, lunghe " +
                        "pause, indicazioni visive o gestualità) per meno del 10% del tempo.",
                    l4 = "Comprende istruzioni e colloqui relativi alle necessità elementari quotidiane per il 75–90% del " +
                        "tempo.",
                    l3 = "Comprende istruzioni e colloqui relativi alle necessità elementari quotidiane per il 50–74% del " +
                        "tempo.",
                    l2 = "Comprende istruzioni e colloqui relativi alle necessità elementari quotidiane per il 25–49% del " +
                        "tempo; riesce a comprendere solo domande o espressioni semplici e comuni («ciao», «come stai?») " +
                        "o gesti; sono richieste facilitazioni per più della metà del tempo.",
                    l1 = "Capisce istruzioni e conversazioni relative alle necessità elementari per meno del 25% delle " +
                        "volte; non comprende domande e affermazioni semplici e comuni o gesti; non risponde in modo " +
                        "appropriato o coerente nonostante i tentativi di facilitazione.",
                ),
                decisionTree = standardTree(
                    facilitationNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per capire concetti complessi e astratti, come affari di " +
                        "famiglia, avvenimenti di attualità e situazione finanziaria della famiglia?",
                    adaptations = "Il paziente ha bisogno di più tempo, necessita di un dispositivo di assistenza (come " +
                        "occhiali per la comprensione visiva o apparecchi acustici per la comprensione uditiva), o ha una " +
                        "leggera difficoltà nel capire informazioni complesse e astratte?",
                    halfEffort = "Il paziente comprende istruzioni e conversazioni su necessità e concetti di base (come " +
                        "fame, sete o disagio) per almeno metà delle volte?",
                    supervisionOnly = "Il paziente ha bisogno di aiuto per capire le istruzioni e le conversazioni su " +
                        "normali necessità quotidiane, ma solo raramente (meno del 10% delle volte)?",
                    occasionalHelp = "Il paziente ha bisogno di aiuto per capire istruzioni e conversazioni relative a " +
                        "normali necessità quotidiane, ma solo occasionalmente (meno del 25% delle volte)?",
                    totalAssistance = "Il paziente in linea di massima è incapace di capire? Reagisce in modo inadeguato " +
                        "o incoerente nonostante i suggerimenti?",
                ),
            ),
            ToolItem(
                id = "expression",
                number = "O",
                title = "Espressione",
                hint = "Esprimersi chiaramente con linguaggio verbale o non verbale; indicare la modalità più frequente nel campo dedicato.",
                description = "Comprende la capacità di esprimersi chiaramente con linguaggio verbale o non verbale: " +
                    "un'espressione verbale intelligibile o una chiara espressione linguistica mediante scrittura o altri " +
                    "strumenti comunicativi. Indicare la modalità più frequente (verbale o non verbale) o entrambe. " +
                    "Le idee complesse includono la discussione di notizie di attualità, temi religiosi o vicende " +
                    "interpersonali; le esigenze elementari sono le necessità alimentari, il bisogno di urinare o " +
                    "defecare, di lavarsi e di dormire.",
                noteLabel = NOTE,
                options = levels(
                    facilitationNames,
                    l7 = "Esprime idee complesse o astratte chiaramente e fluentemente.",
                    l6 = "Esprime idee complesse o concetti astratti nella maggior parte (ma non nella totalità) delle " +
                        "circostanze, oppure con modesta difficoltà; non sono richieste facilitazioni. Può richiedere " +
                        "supporti tecnico-strumentali per la comunicazione.",
                    l5 = "Esprime le esigenze elementari quotidiane e idee semplici per più del 90% del tempo; deve " +
                        "facilitare il destinatario (es. con frequenti ripetizioni) per meno del 10% del tempo.",
                    l4 = "Esprime le esigenze elementari e idee semplici per il 75–90% del tempo.",
                    l3 = "Esprime le esigenze elementari e idee semplici per il 50–74% del tempo.",
                    l2 = "Esprime le esigenze elementari e idee semplici per il 25–49% del tempo; può produrre soltanto " +
                        "parole singole o gesti; gli viene richiesto di facilitare il destinatario per più della metà " +
                        "del tempo.",
                    l1 = "Esprime le esigenze elementari e idee semplici per meno del 25% del tempo, oppure non riesce a " +
                        "esprimersi in modo appropriato o coerente nonostante si cerchi di facilitarlo.",
                ),
                decisionTree = standardTree(
                    facilitationNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per esprimere concetti complessi e astratti, come " +
                        "problemi famigliari, avvenimenti di attualità e questioni finanziarie di famiglia?",
                    adaptations = "Il paziente ha bisogno di più tempo, di un dispositivo di assistenza (come un supporto " +
                        "tecnico-strumentale per la comunicazione), o ha una lieve difficoltà di espressione (inclusa " +
                        "una lieve disartria o lievi problemi per trovare le parole) ma esprime correttamente concetti " +
                        "complessi e astratti?",
                    halfEffort = "Il paziente esprime necessità e concetti di base (come fame, sete o disagio) per almeno " +
                        "metà delle volte?",
                    supervisionOnly = "Il paziente ha bisogno di aiuto, ma solo raramente (meno del 10% delle volte), per " +
                        "esprimere necessità e concetti di base?",
                    occasionalHelp = "Il paziente ha bisogno di aiuto, ma solo occasionalmente (meno del 25% delle volte), " +
                        "per esprimere necessità e concetti di base?",
                    totalAssistance = "Il paziente è sostanzialmente incapace di esprimersi? Si esprime in modo " +
                        "inappropriato o incoerente nonostante i suggerimenti?",
                ),
            ),
            // ------------------------------------------------------------------
            // Capacità relazionali e cognitive
            // ------------------------------------------------------------------
            ToolItem(
                id = "social",
                number = "P",
                title = "Rapporto con gli altri",
                hint = "Interagire con gli altri in situazioni terapeutiche e sociali, integrando le proprie necessità con quelle altrui.",
                description = "Valuta la capacità di interagire con gli altri in situazioni terapeutiche o in relazioni " +
                    "sociali: quanto la persona riesce a integrare le sue necessità con quelle del prossimo. Esempi di " +
                    "reazioni inappropriate: dare in escandescenze, alzare la voce, linguaggio scurrile od offensivo, riso " +
                    "o pianto eccessivi, aggressività fisica, atteggiamento scostante, totale incapacità di contatto.",
                noteLabel = NOTE,
                options = levels(
                    socialNames,
                    l7 = "Interagisce appropriatamente con il personale di cura, con altri pazienti e con i familiari " +
                        "(controlla le reazioni emotive, accetta le critiche, è consapevole dell'impatto di parole e " +
                        "azioni sugli altri).",
                    l6 = "Interagisce appropriatamente in quasi tutte le situazioni, oppure incontra modeste difficoltà; " +
                        "non è richiesta supervisione. Può richiedere un tempo superiore per adattarsi alla compagnia " +
                        "oppure farmaci per controllare le proprie reazioni.",
                    l5 = "Viene richiesta supervisione (monitoraggio clinico, disposizioni verbali, indicazioni o " +
                        "suggerimenti, esortazioni) soltanto in situazioni stressanti o poco familiari, per non più del " +
                        "10% del tempo; può avere bisogno di essere incoraggiato a instaurare un rapporto.",
                    l4 = "Interagisce appropriatamente per il 75–90% del tempo.",
                    l3 = "Interagisce appropriatamente per il 50–74% del tempo.",
                    l2 = "Interagisce appropriatamente per il 25–49% del tempo; si possono imporre costrizioni o " +
                        "provvedimenti restrittivi.",
                    l1 = "Interagisce appropriatamente per meno del 25% del tempo; si possono imporre costrizioni o " +
                        "provvedimenti restrittivi.",
                ),
                decisionTree = standardTree(
                    socialNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per interagire con gli altri in situazioni sociali e " +
                        "terapeutiche?",
                    adaptations = "Il paziente necessita di tempo addizionale in situazioni sociali ordinarie? Interagisce " +
                        "in modo adeguato con il personale, gli altri pazienti e i membri della famiglia in quasi tutte " +
                        "le situazioni? Ha bisogno di farmaci per interagire appropriatamente?",
                    halfEffort = "Il paziente interagisce in modo adeguato per almeno la metà del tempo?",
                    supervisionOnly = "Il paziente ha bisogno di aiuto per interagire in modo adeguato, ma solo raramente " +
                        "(meno del 10% delle volte) o in condizioni stressanti o poco familiari?",
                    occasionalHelp = "Il paziente necessita di aiuto per interagire in modo adeguato con gli altri, ma " +
                        "solo occasionalmente (meno del 25% delle volte)?",
                    totalAssistance = "Il paziente interagisce in modo adeguato per meno del 25% delle volte nonostante " +
                        "l'assistenza?",
                ),
            ),
            ToolItem(
                id = "problem_solving",
                number = "Q",
                title = "Soluzione di problemi",
                hint = "Prendere decisioni ragionevoli, prudenti e tempestive; intraprendere, pianificare e autocorreggere le attività.",
                description = "Include le capacità che consentono la soluzione di problemi relativi alla vita quotidiana: " +
                    "prendere decisioni ragionevoli, prudenti e tempestive su problemi finanziari, relazioni " +
                    "interpersonali e scelte individuali, e intraprendere, pianificare e autocorreggere iniziative e " +
                    "attività. I problemi complessi comprendono gestire un conto corrente, collaborare alla " +
                    "programmazione della dimissione, assumere autonomamente farmaci, prendere decisioni sul lavoro; i " +
                    "problemi ordinari sono quelli della vita domestica e le situazioni impreviste o pericolose.",
                noteLabel = NOTE,
                options = levels(
                    problemNames,
                    l7 = "Capisce adeguatamente un problema, prende le decisioni appropriate, intraprende e porta avanti " +
                        "una sequenza di operazioni che risolve problemi complessi, ed è in grado di autocorreggersi.",
                    l6 = "Capisce un problema, prende le decisioni appropriate e porta avanti una sequenza di operazioni " +
                        "idonee a risolvere problemi complessi in quasi tutte le situazioni, oppure incontra modeste " +
                        "difficoltà, oppure richiede un tempo superiore al ragionevole.",
                    l5 = "È richiesta supervisione (indicazioni, suggerimenti o esortazioni) per risolvere problemi " +
                        "ordinari, ma solo in condizioni stressanti o poco familiari e per non più del 10% del tempo.",
                    l4 = "Risolve problemi ordinari per il 75–90% del tempo.",
                    l3 = "Risolve problemi ordinari per il 50–74% del tempo.",
                    l2 = "Risolve problemi ordinari per il 25–49% del tempo necessario per intraprendere, pianificare o " +
                        "portare a termine semplici attività della vita quotidiana; si possono imporre costrizioni o " +
                        "provvedimenti restrittivi per evitare rischi.",
                    l1 = "Risolve problemi ordinari per meno del 25% del tempo; è necessario decidere per lui in " +
                        "continuazione; può essere necessaria l'assistenza costante di una persona per risolvere " +
                        "semplici problemi quotidiani; si possono imporre costrizioni o provvedimenti restrittivi.",
                ),
                decisionTree = standardTree(
                    problemNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per risolvere problemi complessi, come gestire un conto " +
                        "corrente bancario o affrontare problemi interpersonali?",
                    adaptations = "Il paziente ha bisogno di un tempo superiore a quanto sia ragionevole per prendere " +
                        "decisioni e risolvere problemi? Ha leggere difficoltà nel decidere di iniziare o si " +
                        "autocorregge in situazioni poco familiari?",
                    halfEffort = "Il paziente risolve problemi di routine in modo adeguato almeno metà delle volte?",
                    supervisionOnly = "Il paziente ha bisogno di aiuto per risolvere problemi di routine, ma solo " +
                        "raramente (meno del 10% delle volte) o in condizioni stressanti?",
                    occasionalHelp = "Il paziente ha bisogno di aiuto per risolvere problemi di routine in modo efficace, " +
                        "ma solo occasionalmente (meno del 25% delle volte)?",
                    totalAssistance = "Il paziente ha sempre bisogno di aiuto per risolvere problemi? Non è in grado di " +
                        "risolvere problemi?",
                ),
            ),
            ToolItem(
                id = "memory",
                number = "R",
                title = "Memoria",
                hint = "Riconoscere le persone, ricordare le situazioni quotidiane ed eseguire le richieste senza ripetizioni.",
                description = "Comprende la capacità di identificare e ricordare quanto avviene nel contesto di attività " +
                    "della vita quotidiana, sia in situazioni di ricovero sia nella normale vita sociale: immagazzinare " +
                    "e «ripescare» informazioni, in particolare verbali e visive. I deficit di memoria comportano " +
                    "deficit nell'apprendimento e nell'esecuzione di varie attività.",
                noteLabel = NOTE,
                options = levels(
                    memoryNames,
                    l7 = "Riconosce le persone già incontrate più volte e ricorda le situazioni quotidiane; esegue le " +
                        "richieste degli altri senza bisogno che gli siano ripetute.",
                    l6 = "Riconosce le persone che incontra di frequente, ricorda situazioni quotidiane e richieste ma con " +
                        "modesta difficoltà; può avere bisogno di ricorrere a indicazioni, stimoli o dispositivi da lui " +
                        "stesso predisposti o presenti nell'ambiente che lo aiutino a ricordare.",
                    l5 = "Ha bisogno di facilitazioni da parte degli altri (suggerimenti e indicazioni, ripetizione di " +
                        "messaggi, stimoli od oggetti che gli ricordino che cosa fare) ma solo in condizioni stressanti o " +
                        "poco familiari e per non oltre il 10% del tempo.",
                    l4 = "«Riconosce» e «si ricorda» per il 75–90% del tempo.",
                    l3 = "«Riconosce» e «si ricorda» per il 50–74% del tempo.",
                    l2 = "«Riconosce» e «si ricorda» per il 25–49% del tempo; sono necessarie facilitazioni per più della " +
                        "metà del tempo.",
                    l1 = "«Riconosce» e «si ricorda» per meno del 25% del tempo, oppure non ne è proprio in grado.",
                ),
                decisionTree = standardTree(
                    memoryNames,
                    needsHelp = "Il paziente ha bisogno di aiuto per ricordare persone, situazioni quotidiane e richieste?",
                    adaptations = "Il paziente ha modeste difficoltà nel riconoscere le persone, ricordare situazioni " +
                        "quotidiane ed eseguire le richieste che gli vengono poste, ma senza bisogno di ripetizioni? Può " +
                        "avere bisogno di ricorrere a stimoli, indicazioni o dispositivi da lui stesso predisposti per " +
                        "ricordare?",
                    halfEffort = "Il paziente ricorda persone, abitudini e richieste almeno la metà delle volte?",
                    supervisionOnly = "Il paziente ha bisogno di aiuto per riconoscere e ricordare, ma solo raramente " +
                        "(meno del 10% delle volte) oppure in condizioni stressanti?",
                    occasionalHelp = "Il paziente ha bisogno di aiuto per riconoscere e ricordare, ma solo occasionalmente " +
                        "(meno del 25% delle volte)?",
                    totalAssistance = "Il paziente ha sempre bisogno di aiuto per ricordare? È incapace di riconoscere e " +
                        "ricordare completamente?",
                ),
            ),
        ),
        bands = emptyList(),
    )
}
