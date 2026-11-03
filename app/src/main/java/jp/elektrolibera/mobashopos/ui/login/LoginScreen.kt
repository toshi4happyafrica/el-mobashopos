package jp.elektrolibera.mobashopos.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.elektrolibera.mobashopos.R
import jp.elektrolibera.mobashopos.data.PosRepository
import jp.elektrolibera.mobashopos.data.api.PosException
import jp.elektrolibera.mobashopos.data.api.StaffDto
import jp.elektrolibera.mobashopos.ui.common.BusyButton
import jp.elektrolibera.mobashopos.ui.common.ErrorText
import jp.elektrolibera.mobashopos.ui.common.LoadingBox
import jp.elektrolibera.mobashopos.ui.common.PosScaffold
import kotlinx.coroutines.launch

class LoginViewModel(private val repo: PosRepository) : ViewModel() {
    var staff by mutableStateOf<List<StaffDto>?>(null)
        private set
    var selected by mutableStateOf<StaffDto?>(null)
    var pin by mutableStateOf("")
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun load() {
        error = null
        viewModelScope.launch {
            try {
                staff = repo.staffDirectory()
            } catch (e: PosException) {
                error = e.message
                staff = emptyList()
            }
        }
    }

    fun login() {
        val target = selected ?: return
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                // 成功すると設定が保存され、画面はメインへ切り替わる
                repo.login(target.id, pin)
                // ViewModel は画面をまたいで残るため、次のログインに PIN を残さない
                pin = ""
                selected = null
            } catch (e: PosException) {
                error = e.message
                pin = ""
            } finally {
                busy = false
            }
        }
    }

    fun unenroll() {
        viewModelScope.launch { repo.unenroll() }
    }
}

@Composable
fun LoginScreen(repo: PosRepository, storeName: String?) {
    val vm = viewModel { LoginViewModel(repo) }
    var confirmUnenroll by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.load() }

    PosScaffold(title = storeName ?: stringResource(R.string.login_title)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            val list = vm.staff
            if (list == null) {
                LoadingBox(Modifier.weight(1f))
            } else {
                Text(stringResource(R.string.login_select_staff), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (list.isEmpty()) Text(stringResource(R.string.login_no_staff))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(list, key = { it.id }) { s ->
                        val isSelected = vm.selected?.id == s.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { vm.selected = s; vm.pin = "" },
                            colors = if (isSelected) {
                                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                CardDefaults.cardColors()
                            },
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(s.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(if (s.role == "manager") R.string.role_manager else R.string.role_staff),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                if (vm.selected != null) {
                    OutlinedTextField(
                        value = vm.pin,
                        onValueChange = { v -> vm.pin = v.filter { it.isDigit() }.take(8) },
                        label = { Text(stringResource(R.string.login_pin)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    BusyButton(
                        text = stringResource(R.string.login_submit),
                        busy = vm.busy,
                        enabled = vm.pin.length >= 4,
                        onClick = vm::login,
                    )
                }
            }
            ErrorText(vm.error)
            TextButton(onClick = { confirmUnenroll = true }) { Text(stringResource(R.string.login_unenroll)) }
        }
    }

    if (confirmUnenroll) {
        AlertDialog(
            onDismissRequest = { confirmUnenroll = false },
            text = { Text(stringResource(R.string.login_unenroll_confirm)) },
            confirmButton = {
                TextButton(onClick = { confirmUnenroll = false; vm.unenroll() }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnenroll = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
