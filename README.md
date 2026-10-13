# STRADE su Misura

App Android per terapisti occupazionali: strumenti e scale di valutazione compilabili, archiviate
in locale e condivisibili come report PDF professionali.

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
- **Compilazione guidata** – livelli descritti per esteso, punteggio e completamento aggiornati in
  tempo reale, gestione dell'alternativa deambulazione/carrozzina, data, valutatore, contesto e
  osservazioni. Le valutazioni incomplete si salvano come parziali.
- **Report PDF** – documento A4 con intestazione STRADE, dati della persona e della valutazione,
  punteggio con barra interpretativa, tabella delle attività con il livello rilevato, osservazioni,
  legenda, riferimento bibliografico, spazio per la firma e numerazione delle pagine. Il report
  viene impaginato in una sola pagina quando possibile (tre livelli di densità) e i testi vengono
  mandati a capo o accorciati per restare nei loro riquadri. Anteprima in app, condivisione o
  salvataggio sul dispositivo.
- **Impostazioni** – profilo del valutatore (nome, qualifica, struttura, luogo) usato nei report.
- **Privacy** – tutti i dati restano sul dispositivo (database Room locale); nessun server.

## Tecnologie

| Componente | Scelta |
|---|---|
| Linguaggio | Kotlin 2.2 |
| Interfaccia | Jetpack Compose + Material 3, tema chiaro/scuro |
| Persistenza | Room (SQLite) |
| Navigazione | Navigation Compose |
| PDF | `android.graphics.pdf.PdfDocument` con motore di impaginazione interno (`pdf/PdfKit.kt`); testo in Roboto statico incluso negli asset (Apache 2.0), per evitare i glifi Type3 generati dai font variabili di sistema |
| Build | Gradle 8.13, Android Gradle Plugin 8.11, JDK 17, `minSdk 26`, `targetSdk 35` |

Struttura principale del codice (`app/src/main/java/it/stradesumisura/app`):

```
data/        entità Room, DAO, repository, impostazioni
domain/      modello generico degli strumenti (ToolDefinition) e registro degli strumenti
domain/tools/ definizione delle singole scale (BarthelModifiedIndex.kt, AdlObservation.kt, PerformanceQualityRatingScale.kt)
pdf/         impaginazione e generazione dei report
ui/          tema, componenti, schermate, ViewModel e navigazione
```

## Compilazione

Il progetto si compila su GitHub Actions (`.github/workflows/android.yml`) a ogni push su `main`:
il workflow produce l'APK di release firmato e l'APK di debug come artefatti del run.
Creando un tag `vX.Y.Z` viene pubblicata automaticamente una **Release** con l'APK allegato.

In locale (con Android Studio o JDK 17 + Android SDK):

```bash
./gradlew assembleDebug
```

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

1. Creare un oggetto in `domain/tools/` che espone una `ToolDefinition` (nome, descrizione,
   istruzioni, riferimento, item con opzioni e punteggi, fasce interpretative). Gli item possono
   dichiarare `alternativeOf` per le alternative mutuamente esclusive oppure `computed` per i
   punteggi calcolati da altri item (`ComputedScore.MaxOf`, `ComputedScore.FloorMeanOf`);
   `highlightedItemIds` evidenzia i valori inseriti accanto al punteggio calcolato.
2. Registrarlo in `ToolRegistry.tools`.

Compilazione, salvataggio, punteggio, schermate e PDF funzionano automaticamente per ogni
strumento registrato.

## Licenza

© STRADE su Misura. Tutti i diritti riservati.
