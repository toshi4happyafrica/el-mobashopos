package jp.elektrolibera.mobashopos.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.elektrolibera.mobashopos.R
import jp.elektrolibera.mobashopos.data.PosRepository
import jp.elektrolibera.mobashopos.data.api.CheckDto
import jp.elektrolibera.mobashopos.data.api.PaymentResponse
import jp.elektrolibera.mobashopos.data.api.PosException
import jp.elektrolibera.mobashopos.domain.MoneyFormat
import jp.elektrolibera.mobashopos.ui.common.BusyButton
import jp.elektrolibera.mobashopos.ui.common.ErrorText
import jp.elektrolibera.mobashopos.ui.common.LoadingBox
import jp.elektrolibera.mobashopos.ui.common.PosScaffold
import kotlinx.coroutines.launch
import java.util.UUID

class CheckoutViewModel(private val repo: PosRepository, val tableId: String) : ViewModel() {
    var tableLabel by mutableStateOf("")
        private set
    var loading by mutableStateOf(true)
        private set
    var check by mutableStateOf<CheckDto?>(null)
        private set
    var tenderedInput by mutableStateOf("")
    var paying by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var result by mutableStateOf<PaymentResponse?>(null)
        private set

    /** 会計1回分の冪等キー。通信失敗で再送するときは同じ値を使う */
    private var pendingRequestId: String? = null

    init {
        viewModelScope.launch {
            val t = repo.table(tableId)
            tableLabel = t?.name ?: t?.code ?: ""
        }
        reload()
    }

    fun reload() {
        loading = true
        error = null
        pendingRequestId = null
        viewModelScope.launch {
            try {
                check = repo.openCheck(tableId)
            } catch (e: PosException) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun pay(tendered: Long) {
        val current = check ?: return
        if (paying) return
        paying = true
        error = null
        val requestId = pendingRequestId ?: UUID.randomUUID().toString().also { pendingRequestId = it }
        viewModelScope.launch {
            try {
                result = repo.pay(requestId, current, tendered)
                pendingRequestId = null
            } catch (e: PosException) {
                error = e.message
                // 合計が変わっていた（会計中に追加注文など）場合は伝票を取り直す
                if (e.kind == PosException.Kind.CONFLICT) reload()
            } finally {
                paying = false
            }
        }
    }
}

@Composable
fun CheckoutScreen(repo: PosRepository, tableId: String, onBack: () -> Unit, onDone: () -> Unit) {
    val vm = viewModel(key = "checkout-$tableId") { CheckoutViewModel(repo, tableId) }
    val meta by repo.meta.collectAsState(initial = null)
    val minorUnit = meta?.minorUnit ?: 0

    PosScaffold(title = stringResource(R.string.checkout_title, vm.tableLabel), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            val check = vm.check
            when {
                vm.loading -> LoadingBox(Modifier.weight(1f))
                check == null -> {
                    Text(stringResource(R.string.checkout_no_open_check))
                    ErrorText(vm.error)
                    OutlinedButton(onClick = vm::reload) { Text(stringResource(R.string.checkout_reload)) }
                }
                else -> CheckContent(vm, check, minorUnit, Modifier.weight(1f))
            }
        }
    }

    val done = vm.result
    if (done != null) {
        fun fmtAll(list: List<jp.elektrolibera.mobashopos.data.api.MoneyDto>): String =
            if (list.isEmpty()) MoneyFormat.format(0, done.billed.currency, minorUnit)
            else list.joinToString(" + ") { MoneyFormat.format(it.amount, it.currency, minorUnit) }
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text(stringResource(R.string.checkout_done_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.checkout_done_message,
                        MoneyFormat.format(done.billed.amount, done.billed.currency, minorUnit),
                        fmtAll(done.tendered),
                        fmtAll(done.change),
                    ),
                )
            },
            confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun CheckContent(vm: CheckoutViewModel, check: CheckDto, minorUnit: Int, listModifier: Modifier) {
    val currency = check.total.currency
    fun fmt(amount: Long) = MoneyFormat.format(amount, currency, minorUnit)
    val tendered = MoneyFormat.parse(vm.tenderedInput, minorUnit)

    Text(
        stringResource(R.string.checkout_total, fmt(check.total.amount), check.totalQuantity),
        style = MaterialTheme.typography.headlineSmall,
    )
    LazyColumn(listModifier.fillMaxWidth()) {
        items(check.lines, key = { it.id }) { line ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(stringResource(R.string.order_line, line.name, line.quantity), Modifier.weight(1f))
                Text(fmt(line.lineTotal.amount))
            }
            HorizontalDivider()
        }
    }
    Surface(tonalElevation = 2.dp) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vm.tenderedInput,
                    onValueChange = { vm.tenderedInput = it },
                    label = { Text(stringResource(R.string.checkout_tendered)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (minorUnit > 0) KeyboardType.Decimal else KeyboardType.Number,
                    ),
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = {
                    vm.tenderedInput = java.math.BigDecimal.valueOf(check.total.amount).movePointLeft(minorUnit).toPlainString()
                }) { Text(stringResource(R.string.checkout_exact)) }
            }
            if (tendered != null && tendered >= check.total.amount) {
                Text(stringResource(R.string.checkout_change_preview, fmt(tendered - check.total.amount)))
            } else if (vm.tenderedInput.isNotBlank()) {
                ErrorText(stringResource(R.string.checkout_invalid_amount))
            }
            ErrorText(vm.error)
            BusyButton(
                text = stringResource(R.string.checkout_pay),
                busy = vm.paying,
                enabled = tendered != null && tendered >= check.total.amount,
            ) { tendered?.let(vm::pay) }
        }
    }
}
