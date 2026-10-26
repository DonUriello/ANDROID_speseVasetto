package it.elia.gestionespese

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.elia.gestionespese.core.Balances
import it.elia.gestionespese.core.BudgetMath
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.data.SpeseRepository
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.GestioneSpeseTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GestioneSpeseTheme {
                HomeScreen()
            }
        }
    }
}

/** Stato provvisorio della tappa 1: solo i totali, per verificare che dati e calcoli siano collegati. */
class HomeViewModel(repository: SpeseRepository) : ViewModel() {
    val balances: StateFlow<Balances> = repository.data
        .map { BudgetMath(it.movements, it.settings.payday, LocalDate.now()).balances() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Balances(emptyMap(), 0))

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GestioneSpeseApp
                HomeViewModel(app.repository)
            }
        }
    }
}

@Composable
private fun HomeScreen(vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val balances by vm.balances.collectAsStateWithLifecycle()
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("GESTIONE SPESE", style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.accent)
            Text("Totale conto", style = MaterialTheme.typography.bodyMedium)
            Text(Formats.eur(balances.totalAccount), style = MaterialTheme.typography.displaySmall)
            HorizontalDivider()
            Text("Totale nei budget: ${Formats.eur(balances.totalInBudgets)}")
            val toAssignColor = when {
                balances.toAssign > 0 -> AppTheme.colors.ok
                balances.toAssign < 0 -> AppTheme.colors.neg
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text("Da assegnare: ${Formats.eur(balances.toAssign)}", color = toAssignColor)
        }
    }
}
