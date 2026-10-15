package it.stradesumisura.app.domain.tools

import it.stradesumisura.app.domain.ScoreBand
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolItem
import it.stradesumisura.app.domain.ToolOption

/**
 * Indice di Barthel Modificato (Modified Barthel Index, MBI).
 * Shah S, Vanclay F, Cooper B. Improving the sensitivity of the Barthel Index for stroke
 * rehabilitation. J Clin Epidemiol. 1989;42(8):703–709.
 *
 * Dieci attività di base della vita quotidiana, ciascuna con cinque livelli ponderati.
 * Punteggio totale 0–100. L'item «Uso della carrozzina» sostituisce «Deambulazione» quando la
 * persona non è in grado di camminare.
 */
object BarthelModifiedIndex {

    const val ID = "barthel_modified"

    private fun option(score: Int, title: String, description: String) = ToolOption(score, title, description)

    val definition: ToolDefinition = ToolDefinition(
        id = ID,
        name = "Indice di Barthel Modificato",
        shortName = "Barthel",
        citation = "Shah, Vanclay e Cooper, 1989",
        subtitle = "Autonomia nelle attività di base della vita quotidiana",
        domain = "Attività di base della vita quotidiana",
        duration = "5–10 minuti",
        description = "L'Indice di Barthel Modificato (Shah, Vanclay & Cooper, 1989) misura il grado di " +
            "indipendenza della persona in dieci attività di base della vita quotidiana: igiene personale, " +
            "bagno, alimentazione, uso dei servizi igienici, scale, vestirsi, controllo dell'alvo, controllo " +
            "vescicale, deambulazione (o uso della carrozzina) e trasferimenti. Rispetto alla versione " +
            "originale, ogni attività è graduata su cinque livelli ponderati, aumentando la sensibilità " +
            "dello strumento ai cambiamenti clinici. Il punteggio totale va da 0 (dipendenza totale) a " +
            "100 (indipendenza completa).",
        instructions = listOf(
            "Valutare ciò che la persona fa effettivamente nella vita quotidiana, non ciò che potrebbe " +
                "fare: la fonte principale è l'osservazione diretta, integrata dal colloquio con la persona, " +
                "i familiari o il personale di assistenza.",
            "Il punteggio descrive le prestazioni delle ultime 24–48 ore. La necessità di supervisione " +
                "significa che la persona non è indipendente.",
            "Per ogni attività selezionare il livello che meglio descrive la situazione osservata. " +
                "I punteggi sono ponderati e sommati in un totale da 0 a 100.",
            "L'item «Uso della carrozzina» va compilato esclusivamente in alternativa a «Deambulazione», " +
                "quando la persona non è in grado di camminare.",
            "L'uso di ausili per raggiungere l'indipendenza è ammesso.",
        ),
        reference = "Shah S, Vanclay F, Cooper B. Improving the sensitivity of the Barthel Index for stroke " +
            "rehabilitation. Journal of Clinical Epidemiology. 1989;42(8):703–709.",
        items = listOf(
            ToolItem(
                id = "hygiene",
                title = "Igiene personale",
                options = listOf(
                    option(0, "Dipendenza totale",
                        "La persona non è in grado di provvedere alla propria igiene personale ed è dipendente in tutti gli aspetti."),
                    option(1, "Assistenza in tutte le fasi",
                        "È necessaria assistenza in tutte le fasi dell'igiene personale, ma la persona è in grado di dare un qualche contributo."),
                    option(3, "Assistenza in alcune fasi",
                        "È necessaria una certa assistenza in una o più fasi dell'igiene personale."),
                    option(4, "Assistenza minima",
                        "La persona è in grado di provvedere alla propria igiene personale, ma richiede un'assistenza minima prima e/o dopo l'attività."),
                    option(5, "Indipendente",
                        "La persona è in grado di lavarsi mani e viso, pettinarsi, lavarsi i denti e radersi. Un uomo può usare qualsiasi tipo di rasoio, ma deve inserire la lametta o collegare il rasoio elettrico senza aiuto e prenderlo dal cassetto o dall'armadietto. Una donna deve truccarsi da sola, se lo fa abitualmente, ma non è necessario che acconci i capelli."),
                ),
            ),
            ToolItem(
                id = "bathing",
                title = "Fare il bagno o la doccia",
                options = listOf(
                    option(0, "Dipendenza totale",
                        "Totalmente dipendente nel fare il bagno."),
                    option(1, "Assistenza in tutti gli aspetti",
                        "È necessaria assistenza in tutti gli aspetti del bagno, ma la persona è in grado di dare un qualche contributo."),
                    option(3, "Assistenza per trasferimento o lavaggio",
                        "È necessaria assistenza per il trasferimento nella doccia o nella vasca, oppure per lavarsi o asciugarsi; include l'incapacità di completare l'attività a causa della condizione o della malattia."),
                    option(4, "Supervisione per sicurezza",
                        "È necessaria supervisione per la sicurezza nel regolare la temperatura dell'acqua o nel trasferimento."),
                    option(5, "Indipendente",
                        "La persona può usare la vasca, la doccia o lavarsi completamente con la spugna. Deve essere in grado di eseguire tutte le fasi del metodo scelto senza la presenza di un'altra persona."),
                ),
            ),
            ToolItem(
                id = "feeding",
                title = "Alimentazione",
                options = listOf(
                    option(0, "Dipendente, deve essere imboccata",
                        "Dipendente in tutti gli aspetti e deve essere imboccata; può essere necessaria l'alimentazione tramite sondino nasogastrico."),
                    option(2, "Assistenza attiva durante il pasto",
                        "Può manovrare una posata, di solito un cucchiaio, ma qualcuno deve fornire assistenza attiva durante il pasto."),
                    option(5, "Supervisione e aiuto nella preparazione",
                        "In grado di alimentarsi con supervisione. È necessaria assistenza per le attività associate, come aggiungere latte o zucchero nel tè, sale e pepe, spalmare il burro, girare il piatto o altre attività di preparazione."),
                    option(8, "Indipendente con vassoio preparato",
                        "Indipendente nell'alimentarsi con il vassoio preparato, ma può aver bisogno che la carne sia tagliata, che il cartone del latte o il barattolo siano aperti, ecc. Non è richiesta la presenza di un'altra persona."),
                    option(10, "Indipendente",
                        "La persona può alimentarsi dal vassoio o dal tavolo quando qualcuno le mette il cibo a portata di mano. Deve indossare da sola un eventuale ausilio, tagliare il cibo e, se lo desidera, usare sale e pepe, spalmare il burro, ecc."),
                ),
            ),
            ToolItem(
                id = "toilet",
                title = "Uso dei servizi igienici",
                options = listOf(
                    option(0, "Dipendenza totale",
                        "Totalmente dipendente nell'uso dei servizi igienici."),
                    option(2, "Assistenza in tutti gli aspetti",
                        "È necessaria assistenza in tutti gli aspetti dell'uso dei servizi igienici."),
                    option(5, "Assistenza parziale",
                        "Può essere necessaria assistenza nella gestione degli indumenti, nel trasferimento o nel lavarsi le mani."),
                    option(8, "Supervisione per sicurezza",
                        "Può essere necessaria supervisione per la sicurezza con il WC normale. Di notte può essere usata una comoda, ma è necessaria assistenza per svuotarla e pulirla."),
                    option(10, "Indipendente",
                        "La persona è in grado di sedersi e alzarsi dal WC, sistemare gli indumenti e usare la carta igienica senza aiuto. Se necessario può usare padella, comoda o pappagallo di notte, ma deve essere in grado di svuotarli e pulirli."),
                ),
            ),
            ToolItem(
                id = "stairs",
                title = "Salire e scendere le scale",
                options = listOf(
                    option(0, "Non in grado",
                        "La persona non è in grado di salire o scendere le scale."),
                    option(2, "Assistenza in tutti gli aspetti",
                        "È necessaria assistenza in tutti gli aspetti, incluso l'aiuto con gli ausili per la deambulazione."),
                    option(5, "Supervisione e assistenza",
                        "La persona è in grado di salire e scendere, ma non riesce a portare gli ausili e necessita di supervisione e assistenza."),
                    option(8, "Supervisione occasionale",
                        "In generale non è necessaria assistenza. A volte è necessaria supervisione per la sicurezza, per esempio a causa di rigidità mattutina, affanno, ecc."),
                    option(10, "Indipendente",
                        "La persona è in grado di salire e scendere una rampa di scale in sicurezza senza aiuto o supervisione. Può usare il corrimano, il bastone o le stampelle quando necessario ed è in grado di portare questi ausili mentre sale o scende."),
                ),
            ),
            ToolItem(
                id = "dressing",
                title = "Vestirsi",
                options = listOf(
                    option(0, "Dipendenza totale",
                        "La persona è dipendente in tutti gli aspetti del vestirsi e non è in grado di partecipare all'attività."),
                    option(2, "Partecipa, ma dipendente",
                        "La persona è in grado di partecipare in qualche misura, ma è dipendente in tutti gli aspetti del vestirsi."),
                    option(5, "Assistenza per indossare o togliere",
                        "È necessaria assistenza per indossare e/o togliere qualsiasi indumento."),
                    option(8, "Assistenza minima per le chiusure",
                        "È necessaria solo un'assistenza minima per le chiusure degli indumenti, come bottoni, cerniere, reggiseno, scarpe, ecc."),
                    option(10, "Indipendente",
                        "La persona è in grado di indossare, togliere e allacciare tutti gli indumenti e le scarpe, compresi eventuali corsetti o tutori prescritti."),
                ),
            ),
            ToolItem(
                id = "bowel",
                title = "Controllo dell'alvo",
                options = listOf(
                    option(0, "Incontinente",
                        "La persona è incontinente per le feci."),
                    option(2, "Aiuto per posizione e facilitazione",
                        "La persona ha bisogno di aiuto per assumere la posizione appropriata e per le tecniche di facilitazione dell'evacuazione."),
                    option(5, "Assistenza per pulizia e ausili, incidenti frequenti",
                        "La persona può assumere la posizione appropriata, ma non può usare le tecniche di facilitazione né pulirsi senza assistenza e ha frequenti incidenti. È necessaria assistenza con gli ausili per l'incontinenza, come i pannoloni."),
                    option(8, "Supervisione, incidenti occasionali",
                        "La persona può avere bisogno di supervisione nell'uso di supposte o clisteri e ha occasionali incidenti."),
                    option(10, "Continente",
                        "La persona controlla l'alvo e non ha incidenti; può usare supposte o praticare un clistere quando necessario."),
                ),
            ),
            ToolItem(
                id = "bladder",
                title = "Controllo vescicale",
                options = listOf(
                    option(0, "Incontinente o catetere a permanenza",
                        "La persona è dipendente nella gestione vescicale, è incontinente o ha un catetere a permanenza."),
                    option(2, "Incontinente, collabora con i dispositivi",
                        "La persona è incontinente, ma è in grado di collaborare all'applicazione di un dispositivo interno o esterno."),
                    option(5, "Asciutta di giorno, non di notte",
                        "La persona è generalmente asciutta di giorno ma non di notte, e necessita di un certo aiuto con i dispositivi."),
                    option(8, "Asciutta giorno e notte, incidenti occasionali",
                        "La persona è generalmente asciutta di giorno e di notte, ma può avere occasionali incidenti o necessitare di un'assistenza minima con i dispositivi interni o esterni."),
                    option(10, "Continente",
                        "La persona è in grado di controllare la vescica giorno e notte e/o è indipendente con i dispositivi interni o esterni."),
                ),
            ),
            ToolItem(
                id = "ambulation",
                title = "Deambulazione",
                hint = "Se la persona non è in grado di camminare, compilare in alternativa «Uso della carrozzina».",
                options = listOf(
                    option(0, "Dipendente",
                        "Dipendente nella deambulazione."),
                    option(3, "Presenza costante di una o più persone",
                        "È necessaria la presenza costante di una o più persone durante la deambulazione."),
                    option(8, "Assistenza di una persona",
                        "È necessaria assistenza per raggiungere gli ausili e/o per manovrarli. È necessaria una persona per fornire assistenza."),
                    option(12, "Indipendente, ma meno di 50 m o con supervisione",
                        "La persona è indipendente nella deambulazione, ma non riesce a percorrere 50 metri senza aiuto, oppure è necessaria supervisione per sicurezza o per fiducia in situazioni pericolose."),
                    option(15, "Indipendente per 50 metri",
                        "La persona deve essere in grado di indossare eventuali tutori, bloccarli e sbloccarli, alzarsi in piedi, sedersi e sistemare gli ausili necessari per l'uso. Deve essere in grado di usare stampelle, bastoni o deambulatore e percorrere 50 metri senza aiuto o supervisione."),
                ),
            ),
            ToolItem(
                id = "wheelchair",
                title = "Uso della carrozzina",
                hint = "Solo se la persona non è in grado di deambulare: sostituisce l'item «Deambulazione».",
                alternativeOf = "ambulation",
                options = listOf(
                    option(0, "Dipendente",
                        "Dipendente negli spostamenti in carrozzina."),
                    option(1, "Brevi distanze su superficie piana",
                        "La persona può spingersi autonomamente per brevi distanze su superficie piana, ma è necessaria assistenza per tutte le altre fasi della gestione della carrozzina."),
                    option(3, "Assistenza costante di una persona",
                        "È necessaria la presenza di una persona e un'assistenza costante per manovrare la carrozzina verso il tavolo, il letto, ecc."),
                    option(4, "Autonoma, aiuto nei passaggi stretti",
                        "La persona può spingersi autonomamente per un tempo ragionevole su terreni abitualmente frequentati. Può essere ancora necessaria un'assistenza minima nei passaggi stretti."),
                    option(5, "Indipendente per 50 metri",
                        "Per spingere la carrozzina in modo indipendente, la persona deve essere in grado di girare agli angoli, invertire la direzione, accostare la carrozzina a tavolo, letto, WC, ecc. Deve essere in grado di spingere la carrozzina per almeno 50 metri."),
                ),
            ),
            ToolItem(
                id = "transfers",
                title = "Trasferimenti letto–sedia",
                options = listOf(
                    option(0, "Non partecipa, servono due persone",
                        "Non è in grado di partecipare al trasferimento. Sono necessarie due persone per trasferirla, con o senza un sollevatore."),
                    option(3, "Partecipa, assistenza massima di una persona",
                        "È in grado di partecipare, ma è necessaria la massima assistenza di un'altra persona in tutti gli aspetti del trasferimento."),
                    option(8, "Assistenza di una persona",
                        "Il trasferimento richiede l'assistenza di un'altra persona. L'assistenza può essere necessaria in qualsiasi fase del trasferimento."),
                    option(12, "Presenza o supervisione per sicurezza",
                        "È necessaria la presenza di un'altra persona per dare sicurezza o per fornire supervisione."),
                    option(15, "Indipendente",
                        "La persona può avvicinarsi al letto in carrozzina in sicurezza, bloccare i freni, sollevare le pedane, spostarsi sul letto in sicurezza, sdraiarsi, mettersi seduta sul bordo del letto, cambiare la posizione della carrozzina e ritornarvi in sicurezza. Deve essere indipendente in tutte le fasi di questa attività."),
                ),
            ),
        ),
        bands = listOf(
            ScoreBand(0, 24, "Dipendenza totale",
                "La persona dipende completamente dall'assistenza di altri nelle attività di base.", 0xFFB4455A),
            ScoreBand(25, 49, "Dipendenza grave",
                "La persona necessita di assistenza rilevante nella maggior parte delle attività.", 0xFFD3714F),
            ScoreBand(50, 74, "Dipendenza moderata",
                "La persona svolge parte delle attività con assistenza o supervisione.", 0xFFD9A441),
            ScoreBand(75, 90, "Dipendenza lieve",
                "La persona è in larga misura autonoma, con aiuto limitato ad alcune attività.", 0xFF9BB357),
            ScoreBand(91, 99, "Dipendenza minima",
                "La persona è quasi del tutto indipendente nelle attività di base.", 0xFF5F9E6E),
            ScoreBand(100, 100, "Indipendenza",
                "La persona è indipendente in tutte le attività di base della vita quotidiana.", 0xFF2F7F5E),
        ),
    )
}
