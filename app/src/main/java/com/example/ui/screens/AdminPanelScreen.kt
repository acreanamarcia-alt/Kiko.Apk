package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.components.LayoutBreakpoint
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.desktopHoverable
import com.example.ui.components.formatBrl
import com.example.ui.components.nicheAccentColor
import com.example.ui.components.rememberDesktopHoverBorder
import com.example.ui.viewmodel.UiState

@Composable
fun AdminPanelScreen(
    breakpoint: LayoutBreakpoint,
    productsState: UiState<List<AppProduct>>,
    ordersState: UiState<List<AppOrder>>,
    onSaveProduct: (existing: AppProduct?, updated: AppProduct) -> Unit,
    onAdjustStock: (AppProduct, Long) -> Unit,
    onToggleActive: (AppProduct) -> Unit,
    onDeleteProduct: (AppProduct) -> Unit
) {
    var editingProduct by remember { mutableStateOf<AppProduct?>(null) }
    var showCreateEditor by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<AppProduct?>(null) }

    val products = (productsState as? UiState.Success)?.data.orEmpty()
    val orders = (ordersState as? UiState.Success)?.data.orEmpty()

    val catalogRevenue = products.sumOf { it.price * it.salesCount }
    val liveOrdersRevenue = orders.filter { it.paymentStatus == "PAID" }.sumOf { it.totalAmount }
    val totalStock = products.sumOf { it.stockCount }
    val activeAppsCount = products.count { it.active }
    val lowStockCount = products.count { it.stockCount <= 5L }
    val totalSales = products.sumOf { it.salesCount }

    val gridColumns = when (breakpoint) {
        LayoutBreakpoint.COMPACT -> GridCells.Fixed(1)
        LayoutBreakpoint.MEDIUM -> GridCells.Adaptive(minSize = 320.dp)
        LayoutBreakpoint.EXPANDED -> GridCells.Adaptive(minSize = 350.dp)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showCreateEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("admin_add_product_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Novo aplicativo")
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = gridColumns,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (breakpoint == LayoutBreakpoint.COMPACT) 16.dp else 20.dp,
                end = if (breakpoint == LayoutBreakpoint.COMPACT) 16.dp else 20.dp,
                top = 14.dp,
                bottom = 88.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Painel Administrativo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dashboard de vendas, controle de estoque, edição e exclusão de apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                if (breakpoint == LayoutBreakpoint.COMPACT) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricKpiCard(
                            label = "Faturamento Apps",
                            value = formatBrl(catalogRevenue),
                            subtitle = "Gateway hoje: ${formatBrl(liveOrdersRevenue)}",
                            accentColor = MaterialTheme.colorScheme.secondary,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                        MetricKpiCard(
                            label = "Vendas & Estoque",
                            value = "$totalSales vend.",
                            subtitle = "$totalStock licenças em estoque",
                            accentColor = MaterialTheme.colorScheme.primary,
                            icon = Icons.Filled.Inventory2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricKpiCard(
                            label = "Faturamento Apps",
                            value = formatBrl(catalogRevenue),
                            subtitle = "Total acumulado catálogo",
                            accentColor = MaterialTheme.colorScheme.secondary,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                        MetricKpiCard(
                            label = "Gateway Hoje",
                            value = formatBrl(liveOrdersRevenue),
                            subtitle = "${orders.count { it.paymentStatus == "PAID" }} pedidos pagos",
                            accentColor = Color(0xFF059669),
                            icon = Icons.Filled.Payments,
                            modifier = Modifier.weight(1f)
                        )
                        MetricKpiCard(
                            label = "Total Vendas",
                            value = "$totalSales vend.",
                            subtitle = "$activeAppsCount apps ativos na vitrine",
                            accentColor = MaterialTheme.colorScheme.primary,
                            icon = Icons.Filled.ShoppingCartCheckout,
                            modifier = Modifier.weight(1f)
                        )
                        MetricKpiCard(
                            label = "Licenças em Estoque",
                            value = "$totalStock unid.",
                            subtitle = if (lowStockCount > 0) "$lowStockCount com estoque baixo" else "Estoque saudável",
                            accentColor = if (lowStockCount > 0) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                            icon = Icons.Filled.Inventory2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (lowStockCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "$lowStockCount aplicativo(s) com estoque baixo (<= 5 licenças). Reponha pelo botão + abaixo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            if (products.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SalesByNicheChartCard(
                        products = products,
                        isWide = breakpoint != LayoutBreakpoint.COMPACT
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Catálogo e Estoque (${products.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            editingProduct = null
                            showCreateEditor = true
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("+ Novo App")
                    }
                }
            }

            if (products.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyStateCard(
                        icon = Icons.Filled.ShoppingBag,
                        title = "Nenhum aplicativo cadastrado",
                        subtitle = "Toque no botão + para cadastrar um aplicativo com foto de capa."
                    )
                }
            } else {
                items(products, key = { it.id }) { product ->
                    AdminProductInventoryCard(
                        product = product,
                        onEdit = {
                            editingProduct = product
                            showCreateEditor = true
                        },
                        onStockDelta = { delta -> onAdjustStock(product, delta) },
                        onToggleActive = { onToggleActive(product) },
                        onRequestDelete = { deleteCandidate = product }
                    )
                }
            }
        }
    }

    if (showCreateEditor) {
        ProductEditorDialog(
            initial = editingProduct,
            onDismiss = {
                showCreateEditor = false
                editingProduct = null
            },
            onSave = { updated ->
                onSaveProduct(editingProduct, updated)
                showCreateEditor = false
                editingProduct = null
            }
        )
    }

    deleteCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Excluir aplicativo?") },
            text = {
                Text("Deseja remover permanentemente '${target.title}' do catálogo e estoque?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(target)
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_product_button")
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SalesByNicheChartCard(
    products: List<AppProduct>,
    isWide: Boolean
) {
    val grouped = remember(products) {
        products.groupBy { it.niche }.mapValues { entry ->
            entry.value.sumOf { it.price * it.salesCount }
        }.entries.sortedByDescending { it.value }
    }
    val maxVal = (grouped.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Desempenho de Vendas por Nicho",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(140.dp)
                    ) {
                        val count = grouped.size.coerceAtLeast(1)
                        val barSlot = size.width / count
                        val barWidth = (barSlot * 0.54f).coerceAtMost(52.dp.toPx())

                        grouped.forEachIndexed { index, entry ->
                            val ratio = (entry.value / maxVal).toFloat().coerceIn(0.08f, 1f)
                            val barHeight = size.height * ratio
                            val x = index * barSlot + (barSlot - barWidth) / 2f
                            val y = size.height - barHeight
                            drawRoundRect(
                                color = nicheAccentColor(entry.key),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        grouped.forEach { (niche, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = niche,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = nicheAccentColor(niche),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatBrl(value),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val count = grouped.size.coerceAtLeast(1)
                    val barSlot = size.width / count
                    val barWidth = (barSlot * 0.56f).coerceAtMost(46.dp.toPx())

                    grouped.forEachIndexed { index, entry ->
                        val ratio = (entry.value / maxVal).toFloat().coerceIn(0.08f, 1f)
                        val barHeight = size.height * ratio
                        val x = index * barSlot + (barSlot - barWidth) / 2f
                        val y = size.height - barHeight
                        drawRoundRect(
                            color = nicheAccentColor(entry.key),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                grouped.forEach { (niche, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = niche,
                            style = MaterialTheme.typography.bodySmall,
                            color = nicheAccentColor(niche),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatBrl(value),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminProductInventoryCard(
    product: AppProduct,
    onEdit: () -> Unit,
    onStockDelta: (Long) -> Unit,
    onToggleActive: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val (interactionSource, hoverBorder) = rememberDesktopHoverBorder()

    Card(
        shape = RoundedCornerShape(18.dp),
        border = hoverBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .desktopHoverable(interactionSource)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppCoverImage(
                    title = product.title,
                    niche = product.niche,
                    coverKey = product.coverKey,
                    coverUri = product.coverUri,
                    rating = product.rating,
                    modifier = Modifier.size(88.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.niche} • ${formatBrl(product.price)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${product.salesCount} vendas • ${if (product.active) "Ativo" else "Pausado"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Estoque: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { onStockDelta(-1L) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Reduzir estoque")
                        }
                        Text(
                            text = "${product.stockCount}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onStockDelta(1L) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Aumentar estoque")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("edit_product_${product.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar")
                }

                OutlinedButton(
                    onClick = onToggleActive,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (product.active) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (product.active) "Pausar" else "Ativar")
                }

                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_product_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Excluir produto",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    initial: AppProduct?,
    onDismiss: () -> Unit,
    onSave: (AppProduct) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var niche by remember { mutableStateOf(initial?.niche ?: "Lanchonete") }
    var shortDesc by remember { mutableStateOf(initial?.shortDescription ?: "") }
    var fullDesc by remember { mutableStateOf(initial?.fullDescription ?: "") }
    var priceText by remember { mutableStateOf(initial?.price?.toString() ?: "1490.0") }
    var monthlyText by remember { mutableStateOf(initial?.monthlyFee?.toString() ?: "149.0") }
    var stockText by remember { mutableStateOf(initial?.stockCount?.toString() ?: "10") }
    var featuresText by remember {
        mutableStateOf(initial?.features?.joinToString(", ") ?: "Cardápio Digital, PIX Automático, Push")
    }
    var coverUri by remember { mutableStateOf(initial?.coverUri) }
    var coverKey by remember {
        mutableStateOf(initial?.coverKey ?: coverKeyForNiche(initial?.niche ?: "Lanchonete"))
    }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                coverUri = uri.toString()
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "Novo Aplicativo Móvel" else "Editar Aplicativo")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppCoverImage(
                    title = title.ifBlank { "Prévia" },
                    niche = niche,
                    coverKey = coverKey,
                    coverUri = coverUri,
                    rating = initial?.rating ?: 4.9,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                )

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Anexar Foto Capa da Galeria")
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome do Aplicativo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Nicho de Mercado", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AppNiches.ALL) { item ->
                        FilterChip(
                            selected = niche == item,
                            onClick = {
                                niche = item
                                if (coverUri.isNullOrBlank()) {
                                    coverKey = coverKeyForNiche(item)
                                }
                            },
                            label = { Text(item) }
                        )
                    }
                }

                OutlinedTextField(
                    value = shortDesc,
                    onValueChange = { shortDesc = it },
                    label = { Text("Resumo na vitrine") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fullDesc,
                    onValueChange = { fullDesc = it },
                    label = { Text("Descrição completa") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Valor (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthlyText,
                        onValueChange = { monthlyText = it },
                        label = { Text("Mensal (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("Estoque de licenças") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = featuresText,
                    onValueChange = { featuresText = it },
                    label = { Text("Recursos (separados por vírgula)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val features = featuresText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .take(10)
                        .ifEmpty { listOf("App Nativo Android") }

                    onSave(
                        AppProduct(
                            title = title.ifBlank { "App $niche" },
                            niche = niche,
                            shortDescription = shortDesc.ifBlank { "Solução mobile completa para $niche" },
                            fullDescription = fullDesc.ifBlank { shortDesc.ifBlank { "Aplicativo personalizado para $niche." } },
                            price = priceText.replace(",", ".").toDoubleOrNull() ?: 1490.0,
                            monthlyFee = monthlyText.replace(",", ".").toDoubleOrNull() ?: 149.0,
                            stockCount = stockText.toLongOrNull()?.coerceAtLeast(0L) ?: 10L,
                            salesCount = initial?.salesCount ?: 0L,
                            coverKey = coverKey,
                            coverUri = coverUri,
                            features = features,
                            active = initial?.active ?: true,
                            rating = initial?.rating ?: 4.9
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_product_dialog_button")
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun coverKeyForNiche(niche: String): String {
    return when (niche) {
        "Lanchonete" -> "lanchonete"
        "Salão de Beleza" -> "salao"
        "Barbearia" -> "barbearia"
        "Distribuidora" -> "distribuidora"
        "Marmitex" -> "marmitex"
        else -> "lanchonete"
    }
}
