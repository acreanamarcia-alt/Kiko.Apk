package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppProduct
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repositório Firestore dedicado à leitura, gravação e controle de estoque de [AppProduct].
 */
class AppProductRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário precisa estar autenticado com Google.")
    }

    fun observeProducts(ownerId: String? = null): Flow<List<AppProduct>> = flow {
        val targetOwnerId = ownerId ?: requireUserId()
        emitAll(
            db.collection(COLLECTION_PRODUCTS)
                .whereEqualTo("ownerId", targetOwnerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AppProduct::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.salesCount }
                }
                .catch { error -> logAndRethrow(error, OperationType.LIST, COLLECTION_PRODUCTS, targetOwnerId) }
        )
    }

    fun observeProductsByNiche(niche: String, ownerId: String? = null): Flow<List<AppProduct>> {
        return observeProducts(ownerId).map { list ->
            if (niche == "Todos") list else list.filter { it.niche == niche }
        }
    }

    fun observeLowStockProducts(threshold: Long = 5L, ownerId: String? = null): Flow<List<AppProduct>> {
        return observeProducts(ownerId).map { list ->
            list.filter { it.stockCount <= threshold }
        }
    }

    suspend fun getProducts(ownerId: String? = null): Result<List<AppProduct>> {
        val targetOwnerId = try {
            ownerId ?: requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val snap = db.collection(COLLECTION_PRODUCTS)
                .whereEqualTo("ownerId", targetOwnerId)
                .get()
                .await()
            val products = snap.toObjects(
                AppProduct::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.salesCount }
            Result.success(products)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_PRODUCTS)
            Result.failure(e)
        }
    }

    suspend fun getProductById(productId: String): Result<AppProduct?> {
        require(productId.isNotBlank()) { "ID do aplicativo obrigatório para leitura." }
        val docRef = db.collection(COLLECTION_PRODUCTS).document(productId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(AppProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createProduct(product: AppProduct): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val id = "app_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection(COLLECTION_PRODUCTS).document(id)
        return try {
            docRef.set(product.toCreateMap(uid)).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: AppProduct): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(product.id.isNotBlank()) { "ID do produto obrigatório para atualização" }
        val docRef = db.collection(COLLECTION_PRODUCTS).document(product.id)
        return try {
            val updates = mutableMapOf<String, Any?>(
                "title" to product.title.trim(),
                "niche" to product.niche,
                "shortDescription" to product.shortDescription.trim(),
                "fullDescription" to product.fullDescription.trim(),
                "price" to product.price,
                "monthlyFee" to product.monthlyFee,
                "stockCount" to product.stockCount.coerceIn(0L, MAX_STOCK_LIMIT),
                "salesCount" to product.salesCount.coerceAtLeast(0L),
                "coverKey" to product.coverKey,
                "coverUri" to product.coverUri?.takeIf { it.isNotBlank() },
                "features" to product.features.take(10),
                "active" to product.active,
                "rating" to product.rating,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateStock(productId: String, newStockCount: Long): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(productId.isNotBlank()) { "ID do aplicativo obrigatório para atualização de estoque." }
        val clampedStock = newStockCount.coerceIn(0L, MAX_STOCK_LIMIT)
        val docRef = db.collection(COLLECTION_PRODUCTS).document(productId)
        return try {
            docRef.update(
                mapOf(
                    "stockCount" to clampedStock,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adjustStock(productId: String, delta: Long): Result<Long> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(productId.isNotBlank()) { "ID do aplicativo obrigatório para ajuste de estoque." }
        val docRef = db.collection(COLLECTION_PRODUCTS).document(productId)
        return try {
            val updatedStock = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentStock = snapshot.getLong("stockCount") ?: 0L
                val nextStock = (currentStock + delta).coerceIn(0L, MAX_STOCK_LIMIT)
                transaction.update(
                    docRef,
                    mapOf(
                        "stockCount" to nextStock,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                nextStock
            }.await()
            Result.success(updatedStock)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setProductActive(productId: String, active: Boolean): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(productId.isNotBlank()) { "ID do aplicativo obrigatório." }
        val docRef = db.collection(COLLECTION_PRODUCTS).document(productId)
        return try {
            docRef.update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(productId.isNotBlank()) { "ID do aplicativo obrigatório para exclusão." }
        val docRef = db.collection(COLLECTION_PRODUCTS).document(productId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun logAndRethrow(
        error: Throwable,
        op: OperationType,
        path: String,
        expectedUid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    companion object {
        const val COLLECTION_PRODUCTS = "products"
        const val MAX_STOCK_LIMIT = 100_000L
    }
}
