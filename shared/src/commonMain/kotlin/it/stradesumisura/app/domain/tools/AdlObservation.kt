package it.stradesumisura.app.domain.tools

import it.stradesumisura.app.domain.ComputedScore
import it.stradesumisura.app.domain.ScoreBand
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolField
import it.stradesumisura.app.domain.ToolItem
import it.stradesumisura.app.domain.ToolOption

/**
 * Osservazione ADL (ADL Observation).
 * Scheda di osservazione strutturata di un compito della vita quotidiana prevista dalle linee
 * guida olandesi di terapia occupazionale per la valutazione e il trattamento dell'aprassia
 * conseguente a ictus dell'emisfero sinistro (Hogeschool van Amsterdam / NVE, 2003); versione
 * italiana a cura del Centro di Ricerca in Riabilitazione Neuromotoria e Cognitiva
 * dell'Università degli Studi di Verona (2012).
 *
 * Il terapista valuta con una scala 0–3 le tre fasi dell'azione (orientamento, esecuzione,
 * controllo). Il punteggio di indipendenza del compito è calcolato automaticamente come il
 * livello più alto tra le tre fasi: il bisogno di supporto anche in una sola fase compromette
 * l'intero compito.
 */
object AdlObservation {

    const val ID = "adl_observation"

    private const val ORIENTATION = "orientation"
    private const val EXECUTION = "execution"
    private const val CONTROL = "control"
    private const val INDEPENDENCE = "independence"

    private val independenceOptions = listOf(
        ToolOption(
            0, "Autonomia completa",
            "Dopo le istruzioni iniziali, il cliente è in grado di eseguire il compito in piena autonomia e, " +
                "nel caso commetta o stia per commettere un errore, di correggere le proprie azioni senza " +
                "necessità di aiuto esterno. Il cliente sceglie e attua autonomamente proprie strategie di " +
                "controllo delle azioni e, se necessario, le adatta sulla base delle proprie percezioni.",
        ),
        ToolOption(
            1, "Intervento verbale",
            "Dopo le istruzioni iniziali, il cliente ha bisogno di un intervento verbale per svolgere una o " +
                "più fasi del compito.",
        ),
        ToolOption(
            2, "Intervento fisico",
            "Dopo le istruzioni iniziali, il cliente ha bisogno di un intervento fisico per svolgere una o " +
                "più fasi del compito.",
        ),
        ToolOption(
            3, "Incapace di completare il compito",
            "Il cliente è incapace di portare a termine il compito, nonostante gli interventi verbali e " +
                "fisici; dopo questi interventi, il cliente non riprende l'esecuzione del compito ed è il " +
                "terapista occupazionale a doverlo eseguire e concludere.",
        ),
    )

    private fun phaseOptions(phase: String) = listOf(
        ToolOption(
            0, "Autonomia completa",
            "La fase di $phase viene svolta in piena autonomia; eventuali errori, commessi o imminenti, " +
                "vengono corretti dal cliente senza aiuto esterno.",
        ),
        ToolOption(
            1, "Intervento verbale",
            "Dopo le istruzioni iniziali, il cliente ha bisogno di un intervento verbale per svolgere la " +
                "fase di $phase.",
        ),
        ToolOption(
            2, "Intervento fisico",
            "Dopo le istruzioni iniziali, il cliente ha bisogno di un intervento fisico per svolgere la " +
                "fase di $phase.",
        ),
        ToolOption(
            3, "Fase non completata",
            "Il cliente non porta a termine la fase di $phase nonostante gli interventi verbali e fisici " +
                "ed è il terapista occupazionale a doverla eseguire e concludere.",
        ),
    )

    val definition: ToolDefinition = ToolDefinition(
        id = ID,
        name = "Osservazione ADL",
        shortName = "ADL",
        citation = "Hogeschool van Amsterdam e NVE, 2003; Università di Verona, 2012",
        subtitle = "Osservazione strutturata dell'esecuzione di un compito della vita quotidiana",
        domain = "Aprassia · Attività della vita quotidiana",
        duration = "15–30 minuti per compito",
        description = "L'Osservazione ADL è la scheda di osservazione strutturata delle attività della vita " +
            "quotidiana prevista dalle linee guida olandesi di terapia occupazionale per la valutazione e il " +
            "trattamento dell'aprassia conseguente a ictus dell'emisfero sinistro (Hogeschool van Amsterdam e " +
            "Associazione Olandese di Ergoterapia, 2003), nella versione italiana del Centro di Ricerca in " +
            "Riabilitazione Neuromotoria e Cognitiva dell'Università degli Studi di Verona (2012). Il terapista " +
            "osserva l'esecuzione di un compito concreto e valuta, con una scala a quattro livelli " +
            "(0 = autonomia completa, 1 = intervento verbale, 2 = intervento fisico, 3 = incapacità di " +
            "completare), la qualità della performance nelle tre fasi dell'azione: orientamento, esecuzione e " +
            "controllo. Il punteggio di indipendenza del compito è calcolato automaticamente come il livello " +
            "più alto tra le tre fasi, perché il bisogno di supporto anche in una sola fase compromette " +
            "l'intero compito.",
        instructions = listOf(
            "Scegliere un compito significativo per la persona e osservarne l'esecuzione completa nel " +
                "contesto reale, dopo aver fornito le istruzioni iniziali. Ogni scheda si riferisce a un " +
                "singolo compito: per osservare più compiti compilare una scheda per ciascuno.",
            "Registrare nelle osservazioni ciò che si vede: errori, strategie, tipo di aiuto necessario, tempi.",
            "Valutare ciascuna fase (orientamento, esecuzione, controllo) con la scala: 0 = autonomia " +
                "completa; 1 = intervento verbale; 2 = intervento fisico; 3 = fase non completata.",
            "Il punteggio di indipendenza del compito si calcola da solo: è il livello più alto rilevato " +
                "nelle tre fasi.",
            "Un punteggio più basso indica maggiore autonomia: 0 è il risultato migliore, 3 il peggiore.",
        ),
        reference = "Istituto Superiore di Amsterdam (Hogeschool van Amsterdam) / Associazione Olandese di " +
            "Ergoterapia (NVE), 2003. Modulo di registrazione delle linee guida di terapia occupazionale per la " +
            "valutazione e il trattamento dell'aprassia conseguente ad ictus all'emisfero sinistro (ADL " +
            "Observation). I moduli possono essere fotocopiati per uso personale. Copyright 2012 Centro di " +
            "Ricerca in Riabilitazione Neuromotoria e Cognitiva, Università degli Studi di Verona.",
        fields = listOf(
            ToolField("task", "Compito osservato", "es. lavarsi il viso, preparare un caffè, indossare una camicia", shortLabel = "Compito"),
            ToolField("diagnosis", "Diagnosi", "es. ictus emisfero sinistro con aprassia"),
            ToolField("time", "Ora dell'osservazione", "es. 10:30", shortLabel = "Ora"),
        ),
        items = listOf(
            ToolItem(
                id = ORIENTATION,
                title = "Fase di orientamento",
                hint = "Preparazione all'esecuzione: orientamento rispetto all'ambiente, al compito e alle proprie abilità.",
                description = "La fase di orientamento è la prima fase e prevede la preparazione all'esecuzione " +
                    "del compito. Ciò significa che, in una determinata situazione, il cliente si orienta rispetto " +
                    "all'ambiente che lo circonda, al compito che deve svolgere e alle proprie abilità. Durante la " +
                    "fase di orientamento prende forma il concetto mentale, che comprende il piano d'azione e " +
                    "l'esito finale dell'esecuzione del compito.",
                options = phaseOptions("orientamento"),
            ),
            ToolItem(
                id = EXECUTION,
                title = "Fase di esecuzione",
                hint = "Realizzazione del piano d'azione: avvio, svolgimento e conclusione del compito al momento giusto.",
                description = "La seconda fase consiste nell'esecuzione del piano d'azione. Il compito viene " +
                    "iniziato, eseguito (correttamente) e concluso al momento giusto. La fase di esecuzione è " +
                    "incentrata sulla parte visibile dello svolgimento del compito. Vengono osservati aspetti come " +
                    "la fluidità dell'azione motoria, la correlazione temporale tra un movimento e quello " +
                    "successivo, l'interruzione dell'azione quando il compito è completato.",
                options = phaseOptions("esecuzione"),
            ),
            ToolItem(
                id = CONTROL,
                title = "Fase di controllo",
                hint = "Verifica continua della propria performance rispetto a oggetti, ambiente e risultato.",
                description = "La terza fase della performance occupazionale è quella di controllo. Durante lo " +
                    "svolgimento del compito, il cliente dovrà controllarsi in ogni momento; ciò significa che " +
                    "dovrà controllare e valutare la propria performance in riferimento agli oggetti, all'ambiente " +
                    "circostante e al risultato delle proprie azioni in relazione allo scopo del compito. Il " +
                    "controllo avviene sulla base della percezione della propria performance rispetto al concetto " +
                    "interiorizzato del compito, presente nella memoria di lavoro. In questo modo il cliente può " +
                    "controllare se il suo modo di agire (il processo) e il risultato (il «prodotto» " +
                    "dell'esecuzione del compito) sono in linea con il piano d'azione scelto e predisposto.",
                options = phaseOptions("controllo"),
            ),
            ToolItem(
                id = INDEPENDENCE,
                title = "Indipendenza",
                hint = "Calcolata automaticamente: è il livello più alto rilevato nelle tre fasi, perché il " +
                    "bisogno di supporto anche in una sola fase compromette l'intero compito.",
                options = independenceOptions,
                computed = ComputedScore.MaxOf(listOf(ORIENTATION, EXECUTION, CONTROL)),
            ),
        ),
        bands = listOf(
            ScoreBand(0, 0, "Autonomia completa",
                "Il compito viene eseguito in piena autonomia, con correzione spontanea degli eventuali errori.", 0xFF2F7F5E),
            ScoreBand(1, 1, "Intervento verbale",
                "Il compito viene completato con un intervento verbale in una o più fasi.", 0xFF9BB357),
            ScoreBand(2, 2, "Intervento fisico",
                "Il compito viene completato con un intervento fisico in una o più fasi.", 0xFFD3714F),
            ScoreBand(3, 3, "Dipendenza",
                "Il compito non viene portato a termine nonostante gli interventi verbali e fisici.", 0xFFB4455A),
        ),
        headlineItemId = INDEPENDENCE,
        totalLabel = "Indipendenza",
        itemNounSingular = "fase",
        itemNounPlural = "fasi",
        itemsSectionTitle = "Fasi del compito",
        notesLabel = "Osservazioni",
        notesHint = "Descrizione dell'esecuzione del compito: errori, strategie, tipo di aiuto, tempi.",
        summaryFieldId = "task",
    )
}
