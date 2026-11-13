package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.VitrineRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
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
    val icon: ImageVector
)

private val APP_TABS = listOf(
    NavTab("store", "Vitrine", Icons.Filled.Storefront),
    NavTab("admin", "Painel", Icons.Filled.Dashboard),
    NavTab("visits", "Visitas", Icons.Filled.CalendarMonth),
    NavTab("finance", "Financeiro", Icons.Filled.AccountBalanceWallet),
    NavTab("notifications", "Push", Icons.Filled.Notifications)
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "VitrineApps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                signOutUser(credentialManager, scope)
                            },
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
                if (!useNavigationRail) {
                    NavigationBar {
                        APP_TABS.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo("store") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
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
                if (useNavigationRail) {
                    NavigationRail {
                        APP_TABS.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo("store") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (tab.route == "notifications" && unreadCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = "store"
                    ) {
                        composable("store") {
                            StorefrontScreen(
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
                                visitsState = visitsState,
                                productsState = productsState,
                                onScheduleVisit = viewModel::scheduleVisit,
                                onUpdateStatus = viewModel::updateVisitStatus,
                                onDeleteVisit = viewModel::deleteVisit
                            )
                        }
                        composable("finance") {
                            FinanceReportsScreen(
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
                                notificationsState = notificationsState,
                                onSendPush = viewModel::sendPushNotification,
                                onMarkRead = viewModel::markNotificationRead
                            )
                        }
                    }
                }
            }
        }
    }
}
