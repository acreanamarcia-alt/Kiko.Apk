package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AppProduct
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AppProductRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createGetAndUpdateStock_authenticatedOwner_persistsInFirestore() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = AppProductRepository(firestore, auth)

        val product = AppProduct(
            title = "App Marmitex ${UUID.randomUUID().toString().take(5)}",
            niche = "Marmitex",
            shortDescription = "Cardápio diário com assinatura semanal",
            fullDescription = "Gestão completa de marmitex com cardápio dinâmico e PIX.",
            price = 1590.0,
            monthlyFee = 159.0,
            stockCount = 12L,
            salesCount = 0L,
            coverKey = "marmitex",
            features = listOf("Cardápio Diário", "PIX"),
            active = true,
            rating = 4.9
        )

        val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProduct(product).getOrThrow()
        }
        assertTrue(productId.isNotBlank())

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProductById(productId).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals(12L, fetched!!.stockCount)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateStock(productId, 20L).getOrThrow()
        }
        val afterUpdate = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProductById(productId).getOrThrow()
        }
        assertEquals(20L, afterUpdate!!.stockCount)

        val adjusted = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.adjustStock(productId, -5L).getOrThrow()
        }
        assertEquals(15L, adjusted)

        val clampedZero = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.adjustStock(productId, -99L).getOrThrow()
        }
        assertEquals(0L, clampedZero)
    }

    @Test
    fun observeLowStock_setActiveAndDelete_updatesRealtimeStreams() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = AppProductRepository(firestore, auth)

        val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProduct(
                AppProduct(
                    title = "Gás Express ${UUID.randomUUID().toString().take(5)}",
                    niche = "Distribuidora",
                    shortDescription = "Recompra de gás e bebidas em 1 clique",
                    fullDescription = "App para distribuidoras de bebidas e gás.",
                    price = 1890.0,
                    monthlyFee = 189.0,
                    stockCount = 3L,
                    coverKey = "distribuidora",
                    features = listOf("Recompra Rápida")
                )
            ).getOrThrow()
        }

        val lowStockList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLowStockProducts(threshold = 5L).first { list ->
                list.any { it.id == productId }
            }
        }
        assertTrue(lowStockList.any { it.id == productId })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setProductActive(productId, false).getOrThrow()
        }
        val paused = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProductById(productId).getOrThrow()
        }
        assertFalse(paused!!.active)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteProduct(productId).getOrThrow()
        }
        val deleted = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProductById(productId).getOrThrow()
        }
        assertNull(deleted)
    }

    @Test
    fun getProductById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = AppProductRepository(firestore, auth)
        val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createProduct(
                AppProduct(
                    title = "App Privado ${UUID.randomUUID().toString().take(5)}",
                    niche = "Barbearia",
                    shortDescription = "Agendamento rápido para barbearia",
                    fullDescription = "Clube de assinatura e fidelidade.",
                    price = 1390.0,
                    monthlyFee = 129.0,
                    stockCount = 7L,
                    coverKey = "barbearia",
                    features = listOf("Agenda")
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = AppProductRepository(firestore, auth)
        var captured: Throwable? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getProductById(productId).getOrThrow()
            }
        } catch (t: Throwable) {
            captured = t
        }

        val firestoreException = captured?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException for cross-user read", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    @Test
    fun observeProducts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = AppProductRepository(firestore, auth)

        var captured: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProducts("some_uid").first()
            }
        } catch (t: Throwable) {
            captured = t
        }

        val firestoreException = captured?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $captured", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_product_repo@test.com"
        const val BOB_EMAIL = "bob_product_repo@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
