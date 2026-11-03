package jp.elektrolibera.mobashopos.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.elektrolibera.mobashopos.R
import jp.elektrolibera.mobashopos.data.PosRepository
import jp.elektrolibera.mobashopos.data.api.PosException
import jp.elektrolibera.mobashopos.data.settings.SessionState
import jp.elektrolibera.mobashopos.ui.common.BusyButton
import jp.elektrolibera.mobashopos.ui.common.ErrorText
import jp.elektrolibera.mobashopos.ui.common.PosScaffold
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: PosRepository) : ViewModel() {
    var syncing by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun sync(doneText: (Int) -> String, unchangedText: String) {
        if (syncing) return
        syncing = true
        message = null
        error = null
        viewModelScope.launch {
            try {
                val version = repo.syncMaster(force = false)
                message = if (version == null) unchangedText else doneText(version)
            } catch (e: PosException) {
                error = e.message
            } finally {
                syncing = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch { repo.logout() }
    }
}

@Composable
fun HomeScreen(
    repo: PosRepository,
    session: SessionState,
    onOrder: () -> Unit,
    onCheckout: () -> Unit,
) {
    val vm = viewModel { HomeViewModel(repo) }
    val meta by repo.meta.collectAsState(initial = null)
    val context = LocalContext.current

    PosScaffold(title = stringResource(R.string.home_title)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.home_store, meta?.storeName ?: session.storeName.orEmpty()),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.home_staff, session.staffName.orEmpty()))
            Text(
                meta?.let { stringResource(R.string.home_master_version, it.version) }
                    ?: stringResource(R.string.home_master_none),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            BusyButton(text = stringResource(R.string.home_order), busy = false, enabled = meta != null, onClick = onOrder)
            BusyButton(text = stringResource(R.string.home_checkout), busy = false, enabled = meta != null, onClick = onCheckout)
            BusyButton(text = stringResource(R.string.home_sync), busy = vm.syncing) {
                vm.sync(
                    doneText = { v -> context.getString(R.string.home_sync_done, v) },
                    unchangedText = context.getString(R.string.home_sync_unchanged),
                )
            }
            vm.message?.let { Text(it) }
            ErrorText(vm.error)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = vm::logout, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_logout))
            }
        }
    }
}
