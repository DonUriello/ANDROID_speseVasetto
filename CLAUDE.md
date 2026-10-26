# Gestione spese (Android)

App Android nativa personale per la gestione delle spese a budget/vasetti. Utente unico: Elia (sviluppatore Java/Spring Boot, conosce bene Java, meno Android).

## Da leggere prima di tutto
- `SPEC.md`: requisiti, modello dei dati, regole di calcolo, schermate, piano di lavoro e casi di test.
- `reference/prototipo-web.html`: prototipo web funzionante. Per qualsiasi dettaglio di comportamento non coperto da SPEC.md, il prototipo fa fede.
- `reference/backup-2026-10-26.json`: dati reali attuali, da importare con la funzione di backup.

## Regole del progetto
- Kotlin, Jetpack Compose, Material 3, Room, DataStore.
- Importi sempre in centesimi `Long`; mai `Double` per i soldi.
- Nessun permesso `INTERNET`: l'app è offline e i dati restano sul telefono.
- Interfaccia in italiano.
- La logica di calcolo sta in classi pure con test unitari (`./gradlew test`); i casi minimi sono in SPEC.md §9.
- Le build di release sono firmate sempre con la stessa chiave (secret su GitHub), altrimenti l'aggiornamento cancella i dati.
- Elia compila tramite GitHub Actions e installa l'APK sul telefono: tieni il workflow funzionante a ogni modifica.
- Lavora per le tappe di SPEC.md §8, una alla volta, con commit piccoli e descrittivi.
