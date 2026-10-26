# Gestione spese

App Android personale per il budgeting "a vasetti". Requisiti in [SPEC.md](SPEC.md), regole del progetto in [CLAUDE.md](CLAUDE.md).

## Struttura

| Modulo | Contenuto |
|---|---|
| `core` | Kotlin puro, senza Android: modello, regole di calcolo (`BudgetMath`), formattazione, formato di backup. Tutti i test unitari della logica stanno qui. |
| `app` | Applicazione Android: Compose + Material 3, Room (`data/db`), DataStore (`data/settings`), repository unico (`data/SpeseRepository`). |

Lo schema del database Room è esportato in `app/schemas/` e va tenuto nel repository: serve per le migrazioni.

## Comandi

```bash
./gradlew test             # test unitari (core + app)
./gradlew assembleRelease  # APK di release (firmato se ci sono le variabili SIGNING_*)
```

La build per il telefono la fa GitHub Actions: vedi [docs/FIRMA.md](docs/FIRMA.md).
