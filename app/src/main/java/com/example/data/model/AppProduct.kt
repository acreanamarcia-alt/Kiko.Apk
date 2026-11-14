package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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
    constructor(
        name: String,
        niche: String,
        description: String,
        price: Double,
        imageUrl: String? = null
    ) : this(
        title = name,
        niche = niche,
        shortDescription = description.take(240),
        fullDescription = description,
        price = price,
        coverKey = defaultCoverKeyForNiche(niche),
        coverUri = imageUrl?.takeIf { it.isNotBlank() }
    )

    @get:Exclude
    val name: String
        get() = title

    @get:Exclude
    val description: String
        get() = fullDescription.ifBlank { shortDescription }

    @get:Exclude
    val imageUrl: String
        get() = coverUri?.takeIf { it.isNotBlank() } ?: coverKey

    @get:Exclude
    val imageURL: String
        get() = imageUrl

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

    companion object {
        fun defaultCoverKeyForNiche(niche: String): String {
            return when (niche) {
                "Lanchonete" -> "lanchonete"
                "Salão de Beleza" -> "salao"
                "Barbearia" -> "barbearia"
                "Distribuidora" -> "distribuidora"
                "Marmitex" -> "marmitex"
                else -> "lanchonete"
            }
        }
    }
}
