package jp.elektrolibera.mobashopos.data.api

import jp.elektrolibera.mobashopos.BuildConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

val apiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** 接続先 URL ごとに Retrofit を作り直して使い回す */
class ApiClient {
    private var cachedUrl: String? = null
    private var cachedApi: PosApi? = null

    private val okHttp: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            // ヘッダー（トークン）はログに出さない
            builder.addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        }
        builder.build()
    }

    @Synchronized
    fun api(baseUrl: String): PosApi {
        val url = normalizeBaseUrl(baseUrl)
        if (url.toHttpUrlOrNull() == null) {
            throw PosException(PosException.Kind.INVALID, null, "サーバーURLが正しくありません（例 http://10.0.2.2:8787/）")
        }
        val current = cachedApi
        if (current != null && cachedUrl == url) return current
        val api = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttp)
            .addConverterFactory(apiJson.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .build()
            .create(PosApi::class.java)
        cachedUrl = url
        cachedApi = api
        return api
    }

    companion object {
        fun normalizeBaseUrl(raw: String): String {
            val trimmed = raw.trim()
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
    }
}

/** 画面に表示できる形に整えた API エラー */
class PosException(
    val kind: Kind,
    val code: String?,
    override val message: String,
    val httpStatus: Int? = null,
) : Exception(message) {
    enum class Kind { NETWORK, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, LOCKED, INVALID, SERVER }
}

fun kindOf(status: Int): PosException.Kind = when (status) {
    401 -> PosException.Kind.UNAUTHORIZED
    403 -> PosException.Kind.FORBIDDEN
    404 -> PosException.Kind.NOT_FOUND
    409 -> PosException.Kind.CONFLICT
    423 -> PosException.Kind.LOCKED
    400, 422 -> PosException.Kind.INVALID
    else -> PosException.Kind.SERVER
}

private fun parseError(status: Int, body: String?): PosException {
    val parsed = body?.let { runCatching { apiJson.decodeFromString<ErrorEnvelope>(it) }.getOrNull() }
    val message = parsed?.error?.message ?: "サーバーエラー（HTTP $status）"
    return PosException(kindOf(status), parsed?.error?.code, message, status)
}

fun Response<*>.toPosException(): PosException = parseError(code(), errorBody()?.string())

/** API 呼び出しの例外を PosException にそろえる */
suspend fun <T> apiCall(block: suspend () -> T): T {
    try {
        return block()
    } catch (e: HttpException) {
        throw parseError(e.code(), e.response()?.errorBody()?.string())
    } catch (e: PosException) {
        throw e
    } catch (e: IOException) {
        throw PosException(PosException.Kind.NETWORK, null, "サーバーに接続できません（${e.message ?: "通信エラー"}）")
    } catch (e: kotlinx.serialization.SerializationException) {
        throw PosException(PosException.Kind.SERVER, null, "サーバーの応答を読み取れません")
    } catch (e: IllegalArgumentException) {
        // SerializationException（IllegalArgumentException の子）より後に置くこと
        throw PosException(PosException.Kind.INVALID, null, e.message ?: "入力内容が正しくありません")
    }
}
