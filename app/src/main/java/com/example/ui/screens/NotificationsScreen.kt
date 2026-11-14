package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNiches
import com.example.data.model.PushNotificationItem
import com.example.notifications.NotificationHelper
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LayoutBreakpoint
import com.example.ui.components.desktopHoverable
import com.example.ui.components.rememberDesktopHoverBorder
import com.example.ui.viewmodel.UiState

@Composable
fun NotificationsScreen(
    breakpoint: LayoutBreakpoint,
    notificationsState: UiState<List<PushNotificationItem>>,
    onSendPush: (title: String, body: String, segment: String, type: String, dispatchedToDevice: Boolean) -> Unit,
    onMarkRead: (String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var targetSegment by remember { mutableStateOf("TODOS") }
    var campaignType by remember { mutableStateOf("PROMOCAO") }

    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
    val segments = remember { listOf("TODOS") + AppNiches.ALL }

    if (breakpoint == LayoutBreakpoint.EXPANDED) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NotificationsHeader()
                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    NotificationPermissionBanner(
                        onRequest = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }
                PushCampaignComposerCard(
                    title = title,
                    onTitleChange = { title = it },
                    body = body,
                    onBodyChange = { body = it },
                    targetSegment = targetSegment,
                    onSegmentChange = { targetSegment = it },
                    campaignType = campaignType,
                    onCampaignTypeChange = { campaignType = it },
                    segments = segments,
                    onDispatch = {
                        val dispatched = NotificationHelper.showPushNotification(context, title, body)
                        onSendPush(title, body, targetSegment, campaignType, dispatched)
                        title = ""
                        body = ""
                    }
                )
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            LazyColumn(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Histórico de Envios (${notifications.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (notifications.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.NotificationsActive,
                            title = "Nenhuma notificação enviada",
                            subtitle = "Crie sua primeira campanha ao lado para notificar clientes."
                        )
                    }
                } else {
                    items(notifications, key = { it.id }) { item ->
                        NotificationHistoryCard(
                            item = item,
                            onMarkRead = { if (!item.isRead) onMarkRead(item.id) }
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    NotificationsHeader()
                }

                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        NotificationPermissionBanner(
                            onRequest = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        )
                    }
                }

                item {
                    PushCampaignComposerCard(
                        title = title,
                        onTitleChange = { title = it },
                        body = body,
                        onBodyChange = { body = it },
                        targetSegment = targetSegment,
                        onSegmentChange = { targetSegment = it },
                        campaignType = campaignType,
                        onCampaignTypeChange = { campaignType = it },
                        segments = segments,
                        onDispatch = {
                            val dispatched = NotificationHelper.showPushNotification(context, title, body)
                            onSendPush(title, body, targetSegment, campaignType, dispatched)
                            title = ""
                            body = ""
                        }
                    )
                }

                item {
                    Text(
                        text = "Histórico de Envios (${notifications.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (notifications.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.NotificationsActive,
                            title = "Nenhuma notificação enviada",
                            subtitle = "Crie sua primeira campanha acima para notificar clientes."
                        )
                    }
                } else {
                    items(notifications, key = { it.id }) { item ->
                        NotificationHistoryCard(
                            item = item,
                            onMarkRead = { if (!item.isRead) onMarkRead(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsHeader() {
    Column {
        Text(
            text = "Notificações Push para Clientes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Dispare campanhas segmentadas por nicho com alerta direto no dispositivo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationPermissionBanner(onRequest: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Permissão de Notificação",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Autorize alertas push para visualizar campanhas na barra do Android.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onRequest,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Permitir")
            }
        }
    }
}

@Composable
private fun PushCampaignComposerCard(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    targetSegment: String,
    onSegmentChange: (String) -> Unit,
    campaignType: String,
    onCampaignTypeChange: (String) -> Unit,
    segments: List<String>,
    onDispatch: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Nova Campanha Push",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Título da notificação") },
                placeholder = { Text("Ex: 25% OFF no App Lanchonete!") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("push_title_input")
            )

            OutlinedTextField(
                value = body,
                onValueChange = onBodyChange,
                label = { Text("Mensagem para o cliente") },
                placeholder = { Text("Ex: Lance seu delivery próprio ainda esta semana...") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("push_body_input")
            )

            Text("Segmento alvo:", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(segments) { seg ->
                    FilterChip(
                        selected = targetSegment == seg,
                        onClick = { onSegmentChange(seg) },
                        label = { Text(seg) }
                    )
                }
            }

            Text("Tipo de campanha:", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("PROMOCAO", "NOVO_APP", "VISITA", "PAGAMENTO")) { tipo ->
                    FilterChip(
                        selected = campaignType == tipo,
                        onClick = { onCampaignTypeChange(tipo) },
                        label = { Text(tipo) }
                    )
                }
            }

            Button(
                onClick = onDispatch,
                enabled = title.isNotBlank() && body.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("send_push_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Disparar Notificação Push")
            }
        }
    }
}

@Composable
private fun NotificationHistoryCard(
    item: PushNotificationItem,
    onMarkRead: () -> Unit
) {
    val (interactionSource, hoverBorder) = rememberDesktopHoverBorder()

    Card(
        shape = RoundedCornerShape(16.dp),
        border = hoverBorder,
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .desktopHoverable(interactionSource)
            .clickable(onClick = onMarkRead)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = item.targetSegment,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.campaignType} • ${item.sentBy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MarkEmailRead,
                        contentDescription = null,
                        tint = if (item.isRead) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (item.isRead) "Lida" else "Nova",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isRead) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
