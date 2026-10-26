# Gestione spese — specifica per l'app Android nativa

App personale di budgeting "a vasetti" (envelope budgeting). Lo stipendio viene smistato in budget con quote fisse, ogni budget conserva il saldo da un mese all'altro e si svuota con le spese.

Questa specifica descrive il comportamento del prototipo web già in uso (`reference/prototipo-web.html`). In caso di dubbio su un dettaglio, il comportamento del prototipo fa fede.

---

## 1. Vincoli

- **Solo Android**, un solo utente, nessun account.
- **Completamente offline**: tutti i dati vivono sul telefono. L'app non dichiara il permesso `INTERNET`.
- **Lingua dell'interfaccia**: italiano. Valuta: euro, formato `it-IT` (es. `1.234,56 €`).
- **Importi sempre in centesimi** (`Long`). Mai `Double` per i soldi.
- **Date** come `java.time.LocalDate`, salvate come stringa ISO `yyyy-MM-dd`.

## 2. Stack consigliato

| Area | Scelta |
|---|---|
| Linguaggio | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Dati | Room (SQLite) per budget, movimenti e spese rapide; DataStore per le impostazioni |
| Architettura | ViewModel + StateFlow, repository unico, logica di calcolo in classi pure testabili senza Android |
| Backup | kotlinx.serialization (JSON) + Storage Access Framework (`CreateDocument` / `OpenDocument`) |
| Build | Gradle (Kotlin DSL), compilazione su GitHub Actions |
| Package | `it.elia.gestionespese` |
| minSdk | 26 (da confermare con la versione del telefono di Elia) |

Usare le versioni stabili correnti di AGP, Kotlin, Compose BOM e Room al momento della creazione del progetto.

## 3. Modello dei dati

### Category (budget)
| Campo | Tipo | Note |
|---|---|---|
| id | String | generato lato client, stabile (serve per import/export) |
| name | String | obbligatorio |
| kind | `SPESA` \| `OBIETTIVO` | |
| fill | `AUTO` \| `MANUALE` | significativo solo per `OBIETTIVO` |
| quota | Long | centesimi a stipendio, ≥ 0 |
| target | Long? | soglia, solo per `OBIETTIVO` |
| deadline | LocalDate? | scadenza, solo per `OBIETTIVO` |
| order | Int | ordine di riempimento e di visualizzazione |

### Shortcut (spesa rapida)
`id`, `name`, `amount` (Long > 0), `categoryId`, `order`.

### Movement
| Campo | Tipo | Note |
|---|---|---|
| id | String | |
| kind | `STIPENDIO` \| `ENTRATA` \| `SPESA` \| `ASSEGNA` \| `RETTIFICA` \| `PRELEVA` | |
| date | LocalDate | |
| amount | Long | vedi tabella sotto; per `RETTIFICA` ha segno |
| categoryId | String? | solo `SPESA` |
| note | String? | |
| total | Long? | solo `RETTIFICA`: totale conto impostato |
| createdAt | Long | epoch millis, per ordinare movimenti dello stesso giorno |

### MovementAllocation
`movementId`, `categoryId`, `amount` (Long **con segno**). Una riga per ogni budget toccato da `STIPENDIO`, `ENTRATA`, `ASSEGNA`, `PRELEVA`.

**Niente foreign key con cascade su `categoryId`**: se un budget viene eliminato, i suoi movimenti restano nello storico e vengono mostrati come "Budget eliminato".

### Settings (DataStore)
`payday: Int` (1–31, default 27): giorno del mese in cui arriva lo stipendio.

### Significato dei movimenti

| kind | amount | allocations | Effetto |
|---|---|---|---|
| STIPENDIO | importo > 0 | quote ≥ 0 verso i budget | entra sul conto; il non allocato va in "Da assegnare" |
| ENTRATA | importo > 0 | opzionali ≥ 0 | come sopra, di default tutto in "Da assegnare" |
| SPESA | importo > 0 | nessuna | esce dal budget `categoryId` |
| ASSEGNA | 0 | importi > 0 | da "Da assegnare" ai budget |
| PRELEVA | 0 | `{origine: -x}` e opzionalmente `{destinazione: +x}` | toglie soldi da un budget verso "Da assegnare" o verso un altro budget. Non è una spesa |
| RETTIFICA | differenza con segno | nessuna | corregge il totale conto; la differenza entra o esce da "Da assegnare" |

## 4. Regole di calcolo

Tutte queste funzioni devono stare in una classe pura (es. `BudgetMath`) con test unitari.

### 4.1 Saldi
```
per ogni movimento (eventualmente fino a un mese incluso):
  se kind ∈ {STIPENDIO, ENTRATA, RETTIFICA}: daAssegnare += amount
  per ogni allocation (cat, v): saldo[cat] += v; daAssegnare -= v
  se kind == SPESA: saldo[categoryId] -= amount

totaleNeiBudget = somma di saldo[*]   (inclusi eventuali budget eliminati con saldo ≠ 0)
totaleConto     = totaleNeiBudget + daAssegnare
```
Un saldo può essere negativo ("In rosso").

### 4.2 Prossimi stipendi
```
nextPaydays(limit, until?):
  meseCorrente = mese di oggi
  se esiste già un movimento STIPENDIO nel mese corrente → si parte dal mese successivo
  altrimenti → si parte dal mese corrente
  per ogni mese: data = (anno, mese, min(payday, ultimo giorno del mese))
  ci si ferma a `limit` date o quando data > until

salariesUntil(deadline) = numero di nextPaydays(∞, until = deadline)
```

### 4.3 Piano obiettivo (solo OBIETTIVO con target > 0 e deadline)
```
miss    = max(0, target - saldo)
n       = salariesUntil(deadline)
quota   = 0 se miss == 0; ceil(miss / n) se n > 0; altrimenti miss
overdue = (n == 0 && miss > 0)
```

### 4.4 Quota effettiva a stipendio
- `SPESA` → `quota`
- `OBIETTIVO` + `MANUALE` → `quota` (decisa dall'utente; 0 = lo riempie a mano)
- `OBIETTIVO` + `AUTO` con target e deadline → `piano.quota`
- `OBIETTIVO` + `AUTO` senza deadline → `quota`

### 4.5 Smistamento dello stipendio
Quando si apre il modulo "Stipendio" e l'utente scrive l'importo, si propongono le quote **in ordine di `order`**:
```
rimasto = importo
per ogni budget in ordine: proposta = min(quotaEffettiva, max(0, rimasto)); rimasto -= proposta
"Da assegnare" = importo - somma proposte
```
Le proposte sono modificabili a mano prima di confermare. Validazioni: importo > 0, nessuna quota negativa, somma quote ≤ importo. Data proposta: il giorno `payday` del mese corrente.

### 4.6 Livello del vasetto (0–100%)
- **OBIETTIVO con target**: `saldo / target`.
- **Tutti gli altri**: `pieno = max(0, saldo a fine mese precedente) + somma delle allocation positive ricevute nel mese corrente`; `livello = saldo / pieno`. Se `pieno ≤ 0`, livello 0 con testo "Ancora vuoto questo mese" (o "Sotto zero" se il saldo è negativo).
- Valore limitato tra 0 e 100. Il disegno usa 10 strati di monete: `strati = round(livello / 10)`.

### 4.7 Tempo per riempire un obiettivo
Con `q` = quota effettiva > 0 e `miss` > 0:
```
need   = ceil(miss / q)
quando = nextPaydays(need)[need - 1]
late   = deadline != null && quando > deadline   → mostrare in rosso "dopo la scadenza, servono {piano.quota} a stipendio"
```
Se `q == 0` e c'è un piano: "Quota a zero: per arrivarci servono {piano.quota} a stipendio".

### 4.8 Riepilogo mensile
- **Entrate del mese** = somma `amount` di STIPENDIO + ENTRATA.
- **Spese del mese** = somma `amount` di SPESA.
- RETTIFICA e PRELEVA non sono né entrate né spese.
- Per budget: "Entrato" = somma delle allocation positive del mese, escluso PRELEVA; "Speso" = somma delle SPESA; "Saldo a fine mese" = saldo calcolato fino a quel mese incluso.
- Grafico: ultimi 12 mesi fino al mese corrente, a partire dal primo mese con dati; barre affiancate entrate e spese per mese.

### 4.9 Eliminazione di un budget
Con conferma esplicita. Mostrare saldo, numero di movimenti collegati e numero di spese rapide collegate.
- Saldo > 0: l'utente sceglie la destinazione ("Da assegnare" o un altro budget) → movimento PRELEVA `{budget: -saldo, dest: +saldo}`, nota "Chiusura budget {nome}".
- Saldo < 0: PRELEVA `{budget: -saldo}` (positivo, coperto da "Da assegnare").
- Poi si eliminano le spese rapide collegate e il budget. I movimenti restano.

### 4.10 Rettifica del totale conto ("Reimposta totale iniziale")
L'utente scrive il totale reale. `diff = nuovoTotale - totaleConto`. Se `diff ≠ 0`, movimento RETTIFICA con `amount = diff`, `total = nuovoTotale`.

## 5. Schermate

### Struttura generale
- **Testata**: etichetta "GESTIONE SPESE"; tre righe separate da una linea orizzontale, senza riquadri: **Totale conto** (grande), **Totale nei budget**, **Da assegnare** (verde se > 0, rosso se < 0). Sotto, pulsante testuale "Reimposta totale iniziale".
- **Azioni in alto**: "+ Stipendio" (primario) e "Assegna dal Da assegnare" (attivo solo se Da assegnare > 0).
- **Due pulsanti flottanti rotondi**, affiancati in orizzontale in basso a destra, solo simbolo: **+** (entrata extra, bordo verde) e **−** (uscita/spesa, pieno). Stanno sopra la barra di navigazione.
- **Barra di navigazione in basso**: Budget · Movimenti · Riepilogo · Impostazioni · **Spese rapide**. Spese rapide ha un colore diverso (arancio scuro) e non è una scheda: apre un bottom sheet.
- Tutti i moduli si aprono come **bottom sheet**.

### Budget
- Due gruppi, **Spese** e **Obiettivi**, ciascuno con il totale accanto al titolo.
- Ogni budget è una riga a tutta larghezza (una per riga su telefono), non cliccabile:
  - a sinistra il **vasetto** (vetro, coperchio, monete; vedi `reference/prototipo-web.html`, funzione `jarSvg`);
  - al centro, dall'alto: nome; saldo in grande (rosso se negativo); riga del livello ("Restano 90,00 € di 150,00 € · 60%" oppure "45% di 800,00 €"); righe informative (quota a stipendio, scadenza, stipendi prima della scadenza, "Pieno con N stipendi, il {data}", avvisi in rosso);
  - a destra, in colonna: badge (Spese blu, Obiettivo ambra, In rosso, Raggiunto), sotto il pulsante **⇄** (fondo verde: togli/svuota/sposta) e il pulsante **☰** (fondo blu: apre Movimenti filtrati su quel budget).
- Obiettivi: sfondo leggermente ambrato.
- Stato vuoto: invito a creare il primo budget, con opzione "Parti da un esempio modificabile".

### Moduli
- **Stipendio**: importo, data, elenco budget con quote proposte modificabili e sottotitolo (quota / "obiettivo · X calcolati" / "obiettivo manuale · quota X"), riga finale "Resta in Da assegnare".
- **Entrata extra** (+): importo, data, descrizione, quote opzionali a zero.
- **Spesa** (−): importo, data, budget (con saldo accanto), descrizione; avviso se il budget andrà in rosso.
- **Assegna**: importi per budget, "Resta da assegnare", validazione ≤ disponibile.
- **Togli da un budget** (⇄): saldo attuale, importo, pulsante "Svuota: tutto il saldo", destinazione (Da assegnare o altro budget), motivo facoltativo. Se saldo ≤ 0: solo messaggio.
- **Reimposta totale iniziale**: totale reale, data, totale attuale e differenza in tempo reale.
- **Spese rapide** (bottom sheet): lista di righe grandi (nome, importo, budget). Tocco = registra SPESA con data di oggi, chiude il sheet e mostra Snackbar "… registrata" con **Annulla**. Pulsante ✎ = apre il modulo Spesa precompilato.

### Movimenti
- Filtri: **Budget** (tutti / uno) e **Mese** (tutti / uno). Quando il filtro budget è attivo: chip "Solo {nome}" + "Mostra tutti".
- Il filtro budget include ogni movimento con `categoryId` o allocation su quel budget.
- Lista raggruppata per mese, dal più recente. Ogni riga: data, titolo (nota o tipo), sottotitolo (budget; ripartizione delle quote; "Origine → Destinazione" per PRELEVA; "Totale impostato a X" per RETTIFICA), importo (+ verde per entrate).
- Eliminazione a due tocchi ("Elimina" → "Conferma eliminazione", torna normale dopo 4 s).

### Riepilogo
- Grafico a barre entrate/spese degli ultimi 12 mesi con legenda e valori al tocco; tabella dei valori espandibile.
- Dettaglio del mese selezionato: Entrate, Spese, Differenza; tabella per budget (Entrato, Speso, Saldo a fine mese) con totale.

### Impostazioni
- **Stipendio**: giorno di arrivo (salvataggio immediato, Snackbar di conferma).
- **Budget**: "+ Nuovo budget" apre un modulo compatto in cima (bordo tratteggiato verde) con Crea / Annulla; nulla viene salvato prima di Crea.
- Ogni budget esistente: intestazione con frecce ↑↓ (riordino salvato subito), nome salvato e badge; campi Nome, Tipo, Riempimento (solo obiettivi), Quota a stipendio (sostituita da "X · auto" per obiettivo AUTO con soglia e scadenza), Soglia ed Entro il (solo obiettivi).
- Le modifiche sono **bozze**: la riga si colora quando ci sono modifiche, **Salva** (verde) si attiva; Snackbar "Budget "{nome}" salvato". **Elimina** (rosso) apre la conferma del punto 4.9.
- **Spese rapide**: stessa logica (modulo compatto per la nuova, Salva verde, Elimina rosso).
- **Backup**: "Esporta backup" e "Importa backup" (vedi sezione 6).

### Palette (dal prototipo)
| Token | Chiaro | Scuro | Uso |
|---|---|---|---|
| accent | #0f6b5c | #4fbfa6 | azioni principali |
| spend | #2d5ea3 | #8db4ec | badge Spese, pulsante ☰ |
| goal | #8a5a00 | #e3b55a | badge Obiettivo, valori evidenziati |
| neg | #b3261e | #f08a80 | in rosso, Elimina |
| ok | #25804f | #4cb87e | Salva |
| quick | #b4531a | #e08a4a | Spese rapide |
| in / out (grafico) | #1F8A70 / #C46F1C | #2FA583 / #CF7F30 | entrate / spese |

Tema chiaro e scuro, entrambi curati.

## 6. Backup e persistenza

- Formato: vedi `reference/backup-2026-10-26.json` (dati reali attuali di Elia, da importare al primo avvio). Struttura:
  ```json
  { "format": "gestione-spese-backup", "version": 1, "exportedAt": "...", "currency": "EUR", "amountsIn": "cents",
    "settings": { "payday": 10 },
    "categories": [ { "id", "name", "kind": "spesa|obiettivo", "fill": "auto|manuale", "quota", "target", "deadline", "order" } ],
    "shortcuts":  [ { "id", "name", "amount", "categoryId", "order" } ],
    "movements":  [ { "id", "kind": "stipendio|entrata|spesa|assegna|rettifica|preleva", "date", "amount", "alloc": { "<categoryId>": cents }, "categoryId", "note", "total", "ts" } ] }
  ```
  `kind` è minuscolo nel file; `alloc` è una mappa da categoryId a centesimi con segno; `ts` corrisponde a `createdAt`.
- **Esporta**: scrive il file JSON dove sceglie l'utente (SAF), nome `backup-gestione-spese-yyyy-MM-dd.json`.
- **Importa**: legge il file, mostra un riepilogo (n. budget, spese rapide, movimenti) e chiede conferma; **sostituisce** tutti i dati in una transazione.
- **Primo avvio** senza dati: proporre "Importa backup" oltre a "Crea il primo budget".
- Abilitare il backup automatico di Android (`allowBackup` + regole `dataExtractionRules` che includano database e DataStore).

## 7. Build e installazione

- Workflow GitHub Actions su push a `main`: setup JDK, `./gradlew test assembleRelease`, upload dell'APK come artifact scaricabile.
- **Firma**: creare una volta un keystore di release e salvarlo nei secret del repository (keystore in base64, password, alias). Tutte le build vanno firmate con quella chiave. **Motivo**: se ogni build usa una chiave diversa (es. la debug key generata dal runner), Android rifiuta l'aggiornamento e bisogna disinstallare l'app, **perdendo i dati**.
- `versionCode` incrementale (es. dal numero di run del workflow).

## 8. Piano di lavoro

1. **Fondamenta**: progetto Gradle, Room (entità, DAO, migrazioni con schema esportato), DataStore, `BudgetMath` con i test della sezione 9, workflow GitHub Actions con firma.
2. **Budget**: testata, lista con vasetti, moduli Stipendio / Entrata / Spesa / Assegna / Togli / Rettifica, pulsanti flottanti, barra in basso, Spese rapide.
3. **Movimenti, Riepilogo, Impostazioni** completi.
4. **Backup**: export/import + import dei dati reali da `reference/backup-2026-10-26.json`.
5. Dopo, se richiesto: widget home (Glance) con i vasetti, notifica il giorno dello stipendio (WorkManager), sblocco biometrico.

## 9. Casi di test minimi per `BudgetMath`

Con `oggi = 2026-10-26`, `payday = 10`, stipendio di ottobre già registrato:
- `nextPaydays(3)` → 2026-11-10, 2026-12-10, 2027-01-10.
- `salariesUntil(2026-12-11)` → 2; `salariesUntil(2026-12-05)` → 1.
- Obiettivo target 80000, saldo 0, deadline 2026-12-11 → piano `n=2`, `quota=40000`.
- Obiettivo MANUALE quota 80000, stesso target → "Pieno con 1 stipendio, il 10 nov 2026".
- Obiettivo MANUALE quota 30000 → need 3, quando 2027-01-10 > deadline → `late = true`.

Con stipendio di ottobre **non** registrato e `oggi = 2026-10-26`: `nextPaydays(1)` → 2026-10-10 (stipendio del mese ancora da registrare).

Smistamento: budget in ordine con quote 15000, 6000, 5000; stipendio 20000 → proposte 15000, 5000, 0; Da assegnare 0.

Saldi: con i movimenti di `reference/backup-2026-10-26.json` → Svago 15000, Benzina 6000, Colazione 5000, Tasse uni 0; Da assegnare 139030; Totale conto 165030.

Livello: budget con saldo a fine mese precedente 2000, allocation nel mese 6000, saldo attuale 4000 → pieno 8000, livello 50%, 5 strati.
