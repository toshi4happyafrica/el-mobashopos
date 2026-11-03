package jp.elektrolibera.mobashopos.data.api

import kotlinx.serialization.Serializable

// サーバー API（el-mobashopsrv/docs/api-spec.md）の JSON に対応するデータクラス

@Serializable
data class MoneyDto(val amount: Long, val currency: String)

@Serializable
data class ErrorEnvelope(val error: ErrorBody)

@Serializable
data class ErrorBody(val code: String, val message: String)

// ---- 端末登録・ログイン ----

@Serializable
data class EnrollRequest(val code: String, val deviceName: String)

@Serializable
data class EnrollResponse(val deviceToken: String, val device: DeviceDto, val store: StoreSummaryDto)

@Serializable
data class DeviceDto(val id: String, val name: String)

@Serializable
data class StoreSummaryDto(val id: String, val name: String, val baseCurrency: String)

@Serializable
data class StaffDto(val id: String, val displayName: String, val role: String)

@Serializable
data class StaffDirectoryResponse(val staff: List<StaffDto>)

@Serializable
data class LoginRequest(val staffId: String, val pin: String)

@Serializable
data class LoginResponse(val staffToken: String, val expiresAt: String, val staff: StaffDto)

// ---- マスター ----

@Serializable
data class MasterResponse(
    val version: Int,
    val store: StoreDto,
    val tables: List<TableDto>,
    val categories: List<CategoryDto>,
    val items: List<ItemDto>,
)

@Serializable
data class StoreDto(
    val id: String,
    val name: String,
    val baseCurrency: String,
    val timezone: String,
    val currencies: List<CurrencyDto>,
)

@Serializable
data class CurrencyDto(
    val currency: String,
    val minorUnit: Int,
    val cashUnit: Long,
    val roundingMode: String,
    val isBase: Boolean,
)

@Serializable
data class TableDto(val id: String, val code: String, val name: String? = null, val sortOrder: Int)

@Serializable
data class CategoryDto(val id: String, val name: String, val sortOrder: Int)

@Serializable
data class ItemDto(
    val id: String,
    val categoryId: String,
    val name: String,
    val price: MoneyDto,
    val sortOrder: Int,
)

// ---- 注文・伝票・会計 ----

@Serializable
data class OrderLineRequest(val menuItemId: String, val quantity: Int)

@Serializable
data class OrderRequest(val clientRequestId: String, val tableId: String, val lines: List<OrderLineRequest>)

@Serializable
data class OrderResponse(val orderId: String, val check: CheckDto)

@Serializable
data class CheckDto(
    val id: String,
    val tableId: String,
    val tableCode: String,
    val status: String,
    val total: MoneyDto,
    val totalQuantity: Int,
    val businessDate: String,
    val openedAt: String,
    val closedAt: String? = null,
    val lines: List<CheckLineDto>,
    val payment: PaymentSummaryDto? = null,
)

@Serializable
data class CheckLineDto(
    val id: String,
    val menuItemId: String,
    val name: String,
    val unitPrice: MoneyDto,
    val quantity: Int,
    val lineTotal: MoneyDto,
    val orderedAt: String,
)

@Serializable
data class PaymentSummaryDto(
    val paymentId: String,
    val billed: MoneyDto,
    val tendered: List<MoneyDto>,
    val change: List<MoneyDto>,
    val roundingAdjustment: Long,
    val paidAt: String,
)

@Serializable
data class PaymentRequest(
    val clientRequestId: String,
    val expectedTotal: MoneyDto,
    val tendered: List<MoneyDto>,
)

@Serializable
data class PaymentResponse(
    val paymentId: String,
    val billed: MoneyDto,
    val tendered: List<MoneyDto>,
    val change: List<MoneyDto>,
    val roundingAdjustment: Long,
    val check: CheckDto,
)
