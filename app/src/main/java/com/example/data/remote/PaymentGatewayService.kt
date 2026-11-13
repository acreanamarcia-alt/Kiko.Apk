package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.UUID
import java.util.concurrent.TimeUnit

data class GatewayPayerRequest(
    val email: String,
    @Json(name = "first_name") val firstName: String,
    val identification: GatewayIdentificationRequest? = null
)

data class GatewayIdentificationRequest(
    val type: String = "CPF",
    val number: String
)

data class GatewayPaymentRequest(
    @Json(name = "transaction_amount") val transactionAmount: Double,
    val description: String,
    @Json(name = "payment_method_id") val paymentMethodId: String,
    val installments: Int = 1,
    val token: String? = null,
    val payer: GatewayPayerRequest
)

data class GatewayTransactionData(
    @Json(name = "qr_code") val qrCode: String? = null,
    @Json(name = "ticket_url") val ticketUrl: String? = null
)

data class GatewayPointOfInteraction(
    @Json(name = "transaction_data") val transactionData: GatewayTransactionData? = null
)

data class GatewayPaymentResponse(
    val id: Long? = null,
    val status: String? = null,
    @Json(name = "status_detail") val statusDetail: String? = null,
    @Json(name = "point_of_interaction") val pointOfInteraction: GatewayPointOfInteraction? = null
)

interface MercadoPagoApi {
    @POST("v1/payments")
    suspend fun createPayment(
        @Header("Authorization") authorization: String,
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body request: GatewayPaymentRequest
    ): Response<GatewayPaymentResponse>
}

data class PaymentGatewayResult(
    val transactionId: String,
    val provider: String,
    val status: String,
    val statusMessage: String,
    val pixCopyPaste: String? = null,
    val isLiveGateway: Boolean
)

class PaymentGatewayClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val api: MercadoPagoApi = Retrofit.Builder()
        .baseUrl("https://api.mercadopago.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(MercadoPagoApi::class.java)

    fun hasLiveGatewayKey(): Boolean {
        val key = BuildConfig.PAYMENT_GATEWAY_API_KEY
        return key.isNotBlank() && key != "SANDBOX_DEMO_GATEWAY" && !key.startsWith("YOUR_")
    }

    suspend fun processPayment(
        amount: Double,
        description: String,
        paymentMethod: String,
        installments: Int,
        customerName: String,
        customerEmail: String,
        customerCpf: String,
        cardToken: String? = null
    ): Result<PaymentGatewayResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.PAYMENT_GATEWAY_API_KEY
        val methodId = when (paymentMethod) {
            "PIX" -> "pix"
            "BOLETO" -> "bolbradesco"
            else -> "visa"
        }

        if (hasLiveGatewayKey()) {
            try {
                val cleanCpf = customerCpf.filter { it.isDigit() }.ifBlank { "12345678909" }
                val request = GatewayPaymentRequest(
                    transactionAmount = amount,
                    description = description,
                    paymentMethodId = methodId,
                    installments = if (paymentMethod == "CREDIT_CARD") installments else 1,
                    token = cardToken,
                    payer = GatewayPayerRequest(
                        email = customerEmail.ifBlank { "cliente@vitrineapps.com" },
                        firstName = customerName.split(" ").firstOrNull().orEmpty().ifBlank { "Cliente" },
                        identification = GatewayIdentificationRequest(number = cleanCpf)
                    )
                )
                val response = api.createPayment(
                    authorization = "Bearer $apiKey",
                    idempotencyKey = UUID.randomUUID().toString(),
                    request = request
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val normalizedStatus = when (body.status?.lowercase()) {
                        "approved" -> "PAID"
                        "rejected", "cancelled" -> "FAILED"
                        else -> "PENDING"
                    }
                    return@withContext Result.success(
                        PaymentGatewayResult(
                            transactionId = "MP-${body.id ?: UUID.randomUUID().toString().take(8)}",
                            provider = "MERCADO_PAGO",
                            status = normalizedStatus,
                            statusMessage = body.statusDetail ?: "Transação enviada ao gateway em tempo real",
                            pixCopyPaste = body.pointOfInteraction?.transactionData?.qrCode,
                            isLiveGateway = true
                        )
                    )
                } else {
                    val errBody = response.errorBody()?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("Gateway HTTP ${response.code()}: ${errBody.take(140)}")
                    )
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        // Real-time local gateway sandbox settlement when live secret is not configured
        val txId = "SBX-${UUID.randomUUID().toString().uppercase().take(10)}"
        val pixCode = if (paymentMethod == "PIX") {
            "00020126580014BR.GOV.BCB.PIX0136$txId" +
                "520400005303986540${"%.2f".format(amount).replace(",", ".")}5802BR5911VITRINEAPPS6009SAO_PAULO6304A1B2"
        } else null
        val status = if (paymentMethod == "BOLETO") "PENDING" else "PAID"
        Result.success(
            PaymentGatewayResult(
                transactionId = txId,
                provider = "SANDBOX_GATEWAY",
                status = status,
                statusMessage = if (status == "PAID") {
                    "Aprovado em modo Sandbox (Configure PAYMENT_GATEWAY_API_KEY em Secrets para produção)"
                } else {
                    "Boleto gerado em modo Sandbox aguardando compensação"
                },
                pixCopyPaste = pixCode,
                isLiveGateway = false
            )
        )
    }
}
