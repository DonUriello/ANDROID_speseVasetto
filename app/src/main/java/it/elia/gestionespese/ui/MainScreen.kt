@file:OptIn(ExperimentalMaterial3Api::class)

package it.elia.gestionespese.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.elia.gestionespese.core.backup.BackupCodec
import it.elia.gestionespese.ui.budget.BudgetActions
import it.elia.gestionespese.ui.budget.BudgetDetailActions
import it.elia.gestionespese.ui.budget.BudgetDetailScreen
import it.elia.gestionespese.ui.budget.BudgetScreen
import it.elia.gestionespese.ui.components.AppIcons
import it.elia.gestionespese.ui.components.ScreenTitle
import it.elia.gestionespese.ui.movements.MovementFilter
import it.elia.gestionespese.ui.movements.MovementsScreen
import it.elia.gestionespese.ui.settings.SettingsActions
import it.elia.gestionespese.ui.settings.SettingsScreen
import it.elia.gestionespese.ui.sheets.ExpensePrefill
import it.elia.gestionespese.ui.sheets.Sheet
import it.elia.gestionespese.ui.sheets.SheetHost
import it.elia.gestionespese.ui.summary.SummaryScreen
import it.elia.gestionespese.ui.theme.AppTheme
import kotlinx.coroutines.launch

enum class Tab(val label: String, val icon: ImageVector) {
    BUDGET("Budget", AppIcons.Jar),
    MOVIMENTI("Movimenti", AppIcons.List),
    RIEPILOGO("Riepilogo", AppIcons.Chart),
    IMPOSTAZIONI("Impostazioni", AppIcons.Sliders),
}

@Composable
fun MainScreen(vm: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val st by vm.state.collectAsStateWithLifecycle()
    val pendingImport by vm.pendingImport.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.BUDGET) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var filter by remember { mutableStateOf(MovementFilter()) }
    var newCategoryOpen by rememberSaveable { mutableStateOf(false) }
    // Budget aperto nel dettaglio (null = elenco)
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm) {
        vm.messages.collect { msg ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val r = snackbar.showSnackbar(
                    message = msg.text,
                    actionLabel = if (msg.undoMovementId != null) "Annulla" else null,
                    duration = SnackbarDuration.Short,
                )
                if (r == SnackbarResult.ActionPerformed) msg.undoMovementId?.let(vm::undo)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.readBackup(uri)
    }

    val state = st
    val hasCategories = state != null && state.categories.isNotEmpty()
    val detail = state?.category(detailId)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (detail == null) {
                FloatingNav(tab, onTab = {
                    tab = it
                    sheet = null
                }, quickEnabled = hasCategories, onQuick = { sheet = Sheet.Quick })
            }
        },
    ) { padding ->
        if (state == null) {
            Box(Modifier.fillMaxSize())
            return@Scaffold
        }
        val dir = LocalLayoutDirection.current
        val content = PaddingValues(
            start = padding.calculateStartPadding(dir),
            end = padding.calculateEndPadding(dir),
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        )
        if (detail != null) {
            BudgetDetailScreen(
                state,
                detail,
                BudgetDetailActions(
                    onBack = { detailId = null },
                    onTake = { sheet = Sheet.Take(it.id) },
                    onExpense = { sheet = Sheet.Expense(ExpensePrefill("Spesa da ${it.name}", 0, it.id, "")) },
                    onAllMovements = {
                        filter = MovementFilter(categoryId = it.id)
                        detailId = null
                        tab = Tab.MOVIMENTI
                    },
                ),
                content,
            )
        } else {
            when (tab) {
                Tab.BUDGET -> BudgetScreen(
                    state,
                    BudgetActions(
                        onReset = { sheet = Sheet.Reset },
                        onIncome = { sheet = Sheet.Salary },
                        onExpense = { sheet = Sheet.Expense() },
                        onAssign = { sheet = Sheet.Assign },
                        onOpen = { detailId = it.id },
                        onCreateFirst = {
                            newCategoryOpen = true
                            tab = Tab.IMPOSTAZIONI
                        },
                        onSeed = vm::seed,
                        onImport = { importLauncher.launch(arrayOf("*/*")) },
                    ),
                    content,
                )
                Tab.MOVIMENTI -> MovementsScreen(
                    state,
                    filter,
                    onFilter = { filter = it },
                    onDelete = { vm.deleteMovement(it.id) },
                    contentPadding = content,
                )
                Tab.RIEPILOGO -> SummaryScreen(state, content)
                Tab.IMPOSTAZIONI -> SettingsScreen(
                    state,
                    vm,
                    SettingsActions(
                        onDeleteCategory = { sheet = Sheet.DeleteCategory(it.id) },
                        onExport = { exportLauncher.launch(BackupCodec.fileName(state.today)) },
                        onImport = { importLauncher.launch(arrayOf("*/*")) },
                    ),
                    newCategoryOpen = newCategoryOpen,
                    onNewCategoryOpen = { newCategoryOpen = it },
                    header = { ScreenTitle("Impostazioni") },
                    contentPadding = content,
                )
            }
        }

        val open = sheet
        if (open != null) {
            SheetHost(open, state, vm, onOpen = { sheet = it }, onGoToSettings = {
                sheet = null
                detailId = null
                tab = Tab.IMPOSTAZIONI
            })
        }

        pendingImport?.let { data ->
            AlertDialog(
                onDismissRequest = vm::cancelImport,
                title = { Text("Importare il backup?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${data.categories.size} budget · ${data.shortcuts.size} spese rapide · ${data.movements.size} movimenti")
                        Text("Stipendio il giorno ${data.settings.payday}")
                        Text("Tutti i dati attuali sul telefono verranno sostituiti.", color = AppTheme.colors.neg)
                    }
                },
                confirmButton = { TextButton(onClick = vm::confirmImport) { Text("Sostituisci i dati") } },
                dismissButton = { TextButton(onClick = vm::cancelImport) { Text("Annulla") } },
            )
        }
    }
}

/** Barra di navigazione flottante: pillola scura, scheda attiva in un cerchio chiaro, "Spese rapide" al centro. */
@Composable
private fun FloatingNav(tab: Tab, onTab: (Tab) -> Unit, quickEnabled: Boolean, onQuick: () -> Unit) {
    val colors = AppTheme.colors
    val dark = Color(0xFF111113)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 10.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(50), color = dark, shadowElevation = 8.dp) {
            Row(
                Modifier.height(64.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NavButton(Tab.BUDGET, tab, onTab)
                NavButton(Tab.MOVIMENTI, tab, onTab)
                Surface(
                    onClick = onQuick,
                    enabled = quickEnabled,
                    shape = CircleShape,
                    color = colors.chartIn.copy(alpha = if (quickEnabled) 1f else 0.4f),
                    contentColor = Color(0xFF0B1A10),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(AppIcons.Bolt, "Spese rapide", Modifier.size(22.dp)) }
                }
                NavButton(Tab.RIEPILOGO, tab, onTab)
                NavButton(Tab.IMPOSTAZIONI, tab, onTab)
            }
        }
    }
}

@Composable
private fun NavButton(t: Tab, current: Tab, onTab: (Tab) -> Unit) {
    val on = t == current
    Surface(
        onClick = { onTab(t) },
        shape = CircleShape,
        color = if (on) Color.White else Color.Transparent,
        contentColor = if (on) Color(0xFF111113) else Color(0xFFB8B8BD),
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(t.icon, contentDescription = t.label, modifier = Modifier.size(22.dp)) }
    }
}
