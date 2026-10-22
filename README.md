# STRADE su Misura

App per terapisti occupazionali, per **Android** e **Windows 11** (versione portatile, senza
installazione): strumenti e scale di valutazione compilabili, archiviate in locale e condivisibili
come report PDF professionali.

L'identità visiva riprende il marchio STRADE: le due strade che si incrociano (banda nera e banda
diagonale color malva `#C7ADBF`), tipografia geometrica e un'interfaccia pulita in bianco e nero.

## Funzionalità

- **Archivio persone** – anagrafica essenziale (nome, data di nascita, sesso, codice, note) con
  ricerca; ogni persona raccoglie le proprie valutazioni.
- **Strumenti di valutazione** – catalogo estensibile:
  - **Indice di Barthel Modificato** (Shah, Vanclay e Cooper, 1989): 10 attività di base della vita
    quotidiana con cinque livelli ponderati, punteggio 0–100 e fasce interpretative.
  - **Osservazione ADL** (linee guida olandesi per l'aprassia, Hogeschool van Amsterdam e NVE 2003;
    versione italiana Università di Verona 2012): osservazione di un compito con valutazione 0–3
    delle fasi di orientamento, esecuzione e controllo; il punteggio di indipendenza è calcolato
    automaticamente come il livello più alto tra le tre fasi.
  - **PQRS – Performance Quality Rating Scale** (Martini e coll., 2015; approccio CO-OP): il
    terapista descrive il compito osservato e attribuisce due valori da 1 a 10, completezza e
    qualità della performance o del prodotto; il punteggio PQRS è calcolato automaticamente come
    media dei due arrotondata per difetto. I due valori inseriti e quello calcolato sono evidenziati
    nella scheda, nel dettaglio e nel report.
  - **AFMS – Abilità monetarie funzionali** (Assessment of Functional Monetary Skills, versione
    italiana in euro): dieci prove pratiche sull'uso del denaro (riconoscimento di monete e
    banconote, somme, consegna di importi, calcoli scritti, calcolatrice, problemi in parole e la
    sezione facoltativa su fatture e assegni). Ogni prova è un conteggio di risposte corrette: le
    sotto-prove si segnano una per una come riuscite o non riuscite durante la somministrazione,
    con un campo «Commenti» per prova. Senza fasce interpretative: il totale si legge anche in
    percentuale e le prove facoltative non somministrate non entrano nel massimo.
  - **FIM – Misura dell'indipendenza funzionale** (Granger e coll., 1987; versione italiana
    SO.GE.COM.): diciotto voci, tredici motorie (A–M) e cinque cognitive (N–R), su sette livelli
    da 1 (assistenza totale) a 7 (autosufficienza completa); totale 18–126 con i subtotali
    dell'area motoria (13–91) e cognitiva (5–35). Per ogni voce è trascritto l'albero decisionale
    del manuale: il pulsante «Guida con l'albero decisionale» pone le domande una alla volta
    (Sì/No, oppure il numero di episodi di incontinenza) e seleziona il livello raggiunto, che
    resta modificabile. Vescica e Alvo percorrono due alberi (livello di assistenza e frequenza
    degli episodi) e registrano il più basso; la scheda e il report indicano quando il livello è
    stato scelto con la guida. Campi per la modalità di locomozione, comprensione ed espressione.
  - **TAI 4.0 – Transfer Assessment Instrument** (Worobey, Rice e coll., 2018; HERL, Università di
    Pittsburgh), in tre strumenti: trasferimenti autonomi (18 voci), assistiti senza sollevatore
    (21 voci) e con sollevatore (5 voci). Le voci seguono le tre fasi del modulo (preparazione della
    carrozzina, preparazione del corpo, volo e atterraggio) più ausili e assistente; i punteggi
    valgono 1, 0,5, 0 o negativi, cambiano con il tipo di carrozzina (manuale o elettrica, scelto
    nei dati della valutazione) e le opzioni «non applicabile» escono dal massimo. Le posizioni
    della mano di spinta e della mano guida si scelgono per superficie con le foto del modulo
    (scelta multipla, punteggio = media delle posizioni); le voci «segnare tutte le voci che si
    applicano» sommano i punteggi senza scendere sotto 0. Punteggio di ogni trasferimento in
    percentuale del massimo applicabile; il secondo trasferimento è facoltativo e, se compilato,
    il totale è la media delle due percentuali.
  - **Scheda di valutazione regionale** (persone con disabilità, accesso ai servizi diurni e
    residenziali): dati della persona e del servizio (codice fiscale, comune di residenza, servizio
    in cui è già inserita), scheda A con otto categorie ICF valutate con il qualificatore di
    capacità da 0 a 4, scheda B con i problemi fisici e di comportamento di particolare gravità
    (1 = no, 2 = sì senza interventi speciali, 3 = sì con interventi speciali; le voci «Altro» sono
    facoltative, con nota) e sintesi C: la classe da A (problemi di particolare gravità) a E
    (disabilità lieve) è calcolata con i criteri generali di identificazione della scheda e
    mostrata come lettera nell'app e nel report, con il criterio che l'ha determinata.
- **Compilazione guidata** – livelli descritti per esteso, punteggio e completamento aggiornati in
  tempo reale, gestione dell'alternativa deambulazione/carrozzina, data, valutatore, contesto e
  osservazioni. Le valutazioni incomplete si salvano come parziali.
- **Report PDF** – documento A4 con il logo STRADE (Autonomia, Cittadinanza, Occupabilità)
  nell'intestazione, dati della persona e della valutazione,
  punteggio con barra interpretativa, tabella delle attività con il livello rilevato, osservazioni,
  legenda, riferimento bibliografico, spazio per la firma e numerazione delle pagine. Il report
  viene impaginato in una sola pagina quando possibile (tre livelli di densità) e i testi vengono
  mandati a capo o accorciati per restare nei loro riquadri. Anteprima in app; su Android
  condivisione o salvataggio sul dispositivo, su Windows «Salva PDF…», apertura nel lettore PDF
  predefinito e stampa diretta.
- **Impostazioni** – profilo del valutatore (nome, qualifica, struttura, luogo) usato nei report.
- **Privacy** – tutti i dati restano sul dispositivo (database Room locale); nessun server.

## Versione per Windows 11

La versione Windows usa lo stesso codice dell'app Android (scale, punteggi, archivio e report PDF)
con un'interfaccia adattata allo schermo del computer: pannello di navigazione laterale, contenuti
centrati, schede degli strumenti su due colonne, barre di scorrimento e barra del titolo nei colori
dell'app (chiara o scura come Windows).

- **Portatile, senza installazione** – si scarica lo ZIP
  `STRADE-su-Misura-<versione>-Windows-portatile.zip` dalla pagina **Releases**, lo si estrae
  dove si preferisce (anche su una chiavetta USB) e si avvia `STRADE su Misura.exe`. Java è
  incluso. Al primo avvio SmartScreen può chiedere conferma («Ulteriori informazioni» →
  «Esegui comunque») perché l'eseguibile non ha una firma digitale commerciale.
- **Dati** – archivio, impostazioni e dimensioni della finestra sono nella cartella `Dati` accanto
  al programma (se non è scrivibile, in `%LOCALAPPDATA%\STRADE su Misura`). Per una copia di
  sicurezza o per cambiare computer basta copiare la cartella del programma. I PDF generati per
  l'anteprima sono in `Dati/Temporanei` e vengono eliminati a ogni avvio.
- **Report PDF** – stesso impaginato della versione Android, generato con Apache PDFBox con i
  caratteri Roboto incorporati; anteprima nell'app, «Salva PDF…» con la finestra di Windows,
  «Apri» nel lettore predefinito e «Stampa…» con la finestra di stampa di sistema.
- **Sicurezza dei dati** – una sola finestra alla volta sullo stesso archivio; chiudendo il
  programma con una valutazione non salvata viene chiesta conferma; gli errori imprevisti sono
  annotati in `Dati/registro-errori.txt`.

## Tecnologie

| Componente | Scelta |
|---|---|
| Linguaggio | Kotlin 2.2 (Kotlin Multiplatform: Android + JVM desktop) |
| Interfaccia | Compose Multiplatform + Material 3, tema chiaro/scuro |
| Persistenza | Room (SQLite; su Windows con il driver SQLite incluso) |
| Navigazione | Navigation Compose |
| PDF | motore di impaginazione comune (`pdf/PdfKit.kt`, `pdf/AssessmentPdfExporter.kt`) disegnato con `android.graphics.pdf.PdfDocument` su Android e Apache PDFBox su Windows; testo in Roboto statico incluso nelle risorse (Apache 2.0), per evitare i glifi Type3 generati dai font variabili di sistema |
| Pacchetto Windows | Compose Desktop `createDistributable` (jpackage, Java 17 ridotto incluso), distribuito come ZIP portatile |
| Build | Gradle 8.13, Android Gradle Plugin 8.11, JDK 17, `minSdk 26`, `targetSdk 35` |

Moduli:

```
shared/   codice comune (commonMain) e parti specifiche di Android (androidMain) e Windows (desktopMain)
app/      app Android: Application, Activity, manifest, icone
desktop/  app Windows: finestra, cartella dei dati portatile, icona e pacchetto
```

Struttura del codice comune (`shared/src/commonMain/kotlin/it/stradesumisura/app`):

```
data/        entità Room, DAO, repository, impostazioni
domain/      modello generico degli strumenti (ToolDefinition) e registro degli strumenti
domain/tools/ definizione delle singole scale (BarthelModifiedIndex.kt, AdlObservation.kt, PerformanceQualityRatingScale.kt, FunctionalMonetarySkills.kt, FunctionalIndependenceMeasure.kt, TransferAssessmentInstrument.kt, RegionalAssessment.kt)
pdf/         impaginazione e generazione dei report
ui/          tema, componenti, schermate, ViewModel e navigazione
```

## Compilazione

Il progetto si compila su GitHub Actions (`.github/workflows/android.yml`) a ogni push su `main`:
il workflow produce l'APK di release firmato, l'APK di debug (Ubuntu) e lo ZIP portatile per
Windows (runner Windows) come artefatti del run. Creando un tag `vX.Y.Z` viene pubblicata
automaticamente una **Release** con l'APK e lo ZIP per Windows allegati.

In locale (con Android Studio o JDK 17 + Android SDK):

```bash
./gradlew :app:assembleDebug
```

La versione Windows (su Windows, con JDK 17) si avvia con `./gradlew :desktop:run` e si
impacchetta con `./gradlew :desktop:createDistributable`.

### Firma delle release

La chiave di firma non è nel repository. Il workflow la legge dai secret GitHub:

| Secret | Contenuto |
|---|---|
| `STRADE_KEYSTORE_BASE64` | keystore PKCS12 codificato in base64 |
| `STRADE_KEYSTORE_PASSWORD` | password del keystore |
| `STRADE_KEY_ALIAS` | alias della chiave (`strade`) |
| `STRADE_KEY_PASSWORD` | password della chiave |

In locale la stessa configurazione si mette in `keystore/keystore.properties` (cartella ignorata da
git). Senza chiave, la build di release viene firmata con la chiave di debug.

## Installazione sul telefono

1. Scaricare l'APK dalla pagina **Releases** (o dagli artefatti del run in **Actions**).
2. Aprire il file sul dispositivo Android e consentire l'installazione da origini sconosciute.
3. Gli aggiornamenti si installano sopra la versione precedente se firmati con la stessa chiave.

## Aggiungere un nuovo strumento

1. Creare un oggetto in `shared/.../domain/tools/` che espone una `ToolDefinition` (nome, descrizione,
   istruzioni, riferimento, item con opzioni e punteggi, fasce interpretative). Gli item possono
   dichiarare `alternativeOf` per le alternative mutuamente esclusive oppure `computed` per i
   punteggi calcolati da altri item (`ComputedScore.MaxOf`, `ComputedScore.FloorMeanOf`);
   `highlightedItemIds` evidenzia i valori inseriti accanto al punteggio calcolato. Gli item a
   conteggio si creano con `ToolItem.count(...)` (numero di risposte corrette, con eventuali
   `subtasks` da spuntare una per una); `optional = true` esclude dal massimo le prove non
   somministrate e `number` fissa l'etichetta (es. "7a"). Uno strumento senza `bands` mostra il
   punteggio in percentuale se tutti gli item sono a conteggio, altrimenti come somma dei livelli.
   Un item può dichiarare `decisionTree` (`DecisionNode.yesNo`, `choice`, `level`, `LowerOf`):
   il modulo propone allora la guida con le domande e salva il percorso scelto. `groups` divide
   gli item in aree con subtotale (es. motoria e cognitiva nella FIM). `ComputedScore.Rule` calcola un
   item con una regola propria dello strumento (es. la classe della scheda regionale); se tutte le
   fasce hanno un `code` il punteggio principale è una classe e si mostra il codice (es. «B») al
   posto del numero, con `scoreRangeText` per l'intervallo (es. «A–E»).
2. Registrarlo in `ToolRegistry.tools`.

Compilazione, salvataggio, punteggio, schermate e PDF funzionano automaticamente per ogni
strumento registrato, sia su Android sia su Windows. Le immagini delle opzioni vanno in
`shared/src/commonMain/composeResources/drawable/` e si indicano con `Res.drawable.<nome>`.

Il logo dei report è `shared/src/commonMain/composeResources/files/strade_logo.png` (scala di grigi,
ritagliato sul marchio): per cambiarlo basta sostituire il file e aggiornarne le dimensioni in
`PdfImage.Logo` (`pdf/PdfGraphics.kt`).

## Licenza

© STRADE su Misura. Tutti i diritti riservati.
