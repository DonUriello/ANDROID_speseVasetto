# Firma delle build di release

Android installa un aggiornamento solo se l'APK è firmato con **la stessa chiave** della versione già installata.
Se la chiave cambia bisogna disinstallare l'app, e disinstallandola **si perdono i dati**.
Per questo tutte le build di GitHub Actions vanno firmate con un'unica chiave, conservata nei secret del repository.

## 1. Crea il keystore (una volta sola)

Sul tuo computer, con un JDK installato, in una cartella **fuori** dal repository.
Il comando va scritto tutto su una riga (su Windows funziona sia in cmd sia in PowerShell):

```
keytool -genkeypair -v -storetype PKCS12 -keystore gestione-spese-release.jks -alias gestione-spese -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Elia"
```

Usa una password solo con lettere e numeri, la stessa per keystore e chiave, e annotala in un gestore di password.
Verifica che il keystore si apra (deve comparire `gestione-spese ... PrivateKeyEntry`):

```
keytool -list -keystore gestione-spese-release.jks
```

**Conserva il file `.jks` e la password in un posto sicuro** (gestore di password, chiavetta). Se li perdi, non potrai più
aggiornare l'app senza disinstallarla. Non metterli mai nel repository (`.gitignore` li esclude già).

## 2. Aggiungi i secret su GitHub

Nel repository: *Settings → Secrets and variables → Actions → New repository secret*.

| Secret | Valore |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | il keystore in base64 (vedi sotto) |
| `SIGNING_STORE_PASSWORD` | password del keystore |
| `SIGNING_KEY_ALIAS` | `gestione-spese` |
| `SIGNING_KEY_PASSWORD` | password della chiave |

Un secret alla volta: *Name* è il nome nella prima colonna, *Secret* il valore.

Per ottenere il base64 del keystore:

- **Windows, Prompt dei comandi** (`C:\...>`):
  `powershell -NoProfile -Command "[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\percorso\gestione-spese-release.jks')) | Set-Clipboard"`
- **Windows, PowerShell** (`PS C:\...>`):
  `[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\percorso\gestione-spese-release.jks")) | Set-Clipboard`
- **macOS**: `base64 -i gestione-spese-release.jks | pbcopy`
- **Linux**: `base64 -w0 gestione-spese-release.jks`

Il valore giusto è una sola riga lunga che inizia con `MII`. Non usare `certutil -encodehex`, `xxd` o `hexdump`:
producono esadecimale (`fcfc 2dd7 ...`), che non funziona.

## 3. Scarica e installa l'APK

Ogni push esegue il workflow **Build** (`.github/workflows/build.yml`): test, build di release firmata, controlli.
Nella pagina dell'esecuzione, sezione *Artifacts*, trovi `gestione-spese-<numero>`: è uno zip con `app-release.apk`.
Scompattalo sul telefono e installalo (la prima volta Android chiede di consentire l'installazione da quella app).

Il `versionCode` è il numero di esecuzione del workflow, quindi ogni build è un aggiornamento della precedente.

Senza i secret il workflow compila comunque e fa girare i test, ma non pubblica l'APK; su `main` fallisce,
per non dimenticare la configurazione.
