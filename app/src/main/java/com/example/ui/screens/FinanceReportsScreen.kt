package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppOrder
import com.example.data.model.ClientVisit
import com.example.data.model.FinancialEntry
import com.example.data.model.UserProfileView
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.formatBrl
import com.example.ui.viewmodel.UiState

@Composable
fun FinanceReportsScreen(
    selectedProfileView: UserProfileView,
    financialState: UiState<List<FinancialEntry>>,
    ordersState: UiState<List<AppOrder>>,
    visitsState: UiState<List<ClientVisit>>,
    onSelectProfileView: (UserProfileView) -> Unit,
    onAddFinancialEntry: (FinancialEntry) -> Unit,
    onDeleteFinancialEntry: (String) -> Unit
) {
    var showAddEntryDialog by remember { mutableStateOf(false) }

    val entries = (financialState as? UiState.Success)?.data.orEmpty()
    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val visits = (visitsState as? UiState.Success)?.data.orEmpty()

    val scopedEntries = remember(entries, selectedProfileView) {
        entries.filter { it.profileScope == selectedProfileView.scopeKey }
    }

    val totalReceitas = scopedEntries.filter { it.entryType == "RECEITA" }.sumOf { it.amount }
    val totalDespesas = scopedEntries.filter { it.entryType == "DESPESA" }.sumOf { it.amount }
    val saldoLiquido = totalReceitas - totalDespesas

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEntryDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("add_financial_entry_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Novo lançamento financeiro")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Financeiro & Relatórios",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Controle integrado e demonstrativo detalhado por perfil de usuário",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    UserProfileView.entries.forEachIndexed { index, profile ->
                        SegmentedButton(
                            selected = selectedProfileView == profile,
                            onClick = { onSelectProfileView(profile) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = UserProfileView.entries.size
                            ),
                            modifier = Modifier.testTag("profile_tab_${profile.scopeKey.lowercase()}")
                        ) {
                            Text(
                                text = when (profile) {
                                    UserProfileView.ADMIN -> "Admin"
                                    UserProfileView.SELLER -> "Vendedor"
                                    UserProfileView.CLIENT -> "Cliente"
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            item {
                when (selectedProfileView) {
                    UserProfileView.ADMIN -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricKpiCard(
                                label = "Receita Bruta",
                                value = formatBrl(totalReceitas),
                                subtitle = "Entradas consolidadas",
                                accentColor = Color(0xFF059669),
                                icon = Icons.Filled.ArrowUpward,
                                modifier = Modifier.weight(1f)
                            )
                            MetricKpiCard(
                                label = "Lucro Líquido",
                                value = formatBrl(saldoLiquido),
                                subtitle = "Despesas/Taxas: ${formatBrl(totalDespesas)}",
                                accentColor = MaterialTheme.colorScheme.primary,
                                icon = Icons.Filled.AccountBalanceWallet,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    UserProfileView.SELLER -> {
                        val convertedVisits = visits.count { it.status == "CONCLUIDA" }
                        MetricKpiCard(
                            label = "Relatório de Comissões (Vendedor)",
                            value = formatBrl(totalReceitas),
                            subtitle = "${orders.size} vendas • $convertedVisits visitas convertidas",
                            accentColor = Color(0xFF059669),
                            icon = Icons.Filled.Payments
                        )
                    }
                    UserProfileView.CLIENT -> {
                        val paidOrders = orders.count { it.paymentStatus == "PAID" }
                        MetricKpiCard(
                            label = "Investimento & Faturas (Cliente)",
                            value = formatBrl(totalDespesas),
                            subtitle = "$paidOrders licença(s) paga(s) via Gateway",
                            accentColor = MaterialTheme.colorScheme.primary,
                            icon = Icons.Filled.Receipt
                        )
                    }
                }
            }

            item {
                CashflowComparisonCard(
                    profileLabel = selectedProfileView.label,
                    receitas = totalReceitas,
                    despesas = totalDespesas
                )
            }

            if (orders.isNotEmpty()) {
                item {
                    Text(
                        text = "Transações Gateway em Tempo Real (${orders.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(orders, key = { it.id }) { order ->
                    GatewayOrderLedgerCard(order = order)
                }
            }

            item {
                Text(
                    text = "Lançamentos — ${selectedProfileView.label} (${scopedEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (scopedEntries.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Assessment,
                        title = "Sem lançamentos para ${selectedProfileView.label}",
                        subtitle = "Realize uma venda na Vitrine ou toque no botão + para registrar receitas e despesas."
                    )
                }
            } else {
                items(scopedEntries, key = { it.id }) { entry ->
                    FinancialEntryCard(
                        entry = entry,
                        onDelete = { onDeleteFinancialEntry(entry.id) }
                    )
                }
            }
        }
    }

    if (showAddEntryDialog) {
        AddFinancialEntryDialog(
            defaultProfileScope = selectedProfileView.scopeKey,
            onDismiss = { showAddEntryDialog = false },
            onSave = { entry ->
                onAddFinancialEntry(entry)
                showAddEntryDialog = false
            }
        )
    }
}

@Composable
private fun CashflowComparisonCard(
    profileLabel: String,
    receitas: Double,
    despesas: Double
) {
    val maxVal = maxOf(receitas, despesas, 1.0)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Fluxo de Caixa — $profileLabel",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                val recWidth = ((receitas / maxVal).toFloat().coerceIn(0.05f, 1f)) * size.width
                val despWidth = ((despesas / maxVal).toFloat().coerceIn(0.05f, 1f)) * size.width
                val barH = 22.dp.toPx()

                drawRoundRect(
                    color = Color(0xFF059669),
                    topLeft = Offset(0f, 4.dp.toPx()),
                    size = Size(recWidth, barH),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFFDC2626),
                    topLeft = Offset(0f, 36.dp.toPx()),
                    size = Size(despWidth, barH),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Entradas: ${formatBrl(receitas)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                Text("Saídas: ${formatBrl(despesas)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GatewayOrderLedgerCard(order: AppOrder) {
    val badgeColor = when (order.paymentStatus) {
        "PAID" -> Color(0xFF059669)
        "FAILED", "REFUNDED" -> Color(0xFFDC2626)
        else -> Color(0xFFD97706)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                Text(
                    text = "${order.productTitle} (${order.quantity}x)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${order.paymentMethod} • ${order.gatewayProvider} • ${order.gatewayTransactionId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Líquido: ${formatBrl(order.netAmount)} (Taxa: ${formatBrl(order.gatewayFee)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatBrl(order.totalAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = badgeColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = order.paymentStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialEntryCard(
    entry: FinancialEntry,
    onDelete: () -> Unit
) {
    val isIncome = entry.entryType == "RECEITA"
    val color = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = color.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${entry.category} • ${entry.referenceMonth} • ${entry.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = (if (isIncome) "+ " else "- ") + formatBrl(entry.amount),
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Remover lançamento", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AddFinancialEntryDialog(
    defaultProfileScope: String,
    onDismiss: () -> Unit,
    onSave: (FinancialEntry) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf("RECEITA") }
    var category by remember { mutableStateOf("VENDA_APP") }
    var profileScope by remember { mutableStateOf(defaultProfileScope) }

    val categories = listOf("VENDA_APP", "MENSALIDADE", "COMISSAO", "TAXA_GATEWAY", "INFRAESTRUTURA", "MARKETING")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Lançamento Financeiro") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = entryType == "RECEITA",
                        onClick = { entryType = "RECEITA" },
                        label = { Text("Receita") }
                    )
                    FilterChip(
                        selected = entryType == "DESPESA",
                        onClick = { entryType = "DESPESA" },
                        label = { Text("Despesa") }
                    )
                }

                Text("Perfil", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ADMIN", "VENDEDOR", "CLIENTE").forEach { scope ->
                        FilterChip(
                            selected = profileScope == scope,
                            onClick = { profileScope = scope },
                            label = { Text(scope) }
                        )
                    }
                }

                Text("Categoria", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        FinancialEntry(
                            title = title,
                            category = category,
                            entryType = entryType,
                            profileScope = profileScope,
                            amount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            referenceMonth = "2026-11",
                            status = "CONFIRMADO"
                        )
                    )
                },
                enabled = title.isNotBlank() && (amountText.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0.0
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
