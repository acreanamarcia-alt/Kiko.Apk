package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNiches
import com.example.data.model.AppProduct
import com.example.data.model.ClientVisit
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.formatBrl
import com.example.ui.viewmodel.UiState

@Composable
fun VisitsScreen(
    visitsState: UiState<List<ClientVisit>>,
    productsState: UiState<List<AppProduct>>,
    onScheduleVisit: (ClientVisit) -> Unit,
    onUpdateStatus: (ClientVisit, String) -> Unit,
    onDeleteVisit: (String) -> Unit
) {
    var statusFilter by remember { mutableStateOf("TODAS") }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val visits = (visitsState as? UiState.Success)?.data.orEmpty()
    val products = (productsState as? UiState.Success)?.data.orEmpty()

    val openVisits = visits.count { it.status == "AGENDADA" || it.status == "CONFIRMADA" }
    val pipelineValue = visits.filter { it.status != "CANCELADA" }.sumOf { it.estimatedValue }

    val filtered = visits.filter { statusFilter == "TODAS" || it.status == statusFilter }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showScheduleDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_visit_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agendar visita")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Agenda de Visitas a Clientes",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Demonstrações presenciais e online para fechamento de licenças",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricKpiCard(
                        label = "Visitas Ativas",
                        value = "$openVisits",
                        subtitle = "${visits.count { it.status == "CONCLUIDA" }} concluídas",
                        accentColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Filled.CalendarMonth,
                        modifier = Modifier.weight(1f)
                    )
                    MetricKpiCard(
                        label = "Pipeline Estimado",
                        value = formatBrl(pipelineValue),
                        subtitle = "Potencial em negociação",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        icon = Icons.Filled.DoneAll,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("TODAS", "AGENDADA", "CONFIRMADA", "CONCLUIDA", "CANCELADA")) { status ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = { statusFilter = status },
                            label = { Text(status) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.CalendarMonth,
                        title = "Nenhuma visita agendada neste filtro",
                        subtitle = "Toque no botão + para agendar uma demonstração presencial ou online."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { visit ->
                    VisitCard(
                        visit = visit,
                        onUpdateStatus = { newStatus -> onUpdateStatus(visit, newStatus) },
                        onDelete = { onDeleteVisit(visit.id) }
                    )
                }
            }
        }
    }

    if (showScheduleDialog) {
        ScheduleVisitDialog(
            products = products,
            onDismiss = { showScheduleDialog = false },
            onSave = { visit ->
                onScheduleVisit(visit)
                showScheduleDialog = false
            }
        )
    }
}

@Composable
private fun VisitCard(
    visit: ClientVisit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (visit.status) {
        "CONFIRMADA" -> Color(0xFF059669)
        "CONCLUIDA" -> Color(0xFF1D4ED8)
        "CANCELADA" -> Color(0xFFDC2626)
        else -> Color(0xFFD97706)
    }
    val isTerminal = visit.status == "CONCLUIDA" || visit.status == "CANCELADA"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = visit.clientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${visit.niche} • ${visit.appTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = visit.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text("${visit.scheduledDateIso} às ${visit.scheduledTime}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = if (visit.visitType == "ONLINE") Icons.Filled.VideoCall else Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(visit.visitType, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(visit.clientAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text("${visit.clientPhone} • Valor est.: ${formatBrl(visit.estimatedValue)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (visit.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = visit.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isTerminal) {
                    if (visit.status == "AGENDADA") {
                        OutlinedButton(
                            onClick = { onUpdateStatus("CONFIRMADA") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Confirmar")
                        }
                    }
                    Button(
                        onClick = { onUpdateStatus("CONCLUIDA") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Concluir")
                    }
                    IconButton(
                        onClick = { onUpdateStatus("CANCELADA") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancelar visita", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir visita", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ScheduleVisitDialog(
    products: List<AppProduct>,
    onDismiss: () -> Unit,
    onSave: (ClientVisit) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("(11) 9") }
    var clientAddress by remember { mutableStateOf("") }
    var niche by remember { mutableStateOf("Lanchonete") }
    var appTitle by remember { mutableStateOf(products.firstOrNull()?.title ?: "App Delivery") }
    var consultantName by remember { mutableStateOf("Consultor Comercial") }
    var scheduledDateIso by remember { mutableStateOf("2026-11-20") }
    var scheduledTime by remember { mutableStateOf("14:30") }
    var visitType by remember { mutableStateOf("PRESENCIAL") }
    var estimatedValueText by remember { mutableStateOf("1490.0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agendar Visita a Cliente") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nome do Cliente / Empresa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    label = { Text("WhatsApp / Telefone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = clientAddress,
                    onValueChange = { clientAddress = it },
                    label = { Text("Endereço ou Link da Reunião") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Nicho", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AppNiches.ALL) { item ->
                        FilterChip(
                            selected = niche == item,
                            onClick = {
                                niche = item
                                products.firstOrNull { it.niche == item }?.let {
                                    appTitle = it.title
                                    estimatedValueText = it.price.toString()
                                }
                            },
                            label = { Text(item) }
                        )
                    }
                }

                OutlinedTextField(
                    value = appTitle,
                    onValueChange = { appTitle = it },
                    label = { Text("Aplicativo a apresentar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scheduledDateIso,
                        onValueChange = { scheduledDateIso = it },
                        label = { Text("Data (AAAA-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = scheduledTime,
                        onValueChange = { scheduledTime = it },
                        label = { Text("Hora") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = visitType == "PRESENCIAL",
                        onClick = { visitType = "PRESENCIAL" },
                        label = { Text("Presencial") }
                    )
                    FilterChip(
                        selected = visitType == "ONLINE",
                        onClick = { visitType = "ONLINE" },
                        label = { Text("Online / Meet") }
                    )
                }

                OutlinedTextField(
                    value = estimatedValueText,
                    onValueChange = { estimatedValueText = it },
                    label = { Text("Valor Estimado (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = consultantName,
                    onValueChange = { consultantName = it },
                    label = { Text("Consultor / Vendedor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações da visita") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ClientVisit(
                            clientName = clientName,
                            clientPhone = clientPhone.ifBlank { "(11) 99999-0000" },
                            clientAddress = clientAddress.ifBlank { "A confirmar" },
                            niche = niche,
                            appTitle = appTitle.ifBlank { "App $niche" },
                            consultantName = consultantName.ifBlank { "Consultor" },
                            scheduledDateIso = scheduledDateIso.ifBlank { "2026-11-20" },
                            scheduledTime = scheduledTime.ifBlank { "14:00" },
                            visitType = visitType,
                            status = "AGENDADA",
                            notes = notes,
                            estimatedValue = estimatedValueText.replace(",", ".").toDoubleOrNull() ?: 1490.0
                        )
                    )
                },
                enabled = clientName.isNotBlank(),
                modifier = Modifier.testTag("save_visit_dialog_button")
            ) {
                Text("Agendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
