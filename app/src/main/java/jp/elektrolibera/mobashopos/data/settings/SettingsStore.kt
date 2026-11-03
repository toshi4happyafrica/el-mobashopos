package jp.elektrolibera.mobashopos.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pos_settings")

/** 端末の登録状態と店員のログイン状態 */
data class SessionState(
    val serverUrl: String?,
    val deviceToken: String?,
    val deviceName: String?,
    val storeName: String?,
    val staffToken: String?,
    val staffId: String?,
    val staffName: String?,
    val staffRole: String?,
    val masterEtag: String?,
) {
    val isEnrolled: Boolean get() = !serverUrl.isNullOrBlank() && !deviceToken.isNullOrBlank()
    val isLoggedIn: Boolean get() = isEnrolled && !staffToken.isNullOrBlank()
    val isManager: Boolean get() = staffRole == "manager"
}

/**
 * 設定とトークンの保存先。
 * 注意：トークンはアプリ専用領域に保存している（自動バックアップは無効）。
 * 商用化前に Android Keystore による暗号化を検討する（未対応）。
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val serverUrl = stringPreferencesKey("server_url")
        val deviceToken = stringPreferencesKey("device_token")
        val deviceName = stringPreferencesKey("device_name")
        val storeName = stringPreferencesKey("store_name")
        val staffToken = stringPreferencesKey("staff_token")
        val staffId = stringPreferencesKey("staff_id")
        val staffName = stringPreferencesKey("staff_name")
        val staffRole = stringPreferencesKey("staff_role")
        val masterEtag = stringPreferencesKey("master_etag")
    }

    val state: Flow<SessionState> = context.dataStore.data.map { p ->
        SessionState(
            serverUrl = p[Keys.serverUrl],
            deviceToken = p[Keys.deviceToken],
            deviceName = p[Keys.deviceName],
            storeName = p[Keys.storeName],
            staffToken = p[Keys.staffToken],
            staffId = p[Keys.staffId],
            staffName = p[Keys.staffName],
            staffRole = p[Keys.staffRole],
            masterEtag = p[Keys.masterEtag],
        )
    }

    suspend fun current(): SessionState = state.first()

    suspend fun saveEnrollment(serverUrl: String, deviceToken: String, deviceName: String, storeName: String) {
        context.dataStore.edit { p ->
            p.clear()
            p[Keys.serverUrl] = serverUrl
            p[Keys.deviceToken] = deviceToken
            p[Keys.deviceName] = deviceName
            p[Keys.storeName] = storeName
        }
    }

    suspend fun saveStaffSession(token: String, staffId: String, name: String, role: String) {
        context.dataStore.edit { p ->
            p[Keys.staffToken] = token
            p[Keys.staffId] = staffId
            p[Keys.staffName] = name
            p[Keys.staffRole] = role
        }
    }

    suspend fun clearStaffSession() {
        context.dataStore.edit { p ->
            p.remove(Keys.staffToken)
            p.remove(Keys.staffId)
            p.remove(Keys.staffName)
            p.remove(Keys.staffRole)
        }
    }

    suspend fun saveMasterEtag(etag: String?) {
        context.dataStore.edit { p ->
            if (etag == null) p.remove(Keys.masterEtag) else p[Keys.masterEtag] = etag
        }
    }

    /** 端末登録の解除（トークン類をすべて消す） */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
