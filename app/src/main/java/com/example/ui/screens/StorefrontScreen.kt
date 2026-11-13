package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNiches
import com.example.data.model.AppOrder
import com.example.data.model.AppProduct
import com.example.ui.components.AppCoverImage
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatBrl
import com.example.ui.components.nicheAccentColor
import com.example.ui.viewmodel.CheckoutState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorefrontScreen(
    productsState: UiState<List<AppProduct>>,
    selectedNiche: String,
    searchQuery: String,
    hasLiveGatewayKey: Boolean,
    checkoutState: CheckoutState,
    defaultCustomerName: String,
    defaultCustomerEmail: String,
    onSelectNiche: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onCheckoutSubmit: (
        product: AppProduct,
        quantity: Int,
        customerName: String,
        customerEmail: String,
        customerCpf: String,
        paymentMethod: String,
        installments: Int,
        cardLastFour: String?
    ) -> Unit,
    onResetCheckout: () -> Unit
) {
    var detailProduct by remember { mutableStateOf<AppProduct?>(null) }
    var checkoutProduct by remember { mutableStateOf<AppProduct?>(null) }

    val nicheFilters = remember { listOf("Todos") + AppNiches.ALL }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F2557),
                                    Color(0xFF1D4ED8),
                                    Color(0xFF059669)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "APPS PRONTOS WHITE-LABEL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Loja de Aplicativos Móveis por Nicho",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Apps nativos com foto de capa real, implantação rápida e checkout em tempo real via PIX, Cartão ou Boleto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
                placeholder = { Text("Buscar app por nicho, lanchonete, salão, gás...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(nicheFilters) { niche ->
                    FilterChip(
                        selected = selectedNiche == niche,
                        onClick = { onSelectNiche(niche) },
                        label = { Text(niche) },
                        modifier = Modifier.testTag("niche_chip_${niche.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        when (productsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Smartphone,
                        title = "Não foi possível carregar o catálogo",
                        subtitle = productsState.message
                    )
                }
            }
            is UiState.Success -> {
                val filtered = productsState.data.filter { product ->
                    val activeOk = product.active
                    val nicheOk = selectedNiche == "Todos" || product.niche == selectedNiche
                    val queryOk = searchQuery.isBlank() ||
                        product.title.contains(searchQuery, ignoreCase = true) ||
                        product.niche.contains(searchQuery, ignoreCase = true) ||
                        product.shortDescription.contains(searchQuery, ignoreCase = true)
                    activeOk && nicheOk && queryOk
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Smartphone,
                            title = "Nenhum aplicativo encontrado",
                            subtitle = "Tente limpar o filtro de nicho ou cadastrar um novo aplicativo no Painel Administrativo."
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { product ->
                        AppProductStoreCard(
                            product = product,
                            onViewDetails = { detailProduct = product },
                            onBuyNow = {
                                onResetCheckout()
                                checkoutProduct = product
                            }
                        )
                    }
                }
            }
        }
    }

    detailProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { detailProduct = null },
            title = { Text(product.title, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppCoverImage(
                        title = product.title,
                        niche = product.niche,
                        coverKey = product.coverKey,
                        coverUri = product.coverUri,
                        rating = product.rating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = product.fullDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Recursos inclusos:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        product.features.forEach { feat ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = feat,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Implantação: ${formatBrl(product.price)} | Mensalidade: ${formatBrl(product.monthlyFee)}/mês",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Licenças disponíveis: ${product.stockCount} • ${product.salesCount} vendas realizadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        detailProduct = null
                        onResetCheckout()
                        checkoutProduct = product
                    },
                    enabled = product.stockCount > 0L
                ) {
                    Text("Contratar Agora")
                }
            },
            dismissButton = {
                TextButton(onClick = { detailProduct = null }) {
                    Text("Fechar")
                }
            }
        )
    }

    checkoutProduct?.let { product ->
        ModalBottomSheet(
            onDismissRequest = {
                checkoutProduct = null
                onResetCheckout()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PaymentGatewayCheckoutContent(
                product = product,
                hasLiveGatewayKey = hasLiveGatewayKey,
                checkoutState = checkoutState,
                defaultCustomerName = defaultCustomerName,
                defaultCustomerEmail = defaultCustomerEmail,
                onSubmit = { qty, name, email, cpf, method, installments, cardLastFour ->
                    onCheckoutSubmit(product, qty, name, email, cpf, method, installments, cardLastFour)
                },
                onClose = {
                    checkoutProduct = null
                    onResetCheckout()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppProductStoreCard(
    product: AppProduct,
    onViewDetails: () -> Unit,
    onBuyNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            AppCoverImage(
                title = product.title,
                niche = product.niche,
                coverKey = product.coverKey,
                coverUri = product.coverUri,
                rating = product.rating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = if (product.stockCount > 0L) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                    },
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = if (product.stockCount > 0L) "${product.stockCount} licenças" else "Esgotado",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.stockCount > 0L) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = product.shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                product.features.take(4).forEach { feat ->
                    Surface(
                        color = nicheAccentColor(product.niche).copy(alpha = 0.10f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = feat,
                            style = MaterialTheme.typography.labelSmall,
                            color = nicheAccentColor(product.niche),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatBrl(product.price),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+ ${formatBrl(product.monthlyFee)}/mês suporte",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Detalhes")
                    }
                    Button(
                        onClick = onBuyNow,
                        enabled = product.stockCount > 0L,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("buy_product_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Comprar")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentGatewayCheckoutContent(
    product: AppProduct,
    hasLiveGatewayKey: Boolean,
    checkoutState: CheckoutState,
    defaultCustomerName: String,
    defaultCustomerEmail: String,
    onSubmit: (
        qty: Int,
        name: String,
        email: String,
        cpf: String,
        method: String,
        installments: Int,
        cardLastFour: String?
    ) -> Unit,
    onClose: () -> Unit
) {
    var quantity by remember { mutableIntStateOf(1) }
    var customerName by remember { mutableStateOf(defaultCustomerName) }
    var customerEmail by remember { mutableStateOf(defaultCustomerEmail) }
    var customerCpf by remember { mutableStateOf("123.456.789-09") }
    var paymentMethod by remember { mutableStateOf("PIX") }
    var installments by remember { mutableIntStateOf(1) }
    var cardNumber by remember { mutableStateOf("4532 8801 9912 4410") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Checkout Gateway em Tempo Real",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = if (hasLiveGatewayKey) Color(0xFF059669).copy(alpha = 0.14f) else Color(0xFFD97706).copy(alpha = 0.14f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (hasLiveGatewayKey) Color(0xFF059669) else Color(0xFFD97706),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (hasLiveGatewayKey) "Live API" else "Sandbox",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasLiveGatewayKey) Color(0xFF059669) else Color(0xFFD97706),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (checkoutState is CheckoutState.Success) {
            OrderGatewayReceipt(order = checkoutState.order, onClose = onClose)
            Spacer(Modifier.height(24.dp))
            return
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${formatBrl(product.price)} por licença (${product.stockCount} disponíveis)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Diminuir quantidade")
                    }
                    Text(
                        text = "$quantity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { if (quantity < product.stockCount) quantity++ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Aumentar quantidade")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Nome completo / Razão social") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = customerEmail,
                onValueChange = { customerEmail = it },
                label = { Text("E-mail") },
                singleLine = true,
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = customerCpf,
                onValueChange = { customerCpf = it },
                label = { Text("CPF / CNPJ") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))
        Text("Método de Pagamento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = paymentMethod == "PIX",
                onClick = { paymentMethod = "PIX"; installments = 1 },
                leadingIcon = { Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("PIX Instantâneo") }
            )
            FilterChip(
                selected = paymentMethod == "CREDIT_CARD",
                onClick = { paymentMethod = "CREDIT_CARD" },
                leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Cartão Crédito") }
            )
            FilterChip(
                selected = paymentMethod == "BOLETO",
                onClick = { paymentMethod = "BOLETO"; installments = 1 },
                leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Boleto") }
            )
        }

        if (paymentMethod == "CREDIT_CARD") {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Número do cartão") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(1, 3, 6, 12)) { parcela ->
                    FilterChip(
                        selected = installments == parcela,
                        onClick = { installments = parcela },
                        label = { Text("${parcela}x de ${formatBrl((product.price * quantity) / parcela)}") }
                    )
                }
            }
        }

        if (checkoutState is CheckoutState.Error) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = checkoutState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        val total = product.price * quantity
        Button(
            onClick = {
                onSubmit(
                    quantity,
                    customerName.ifBlank { "Cliente VitrineApps" },
                    customerEmail.ifBlank { "cliente@vitrineapps.com" },
                    customerCpf.ifBlank { "12345678909" },
                    paymentMethod,
                    installments,
                    if (paymentMethod == "CREDIT_CARD") cardNumber.filter { it.isDigit() }.takeLast(4) else null
                )
            },
            enabled = checkoutState !is CheckoutState.Processing && customerName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_gateway_payment_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            if (checkoutState is CheckoutState.Processing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Processando no Gateway...")
            } else {
                Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pagar ${formatBrl(total)} Agora", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OrderGatewayReceipt(order: AppOrder, onClose: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Transação ${order.paymentStatus}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gateway: ${order.gatewayProvider} • ID ${order.gatewayTransactionId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Aplicativo: ${order.productTitle} (${order.quantity}x)", style = MaterialTheme.typography.bodyMedium)
            Text("Total: ${formatBrl(order.totalAmount)} via ${order.paymentMethod}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "Taxa Gateway: ${formatBrl(order.gatewayFee)} • Comissão: ${formatBrl(order.sellerCommission)} • Líquido: ${formatBrl(order.netAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!order.pixCopyPaste.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "PIX Copia e Cola:\n${order.pixCopyPaste}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("close_receipt_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Concluir")
            }
        }
    }
}
