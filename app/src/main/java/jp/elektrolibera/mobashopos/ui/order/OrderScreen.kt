package jp.elektrolibera.mobashopos.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.elektrolibera.mobashopos.R
import jp.elektrolibera.mobashopos.data.PosRepository
import jp.elektrolibera.mobashopos.data.api.CheckDto
import jp.elektrolibera.mobashopos.data.api.PosException
import jp.elektrolibera.mobashopos.data.db.MenuItemEntity
import jp.elektrolibera.mobashopos.domain.MoneyFormat
import jp.elektrolibera.mobashopos.domain.OrderCart
import jp.elektrolibera.mobashopos.ui.common.BusyButton
import jp.elektrolibera.mobashopos.ui.common.ErrorText
import jp.elektrolibera.mobashopos.ui.common.PosScaffold
import kotlinx.coroutines.launch
import java.util.UUID

class OrderViewModel(private val repo: PosRepository, val tableId: String) : ViewModel() {
    var tableLabel by mutableStateOf("")
        private set
    var selectedCategoryId by mutableStateOf<String?>(null)
    var cart by mutableStateOf(OrderCart())
        private set
    var confirming by mutableStateOf(false)
    var sending by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var sentCheck by mutableStateOf<CheckDto?>(null)
        private set

    /** 送信1回分の冪等キー。失敗して再送するときは同じ値を使う */
    private var pendingRequestId: String? = null

    init {
        viewModelScope.launch {
            val t = repo.table(tableId)
            tableLabel = t?.name ?: t?.code ?: ""
        }
    }

    fun change(item: MenuItemEntity, delta: Int) {
        cart = cart.change(item.id, item.name, item.priceAmount, delta)
        pendingRequestId = null // 内容が変わったら別の注文として扱う
    }

    fun send() {
        if (sending || cart.isEmpty) return
        sending = true
        error = null
        val requestId = pendingRequestId ?: UUID.randomUUID().toString().also { pendingRequestId = it }
        viewModelScope.launch {
            try {
                val res = repo.placeOrder(requestId, tableId, cart.lines)
                sentCheck = res.check
                cart = OrderCart()
                confirming = false
                pendingRequestId = null
            } catch (e: PosException) {
                error = e.message
            } finally {
                sending = false
            }
        }
    }
}

@Composable
fun OrderScreen(repo: PosRepository, tableId: String, onBack: () -> Unit, onDone: () -> Unit) {
    val vm = viewModel(key = "order-$tableId") { OrderViewModel(repo, tableId) }
    val categories by repo.categories.collectAsState(initial = emptyList())
    val allItems by repo.items.collectAsState(initial = emptyList())
    val meta by repo.meta.collectAsState(initial = null)
    val minorUnit = meta?.minorUnit ?: 0
    val currency = meta?.baseCurrency ?: "JPY"
    fun fmt(amount: Long) = MoneyFormat.format(amount, currency, minorUnit)

    LaunchedEffect(categories) {
        if (vm.selectedCategoryId == null) vm.selectedCategoryId = categories.firstOrNull()?.id
    }

    val title = if (vm.confirming) {
        stringResource(R.string.order_confirm_title, vm.tableLabel)
    } else {
        stringResource(R.string.order_title, vm.tableLabel)
    }

    PosScaffold(
        title = title,
        onBack = { if (vm.confirming) vm.confirming = false else onBack() },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.order_summary, vm.cart.totalQuantity, fmt(vm.cart.totalAmount)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    ErrorText(vm.error)
                    if (vm.confirming) {
                        Text(stringResource(R.string.order_preview_note), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.confirming = false }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.order_edit))
                            }
                            BusyButton(
                                text = stringResource(R.string.order_send),
                                busy = vm.sending,
                                modifier = Modifier.weight(1f),
                                onClick = vm::send,
                            )
                        }
                    } else {
                        Button(
                            onClick = { vm.confirming = true },
                            enabled = !vm.cart.isEmpty,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.order_to_confirm)) }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (vm.confirming) {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(vm.cart.lines, key = { it.menuItemId }) { line ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(stringResource(R.string.order_line, line.name, line.quantity), Modifier.weight(1f))
                            Text(fmt(line.lineTotal))
                        }
                        HorizontalDivider()
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories, key = { it.id }) { c ->
                        if (c.id == vm.selectedCategoryId) {
                            Button(onClick = {}) { Text(c.name) }
                        } else {
                            OutlinedButton(onClick = { vm.selectedCategoryId = c.id }) { Text(c.name) }
                        }
                    }
                }
                val visibleItems = allItems.filter { it.categoryId == vm.selectedCategoryId }
                if (visibleItems.isEmpty()) {
                    Text(stringResource(R.string.order_no_items), Modifier.padding(16.dp))
                }
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(visibleItems, key = { it.id }) { item ->
                        val qty = vm.cart.quantityOf(item.id)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(fmt(item.priceAmount), style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { vm.change(item, -1) }, enabled = qty > 0) { Text("−") }
                            Text(
                                qty.toString(),
                                modifier = Modifier.width(40.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            OutlinedButton(
                                onClick = { vm.change(item, +1) },
                                enabled = qty < OrderCart.MAX_QUANTITY,
                            ) { Text("＋") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    val sent = vm.sentCheck
    if (sent != null) {
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text(stringResource(R.string.order_sent_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.order_sent_message,
                        sent.tableCode,
                        MoneyFormat.format(sent.total.amount, sent.total.currency, minorUnit),
                        sent.totalQuantity,
                    ),
                )
            },
            confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.ok)) } },
        )
    }
}
