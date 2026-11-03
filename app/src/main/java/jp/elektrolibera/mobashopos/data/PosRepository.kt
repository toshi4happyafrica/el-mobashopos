package jp.elektrolibera.mobashopos.data

import jp.elektrolibera.mobashopos.data.api.ApiClient
import jp.elektrolibera.mobashopos.data.api.CheckDto
import jp.elektrolibera.mobashopos.data.api.EnrollRequest
import jp.elektrolibera.mobashopos.data.api.LoginRequest
import jp.elektrolibera.mobashopos.data.api.MoneyDto
import jp.elektrolibera.mobashopos.data.api.OrderLineRequest
import jp.elektrolibera.mobashopos.data.api.OrderRequest
import jp.elektrolibera.mobashopos.data.api.OrderResponse
import jp.elektrolibera.mobashopos.data.api.PaymentRequest
import jp.elektrolibera.mobashopos.data.api.PaymentResponse
import jp.elektrolibera.mobashopos.data.api.PosApi
import jp.elektrolibera.mobashopos.data.api.PosException
import jp.elektrolibera.mobashopos.data.api.StaffDto
import jp.elektrolibera.mobashopos.data.api.apiCall
import jp.elektrolibera.mobashopos.data.api.toPosException
import jp.elektrolibera.mobashopos.data.db.CategoryEntity
import jp.elektrolibera.mobashopos.data.db.MasterDatabase
import jp.elektrolibera.mobashopos.data.db.MasterMetaEntity
import jp.elektrolibera.mobashopos.data.db.MenuItemEntity
import jp.elektrolibera.mobashopos.data.db.TableEntity
import jp.elektrolibera.mobashopos.data.db.clearMaster
import jp.elektrolibera.mobashopos.data.db.replaceMaster
import jp.elektrolibera.mobashopos.data.settings.SessionState
import jp.elektrolibera.mobashopos.data.settings.SettingsStore
import jp.elektrolibera.mobashopos.domain.CartLine

/** サーバー API・端末内マスター・設定をまとめて扱う窓口 */
class PosRepository(
    private val settings: SettingsStore,
    private val db: MasterDatabase,
    private val client: ApiClient,
) {
    private val dao = db.masterDao()

    val session = settings.state
    val meta = dao.meta()
    val tables = dao.tables()
    val categories = dao.categories()
    val items = dao.items()

    suspend fun table(id: String): TableEntity? = dao.table(id)

    private fun bearer(token: String?): String {
        if (token.isNullOrBlank()) throw PosException(PosException.Kind.UNAUTHORIZED, null, "ログインが必要です")
        return "Bearer $token"
    }

    private suspend fun enrolled(): Pair<PosApi, SessionState> {
        val s = settings.current()
        if (!s.isEnrolled) throw PosException(PosException.Kind.UNAUTHORIZED, null, "端末が登録されていません")
        return client.api(s.serverUrl!!) to s
    }

    /** 店員トークンが無効になったらログイン状態を消す（画面はログインへ戻る） */
    private suspend fun <T> staffCall(block: suspend (PosApi, String) -> T): T {
        val (api, s) = enrolled()
        try {
            return apiCall { block(api, bearer(s.staffToken)) }
        } catch (e: PosException) {
            if (e.kind == PosException.Kind.UNAUTHORIZED) settings.clearStaffSession()
            throw e
        }
    }

    // ---- 端末登録・ログイン ----

    suspend fun enroll(serverUrl: String, code: String, deviceName: String) {
        val url = ApiClient.normalizeBaseUrl(serverUrl)
        val res = apiCall { client.api(url).enroll(EnrollRequest(code.trim(), deviceName.trim())) }
        db.clearMaster()
        settings.saveEnrollment(url, res.deviceToken, res.device.name, res.store.name)
        runCatching { syncMaster(force = true) }
    }

    suspend fun staffDirectory(): List<StaffDto> {
        val (api, s) = enrolled()
        return apiCall { api.staffDirectory(bearer(s.deviceToken)) }.staff
    }

    suspend fun login(staffId: String, pin: String) {
        val (api, s) = enrolled()
        val res = apiCall { api.login(bearer(s.deviceToken), LoginRequest(staffId, pin)) }
        settings.saveStaffSession(res.staffToken, res.staff.id, res.staff.displayName, res.staff.role)
        runCatching { syncMaster(force = false) }
    }

    suspend fun logout() {
        runCatching { staffCall { api, auth -> api.logout(auth) } }
        settings.clearStaffSession()
    }

    /** 端末の登録を解除（端末内のトークンとマスターを消す。サーバー側の失効は管理画面で行う） */
    suspend fun unenroll() {
        settings.clearAll()
        db.clearMaster()
    }

    // ---- マスター ----

    /** マスターを取得して端末内に保存。変更がなければ何もしない。戻り値は最新バージョン */
    suspend fun syncMaster(force: Boolean): Int? {
        val (api, s) = enrolled()
        val token = s.staffToken ?: s.deviceToken
        val res = apiCall { api.master(bearer(token), if (force) null else s.masterEtag) }
        if (res.code() == 304) return null
        if (!res.isSuccessful) {
            val err = res.toPosException()
            if (err.kind == PosException.Kind.UNAUTHORIZED && s.staffToken != null) settings.clearStaffSession()
            throw err
        }
        val m = res.body() ?: throw PosException(PosException.Kind.SERVER, null, "マスターが空です")
        val base = m.store.currencies.firstOrNull { it.isBase }
        db.replaceMaster(
            meta = MasterMetaEntity(
                version = m.version,
                storeId = m.store.id,
                storeName = m.store.name,
                baseCurrency = m.store.baseCurrency,
                minorUnit = base?.minorUnit ?: 0,
            ),
            tables = m.tables.map { TableEntity(it.id, it.code, it.name, it.sortOrder) },
            categories = m.categories.map { CategoryEntity(it.id, it.name, it.sortOrder) },
            items = m.items.map {
                MenuItemEntity(it.id, it.categoryId, it.name, it.price.amount, it.price.currency, it.sortOrder)
            },
        )
        settings.saveMasterEtag(res.headers()["ETag"])
        return m.version
    }

    // ---- 注文・会計 ----

    /** clientRequestId は再送時に同じ値を使うこと（二重登録防止） */
    suspend fun placeOrder(clientRequestId: String, tableId: String, lines: List<CartLine>): OrderResponse =
        staffCall { api, auth ->
            api.placeOrder(
                auth,
                OrderRequest(clientRequestId, tableId, lines.map { OrderLineRequest(it.menuItemId, it.quantity) }),
            )
        }

    /** 未会計の伝票。なければ null */
    suspend fun openCheck(tableId: String): CheckDto? = staffCall { api, auth ->
        val res = api.openCheck(auth, tableId)
        when {
            res.isSuccessful -> res.body()
            res.code() == 404 -> null
            else -> throw res.toPosException()
        }
    }

    suspend fun pay(clientRequestId: String, check: CheckDto, tendered: Long): PaymentResponse =
        staffCall { api, auth ->
            api.pay(
                auth,
                check.id,
                PaymentRequest(
                    clientRequestId = clientRequestId,
                    expectedTotal = check.total,
                    tendered = listOf(MoneyDto(tendered, check.total.currency)),
                ),
            )
        }
}
