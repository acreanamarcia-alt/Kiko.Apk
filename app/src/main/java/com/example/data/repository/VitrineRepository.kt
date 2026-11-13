package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppOrder
import com.example.data.model.AppProduct
import com.example.data.model.ClientVisit
import com.example.data.model.FinancialEntry
import com.example.data.model.PushNotificationItem
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.PaymentGatewayClient
import com.example.data.remote.PaymentGatewayResult
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
import kotlin.math.roundToLong

class VitrineRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val paymentGatewayClient: PaymentGatewayClient = PaymentGatewayClient()
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

    fun hasLiveGatewayKey(): Boolean = paymentGatewayClient.hasLiveGatewayKey()

    suspend fun ensureUserProfile(): Result<UserProfile> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Não autenticado"))
        val docRef = db.collection("users").document(user.uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                Result.success(existing ?: UserProfile(id = user.uid, userId = user.uid))
            } else {
                val payload = mapOf(
                    "userId" to user.uid,
                    "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "Empreendedor"),
                    "email" to (user.email ?: "usuario@vitrineapps.com"),
                    "photoUrl" to user.photoUrl?.toString(),
                    "role" to "cliente",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
                val created = docRef.get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                Result.success(created ?: UserProfile(id = user.uid, userId = user.uid))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Real-time Streams ---

    fun observeProducts(ownerId: String): Flow<List<AppProduct>> = flow {
        val path = "products"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerId", ownerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.salesCount }
                }
                .catch { error -> logAndRethrow(error, OperationType.LIST, path, ownerId) }
        )
    }

    fun observeOrders(userId: String): Flow<List<AppOrder>> = flow {
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logAndRethrow(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeVisits(userId: String): Flow<List<ClientVisit>> = flow {
        val path = "visits"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ClientVisit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.scheduledDateIso }
                }
                .catch { error -> logAndRethrow(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeFinancialEntries(userId: String): Flow<List<FinancialEntry>> = flow {
        val path = "financial_entries"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FinancialEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logAndRethrow(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeNotifications(userId: String): Flow<List<PushNotificationItem>> = flow {
        val path = "notifications"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PushNotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logAndRethrow(error, OperationType.LIST, path, userId) }
        )
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

    // --- Products CRUD (Admin & Store) ---

    suspend fun createProduct(product: AppProduct): Result<String> {
        val uid = requireUserId()
        val id = "app_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("products").document(id)
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
        val uid = requireUserId()
        require(product.id.isNotBlank()) { "ID do produto obrigatório para atualização" }
        val docRef = db.collection("products").document(product.id)
        return try {
            val payload = if (product.createdAt != null) {
                product.copy(ownerId = uid, updatedAt = null)
            } else {
                val snap = docRef.get().await()
                val existingCreatedAt = snap.getTimestamp("createdAt")
                    ?: return Result.failure(IllegalStateException("Produto sem createdAt"))
                product.copy(ownerId = uid, createdAt = existingCreatedAt, updatedAt = null)
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("products").document(productId)
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

    suspend fun getProductById(productId: String): Result<AppProduct?> {
        val docRef = db.collection("products").document(productId)
        return try {
            val snap = docRef.get().await()
            Result.success(snap.toObject(AppProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    // --- Real-Time Payment Gateway & Order Checkout ---

    suspend fun checkoutWithGateway(
        product: AppProduct,
        quantity: Int,
        customerName: String,
        customerEmail: String,
        customerCpf: String,
        paymentMethod: String,
        installments: Int,
        cardLastFour: String?
    ): Result<AppOrder> {
        val uid = requireUserId()
        if (product.stockCount < quantity) {
            return Result.failure(IllegalStateException("Estoque de licenças insuficiente para ${product.title}."))
        }

        val totalAmount = roundMoney(product.price * quantity)
        val gatewayResult = paymentGatewayClient.processPayment(
            amount = totalAmount,
            description = "${product.title} ($quantity licenca)",
            paymentMethod = paymentMethod,
            installments = installments,
            customerName = customerName,
            customerEmail = customerEmail,
            customerCpf = customerCpf
        ).getOrElse { return Result.failure(it) }

        val feeRate = when (paymentMethod) {
            "PIX" -> 0.0099
            "CREDIT_CARD" -> 0.0399
            else -> 0.0
        }
        val gatewayFee = if (paymentMethod == "BOLETO") 3.90 else roundMoney(totalAmount * feeRate)
        val sellerCommission = roundMoney(totalAmount * 0.10)
        val netAmount = roundMoney((totalAmount - gatewayFee - sellerCommission).coerceAtLeast(0.0))

        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val order = AppOrder(
            id = orderId,
            userId = uid,
            customerName = customerName.trim(),
            customerEmail = customerEmail.trim(),
            customerDocument = customerCpf.trim(),
            productId = product.id,
            productTitle = product.title,
            niche = product.niche,
            quantity = quantity.toLong(),
            unitPrice = product.price,
            totalAmount = totalAmount,
            gatewayFee = gatewayFee,
            sellerCommission = sellerCommission,
            netAmount = netAmount,
            paymentMethod = paymentMethod,
            paymentStatus = gatewayResult.status,
            gatewayProvider = gatewayResult.provider,
            gatewayTransactionId = gatewayResult.transactionId,
            pixCopyPaste = gatewayResult.pixCopyPaste,
            cardLastFour = cardLastFour?.takeLast(4),
            installments = installments.toLong()
        )

        return try {
            val batch = db.batch()
            val orderRef = db.collection("orders").document(orderId)
            batch.set(orderRef, order.toCreateMap())

            val productRef = db.collection("products").document(product.id)
            batch.update(
                productRef,
                mapOf(
                    "stockCount" to FieldValue.increment(-quantity.toLong()),
                    "salesCount" to FieldValue.increment(quantity.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val entryStatus = if (gatewayResult.status == "PAID") "CONFIRMADO" else "PENDENTE"
            val adminRevenueRef = db.collection("financial_entries")
                .document("fin_${UUID.randomUUID().toString().replace("-", "").take(10)}")
            batch.set(
                adminRevenueRef,
                FinancialEntry(
                    userId = uid,
                    title = "Venda ${product.title} ($paymentMethod)",
                    category = "VENDA_APP",
                    entryType = "RECEITA",
                    profileScope = "ADMIN",
                    amount = totalAmount,
                    relatedOrderId = orderId,
                    niche = product.niche,
                    referenceMonth = "2026-11",
                    status = entryStatus
                ).toCreateMap()
            )

            val feeRef = db.collection("financial_entries")
                .document("fin_${UUID.randomUUID().toString().replace("-", "").take(10)}")
            batch.set(
                feeRef,
                FinancialEntry(
                    userId = uid,
                    title = "Taxa Gateway ${gatewayResult.provider} (#$orderId)",
                    category = "TAXA_GATEWAY",
                    entryType = "DESPESA",
                    profileScope = "ADMIN",
                    amount = gatewayFee,
                    relatedOrderId = orderId,
                    niche = product.niche,
                    referenceMonth = "2026-11",
                    status = entryStatus
                ).toCreateMap()
            )

            val commissionRef = db.collection("financial_entries")
                .document("fin_${UUID.randomUUID().toString().replace("-", "").take(10)}")
            batch.set(
                commissionRef,
                FinancialEntry(
                    userId = uid,
                    title = "Comissão 10% - ${product.title}",
                    category = "COMISSAO",
                    entryType = "RECEITA",
                    profileScope = "VENDEDOR",
                    amount = sellerCommission,
                    relatedOrderId = orderId,
                    niche = product.niche,
                    referenceMonth = "2026-11",
                    status = entryStatus
                ).toCreateMap()
            )

            val clientInvoiceRef = db.collection("financial_entries")
                .document("fin_${UUID.randomUUID().toString().replace("-", "").take(10)}")
            batch.set(
                clientInvoiceRef,
                FinancialEntry(
                    userId = uid,
                    title = "Aquisição Licença ${product.title}",
                    category = "VENDA_APP",
                    entryType = "DESPESA",
                    profileScope = "CLIENTE",
                    amount = totalAmount,
                    relatedOrderId = orderId,
                    niche = product.niche,
                    referenceMonth = "2026-11",
                    status = entryStatus
                ).toCreateMap()
            )

            val notifRef = db.collection("notifications")
                .document("not_${UUID.randomUUID().toString().replace("-", "").take(10)}")
            batch.set(
                notifRef,
                PushNotificationItem(
                    userId = uid,
                    title = "Pagamento ${gatewayResult.status}: ${product.title}",
                    body = "Transação ${gatewayResult.transactionId} via $paymentMethod no valor de R$ ${"%.2f".format(totalAmount)}.",
                    targetSegment = product.niche,
                    campaignType = "PAGAMENTO",
                    sentBy = "Gateway ${gatewayResult.provider}",
                    isRead = false,
                    dispatchedToDevice = true
                ).toCreateMap()
            )

            batch.commit().await()
            Result.success(order)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "orders/$orderId")
            Result.failure(e)
        }
    }

    // --- Client Visits ---

    suspend fun createVisit(visit: ClientVisit): Result<String> {
        val uid = requireUserId()
        val id = "vis_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("visits").document(id)
        return try {
            docRef.set(visit.copy(userId = uid).toCreateMap()).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateVisitStatus(visit: ClientVisit, newStatus: String): Result<Unit> {
        requireUserId()
        require(visit.id.isNotBlank()) { "ID da visita obrigatório" }
        val docRef = db.collection("visits").document(visit.id)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
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

    suspend fun deleteVisit(visitId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("visits").document(visitId)
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

    // --- Financial Entries ---

    suspend fun createFinancialEntry(entry: FinancialEntry): Result<String> {
        val uid = requireUserId()
        val id = "fin_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("financial_entries").document(id)
        return try {
            docRef.set(entry.copy(userId = uid).toCreateMap()).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteFinancialEntry(entryId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("financial_entries").document(entryId)
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

    // --- Push Notifications ---

    suspend fun sendPushNotification(notification: PushNotificationItem): Result<String> {
        val uid = requireUserId()
        val id = "not_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("notifications").document(id)
        return try {
            docRef.set(notification.copy(userId = uid).toCreateMap()).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("notifications").document(notificationId)
        return try {
            docRef.update(
                mapOf(
                    "isRead" to true,
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

    // --- Initial Store Catalog & Operational Seed (runs once when empty) ---

    suspend fun seedInitialCatalogIfEmpty(): Result<Unit> {
        val uid = requireUserId()
        return try {
            val existingProducts = db.collection("products")
                .whereEqualTo("ownerId", uid)
                .limit(1)
                .get()
                .await()

            if (!existingProducts.isEmpty) {
                return Result.success(Unit)
            }

            val batch = db.batch()
            val starterApps = listOf(
                AppProduct(
                    title = "Burger & Sanduba Delivery Pro",
                    niche = "Lanchonete",
                    shortDescription = "Cardápio interativo, combos personalizados, PIX instantâneo e rastreio de motoboy.",
                    fullDescription = "Aplicativo móvel nativo para lanchonetes e hamburguerias com montagem de sanduíches, adicionais, cupons de desconto, integração com impressora térmica na cozinha e pagamento PIX/Cartão direto no app.",
                    price = 1490.0,
                    monthlyFee = 149.0,
                    stockCount = 18L,
                    salesCount = 34L,
                    coverKey = "lanchonete",
                    features = listOf(
                        "Cardápio com adicionais",
                        "Rastreio em tempo real",
                        "PIX & Cartão sem taxa oculta",
                        "Cupom de fidelidade"
                    ),
                    active = true,
                    rating = 4.9
                ),
                AppProduct(
                    title = "Studio Glamour Agenda & Clube",
                    niche = "Salão de Beleza",
                    shortDescription = "Agendamento por profissional, pacotes mensais, lembretes push e comissões.",
                    fullDescription = "Solução completa para salões de beleza e estética. Seus clientes escolhem serviços, horário e profissional em segundos, pagam sinal via PIX e recebem notificações automáticas de retorno.",
                    price = 1690.0,
                    monthlyFee = 169.0,
                    stockCount = 14L,
                    salesCount = 27L,
                    coverKey = "salao",
                    features = listOf(
                        "Agenda multi-profissional",
                        "Sinal de reserva via PIX",
                        "Clube de assinatura VIP",
                        "Notificações de retorno"
                    ),
                    active = true,
                    rating = 4.9
                ),
                AppProduct(
                    title = "BarberClub Black & Fidelidade",
                    niche = "Barbearia",
                    shortDescription = "Reserva rápida de corte e barba, cartão fidelidade digital e venda de pomadas.",
                    fullDescription = "App mobile elegante para barbearias modernas com agendamento em 3 toques, plano de assinatura mensal de cortes ilimitados, vitrine de produtos masculinos e painel de comissão por barbeiro.",
                    price = 1390.0,
                    monthlyFee = 129.0,
                    stockCount = 22L,
                    salesCount = 41L,
                    coverKey = "barbearia",
                    features = listOf(
                        "Agenda 3 toques",
                        "Plano mensal recorrente",
                        "Cartão fidelidade digital",
                        "Loja de produtos"
                    ),
                    active = true,
                    rating = 4.8
                ),
                AppProduct(
                    title = "Gás & Gelada Express 24h",
                    niche = "Distribuidora",
                    shortDescription = "Recompra em 1 clique para botijão de gás e bebidas geladas com rota rápida.",
                    fullDescription = "Desenvolvido para distribuidoras de bebidas, água mineral e gás de cozinha. Oferece botão de recompra expressa, cálculo de frete por bairro, controle de entregadores e alertas de entrega em até 30 min.",
                    price = 1890.0,
                    monthlyFee = 189.0,
                    stockCount = 11L,
                    salesCount = 19L,
                    coverKey = "distribuidora",
                    features = listOf(
                        "Recompra em 1 clique",
                        "Taxa de entrega por bairro",
                        "Controle de vasilhames",
                        "Painel do entregador"
                    ),
                    active = true,
                    rating = 5.0
                ),
                AppProduct(
                    title = "Marmitex Sabor & Cardápio Diário",
                    niche = "Marmitex",
                    shortDescription = "Cardápio do dia automático, planos semanais corporativos e controle de entrega.",
                    fullDescription = "Ideal para restaurantes de marmitex e comida caseira. Permite configurar o cardápio diário em minutos, aceitar assinaturas de almoço semanal/mensal para empresas e gerenciar rotas de entrega por região.",
                    price = 1590.0,
                    monthlyFee = 159.0,
                    stockCount = 16L,
                    salesCount = 31L,
                    coverKey = "marmitex",
                    features = listOf(
                        "Cardápio do dia dinâmico",
                        "Assinatura semanal/mensal",
                        "Tamanhos P, M e G",
                        "Relatório de produção"
                    ),
                    active = true,
                    rating = 4.9
                )
            )

            starterApps.forEachIndexed { idx, app ->
                val prodId = "app_seed_${idx + 1}_${uid.take(6)}"
                batch.set(db.collection("products").document(prodId), app.toCreateMap(uid))
            }

            val visit1 = ClientVisit(
                userId = uid,
                clientName = "Lanchonete Brasa Viva",
                clientPhone = "(11) 98421-3300",
                clientAddress = "Rua Augusta, 1420 - Centro",
                niche = "Lanchonete",
                appTitle = "Burger & Sanduba Delivery Pro",
                consultantName = "Consultor Comercial",
                scheduledDateIso = "2026-11-16",
                scheduledTime = "14:00",
                visitType = "PRESENCIAL",
                status = "CONFIRMADA",
                notes = "Apresentar demonstração do cardápio digital e taxas PIX.",
                estimatedValue = 1490.0
            )
            batch.set(db.collection("visits").document("vis_seed_1_${uid.take(6)}"), visit1.toCreateMap())

            val visit2 = ClientVisit(
                userId = uid,
                clientName = "Distribuidora Polo Norte Gás",
                clientPhone = "(11) 97310-8844",
                clientAddress = "Av. Brasil, 890 - Zona Sul",
                niche = "Distribuidora",
                appTitle = "Gás & Gelada Express 24h",
                consultantName = "Consultor Comercial",
                scheduledDateIso = "2026-11-18",
                scheduledTime = "10:30",
                visitType = "ONLINE",
                status = "AGENDADA",
                notes = "Validar integração com estoque de botijões P13 e fardo gelado.",
                estimatedValue = 1890.0
            )
            batch.set(db.collection("visits").document("vis_seed_2_${uid.take(6)}"), visit2.toCreateMap())

            val finEntries = listOf(
                FinancialEntry(
                    userId = uid,
                    title = "Implantação App Barbearia Club (Histórico)",
                    category = "VENDA_APP",
                    entryType = "RECEITA",
                    profileScope = "ADMIN",
                    amount = 1390.0,
                    niche = "Barbearia",
                    referenceMonth = "2026-11",
                    status = "CONFIRMADO"
                ),
                FinancialEntry(
                    userId = uid,
                    title = "Infraestrutura Cloud & APIs Mensal",
                    category = "INFRAESTRUTURA",
                    entryType = "DESPESA",
                    profileScope = "ADMIN",
                    amount = 290.0,
                    niche = "Outro",
                    referenceMonth = "2026-11",
                    status = "CONFIRMADO"
                ),
                FinancialEntry(
                    userId = uid,
                    title = "Comissão Venda Barbearia Club",
                    category = "COMISSAO",
                    entryType = "RECEITA",
                    profileScope = "VENDEDOR",
                    amount = 139.0,
                    niche = "Barbearia",
                    referenceMonth = "2026-11",
                    status = "CONFIRMADO"
                ),
                FinancialEntry(
                    userId = uid,
                    title = "Assinatura Mensal Suporte VIP",
                    category = "MENSALIDADE",
                    entryType = "DESPESA",
                    profileScope = "CLIENTE",
                    amount = 149.0,
                    niche = "Lanchonete",
                    referenceMonth = "2026-11",
                    status = "CONFIRMADO"
                )
            )
            finEntries.forEachIndexed { idx, entry ->
                batch.set(db.collection("financial_entries").document("fin_seed_${idx + 1}_${uid.take(6)}"), entry.toCreateMap())
            }

            val notif1 = PushNotificationItem(
                userId = uid,
                title = "Novo App Distribuidora Gás & Bebidas!",
                body = "Lance seu app de entregas em 30 minutos com recompra em 1 clique e PIX automático.",
                targetSegment = "Distribuidora",
                campaignType = "NOVO_APP",
                sentBy = "Painel Admin",
                isRead = false,
                dispatchedToDevice = true
            )
            batch.set(db.collection("notifications").document("not_seed_1_${uid.take(6)}"), notif1.toCreateMap())

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products/seed")
            Result.failure(e)
        }
    }

    private fun roundMoney(value: Double): Double = (value * 100.0).roundToLong() / 100.0
}
