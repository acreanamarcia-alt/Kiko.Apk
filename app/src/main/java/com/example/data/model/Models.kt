package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val role: String = "cliente",
    val businessSegment: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppProduct(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val niche: String = "Lanchonete",
    val shortDescription: String = "",
    val fullDescription: String = "",
    val price: Double = 0.0,
    val monthlyFee: Double = 0.0,
    val stockCount: Long = 0L,
    val salesCount: Long = 0L,
    val coverKey: String = "lanchonete",
    val coverUri: String? = null,
    val features: List<String> = emptyList(),
    val active: Boolean = true,
    val rating: Double = 4.9,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerId: String): Map<String, Any> {
        return mapOf(
            "ownerId" to ownerId,
            "title" to title.trim(),
            "niche" to niche,
            "shortDescription" to shortDescription.trim(),
            "fullDescription" to fullDescription.trim(),
            "price" to price,
            "monthlyFee" to monthlyFee,
            "stockCount" to stockCount,
            "salesCount" to salesCount,
            "coverKey" to coverKey,
            "coverUri" to coverUri?.takeIf { it.isNotBlank() },
            "features" to features.take(10),
            "active" to active,
            "rating" to rating,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class AppOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerDocument: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val niche: String = "",
    val quantity: Long = 1L,
    val unitPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val gatewayFee: Double = 0.0,
    val sellerCommission: Double = 0.0,
    val netAmount: Double = 0.0,
    val paymentMethod: String = "PIX",
    val paymentStatus: String = "PENDING",
    val gatewayProvider: String = "MERCADO_PAGO",
    val gatewayTransactionId: String = "",
    val pixCopyPaste: String? = null,
    val cardLastFour: String? = null,
    val installments: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "customerName" to customerName,
            "customerEmail" to customerEmail,
            "customerDocument" to customerDocument,
            "productId" to productId,
            "productTitle" to productTitle,
            "niche" to niche,
            "quantity" to quantity,
            "unitPrice" to unitPrice,
            "totalAmount" to totalAmount,
            "gatewayFee" to gatewayFee,
            "sellerCommission" to sellerCommission,
            "netAmount" to netAmount,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to paymentStatus,
            "gatewayProvider" to gatewayProvider,
            "gatewayTransactionId" to gatewayTransactionId,
            "pixCopyPaste" to pixCopyPaste?.takeIf { it.isNotBlank() },
            "cardLastFour" to cardLastFour?.takeIf { it.isNotBlank() },
            "installments" to installments,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class ClientVisit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientAddress: String = "",
    val niche: String = "Lanchonete",
    val appProductId: String? = null,
    val appTitle: String = "",
    val consultantName: String = "",
    val scheduledDateIso: String = "",
    val scheduledTime: String = "",
    val visitType: String = "PRESENCIAL",
    val status: String = "AGENDADA",
    val notes: String = "",
    val estimatedValue: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "clientName" to clientName.trim(),
            "clientPhone" to clientPhone.trim(),
            "clientAddress" to clientAddress.trim(),
            "niche" to niche,
            "appProductId" to appProductId?.takeIf { it.isNotBlank() },
            "appTitle" to appTitle.trim(),
            "consultantName" to consultantName.trim(),
            "scheduledDateIso" to scheduledDateIso,
            "scheduledTime" to scheduledTime,
            "visitType" to visitType,
            "status" to status,
            "notes" to notes.trim(),
            "estimatedValue" to estimatedValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class FinancialEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "VENDA_APP",
    val entryType: String = "RECEITA",
    val profileScope: String = "ADMIN",
    val amount: Double = 0.0,
    val relatedOrderId: String? = null,
    val niche: String? = null,
    val referenceMonth: String = "2026-11",
    val status: String = "CONFIRMADO",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "title" to title.trim(),
            "category" to category,
            "entryType" to entryType,
            "profileScope" to profileScope,
            "amount" to amount,
            "relatedOrderId" to relatedOrderId?.takeIf { it.isNotBlank() },
            "niche" to niche?.takeIf { it.isNotBlank() },
            "referenceMonth" to referenceMonth,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class PushNotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val targetSegment: String = "TODOS",
    val campaignType: String = "PROMOCAO",
    val sentBy: String = "Painel Admin",
    val isRead: Boolean = false,
    val dispatchedToDevice: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "title" to title.trim(),
            "body" to body.trim(),
            "targetSegment" to targetSegment,
            "campaignType" to campaignType,
            "sentBy" to sentBy,
            "isRead" to isRead,
            "dispatchedToDevice" to dispatchedToDevice,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

enum class UserProfileView(val label: String, val scopeKey: String) {
    ADMIN("Administrador", "ADMIN"),
    SELLER("Vendedor / Consultor", "VENDEDOR"),
    CLIENT("Cliente", "CLIENTE")
}

object AppNiches {
    val ALL = listOf(
        "Lanchonete",
        "Salão de Beleza",
        "Barbearia",
        "Distribuidora",
        "Marmitex",
        "Outro"
    )
}
