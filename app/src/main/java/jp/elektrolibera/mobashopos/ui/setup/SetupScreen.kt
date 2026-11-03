package jp.elektrolibera.mobashopos.ui.setup

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.elektrolibera.mobashopos.BuildConfig
import jp.elektrolibera.mobashopos.R
import jp.elektrolibera.mobashopos.data.PosRepository
import jp.elektrolibera.mobashopos.data.api.PosException
import jp.elektrolibera.mobashopos.ui.common.BusyButton
import jp.elektrolibera.mobashopos.ui.common.ErrorText
import jp.elektrolibera.mobashopos.ui.common.PosScaffold
import kotlinx.coroutines.launch

class SetupViewModel(private val repo: PosRepository) : ViewModel() {
    var serverUrl by mutableStateOf(BuildConfig.DEFAULT_SERVER_URL)
    var code by mutableStateOf("")
    var deviceName by mutableStateOf(Build.MODEL ?: "")
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val canSubmit: Boolean
        get() = serverUrl.startsWith("http") && code.length >= 4 && deviceName.isNotBlank()

    fun submit() {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                // 成功すると設定が保存され、画面はログインへ切り替わる
                repo.enroll(serverUrl, code, deviceName)
                code = ""
            } catch (e: PosException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun SetupScreen(repo: PosRepository) {
    val vm = viewModel { SetupViewModel(repo) }
    PosScaffold(title = stringResource(R.string.setup_title)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(stringResource(R.string.setup_description))
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = vm.serverUrl,
                onValueChange = { vm.serverUrl = it },
                label = { Text(stringResource(R.string.setup_server_url)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.code,
                onValueChange = { vm.code = it.uppercase() },
                label = { Text(stringResource(R.string.setup_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.deviceName,
                onValueChange = { vm.deviceName = it },
                label = { Text(stringResource(R.string.setup_device_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(vm.error)
            Spacer(Modifier.height(16.dp))
            BusyButton(
                text = stringResource(R.string.setup_submit),
                busy = vm.busy,
                enabled = vm.canSubmit,
                onClick = vm::submit,
            )
        }
    }
}
