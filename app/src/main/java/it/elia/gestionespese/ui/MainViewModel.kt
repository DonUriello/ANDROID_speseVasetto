package it.elia.gestionespese.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.elia.gestionespese.GestioneSpeseApp
import it.elia.gestionespese.core.Balances
import it.elia.gestionespese.core.BudgetMath
import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.core.Ids
import it.elia.gestionespese.core.Movement
import it.elia.gestionespese.core.MovementKind
import it.elia.gestionespese.core.SampleData
import it.elia.gestionespese.core.Shortcut
import it.elia.gestionespese.core.backup.BackupCodec
import it.elia.gestionespese.core.backup.BackupData
import it.elia.gestionespese.core.backup.BackupFormatException
import it.elia.gestionespese.data.AppData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/** Fotografia dei dati con i calcoli già pronti per l'interfaccia. */
class UiState(val data: AppData, val today: LocalDate) {
    val math = BudgetMath(data.movements, data.settings.payday, today)
    val balances: Balances = math.balances()
    val categories: List<Category> = data.categories.sortedBy { it.order }
    private val byId = categories.associateBy { it.id }

    /** Spese rapide in ordine, solo quelle con un budget esistente. */
    val quickShortcuts: List<Shortcut> = data.shortcuts.sortedBy { it.order }.filter { it.categoryId in byId }
    val allShortcuts: List<Shortcut> = data.shortcuts.sortedBy { it.order }

    fun category(id: String?): Category? = id?.let(byId::get)
    fun balance(id: String): Long = balances.of(id)
}

/** Messaggio per la Snackbar; con [undoMovementId] compare "Annulla". */
data class UiMessage(val text: String, val undoMovementId: String? = null)

class MainViewModel(private val app: GestioneSpeseApp) : ViewModel() {
    private val repo = app.repository

    val state: StateFlow<UiState?> = repo.data
        .map { UiState(it, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val messageChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = messageChannel.receiveAsFlow()

    /** Backup letto dal file e in attesa di conferma. */
    private val _pendingImport = MutableStateFlow<BackupData?>(null)
    val pendingImport: StateFlow<BackupData?> = _pendingImport.asStateFlow()

    fun message(text: String) {
        messageChannel.trySend(UiMessage(text))
    }

    // ---------- movimenti ----------

    fun newMovement(
        kind: MovementKind,
        date: LocalDate,
        amount: Long,
        categoryId: String? = null,
        note: String? = null,
        total: Long? = null,
        allocations: Map<String, Long> = emptyMap(),
    ) = Movement(
        id = Ids.newId(),
        kind = kind,
        date = date,
        amount = amount,
        categoryId = categoryId,
        note = note?.trim()?.ifEmpty { null },
        total = total,
        createdAt = System.currentTimeMillis(),
        allocations = allocations.filterValues { it != 0L },
    )

    /** Registra un movimento e mostra [text]; con [undoable] la Snackbar offre "Annulla". */
    fun record(movement: Movement, text: String, undoable: Boolean = false) {
        viewModelScope.launch {
            repo.addMovement(movement)
            messageChannel.send(UiMessage(text, if (undoable) movement.id else null))
        }
    }

    fun deleteMovement(id: String, text: String? = "Movimento eliminato") {
        viewModelScope.launch {
            repo.deleteMovement(id)
            if (text != null) messageChannel.send(UiMessage(text))
        }
    }

    fun undo(movementId: String) = deleteMovement(movementId, null)

    fun quickExpense(s: Shortcut) {
        val today = state.value?.today ?: LocalDate.now()
        record(
            newMovement(MovementKind.SPESA, today, s.amount, categoryId = s.categoryId, note = s.name),
            "${s.name} ${Formats.eur(s.amount)} registrata",
            undoable = true,
        )
    }

    // ---------- budget ----------

    fun saveCategory(c: Category, created: Boolean) {
        viewModelScope.launch {
            repo.saveCategory(c)
            messageChannel.send(UiMessage("Budget \"${c.name}\" ${if (created) "creato" else "salvato"}"))
        }
    }

    /** Sposta il budget di una posizione (-1 su, +1 giù) e rinumera l'ordine. */
    fun moveCategory(id: String, delta: Int) {
        val cats = state.value?.categories?.toMutableList() ?: return
        val i = cats.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in cats.indices) return
        cats[i] = cats[j].also { cats[j] = cats[i] }
        val changed = cats.mapIndexedNotNull { k, c -> if (c.order != k) c.copy(order = k) else null }
        viewModelScope.launch { repo.saveCategories(changed) }
    }

    fun nextCategoryOrder(): Int = (state.value?.categories?.maxOfOrNull { it.order } ?: -1) + 1

    /** Elimina un budget (SPEC.md §4.9); [destinationId] null = "Da assegnare". */
    fun deleteCategory(c: Category, destinationId: String?) {
        val st = state.value ?: return
        val balance = st.balance(c.id)
        val alloc = st.math.closingAllocations(c.id, balance, if (balance > 0) destinationId else null)
        val closing = if (alloc.isEmpty()) null else newMovement(
            MovementKind.PRELEVA, st.today, 0, note = "Chiusura budget ${c.name}", allocations = alloc,
        )
        viewModelScope.launch {
            repo.deleteCategory(c.id, closing)
            messageChannel.send(UiMessage("Budget \"${c.name}\" eliminato"))
        }
    }

    // ---------- spese rapide ----------

    fun saveShortcut(s: Shortcut, created: Boolean) {
        viewModelScope.launch {
            repo.saveShortcut(s)
            messageChannel.send(UiMessage("Spesa rapida \"${s.name}\" ${if (created) "creata" else "salvata"}"))
        }
    }

    fun nextShortcutOrder(): Int = (state.value?.allShortcuts?.maxOfOrNull { it.order } ?: -1) + 1

    fun deleteShortcut(s: Shortcut) {
        viewModelScope.launch {
            repo.deleteShortcut(s.id)
            messageChannel.send(UiMessage("Spesa rapida \"${s.name}\" eliminata"))
        }
    }

    // ---------- impostazioni ----------

    fun setPayday(day: Int) {
        if (day !in 1..31 || day == state.value?.data?.settings?.payday) return
        viewModelScope.launch {
            repo.setPayday(day)
            messageChannel.send(UiMessage("Stipendio il giorno $day di ogni mese"))
        }
    }

    fun seed() {
        val (cats, shorts) = SampleData.create(state.value?.today ?: LocalDate.now())
        viewModelScope.launch {
            repo.saveCategories(cats)
            repo.saveShortcuts(shorts)
            messageChannel.send(UiMessage("Budget di esempio creati: modificali in Impostazioni"))
        }
    }

    // ---------- backup ----------

    fun exportBackup(uri: Uri) {
        val st = state.value ?: return
        val data = BackupData(st.data.settings, st.data.categories, st.data.shortcuts, st.data.movements)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val text = BackupCodec.encode(data, OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString())
                    app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            messageChannel.send(UiMessage(if (ok) "Backup esportato" else "Esportazione non riuscita"))
        }
    }

    /** Legge il file scelto e, se è valido, lo propone per l'importazione. */
    fun readBackup(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = app.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                    BackupCodec.decode(text)
                }
            }
            result.onSuccess { _pendingImport.value = it }
                .onFailure {
                    messageChannel.send(UiMessage((it as? BackupFormatException)?.message ?: "File non leggibile"))
                }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    /** Sostituisce tutti i dati con il backup in attesa. */
    fun confirmImport() {
        val data = _pendingImport.value ?: return
        _pendingImport.value = null
        viewModelScope.launch {
            repo.replaceAll(data)
            messageChannel.send(UiMessage("Backup importato: ${data.categories.size} budget, ${data.movements.size} movimenti"))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MainViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GestioneSpeseApp)
            }
        }
    }
}
