@file:OptIn(ExperimentalMaterial3Api::class)

package it.elia.gestionespese.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.elia.gestionespese.core.backup.BackupCodec
import it.elia.gestionespese.ui.budget.BudgetActions
import it.elia.gestionespese.ui.budget.BudgetScreen
import it.elia.gestionespese.ui.components.AccountHeader
import it.elia.gestionespese.ui.components.AppIcons
import it.elia.gestionespese.ui.movements.MovementFilter
import it.elia.gestionespese.ui.movements.MovementsScreen
import it.elia.gestionespese.ui.settings.SettingsActions
import it.elia.gestionespese.ui.settings.SettingsScreen
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            BottomBar(tab, onTab = {
                tab = it
                sheet = null
            }, quickEnabled = hasCategories, onQuick = { sheet = Sheet.Quick })
        },
        floatingActionButton = {
            if (hasCategories) Fabs(onIncome = { sheet = Sheet.Income }, onExpense = { sheet = Sheet.Expense() })
        },
    ) { padding ->
        if (state == null) {
            Box(Modifier.fillMaxSize())
            return@Scaffold
        }
        // Spazio in fondo per non finire sotto i pulsanti flottanti
        val dir = LocalLayoutDirection.current
        val content = PaddingValues(
            start = padding.calculateStartPadding(dir),
            end = padding.calculateEndPadding(dir),
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 96.dp,
        )
        val header: @Composable () -> Unit = {
            AccountHeader(
                state,
                onReset = { sheet = Sheet.Reset },
                onSalary = { sheet = Sheet.Salary },
                onAssign = { sheet = Sheet.Assign },
            )
        }
        when (tab) {
            Tab.BUDGET -> BudgetScreen(
                state,
                BudgetActions(
                    onReset = { sheet = Sheet.Reset },
                    onSalary = { sheet = Sheet.Salary },
                    onAssign = { sheet = Sheet.Assign },
                    onTake = { sheet = Sheet.Take(it.id) },
                    onShowMovements = {
                        filter = MovementFilter(categoryId = it.id)
                        tab = Tab.MOVIMENTI
                    },
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
                header = header,
                contentPadding = content,
            )
            Tab.RIEPILOGO -> SummaryScreen(state, header, content)
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
                header = header,
                contentPadding = content,
            )
        }

        val open = sheet
        if (open != null) {
            SheetHost(open, state, vm, onOpen = { sheet = it }, onGoToSettings = {
                sheet = null
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
                        Text(
                            "Tutti i dati attuali sul telefono verranno sostituiti.",
                            color = AppTheme.colors.neg,
                        )
                    }
                },
                confirmButton = { TextButton(onClick = vm::confirmImport) { Text("Sostituisci i dati") } },
                dismissButton = { TextButton(onClick = vm::cancelImport) { Text("Annulla") } },
            )
        }
    }
}

@Composable
private fun BottomBar(tab: Tab, onTab: (Tab) -> Unit, quickEnabled: Boolean, onQuick: () -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        for (t in Tab.entries) {
            NavigationBarItem(
                selected = t == tab,
                onClick = { onTab(t) },
                icon = { Icon(t.icon, contentDescription = null) },
                label = { Text(t.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
            )
        }
        QuickItem(quickEnabled, onQuick)
    }
}

/** "Spese rapide": non è una scheda, apre un bottom sheet; colore arancio per distinguerla. */
@Composable
private fun RowScope.QuickItem(enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(Modifier.weight(1.1f).padding(vertical = 10.dp, horizontal = 2.dp), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            color = if (enabled) colors.quick else colors.quick.copy(alpha = 0.45f),
            contentColor = colors.onQuick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(AppIcons.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Spese rapide", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 12.sp)
            }
        }
    }
}

/** Pulsanti flottanti: + entrata extra (bordo verde) e − spesa (pieno). */
@Composable
private fun Fabs(onIncome: () -> Unit, onExpense: () -> Unit) {
    val colors = AppTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            onClick = onIncome,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = colors.chartIn,
            border = BorderStroke(2.dp, colors.chartIn),
            shadowElevation = 6.dp,
            modifier = Modifier.size(58.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Text("+", fontSize = 30.sp, fontWeight = FontWeight.Medium) }
        }
        FloatingActionButton(
            onClick = onExpense,
            shape = CircleShape,
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            modifier = Modifier.size(58.dp),
        ) {
            Text("−", fontSize = 30.sp, fontWeight = FontWeight.Medium)
        }
    }
}

