package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppOrder
import com.example.data.model.AppProduct
import com.example.data.model.ClientVisit
import com.example.data.model.FinancialEntry
import com.example.data.model.PushNotificationItem
import com.example.data.model.UserProfile
import com.example.data.model.UserProfileView
import com.example.data.repository.VitrineRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface CheckoutState {
    data object Idle : CheckoutState
    data object Processing : CheckoutState
    data class Success(val order: AppOrder) : CheckoutState
    data class Error(val message: String) : CheckoutState
}

class VitrineViewModel(
    private val repository: VitrineRepository,
    val currentUserId: String
) : ViewModel() {

    private val _selectedNiche = MutableStateFlow("Todos")
    val selectedNiche: StateFlow<String> = _selectedNiche.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProfileView = MutableStateFlow(UserProfileView.ADMIN)
    val selectedProfileView: StateFlow<UserProfileView> = _selectedProfileView.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val hasLiveGatewayKey: Boolean = repository.hasLiveGatewayKey()

    val productsState: StateFlow<UiState<List<AppProduct>>> = repository.observeProducts(currentUserId)
        .map<List<AppProduct>, UiState<List<AppProduct>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapError(it, "produtos"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val ordersState: StateFlow<UiState<List<AppOrder>>> = repository.observeOrders(currentUserId)
        .map<List<AppOrder>, UiState<List<AppOrder>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapError(it, "pedidos"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val visitsState: StateFlow<UiState<List<ClientVisit>>> = repository.observeVisits(currentUserId)
        .map<List<ClientVisit>, UiState<List<ClientVisit>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapError(it, "visitas"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val financialState: StateFlow<UiState<List<FinancialEntry>>> = repository.observeFinancialEntries(currentUserId)
        .map<List<FinancialEntry>, UiState<List<FinancialEntry>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapError(it, "lançamentos financeiros"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val notificationsState: StateFlow<UiState<List<PushNotificationItem>>> = repository.observeNotifications(currentUserId)
        .map<List<PushNotificationItem>, UiState<List<PushNotificationItem>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapError(it, "notificações"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    init {
        viewModelScope.launch {
            repository.ensureUserProfile().onSuccess { _userProfile.value = it }
            repository.seedInitialCatalogIfEmpty()
        }
    }

    fun selectNiche(niche: String) {
        _selectedNiche.value = niche
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProfileView(profileView: UserProfileView) {
        _selectedProfileView.value = profileView
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun resetCheckoutState() {
        _checkoutState.value = CheckoutState.Idle
    }

    fun saveProduct(existing: AppProduct?, updated: AppProduct) {
        viewModelScope.launch {
            val result = if (existing == null) {
                repository.createProduct(updated)
            } else {
                repository.updateProduct(
                    updated.copy(
                        id = existing.id,
                        ownerId = existing.ownerId,
                        salesCount = existing.salesCount,
                        createdAt = existing.createdAt
                    )
                )
            }
            result.onSuccess {
                _feedbackMessage.value = if (existing == null) {
                    "Aplicativo '${updated.title}' publicado na vitrine!"
                } else {
                    "Aplicativo '${updated.title}' atualizado com sucesso!"
                }
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Falha ao salvar aplicativo"
            }
        }
    }

    fun adjustStock(product: AppProduct, delta: Long) {
        viewModelScope.launch {
            repository.adjustProductStock(product.id, delta)
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao atualizar estoque" }
        }
    }

    fun toggleProductActive(product: AppProduct) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(active = !product.active))
                .onSuccess {
                    _feedbackMessage.value = if (!product.active) {
                        "'${product.title}' reativado na vitrine"
                    } else {
                        "'${product.title}' pausado na vitrine"
                    }
                }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao alterar status" }
        }
    }

    fun deleteProduct(product: AppProduct) {
        viewModelScope.launch {
            repository.deleteProduct(product.id)
                .onSuccess { _feedbackMessage.value = "Aplicativo '${product.title}' excluído do catálogo." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Falha ao excluir aplicativo" }
        }
    }

    fun processCheckout(
        product: AppProduct,
        quantity: Int,
        customerName: String,
        customerEmail: String,
        customerCpf: String,
        paymentMethod: String,
        installments: Int,
        cardLastFour: String?
    ) {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Processing
            repository.checkoutWithGateway(
                product = product,
                quantity = quantity,
                customerName = customerName,
                customerEmail = customerEmail,
                customerCpf = customerCpf,
                paymentMethod = paymentMethod,
                installments = installments,
                cardLastFour = cardLastFour
            ).onSuccess { order ->
                _checkoutState.value = CheckoutState.Success(order)
            }.onFailure { error ->
                _checkoutState.value = CheckoutState.Error(
                    error.localizedMessage ?: "Falha ao processar pagamento no gateway"
                )
            }
        }
    }

    fun scheduleVisit(visit: ClientVisit) {
        viewModelScope.launch {
            repository.createVisit(visit)
                .onSuccess { _feedbackMessage.value = "Visita agendada para ${visit.clientName}!" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao agendar visita" }
        }
    }

    fun updateVisitStatus(visit: ClientVisit, newStatus: String) {
        viewModelScope.launch {
            repository.updateVisitStatus(visit, newStatus)
                .onSuccess { _feedbackMessage.value = "Status da visita atualizado para $newStatus." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao atualizar visita" }
        }
    }

    fun deleteVisit(visitId: String) {
        viewModelScope.launch {
            repository.deleteVisit(visitId)
                .onSuccess { _feedbackMessage.value = "Agendamento removido." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao remover agendamento" }
        }
    }

    fun addFinancialEntry(entry: FinancialEntry) {
        viewModelScope.launch {
            repository.createFinancialEntry(entry)
                .onSuccess { _feedbackMessage.value = "Lançamento '${entry.title}' registrado." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao salvar lançamento" }
        }
    }

    fun deleteFinancialEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteFinancialEntry(entryId)
                .onSuccess { _feedbackMessage.value = "Lançamento removido." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao excluir lançamento" }
        }
    }

    fun sendPushNotification(
        title: String,
        body: String,
        targetSegment: String,
        campaignType: String,
        dispatchedToDevice: Boolean
    ) {
        viewModelScope.launch {
            repository.sendPushNotification(
                PushNotificationItem(
                    title = title,
                    body = body,
                    targetSegment = targetSegment,
                    campaignType = campaignType,
                    sentBy = "Painel Admin",
                    isRead = false,
                    dispatchedToDevice = dispatchedToDevice
                )
            ).onSuccess {
                _feedbackMessage.value = if (dispatchedToDevice) {
                    "Notificação push disparada para o segmento $targetSegment!"
                } else {
                    "Campanha registrada no histórico (ative a permissão de notificações para alerta no aparelho)."
                }
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Erro ao enviar notificação"
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    private fun unwrapError(error: Throwable, resource: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing $resource", root)
        return root.message ?: "Falha ao carregar $resource"
    }

    private companion object {
        const val TAG = "VitrineVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
