package jp.elektrolibera.mobashopos.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/** mobaSHOPOS サーバー API v1。認証ヘッダーは呼び出し側で "Bearer xxx" を渡す */
interface PosApi {

    @POST("api/v1/devices/enroll")
    suspend fun enroll(@Body body: EnrollRequest): EnrollResponse

    @GET("api/v1/staff-directory")
    suspend fun staffDirectory(@Header("Authorization") auth: String): StaffDirectoryResponse

    @POST("api/v1/sessions")
    suspend fun login(@Header("Authorization") auth: String, @Body body: LoginRequest): LoginResponse

    @DELETE("api/v1/sessions/current")
    suspend fun logout(@Header("Authorization") auth: String): Response<Unit>

    /** 変更がなければ 304（本文なし） */
    @GET("api/v1/master")
    suspend fun master(
        @Header("Authorization") auth: String,
        @Header("If-None-Match") etag: String?,
    ): Response<MasterResponse>

    @POST("api/v1/orders")
    suspend fun placeOrder(@Header("Authorization") auth: String, @Body body: OrderRequest): OrderResponse

    /** 未会計の伝票がなければ 404 */
    @GET("api/v1/tables/{tableId}/open-check")
    suspend fun openCheck(
        @Header("Authorization") auth: String,
        @Path("tableId") tableId: String,
    ): Response<CheckDto>

    @POST("api/v1/checks/{checkId}/payments")
    suspend fun pay(
        @Header("Authorization") auth: String,
        @Path("checkId") checkId: String,
        @Body body: PaymentRequest,
    ): PaymentResponse
}
