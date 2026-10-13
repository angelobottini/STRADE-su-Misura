package it.stradesumisura.app.domain.tools

import it.stradesumisura.app.domain.ComputedScore
import it.stradesumisura.app.domain.ScoreBand
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolField
import it.stradesumisura.app.domain.ToolItem
import it.stradesumisura.app.domain.ToolOption

/**
 * PQRS – Performance Quality Rating Scale.
 * Scala osservativa a 10 punti della qualità della performance in un compito scelto dalla persona,
 * nata nell'ambito dell'approccio CO-OP (Miller, Polatajko e coll., 2001) e validata da Martini,
 * Rios, Polatajko, Wolf e McEwen (2015).
 *
 * Nella scheda adottata dall'app il terapista occupazionale attribuisce due valori da 1 a 10,
 * la completezza del compito e la qualità della performance o del prodotto, e il punteggio PQRS
 * è calcolato automaticamente come media dei due arrotondata per difetto.
 */
object PerformanceQualityRatingScale {

    const val ID = "pqrs"

    private const val COMPLETENESS = "completeness"
    private const val QUALITY = "quality"
    private const val PQRS = "pqrs_score"

    private fun intermediate(lower: Int, upper: Int) =
        "Livello intermedio tra $lower e $upper, quando la performance non corrisponde pienamente a nessuno dei due ancoraggi."

    private val completenessOptions = listOf(
        ToolOption(1, "0%", "Il compito non viene iniziato o non viene eseguita alcuna parte di esso."),
        ToolOption(2, "25%", "Viene portato a termine circa un quarto del compito."),
        ToolOption(3, "25–50%", intermediate(2, 4)),
        ToolOption(4, "50%", "Viene portata a termine circa la metà del compito."),
        ToolOption(5, "50–75%", intermediate(4, 6)),
        ToolOption(6, "75%", "Vengono portati a termine circa tre quarti del compito."),
        ToolOption(7, "75–100%", intermediate(6, 8)),
        ToolOption(8, "100%", "Il compito viene portato a termine per intero, con qualche passaggio ancora approssimativo."),
        ToolOption(9, "100%", intermediate(8, 10)),
        ToolOption(10, "100%", "Il compito viene portato a termine per intero, in tutti i suoi passaggi."),
    )

    private val qualityOptions = listOf(
        ToolOption(1, "Non valutabile (n/a)", "Il compito non viene eseguito: la qualità della performance non è valutabile."),
        ToolOption(2, "Proprio povero", "Qualità della performance o del prodotto molto scarsa."),
        ToolOption(3, "Tra proprio povero e povero", intermediate(2, 4)),
        ToolOption(4, "Povero", "Qualità della performance o del prodotto scarsa."),
        ToolOption(5, "Tra povero e moderato", intermediate(4, 6)),
        ToolOption(6, "Moderato", "Qualità della performance o del prodotto moderata."),
        ToolOption(7, "Tra moderato e buono", intermediate(6, 8)),
        ToolOption(8, "Buono", "Qualità della performance o del prodotto buona."),
        ToolOption(9, "Tra buono ed eccellente", intermediate(8, 10)),
        ToolOption(10, "Eccellente", "Qualità della performance o del prodotto eccellente."),
    )

    private val pqrsOptions = listOf(
        ToolOption(1, "Compito non eseguito", "La persona non è in grado di eseguire il compito (0%, qualità non valutabile)."),
        ToolOption(2, "25% · Proprio povero", "Esegue circa un quarto del compito con una qualità molto scarsa."),
        ToolOption(3, "Livello intermedio (2–4)", "Performance compresa tra «25% · proprio povero» e «50% · povero»."),
        ToolOption(4, "50% · Povero", "Esegue circa la metà del compito con una qualità scarsa."),
        ToolOption(5, "Livello intermedio (4–6)", "Performance compresa tra «50% · povero» e «75% · moderato»."),
        ToolOption(6, "75% · Moderato", "Esegue circa tre quarti del compito con una qualità moderata."),
        ToolOption(7, "Livello intermedio (6–8)", "Performance compresa tra «75% · moderato» e «100% · buono»."),
        ToolOption(8, "100% · Buono", "Esegue il compito per intero con una qualità buona."),
        ToolOption(9, "Livello intermedio (8–10)", "Performance compresa tra «100% · buono» e «100% · eccellente»."),
        ToolOption(10, "100% · Eccellente", "Esegue il compito per intero con una qualità eccellente."),
    )

    val definition: ToolDefinition = ToolDefinition(
        id = ID,
        name = "PQRS – Performance Quality Rating Scale",
        shortName = "PQRS",
        citation = "Martini e coll., 2015",
        subtitle = "Valutazione osservativa della qualità della performance in un compito",
        domain = "Performance occupazionale · Qualità dell'esecuzione del compito",
        duration = "5–10 minuti per compito, dopo l'osservazione",
        description = "La PQRS (Performance Quality Rating Scale) è una scala osservativa a 10 punti che misura la " +
            "qualità con cui la persona esegue un compito da lei scelto come significativo. È nata nell'ambito " +
            "dell'approccio CO-OP (Cognitive Orientation to daily Occupational Performance) ed è stata validata " +
            "da Martini e collaboratori (2015). Il terapista occupazionale osserva l'esecuzione del compito, dal " +
            "vivo o in registrazione video, e attribuisce due valori da 1 a 10: la completezza del compito (quanta " +
            "parte viene portata a termine: 0%, 25%, 50%, 75%, 100%) e la qualità della performance o del " +
            "prodotto (da «proprio povero» a «eccellente»). Il punteggio PQRS è calcolato automaticamente come " +
            "media dei due valori arrotondata per difetto: 1 indica che il compito non viene eseguito, 10 un " +
            "compito completo ed eseguito in modo eccellente. I livelli pari sono gli ancoraggi descritti dalla " +
            "scala; quelli dispari sono livelli intermedi.",
        instructions = listOf(
            "Scegliere con la persona un compito significativo e descriverlo nella scheda (cosa viene " +
                "osservato, in quale contesto, con quali materiali). Ogni scheda si riferisce a un singolo compito.",
            "Osservare l'intera esecuzione del compito, dal vivo o in video, senza intervenire se non per " +
                "ragioni di sicurezza.",
            "Attribuire il valore di completezza: quanta parte del compito è stata portata a termine " +
                "(1 = 0%, 2 = 25%, 4 = 50%, 6 = 75%, 8 e 10 = 100%; i valori dispari sono intermedi).",
            "Attribuire il valore di qualità della performance o del prodotto: 1 = non valutabile, 2 = proprio " +
                "povero, 4 = povero, 6 = moderato, 8 = buono, 10 = eccellente; i valori dispari sono intermedi.",
            "Il punteggio PQRS si calcola da solo: è la media dei due valori arrotondata per difetto " +
                "(es. completezza 6 e qualità 7 → 6,5 → PQRS 6).",
            "Annotare ciò che motiva i valori attribuiti: errori, strategie, tempi, aiuti ricevuti. Ripetendo " +
                "la valutazione sullo stesso compito si può seguire il cambiamento nel tempo.",
        ),
        reference = "Martini R., Rios J., Polatajko H., Wolf T., McEwen S. (2015). The Performance Quality Rating " +
            "Scale (PQRS): reliability, convergent validity, and internal responsiveness for two scoring systems. " +
            "Disability and Rehabilitation, 37(3), 231–238. Scala sviluppata nell'ambito dell'approccio CO-OP " +
            "(Miller, Polatajko, Missiuna, Mandich e Macnab, 2001).",
        fields = listOf(
            ToolField(
                "task", "Compito valutato",
                "es. preparare un caffè, allacciarsi le scarpe, scrivere un messaggio",
                shortLabel = "Compito",
            ),
            ToolField(
                "task_description", "Descrizione del compito",
                "Come si svolge il compito osservato: contesto, materiali, passaggi richiesti, eventuali adattamenti.",
                multiline = true,
                shortLabel = "Descrizione",
            ),
        ),
        items = listOf(
            ToolItem(
                id = COMPLETENESS,
                title = "Completezza",
                hint = "Quanta parte del compito viene portata a termine (da 1 = 0% a 10 = 100%).",
                description = "La completezza indica la quota del compito effettivamente portata a termine, " +
                    "indipendentemente da come viene eseguita: 1 corrisponde a un compito non iniziato (0%), 2 a " +
                    "un quarto (25%), 4 alla metà (50%), 6 a tre quarti (75%), 8 e 10 al compito completo (100%). " +
                    "I valori dispari sono livelli intermedi tra due ancoraggi.",
                options = completenessOptions,
            ),
            ToolItem(
                id = QUALITY,
                title = "Qualità di performance o del prodotto",
                shortTitle = "Qualità",
                hint = "Come viene eseguito il compito o com'è il risultato prodotto (da 1 = n/a a 10 = eccellente).",
                description = "La qualità riguarda il modo in cui il compito viene eseguito (fluidità, precisione, " +
                    "sicurezza, efficienza) oppure la qualità del prodotto finale, a seconda del compito: 1 = non " +
                    "valutabile perché il compito non viene eseguito, 2 = proprio povero, 4 = povero, 6 = moderato, " +
                    "8 = buono, 10 = eccellente. I valori dispari sono livelli intermedi tra due ancoraggi.",
                options = qualityOptions,
            ),
            ToolItem(
                id = PQRS,
                title = "Punteggio PQRS",
                hint = "Calcolato automaticamente: media di completezza e qualità, arrotondata per difetto.",
                options = pqrsOptions,
                computed = ComputedScore.FloorMeanOf(listOf(COMPLETENESS, QUALITY)),
            ),
        ),
        bands = listOf(
            ScoreBand(1, 1, "Compito non eseguito",
                "La persona non è in grado di eseguire il compito.", 0xFFB4455A),
            ScoreBand(2, 3, "Performance molto scarsa",
                "Esegue solo una piccola parte del compito, con una qualità molto scarsa.", 0xFFD3714F),
            ScoreBand(4, 5, "Performance scarsa",
                "Esegue circa la metà del compito, con una qualità scarsa.", 0xFFDDA04A),
            ScoreBand(6, 7, "Performance moderata",
                "Esegue la maggior parte del compito, con una qualità moderata.", 0xFF9BB357),
            ScoreBand(8, 9, "Performance buona",
                "Esegue il compito per intero, con una qualità buona.", 0xFF2F7F5E),
            ScoreBand(10, 10, "Performance eccellente",
                "Esegue il compito per intero, con una qualità eccellente.", 0xFF1F5E48),
        ),
        headlineItemId = PQRS,
        totalLabel = "Punteggio PQRS",
        itemNounSingular = "criterio",
        itemNounPlural = "criteri",
        itemsSectionTitle = "Criteri valutati",
        notesLabel = "Annotazioni del valutatore",
        notesHint = "Osservazioni che motivano i valori attribuiti: errori, strategie, tempi, aiuti ricevuti.",
        summaryFieldId = "task",
        highlightedItemIds = listOf(COMPLETENESS, QUALITY),
    )
}
