package it.stradesumisura.app.domain.tools

import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolField
import it.stradesumisura.app.domain.ToolItem

/**
 * AFMS – Assessment of Functional Monetary Skills, versione italiana (AFMS-Ita).
 * Valutazione pratica delle abilità di gestione del denaro: riconoscimento di monete e banconote,
 * somme, consegna di importi, calcoli scritti, uso della calcolatrice, problemi della vita quotidiana
 * e pagamento di fatture con assegni (sezione facoltativa).
 *
 * Ogni prova è valutata con il numero di risposte corrette («performance», quantitativa) e con un
 * campo «Commenti» (qualitativo: attenzione, riconoscimento degli errori, strategie). Il modulo
 * originale fornito dall'utente non indica fasce interpretative: il totale si legge come numero e
 * percentuale di risposte corrette.
 */
object FunctionalMonetarySkills {

    const val ID = "afms"

    private const val COMMENTS = "Commenti"

    val definition: ToolDefinition = ToolDefinition(
        id = ID,
        name = "AFMS – Abilità monetarie funzionali",
        shortName = "AFMS",
        citation = "Assessment of Functional Monetary Skills, versione italiana",
        subtitle = "Prove pratiche di riconoscimento, calcolo e uso del denaro",
        domain = "Gestione del denaro · Attività strumentali della vita quotidiana",
        duration = "45–90 minuti, in più sessioni",
        description = "L'AFMS (Assessment of Functional Monetary Skills) valuta con prove pratiche le abilità " +
            "necessarie a gestire il denaro nella vita quotidiana: riconoscere monete e banconote, sommare " +
            "importi, consegnare una cifra richiesta, eseguire calcoli scritti, usare la calcolatrice, risolvere " +
            "problemi di spesa e, per chi paga le fatture con assegni, gestire fatture e assegni. Ogni prova " +
            "ha una valutazione quantitativa (il numero di risposte corrette, la «performance») e una " +
            "qualitativa nel campo «Commenti» (es. capacità di mantenere l'attenzione, di riconoscere e " +
            "correggere gli errori, strategie usate). La versione italiana è adattata all'euro. Il punteggio " +
            "totale è la somma delle risposte corrette; la sezione 7 è facoltativa e, se non somministrata, " +
            "non entra nel totale né nel massimo.",
        instructions = listOf(
            "Preparare i materiali: 2 banconote da 5 €, 1 da 10 €, 1 da 20 €, 1 moneta da 1 € e 1 da 2 €, " +
                "3 monete ciascuna da 1, 2, 5, 10, 20 e 50 centesimi; una calcolatrice e fogli di lavoro; " +
                "per la sezione 7, il libretto degli assegni della persona, fatture e vecchi libretti (vanno " +
                "bene anche fotocopie).",
            "Per ogni prova registrare la performance (numero di risposte corrette) e i commenti qualitativi: " +
                "attenzione, riconoscimento degli errori, tempi, strategie. Nelle prove con l'elenco delle " +
                "sotto-prove segnare ciascuna come riuscita o non riuscita: il conteggio si aggiorna da solo.",
            "In presenza di afasia in produzione o in comprensione, l'esaminatore può modificare la " +
                "presentazione delle prove o la modalità di risposta accettata (es. far indicare o scrivere la " +
                "risposta corretta). Annotare gli adattamenti nel campo dedicato.",
            "È uno strumento di valutazione: astenersi dal fornire indizi che potrebbero influenzare la " +
                "performance in qualunque prova.",
            "Il test va somministrato in più sessioni; il tempo totale è di circa 45–90 minuti. La valutazione " +
                "si può salvare come parziale e completare nelle sessioni successive.",
            "La sezione 7 (pagamento di fatture con assegni) si somministra solo a chi paga le fatture con " +
                "assegni: le sue prove si possono lasciare non valutate.",
        ),
        reference = "Assessment of Functional Monetary Skills (AFMS), versione italiana adattata all'euro " +
            "(AFMS-Ita), dal modulo di somministrazione in uso presso il servizio. Il modulo non riporta " +
            "autori né fasce interpretative: il punteggio è il numero di risposte corrette per prova e in totale.",
        fields = listOf(
            ToolField("sessions", "Sessioni di somministrazione", "es. 3 sessioni: 10/10, 12/10 e 15/10", shortLabel = "Sessioni"),
            ToolField("adaptations", "Adattamenti alla somministrazione", "es. risposta indicata o scritta per afasia", shortLabel = "Adattamenti"),
        ),
        items = listOf(
            ToolItem.count(
                id = "identify",
                number = "1",
                title = "Identificare monete e banconote",
                max = 11,
                hint = "Porre davanti alla persona una banconota da 5, una da 10, una da 20 e una moneta per ciascun valore.",
                description = "Porre di fronte alla persona una banconota da 5 €, una da 10 €, una da 20 € e una " +
                    "moneta per ciascun valore. Esaminatore: «Nomini ciascuna banconota e ciascuna moneta». " +
                    "Una risposta corretta per ciascun valore riconosciuto.",
                subtasks = listOf(
                    "Banconota da 5 euro",
                    "Banconota da 10 euro",
                    "Banconota da 20 euro",
                    "Moneta da 1 centesimo",
                    "Moneta da 2 centesimi",
                    "Moneta da 5 centesimi",
                    "Moneta da 10 centesimi",
                    "Moneta da 20 centesimi",
                    "Moneta da 50 centesimi",
                    "Moneta da 1 euro",
                    "Moneta da 2 euro",
                ),
                noteLabel = COMMENTS,
                unit = "valori riconosciuti",
            ),
            ToolItem.count(
                id = "sums",
                number = "2",
                title = "Somme con monete e banconote",
                max = 15,
                hint = "Usando le monete e le banconote indicate, chiedere alla persona di sommare.",
                description = "Usando le monete e le banconote indicate, chiedere alla persona di sommare gli " +
                    "importi di ciascuna prova. Il risultato atteso è indicato tra parentesi.",
                subtasks = listOf(
                    "5 centesimi e 1 centesimo (0,06 €)",
                    "20 centesimi e 2 centesimi (0,22 €)",
                    "10 centesimi, 1 centesimo e 20 centesimi (0,31 €)",
                    "5 centesimi, 1 centesimo e 50 centesimi (0,56 €)",
                    "2 centesimi, 5 centesimi, 10 centesimi e 20 centesimi (0,37 €)",
                    "Banconota da 10 euro e banconota da 5 euro (15 €)",
                    "Moneta da 1 euro e banconota da 5 euro (6 €)",
                    "Moneta da 2 euro e 20 centesimi (2,20 €)",
                    "Banconota da 10 euro e 5 centesimi (10,05 €)",
                    "Banconota da 10 euro, 20 centesimi e 2 centesimi (10,22 €)",
                    "Banconota da 5 euro, 1 centesimo e 50 centesimi (5,51 €)",
                    "Banconota da 5 euro, 5 centesimi, 1 centesimo e 20 centesimi (5,26 €)",
                    "Banconota da 10 euro, 2 euro, 10 centesimi e 5 centesimi (12,15 €)",
                    "Moneta da 1 euro, banconota da 10 euro, 1 centesimo, 10 centesimi e 20 centesimi (11,31 €)",
                    "Banconota da 5 euro, 10 centesimi, 2 centesimi, 20 centesimi e 5 centesimi (5,37 €)",
                ),
                noteLabel = COMMENTS,
            ),
            ToolItem.count(
                id = "give_amount",
                number = "3",
                title = "Fornire una somma di denaro specifica su richiesta",
                max = 6,
                hint = "Disporre in modo casuale il denaro davanti alla persona e chiedere di consegnare ciascun importo.",
                description = "Materiali e procedura: porre in modo casuale davanti alla persona tre monete da " +
                    "1, 2, 5, 10, 20 e 50 centesimi, da 1 euro e da 2 euro, tre banconote da 5 euro e tre da " +
                    "10 euro. Chiedere di consegnare gli importi indicati; dopo ogni prova rimettere il denaro " +
                    "sul tavolo.",
                subtasks = listOf(
                    "1 euro e 25 centesimi",
                    "20 centesimi",
                    "2 euro e 30 centesimi",
                    "6 euro e 5 centesimi",
                    "2 euro e 57 centesimi",
                    "10 euro e 27 centesimi",
                ),
                noteLabel = COMMENTS,
                unit = "importi corretti",
            ),
            ToolItem.count(
                id = "written_calc",
                number = "4",
                title = "Calcoli numerici scritti",
                max = 8,
                hint = "Fornire alla persona una copia scritta dei calcoli.",
                description = "Fornire alla persona una copia scritta dei calcoli e farli eseguire. Il risultato " +
                    "atteso è indicato tra parentesi.",
                subtasks = listOf(
                    "11 + 4 (15)",
                    "346 + 89 (435)",
                    "17 × 2 (34)",
                    "42 × 3 (126)",
                    "21 − 9 (12)",
                    "56 − 27 (29)",
                    "45 : 9 (5)",
                    "22 : 5 (4,4)",
                ),
                noteLabel = COMMENTS,
                unit = "calcoli corretti",
            ),
            ToolItem.count(
                id = "calculator",
                number = "5",
                title = "Uso della calcolatrice",
                max = 8,
                hint = "Verificare l'uso appropriato della calcolatrice con fogli di lavoro adeguati (8 esercizi).",
                description = "Verificare l'abilità della persona nell'uso appropriato della calcolatrice, " +
                    "fornendo fogli di lavoro adeguati. Registrare il numero di esercizi svolti correttamente " +
                    "su 8.",
                noteLabel = COMMENTS,
                unit = "esercizi corretti",
            ),
            ToolItem.count(
                id = "word_problems",
                number = "6",
                title = "Problemi in parole",
                max = 4,
                hint = "Fornire la copia scritta dei quesiti; incoraggiare il calcolo a mente, senza carta e matita.",
                description = "I problemi riproducono comuni richieste di calcolo della vita quotidiana. La persona " +
                    "è incoraggiata a risolverli a mente (scoraggiare l'uso di fogli e matita). Per i quesiti a) " +
                    "e b) è sufficiente una stima prudente del costo: non è necessario calcolare la risposta " +
                    "precisa. Fornire la copia scritta dei quesiti. La soluzione è indicata tra parentesi.",
                subtasks = listOf(
                    "a) Hai 3 euro e vuoi comprare più francobolli da 33 centesimi possibile. Quanti francobolli puoi comprare? (9)",
                    "b) Puoi comprare due oggetti per un totale di 2,50 euro o tre oggetti per un totale di 3,10 euro. Qual è la scelta migliore? (tre oggetti: circa 1,03 € l'uno contro 1,25 €)",
                    "c) Compri un oggetto per 13,25 euro. Dai alla cassiera 20 euro e lei ti dà indietro 6,75 euro. Il resto è corretto? Se non lo è, qual è la cifra corretta? (sì, è corretto)",
                    "d) Ricevi 200 euro di multa, ma pagandola entro 24 ore ti viene tolto il 35% dal totale. Quanto ti resta da pagare? (130 €)",
                ),
                noteLabel = COMMENTS,
                unit = "problemi risolti",
            ),
            ToolItem.count(
                id = "bills_info",
                number = "7a",
                title = "Fatture: importo, numero di conto e scadenza",
                max = 12,
                hint = "Sezione 7, facoltativa: solo per chi paga le fatture con assegni. Usare fatture e libretti degli assegni della persona.",
                description = "La sezione 7 è per le persone che pagano le fatture con assegni; usare il libretto " +
                    "degli assegni attuale della persona, fatture e vecchi libretti (vanno bene anche fotocopie). " +
                    "7a: selezionare tre fatture e far identificare per ciascuna l'importo dovuto, il numero di " +
                    "conto e la data di scadenza, chiedendo rispettivamente «Quanto devo?», «Qual è il numero di " +
                    "conto?» e «Quando scade questa fattura?». Il modulo prevede un massimo di 12 risposte.",
                noteLabel = COMMENTS,
                optional = true,
            ),
            ToolItem.count(
                id = "cheque_parts",
                number = "7b",
                title = "Componenti dell'assegno",
                max = 7,
                hint = "Facoltativa. Per le prime sei componenti indicare e chiedere «Cosa va qui?»; per il numero chiedere «Qual è il numero dell'assegno?».",
                description = "Chiedere alla persona di identificare le sette componenti di un assegno. Per le " +
                    "prime sei l'esaminatore indica la parte e chiede «Cosa va qui?»; per il numero dell'assegno " +
                    "chiede «Qual è il numero dell'assegno?».",
                subtasks = listOf(
                    "Data",
                    "Luogo",
                    "Importo in cifre",
                    "Importo in lettere",
                    "Nome e cognome del beneficiario",
                    "Firma dell'intestatario",
                    "Numero dell'assegno",
                ),
                noteLabel = COMMENTS,
                optional = true,
                unit = "componenti riconosciute",
            ),
            ToolItem.count(
                id = "pay_one_bill",
                number = "7d",
                title = "Pagamento di una fattura in tre passaggi",
                max = 3,
                hint = "Facoltativa. Prima (7c) far descrivere il processo di pagamento abituale; poi far completare il pagamento di una fattura.",
                description = "7c: chiedere alla persona di descrivere il processo di pagamento delle fatture che " +
                    "usa normalmente (es. «Compila l'assegno per prima cosa? Usa una calcolatrice?») e annotarlo " +
                    "nei commenti. 7d: selezionare una fattura e far completare il pagamento nei tre passaggi: " +
                    "identificare l'importo della fattura, compilare i campi dell'assegno, compilare la matrice.",
                subtasks = listOf(
                    "Identificare l'importo della fattura",
                    "Compilare i campi dell'assegno",
                    "Compilare la matrice dell'assegno",
                ),
                noteLabel = "Commenti (incluso il processo abituale descritto, 7c)",
                optional = true,
                unit = "passaggi corretti",
            ),
            ToolItem.count(
                id = "pay_three_bills",
                number = "7f",
                title = "Pagamento di tre fatture",
                max = 9,
                hint = "Facoltativa. Tre fatture da pagare seguendo il processo descritto: tre passaggi per ciascuna.",
                description = "Selezionare tre fatture e chiedere alla persona di pagarle seguendo il processo " +
                    "descritto in precedenza. Una risposta corretta per ogni passaggio riuscito (tre per fattura).",
                noteLabel = COMMENTS,
                optional = true,
                unit = "passaggi corretti",
            ),
        ),
        bands = emptyList(),
        totalLabel = "Risposte corrette",
        itemNounSingular = "prova",
        itemNounPlural = "prove",
        itemsSectionTitle = "Prove",
        notesLabel = "Commenti generali",
        notesHint = "Osservazioni complessive: attenzione, affaticamento, consapevolezza degli errori, strategie.",
    )
}
