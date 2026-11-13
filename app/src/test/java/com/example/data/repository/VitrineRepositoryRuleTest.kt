package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AppProduct
import com.example.data.model.ClientVisit
import com.example.data.model.PushNotificationItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class VitrineRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createProduct_andCheckoutWithGateway_succeedsAndUpdatesInventory() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VitrineRepository(firestore, auth)

        val product = AppProduct(
            title = "App Sanduba ${UUID.randomUUID().toString().take(5)}",
            niche = "Lanchonete",
            shortDescription = "Delivery rápido para lanchonetes",
            fullDescription = "Cardápio digital, rastreio e pagamento em tempo real.",
            price = 1490.0,
            monthlyFee = 149.0,
            stockCount = 10L,
            salesCount = 0L,
            coverKey = "lanchonete",
            features = listOf("Cardápio Digital", "PIX"),
            active = true,
            rating = 4.9
        )

        val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProduct(product).getOrThrow()
        }
        assertTrue(productId.isNotBlank())

        val createdProduct = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProductById(productId).getOrThrow()
        }
        assertNotNull(createdProduct)
        assertEquals(10L, createdProduct!!.stockCount)

        val order = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.checkoutWithGateway(
                product = createdProduct,
                quantity = 2,
                customerName = "Alice Costa",
                customerEmail = ALICE_EMAIL,
                customerCpf = "12345678909",
                paymentMethod = "PIX",
                installments = 1,
                cardLastFour = null
            ).getOrThrow()
        }
        assertEquals("PAID", order.paymentStatus)
        assertEquals(2980.0, order.totalAmount, 0.01)

        val updatedProduct = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProductById(productId).getOrThrow()
        }
        assertEquals(8L, updatedProduct!!.stockCount)
        assertEquals(2L, updatedProduct.salesCount)

        val observedProducts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProducts(uid).first { list -> list.any { it.id == productId } }
        }
        assertTrue(observedProducts.any { it.id == productId })
    }

    @Test
    fun createVisitAndNotification_authenticatedOwner_emitsInFlows() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VitrineRepository(firestore, auth)

        val visitId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createVisit(
                ClientVisit(
                    clientName = "Barbearia Corte Fino",
                    clientPhone = "(11) 99999-0000",
                    clientAddress = "Rua das Flores, 55",
                    niche = "Barbearia",
                    appTitle = "BarberClub Black",
                    consultantName = "Marcos Silva",
                    scheduledDateIso = "2026-11-22",
                    scheduledTime = "15:00",
                    visitType = "PRESENCIAL",
                    status = "AGENDADA",
                    notes = "Apresentar clube de assinatura",
                    estimatedValue = 1390.0
                )
            ).getOrThrow()
        }

        val visits = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeVisits(uid).first { list -> list.any { it.id == visitId } }
        }
        assertTrue(visits.any { it.id == visitId })

        val notifId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendPushNotification(
                PushNotificationItem(
                    title = "Promoção Marmitex",
                    body = "Desconto especial na implantação.",
                    targetSegment = "Marmitex",
                    campaignType = "PROMOCAO",
                    sentBy = "Admin"
                )
            ).getOrThrow()
        }
        val notifications = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeNotifications(uid).first { list -> list.any { it.id == notifId } }
        }
        assertTrue(notifications.any { it.id == notifId })
    }

    @Test
    fun getProductById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = VitrineRepository(firestore, auth)
        val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createProduct(
                AppProduct(
                    title = "App Privado Alice",
                    niche = "Salão de Beleza",
                    shortDescription = "Agenda para salão",
                    fullDescription = "Sistema completo de salão.",
                    price = 1690.0,
                    monthlyFee = 169.0,
                    stockCount = 5L,
                    coverKey = "salao",
                    features = listOf("Agenda")
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = VitrineRepository(firestore, auth)
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
        val repository = VitrineRepository(firestore, auth)

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
        const val ALICE_EMAIL = "alice_vitrine@test.com"
        const val BOB_EMAIL = "bob_vitrine@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
