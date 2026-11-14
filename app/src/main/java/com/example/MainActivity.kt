package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.VitrineRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.LayoutBreakpoint
import com.example.ui.components.layoutBreakpointFor
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.FinanceReportsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.StorefrontScreen
import com.example.ui.screens.VisitsScreen
import com.example.ui.theme.VitrineAppsTheme
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.VitrineViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private data class NavTab(
    val route: String,
    val label: String,
    val desktopLabel: String,
    val icon: ImageVector
)

private val APP_TABS = listOf(
    NavTab("store", "Vitrine", "Vitrine de Apps", Icons.Filled.Storefront),
    NavTab("admin", "Painel", "Painel Admin", Icons.Filled.Dashboard),
    NavTab("visits", "Visitas", "Agenda de Visitas", Icons.Filled.CalendarMonth),
    NavTab("finance", "Financeiro", "Financeiro & Relatórios", Icons.Filled.AccountBalanceWallet),
    NavTab("notifications", "Push", "Notificações Push", Icons.Filled.Notifications)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VitrineAppsTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MainFeatureScreen(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "Empreendedor",
                userEmail = user.email ?: "cliente@vitrineapps.com"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String
) {
    val viewModel: VitrineViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                VitrineViewModel(VitrineRepository(db), currentUserId)
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val visitsState by viewModel.visitsState.collectAsStateWithLifecycle()
    val financialState by viewModel.financialState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val selectedNiche by viewModel.selectedNiche.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedProfileView by viewModel.selectedProfileView.collectAsStateWithLifecycle()
    val checkoutState by viewModel.checkoutState.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val unreadCount = (notificationsState as? UiState.Success)?.data?.count { !it.isRead } ?: 0

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "store"

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo("store") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val breakpoint = layoutBreakpointFor(maxWidth)

        val contentNavHost: @Composable (Modifier) -> Unit = { contentModifier ->
            VitrineNavHost(
                modifier = contentModifier,
                navController = navController,
                breakpoint = breakpoint,
                viewModel = viewModel,
                productsState = productsState,
                ordersState = ordersState,
                visitsState = visitsState,
                financialState = financialState,
                notificationsState = notificationsState,
                selectedNiche = selectedNiche,
                searchQuery = searchQuery,
                selectedProfileView = selectedProfileView,
                checkoutState = checkoutState,
                userDisplayName = userDisplayName,
                userEmail = userEmail
            )
        }

        if (breakpoint == LayoutBreakpoint.EXPANDED) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(256.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.vitrine_launcher_icon_1794559013037),
                                        contentDescription = "Logo VitrineApps",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "VitrineApps",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Desktop & Mobile",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                Spacer(Modifier.height(14.dp))

                                APP_TABS.forEach { tab ->
                                    NavigationDrawerItem(
                                        selected = currentRoute == tab.route,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            if (tab.route == "notifications" && unreadCount > 0) {
                                                BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                                    Icon(tab.icon, contentDescription = tab.desktopLabel)
                                                }
                                            } else {
                                                Icon(tab.icon, contentDescription = tab.desktopLabel)
                                            }
                                        },
                                        label = { Text(tab.desktopLabel) },
                                        modifier = Modifier
                                            .padding(vertical = 3.dp)
                                            .testTag("nav_tab_${tab.route}"),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = userDisplayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { signOutUser(credentialManager, scope) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("sign_out_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Sair da conta")
                                    }
                                }
                            }
                        }
                    }
                }
            ) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    contentNavHost(
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.vitrine_launcher_icon_1794559013037),
                                    contentDescription = "Logo VitrineApps",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "VitrineApps",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { signOutUser(credentialManager, scope) },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sair da conta"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (breakpoint == LayoutBreakpoint.COMPACT) {
                        NavigationBar {
                            APP_TABS.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        if (tab.route == "notifications" && unreadCount > 0) {
                                            BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                                Icon(tab.icon, contentDescription = tab.label)
                                            }
                                        } else {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    if (breakpoint == LayoutBreakpoint.MEDIUM) {
                        NavigationRail {
                            APP_TABS.forEach { tab ->
                                NavigationRailItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        if (tab.route == "notifications" && unreadCount > 0) {
                                            BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                                Icon(tab.icon, contentDescription = tab.label)
                                            }
                                        } else {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }

                    contentNavHost(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun VitrineNavHost(
    modifier: Modifier,
    navController: NavHostController,
    breakpoint: LayoutBreakpoint,
    viewModel: VitrineViewModel,
    productsState: UiState<List<com.example.data.model.AppProduct>>,
    ordersState: UiState<List<com.example.data.model.AppOrder>>,
    visitsState: UiState<List<com.example.data.model.ClientVisit>>,
    financialState: UiState<List<com.example.data.model.FinancialEntry>>,
    notificationsState: UiState<List<com.example.data.model.PushNotificationItem>>,
    selectedNiche: String,
    searchQuery: String,
    selectedProfileView: com.example.data.model.UserProfileView,
    checkoutState: com.example.ui.viewmodel.CheckoutState,
    userDisplayName: String,
    userEmail: String
) {
    Box(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = "store"
        ) {
            composable("store") {
                StorefrontScreen(
                    breakpoint = breakpoint,
                    productsState = productsState,
                    selectedNiche = selectedNiche,
                    searchQuery = searchQuery,
                    hasLiveGatewayKey = viewModel.hasLiveGatewayKey,
                    checkoutState = checkoutState,
                    defaultCustomerName = userDisplayName,
                    defaultCustomerEmail = userEmail,
                    onSelectNiche = viewModel::selectNiche,
                    onSearchChange = viewModel::updateSearchQuery,
                    onCheckoutSubmit = viewModel::processCheckout,
                    onResetCheckout = viewModel::resetCheckoutState
                )
            }
            composable("admin") {
                AdminPanelScreen(
                    breakpoint = breakpoint,
                    productsState = productsState,
                    ordersState = ordersState,
                    onSaveProduct = viewModel::saveProduct,
                    onAdjustStock = viewModel::adjustStock,
                    onToggleActive = viewModel::toggleProductActive,
                    onDeleteProduct = viewModel::deleteProduct
                )
            }
            composable("visits") {
                VisitsScreen(
                    breakpoint = breakpoint,
                    visitsState = visitsState,
                    productsState = productsState,
                    onScheduleVisit = viewModel::scheduleVisit,
                    onUpdateStatus = viewModel::updateVisitStatus,
                    onDeleteVisit = viewModel::deleteVisit
                )
            }
            composable("finance") {
                FinanceReportsScreen(
                    breakpoint = breakpoint,
                    selectedProfileView = selectedProfileView,
                    financialState = financialState,
                    ordersState = ordersState,
                    visitsState = visitsState,
                    onSelectProfileView = viewModel::selectProfileView,
                    onAddFinancialEntry = viewModel::addFinancialEntry,
                    onDeleteFinancialEntry = viewModel::deleteFinancialEntry
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    breakpoint = breakpoint,
                    notificationsState = notificationsState,
                    onSendPush = viewModel::sendPushNotification,
                    onMarkRead = viewModel::markNotificationRead
                )
            }
        }
    }
}
