# STRADE su Misura – Manuale d'uso (Android)

Versione del manuale: 9 novembre 2026 · Angelo Bottini

## Introduzione

STRADE su Misura è l'app Android per terapisti occupazionali che permette di compilare scale e strumenti di valutazione, archiviare le valutazioni per persona e produrre report PDF pronti da condividere. Tutto resta sul telefono o sul tablet: non esiste un server e nessun dato viene inviato all'esterno.

> **Nota**
>
> Questo manuale descrive la versione Android 1.8.0 (novembre 2026). Le stesse funzioni esistono nella versione portatile per Windows 11, che ha un manuale a parte.

### Requisiti

- Telefono o tablet con Android 8.0 (Oreo) o successivo.
- Circa 40 MB di spazio libero per l'app; le valutazioni occupano pochi kilobyte ciascuna.
- Nessuna connessione a Internet necessaria, se non per scaricare l'APK.

### Cosa si può fare

| Funzione | In breve |
| --- | --- |
| Archivio persone | Anagrafica essenziale (nome, nascita, sesso, codice, note) con ricerca |
| Strumenti | Dieci scale e schede, con scheda informativa, istruzioni e riferimento bibliografico |
| Compilazione guidata | Livelli descritti per esteso, punteggio e completamento aggiornati in tempo reale, salvataggio anche parziale |
| Report PDF | Documento A4 con logo STRADE, dati, punteggio, tabella delle voci, osservazioni, firma; anteprima, condivisione o salvataggio |
| Impostazioni | Profilo del valutatore proposto nelle valutazioni e stampato nei report |

## Installazione e aggiornamento

L'app non è sul Play Store: si installa dal file APK pubblicato nella pagina [Releases](https://github.com/angelobottini/STRADE-su-Misura/releases) del progetto su GitHub.

### Prima installazione

1. Dal telefono aprire la pagina Releases e, nell'ultima versione, toccare il file `STRADE-su-Misura-<versione>.apk` (non lo ZIP, che è la versione Windows).
2. Al termine del download aprire il file dalla notifica o dall'app File.
3. Alla richiesta di consentire l'installazione da origini sconosciute, toccare **Impostazioni**, attivare **Consenti da questa origine** per il browser o l'app File e tornare indietro.
4. Toccare **Installa**. Se Play Protect mostra un avviso perché l'app non è del Play Store, scegliere **Installa comunque**.
5. L'icona STRADE su Misura compare nella schermata Home o nel cassetto delle app.

### Aggiornamento

- Scaricare l'APK della nuova versione e aprirlo: Android lo installa sopra la versione precedente, conservando persone, valutazioni e impostazioni.
- L'aggiornamento riesce perché tutte le versioni sono firmate con la stessa chiave.
- Il numero di versione installato è visibile in **Impostazioni**, sotto il nome dell'app.

> **Attenzione**
>
> Non disinstallare l'app prima di aggiornare: si perderebbero i dati.

### Permessi

L'app non chiede permessi speciali: non accede a fotocamera, posizione, contatti o rete. La condivisione e il salvataggio dei PDF usano le finestre standard di Android.

## Primi passi

La barra in basso ha quattro sezioni: **Panoramica**, **Persone**, **Strumenti** e **Impostazioni**. La freccia in alto a sinistra torna sempre alla schermata precedente.

| Sezione | Cosa contiene |
| --- | --- |
| Panoramica | Numero di persone in archivio e di valutazioni salvate, le valutazioni recenti (toccandone una si apre il dettaglio), l'elenco degli strumenti con il pulsante per avviare subito una valutazione |
| Persone | L'archivio con ricerca e il pulsante **Nuova persona** |
| Strumenti | Le schede di tutte le scale disponibili, con **Scheda** (informazioni) e **Nuova valutazione** |
| Impostazioni | Il profilo del valutatore, la versione installata e le informazioni sulla privacy |

### Prima cosa da fare: il profilo del valutatore

1. Aprire **Impostazioni**.
2. Compilare **Nome e cognome**, **Qualifica** (proposta: Terapista occupazionale), **Struttura o studio** e **Luogo (per la firma)**.
3. Lasciare attivo **Includi lo spazio per la firma nel PDF** se si vuole la riga per la firma in fondo al report.
4. Toccare **Salva impostazioni**.

Il nome viene proposto come valutatore in ogni nuova valutazione (resta modificabile caso per caso); struttura, nome e qualifica compaiono nell'intestazione dei report, il luogo accanto alla data nella riga della firma.

### Flusso di lavoro tipico

1. Creare la persona in **Persone** (una sola volta).
2. Scegliere lo strumento in **Strumenti** o dalla scheda della persona con **Nuova valutazione**.
3. Compilare le voci e salvare, anche in più sessioni.
4. Dal dettaglio della valutazione generare il **Report PDF** e condividerlo o salvarlo.

## Archivio persone

Ogni valutazione appartiene a una persona dell'archivio: va quindi creata prima la scheda della persona, una sola volta.

### Creare una persona

1. In **Persone** toccare **Nuova persona** (oppure, avviando una valutazione, **Nuova persona** nella schermata «Per chi è la valutazione?»).
2. Compilare **Nome** e **Cognome** (obbligatori), poi se utili **Data di nascita**, **Sesso**, **Codice identificativo** (numero di cartella o altro riferimento interno) e **Note**.
3. Toccare **Salva persona**.

L'età viene calcolata dalla data di nascita e stampata nel report accanto alla nascita, riferita alla data della valutazione.

**Cercare**: nella barra in alto digitare nome, cognome o codice; l'elenco si filtra mentre si scrive. La **X** cancella la ricerca.

**Scheda della persona**: toccando una persona si apre la scheda con i dati, le note e l'elenco delle valutazioni, dalla più recente. Da qui:

- **Nuova valutazione** apre la scelta dello strumento e poi il modulo già intestato alla persona.
- La matita in alto apre **Modifica persona**; il cestino elimina la scheda.
- Toccando una valutazione si apre il suo dettaglio.

> **Attenzione**
>
> **Eliminare una persona** cancella anche tutte le sue valutazioni, dopo conferma. L'operazione non si può annullare: in caso di dubbio esportare prima i report PDF.

## Strumenti disponibili

La sezione **Strumenti** elenca dieci scale e schede. Per ciascuna il pulsante **Scheda** apre la pagina informativa (descrizione, istruzioni per la compilazione, dati raccolti, elenco delle voci con i punteggi, interpretazione del punteggio, riferimento bibliografico); **Nuova valutazione** chiede per chi è la valutazione e apre il modulo.

| Strumento | Sigla | Ambito | Punteggio | Durata indicativa |
| --- | --- | --- | --- | --- |
| Indice di Barthel Modificato | Barthel | Attività di base della vita quotidiana | 0–100 con sei fasce, da dipendenza totale a indipendenza | 5–10 minuti |
| Osservazione ADL | ADL | Osservazione di un compito (aprassia) | Orientamento, esecuzione e controllo 0–3; indipendenza = il livello più alto dei tre | 15–30 minuti per compito |
| PQRS – Performance Quality Rating Scale | PQRS | Qualità della performance (approccio CO-OP) | Completezza e qualità 1–10; PQRS = media arrotondata per difetto, sei fasce | 5–10 minuti per compito |
| AFMS – Abilità monetarie funzionali | AFMS | Gestione del denaro | Numero di risposte corrette per prova e in totale, letto in percentuale; sezione 7 facoltativa | 45–90 minuti in più sessioni |
| FIM – Misura dell'indipendenza funzionale | FIM | ADL, controllo sfinterico, trasferimenti, locomozione, comunicazione, cognitività | 18 voci su 7 livelli, totale 18–126; subtotali motorio 13–91 e cognitivo 5–35 | 30–45 minuti |
| TAI 4.0 – Trasferimenti autonomi | TAI | Trasferimenti in carrozzina senza aiuto | Percentuale del massimo applicabile per trasferimento; 18 voci | 15–25 minuti per trasferimento |
| TAI 4.0 – Trasferimenti assistiti senza sollevatore | TAI-A | Trasferimenti con l'aiuto di una persona | Come sopra; 21 voci | 15–25 minuti per trasferimento |
| TAI 4.0 – Trasferimenti con sollevatore | TAI-S | Trasferimenti con sollevatore meccanico | Come sopra; 5 voci | 10 minuti per trasferimento |
| Scheda di valutazione regionale | Regionale | Disabilità, accesso ai servizi diurni e residenziali | Classe da A (particolare gravità) a E (lieve), calcolata dalle schede A e B | Dopo osservazione e colloquio |

> **Nota**
>
> Le particolarità di compilazione di ogni strumento sono nella sezione «Guide per strumento».

## Compilare una valutazione

Il modulo è uguale per tutti gli strumenti: in alto il riquadro del punteggio, poi i dati della valutazione, le voci da compilare e le osservazioni finali. Il pulsante **Salva valutazione** resta sempre visibile in fondo.

**Riquadro del punteggio**: mostra il totale e il massimo, quante voci sono compilate sul totale richiesto e, quando lo strumento le prevede, la fascia interpretativa o la percentuale. Si aggiorna a ogni risposta.

### Dati della valutazione

- **Data della valutazione** (oggi, modificabile dal calendario), **Valutatore** (proposto dal profilo) e **Contesto** (domicilio, reparto, ambulatorio…).
- Campi propri dello strumento, se previsti: ad esempio il compito osservato nella PQRS, le modalità di locomozione nella FIM, il **tipo di carrozzina** nella TAI (pulsanti a scelta) o il codice fiscale nella scheda regionale.

### Tipi di risposta

| Tipo | Come si compila | Dove si trova |
| --- | --- | --- |
| Livelli | Toccare l'opzione: ogni livello ha titolo, descrizione e punteggio nel cerchietto a destra | Barthel, ADL, PQRS, FIM, scheda regionale, TAI |
| Alternativa | Due pulsanti in alto nella voce scelgono tra due versioni della stessa attività (deambulazione o carrozzina nel Barthel) | Barthel |
| Albero decisionale | **Guida con l'albero decisionale** pone le domande una alla volta e seleziona il livello raggiunto, che resta modificabile; la voce indica se il livello viene dalla guida | FIM |
| Conteggio | Pulsanti **−** e **+** o cursore per il numero di risposte corrette; **Segna come non valutata** azzera | AFMS |
| Sotto-prove | Per ogni sotto-prova i pulsanti ✓ (riuscita) e ✗ (non riuscita); il conteggio si aggiorna da solo e la prova è completa quando sono tutte segnate | AFMS |
| Scelta multipla | Caselle di spunta: si segnano tutte le voci che si applicano e i punteggi si sommano | TAI |
| Scelta per gruppo con foto | Prima il chip della superficie, poi le foto delle posizioni della mano: toccare una foto la seleziona; scegliendo un'altra superficie la selezione precedente si azzera | TAI |
| Non applicabile | Opzioni contrassegnate **n.a.**: la voce risulta compilata ma esce dal punteggio massimo | TAI |

**Mostra definizione** apre, dove presente, il testo esteso di ciò che si osserva. Le voci con **Note** o **Commenti** hanno un campo di testo proprio, stampato nel report sotto il livello rilevato.

**Osservazioni**: il campo in fondo raccoglie note cliniche, ausili usati e condizioni della valutazione.

### Salvare

- Si può salvare in qualunque momento. Se mancano voci, la barra in basso avvisa «Mancano N voci: la valutazione verrà salvata come parziale» e negli elenchi il punteggio porta la dicitura **parziale**.
- Una valutazione parziale si completa in seguito con **Modifica**: comodo per l'AFMS, che si somministra in più sessioni.
- Uscendo con modifiche non salvate l'app chiede conferma.

## Guide per strumento

Ogni strumento ha le proprie istruzioni complete nella sua **Scheda**; qui le particolarità che cambiano il modo di compilare.

### Indice di Barthel Modificato

- Dieci attività con cinque livelli ponderati (i punti variano da attività ad attività).
- Per la mobilità scegliere con i due pulsanti tra **Deambulazione** e **Carrozzina**: si compila solo l'alternativa in uso.
- Il totale 0–100 ricade in una fascia (dipendenza totale, grave, moderata, lieve, minima, indipendenza), visibile solo a valutazione completa.

### Osservazione ADL

- Descrivere il compito osservato nel campo dedicato, poi valutare da 0 a 3 le tre fasi: orientamento, esecuzione e controllo.
- L'**indipendenza** è calcolata automaticamente come il livello più alto tra le tre fasi e corrisponde a una fascia (autonomia completa, intervento verbale, intervento fisico, dipendenza).
- Un solo campo di osservazioni generali, stampato per esteso nel report.

### PQRS

- Descrivere il compito, poi inserire **Completezza** e **Qualità** da 1 a 10.
- Il punteggio PQRS è la media dei due valori arrotondata per difetto: i due valori e il risultato sono evidenziati nel modulo, nel dettaglio e nel report, con la formula del calcolo.

### AFMS

- Dieci prove a conteggio: dove c'è l'elenco delle sotto-prove segnare ciascuna con ✓ o ✗ durante la somministrazione; altrove inserire il numero di risposte corrette.
- La **sezione 7** (fatture e assegni) è facoltativa: se non somministrata non conta né come mancante né nel massimo.
- Il totale si legge come numero e percentuale di risposte corrette; i campi **Sessioni** e **Adattamenti** registrano come è avvenuta la somministrazione.

### FIM

- Diciotto voci (A–R) su sette livelli, in due aree con subtotale: motoria (13–91) e cognitiva (5–35).
- Per ogni voce il pulsante **Guida con l'albero decisionale** pone le domande del manuale (Sì/No, oppure il numero di episodi di incontinenza) e seleziona il livello; **Vescica** e **Alvo** percorrono due alberi e registrano il più basso.
- Compilare i campi per la modalità di locomozione, comprensione ed espressione. Il totale ha senso solo con tutte le 18 voci.

### TAI 4.0 (tre versioni)

- Scegliere prima il **tipo di carrozzina** nei dati: alcuni punteggi cambiano tra manuale ed elettrica e finché manca il modulo lo segnala in rosso. Nella versione autonoma indicare anche se si usa la tavoletta.
- Le voci seguono le fasi del modulo; ogni trasferimento è un'area a sé. Il **Trasferimento 2** (ritorno) è facoltativo: se compilato, il totale è la media delle percentuali dei due trasferimenti.
- **Mano di spinta** e **mano guida**: scegliere la superficie con i chip (carrozzina, tavoletta, superficie rigida o morbida, sedia, bagno) e toccare tutte le posizioni usate; il punteggio della voce è la media. **Mano in grembo** rende la voce non applicabile.
- Le voci «segnare tutte le voci che si applicano» sommano anche i punteggi negativi senza scendere sotto 0. Le opzioni **n.a.** (niente braccioli, niente freni, nessun appoggio…) escono dal massimo.
- Le misure sono in centimetri con il valore originale in pollici tra parentesi.

### Scheda di valutazione regionale

- Compilare i dati della persona e del servizio (codice fiscale, comune, servizio in cui la persona è già inserita).
- **Scheda A**: otto categorie ICF con il qualificatore di capacità da 0 a 4. **Scheda B**: problemi di particolare gravità con 1 (no), 2 (sì senza interventi speciali) o 3 (sì con interventi speciali); le voci **Altro** sono facoltative e vanno specificate nella nota.
- La **classe** da A a E si calcola da sola con i criteri della scheda ed è mostrata come lettera, con il criterio che l'ha determinata; le considerazioni del valutatore vanno nelle osservazioni.

## Dettaglio, modifica ed eliminazione

Toccando una valutazione (in Panoramica o nella scheda della persona) si apre il **dettaglio**: punteggio con fascia o percentuale e barra, dati della valutazione, campi dello strumento, elenco delle voci con il livello rilevato, le note per voce e le osservazioni, data dell'ultima modifica.

- **Modifica** (matita in alto o pulsante in basso) riapre il modulo con le risposte salvate: serve per completare una valutazione parziale o correggere un errore. La data dell'ultima modifica si aggiorna.
- **Report PDF** genera l'anteprima del report (sezione seguente).
- Il **cestino** elimina la valutazione dopo conferma; l'operazione non si può annullare.
- Toccando il riquadro della persona si passa alla sua scheda.

Negli elenchi ogni valutazione mostra la sigla dello strumento, la data, il valutatore e il punteggio: colorato secondo la fascia se completa, grigio con la dicitura **parziale** se mancano voci.

## Report PDF

Dal dettaglio di una valutazione, **Report PDF** genera un documento A4 e ne mostra l'anteprima pagina per pagina. In basso compaiono due pulsanti:

- **Condividi** apre il menu di condivisione di Android: e-mail, WhatsApp, Drive, stampa tramite il servizio di stampa del telefono o qualunque altra app installata.
- **Salva** apre la finestra di Android per scegliere la cartella (per esempio Download) e salva il file con il nome proposto `Sigla_Cognome_Nome_data.pdf`.

### Contenuto del report

| Parte | Contenuto |
| --- | --- |
| Intestazione | Logo STRADE, dicitura «Report di valutazione», struttura, nome e qualifica del valutatore dalle Impostazioni |
| Schede | Persona (nome, nascita ed età, sesso, codice) e Valutazione (data, valutatore, contesto, campi dello strumento) |
| Punteggio | Totale e massimo, fascia interpretativa con descrizione o percentuale, barra, subtotali delle aree e valori evidenziati con la formula di calcolo |
| Voci | Tabella con numero, titolo, livello rilevato e descrizione, note per voce, punti su massimo; righe di sezione e subtotali |
| Osservazioni e legenda | Il testo delle osservazioni e, per gli strumenti con fasce, la legenda con la fascia corrente evidenziata |
| Chiusura | Riferimento bibliografico, luogo e data con la riga per la firma (se attiva nelle Impostazioni), piè di pagina con numero di pagina e avviso di riservatezza |

Il report sta in una sola pagina quando possibile: l'app prova tre densità di impaginazione e usa la prima che basta; gli strumenti lunghi (FIM, TAI) vanno su più pagine con l'intestazione della tabella ripetuta. Le valutazioni parziali si possono esportare: il report lo indica nel pannello del punteggio.

> **Nota**
>
> I PDF generati per l'anteprima restano nella memoria temporanea dell'app finché non vengono salvati o condivisi; Android può eliminarli quando serve spazio.

## Dati e privacy

Persone, valutazioni e impostazioni sono salvate in un database locale nella memoria privata dell'app, non leggibile da altre app. L'app non usa la rete: nessun dato viene inviato a server esterni e non esistono account.

### Copie di sicurezza

- Se sul telefono è attivo il **backup di Google** (Impostazioni di Android → Google → Backup), il database e le impostazioni dell'app sono inclusi e vengono ripristinati sul nuovo telefono al primo avvio, o trasferiti con la procedura di passaggio da un dispositivo all'altro.
- Non esiste ancora un'esportazione dell'intero archivio dall'app: per conservare una valutazione fuori dal telefono salvare o condividere il suo report PDF.

> **Attenzione**
>
> **Disinstallare l'app cancella tutti i dati** (salvo il ripristino dal backup di Google). Per aggiornare non serve disinstallare.

### Dati personali e sanitari

- Il telefono dovrebbe essere protetto da blocco schermo e, se contiene dati di persone assistite, usato secondo le regole della struttura e la normativa sulla protezione dei dati.
- I report PDF contengono dati personali e sanitari: ogni pagina porta l'avviso di riservatezza. Condividerli solo con canali ammessi dalla struttura.
- Il **Codice identificativo** permette, se si preferisce, di archiviare le persone con un riferimento interno al posto di dati ulteriori.

*Il carattere tipografico dei report è Roboto (Google, licenza Apache 2.0); le immagini della TAI sono tratte dal modulo originale.*

## Risoluzione dei problemi e domande frequenti

| Situazione | Cosa fare |
| --- | --- |
| «App non installata» o «Pacchetto in conflitto» durante l'aggiornamento | L'APK scaricato non è quello di release firmato (per esempio è la versione debug) oppure è di una versione più vecchia. Scaricare l'APK della Release più recente senza il suffisso `-debug`. |
| Android blocca l'installazione | Consentire l'installazione da origini sconosciute per il browser o l'app File usata per aprire l'APK, poi riaprire il file. |
| Play Protect avvisa che l'app non è verificata | È normale per le app fuori dal Play Store: scegliere **Installa comunque**. |
| Il punteggio mostra «parziale» | Mancano voci: aprire la valutazione con **Modifica** e compilarle. Il riquadro in alto dice quante ne mancano. |
| Nella TAI alcune voci hanno un avviso rosso | Non è stato scelto il tipo di carrozzina nei dati della valutazione: selezionarlo e i punteggi si aggiornano. |
| Il report non si genera («Impossibile generare il PDF») | Toccare **Riprova**; se persiste, liberare spazio sul telefono e riprovare. Le risposte restano salvate. |
| Il PDF salvato non si trova | È nella cartella scelta nella finestra di salvataggio (di solito Download), con nome `Sigla_Cognome_Nome_data.pdf`. |
| Ho eliminato una persona per errore | Non c'è recupero nell'app; i dati tornano solo da un backup di Google eseguito prima dell'eliminazione. |
| Il nome del valutatore è sbagliato in un report | Correggerlo nel campo **Valutatore** della valutazione (vale per quel report) o in **Impostazioni** (vale per le nuove valutazioni). |

### Domande frequenti

- **Posso usare l'app su più dispositivi con lo stesso archivio?** No: ogni dispositivo ha il proprio archivio locale. I report PDF si condividono liberamente.
- **Serve Internet?** Solo per scaricare l'APK. Tutto il resto funziona offline.
- **Posso modificare una valutazione dopo averla esportata?** Sì: il PDF già condiviso non cambia, basta generarne uno nuovo.
- **Le scale sono quelle originali?** Le voci, i livelli e i punteggi sono trascritti dai manuali e dai moduli originali, in italiano; ogni Scheda riporta il riferimento bibliografico. Le interpretazioni cliniche restano al valutatore.
- **Come segnalo un problema o chiedo una nuova scala?** Dalla pagina del progetto su GitHub (sezione Issues) o direttamente all'autore.

> **Versione Windows**
>
> La stessa app esiste come programma portatile per Windows 11 (ZIP nella pagina Releases), con archivio separato nella cartella `Dati` accanto al programma. Le valutazioni non si sincronizzano tra telefono e computer.
