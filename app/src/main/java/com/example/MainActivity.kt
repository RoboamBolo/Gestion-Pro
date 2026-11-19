package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.EmployeeRole
import com.example.ui.navigation.Routes
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.clients.ClientsScreen
import com.example.ui.screens.commercial.CommercialScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.equipments.EquipmentsScreen
import com.example.ui.screens.interventions.InterventionsScreen
import com.example.ui.screens.modules.ModulesHubScreen
import com.example.ui.screens.notifications.NotificationsAndAuditScreen
import com.example.ui.screens.planning.PlanningScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.sav.SavScreen
import com.example.ui.screens.stock.StockScreen
import com.example.ui.screens.team.TeamScreen
import com.example.ui.screens.timetracking.TimeTrackingScreen
import com.example.ui.theme.GestionProTheme
import com.example.viewmodel.GestionViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GestionViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            GestionProTheme(darkTheme = state.isDarkMode) {
                if (state.currentUser == null) {
                    AuthScreen(
                        employees = state.employees,
                        onSignIn = { viewModel.signInAs(it) },
                        onResetPin = { emp, pin -> viewModel.resetPin(emp, pin) }
                    )
                } else {
                    GestionProMainShell(viewModel = viewModel)
                }
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestionProMainShell(viewModel: GestionViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.DASHBOARD
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val bottomItems = listOf(
        NavItem(Routes.DASHBOARD, stringResource(R.string.nav_dashboard), Icons.Default.Dashboard),
        NavItem(Routes.INTERVENTIONS, stringResource(R.string.nav_interventions), Icons.Default.Build),
        NavItem(Routes.PLANNING, stringResource(R.string.nav_planning), Icons.Default.CalendarMonth),
        NavItem(Routes.CLIENTS, stringResource(R.string.nav_clients), Icons.Default.Business),
        NavItem(Routes.MODULES, stringResource(R.string.nav_modules), Icons.Default.Apps)
    )

    val role = EmployeeRole.entries.find { it.name == state.currentUser?.role } ?: EmployeeRole.TECHNICIEN
    val drawerItems = buildList {
        add(NavItem(Routes.DASHBOARD, "Tableau de bord", Icons.Default.Dashboard))
        add(NavItem(Routes.INTERVENTIONS, "Interventions", Icons.Default.Build))
        add(NavItem(Routes.PLANNING, "Planning & Calendrier", Icons.Default.CalendarMonth))
        add(NavItem(Routes.CLIENTS, "Clients", Icons.Default.Business))
        if (role.canAccessTeamAdmin() || role == EmployeeRole.PLANIFICATEUR || role == EmployeeRole.CHEF_EQUIPE) {
            add(NavItem(Routes.TEAM, "Équipe & Techniciens", Icons.Default.Badge))
        }
        add(NavItem(Routes.EQUIPMENTS, "Équipements & Maintenance", Icons.Default.PrecisionManufacturing))
        add(NavItem(Routes.STOCK, "Stocks & Fournitures", Icons.Default.Inventory2))
        if (role.canAccessBilling()) {
            add(NavItem(Routes.COMMERCIAL, "Commercial (Devis / Factures)", Icons.Default.ReceiptLong))
        }
        add(NavItem(Routes.SAV, "SAV & Incidents", Icons.Default.SupportAgent))
        add(NavItem(Routes.TIMETRACKING, "Pointage Personnel", Icons.Default.AccessTimeFilled))
        if (role.canAccessReports()) {
            add(NavItem(Routes.REPORTS, "Rapports & Exports", Icons.Default.Assessment))
        }
        add(NavItem(Routes.NOTIFICATIONS, "Notifications & Paramètres", Icons.Default.Notifications))
    }

    val screenTitle = drawerItems.find { it.route == currentRoute }?.label ?: "Gestion Pro"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 300.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GESTION PRO", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "${state.currentUser?.fullName ?: ""} • ${role.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigateTo(item.route)
                        },
                        icon = { Icon(item.icon, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                Spacer(Modifier.weight(1f))
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Changer d'utilisateur / Déconnexion") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.signOut()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(screenTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (state.isOfflineMode) {
                                    Text(
                                        if (!state.isNetworkConnected) "Hors connexion (${state.metrics.pendingSyncCount} en attente)"
                                        else "Mode hors ligne forcé (${state.metrics.pendingSyncCount} en attente)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.toggleDarkMode() }) {
                                Icon(
                                    imageVector = if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Mode clair/sombre"
                                )
                            }
                            IconButton(onClick = { navigateTo(Routes.NOTIFICATIONS) }) {
                                BadgedBox(
                                    badge = {
                                        if (state.metrics.unreadNotificationsCount > 0) {
                                            Badge { Text("${state.metrics.unreadNotificationsCount}") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isWide) {
                        NavigationBar {
                            bottomItems.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = { navigateTo(item.route) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWide) {
                        NavigationRail {
                            bottomItems.forEach { item ->
                                NavigationRailItem(
                                    selected = currentRoute == item.route,
                                    onClick = { navigateTo(item.route) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, maxLines = 1) }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.DASHBOARD,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(Routes.DASHBOARD) {
                                DashboardScreen(
                                    state = state,
                                    onNavigate = navigateTo,
                                    onSyncOffline = { viewModel.syncOfflineData() },
                                    onToggleOffline = { viewModel.toggleOfflineMode() }
                                )
                            }
                            composable(Routes.INTERVENTIONS) {
                                InterventionsScreen(
                                    state = state,
                                    observeLogs = { viewModel.observeInterventionLogs(it) },
                                    onSaveIntervention = { viewModel.saveIntervention(it) },
                                    onUpdateStatus = { item, st, notes, signer, inv ->
                                        viewModel.updateInterventionStatus(item, st, notes, signer, inv)
                                    },
                                    onDeleteIntervention = { viewModel.deleteIntervention(it) }
                                )
                            }
                            composable(Routes.PLANNING) {
                                PlanningScreen(
                                    state = state,
                                    onReschedule = { item, date, time, tech ->
                                        viewModel.rescheduleIntervention(item, date, time, tech)
                                    }
                                )
                            }
                            composable(Routes.CLIENTS) {
                                ClientsScreen(
                                    state = state,
                                    onSaveClient = { viewModel.saveClient(it) },
                                    onDeleteClient = { viewModel.deleteClient(it) }
                                )
                            }
                            composable(Routes.MODULES) {
                                ModulesHubScreen(state = state, onNavigate = navigateTo)
                            }
                            composable(Routes.TEAM) {
                                TeamScreen(
                                    state = state,
                                    onSaveEmployee = { viewModel.saveEmployee(it) },
                                    onUpdateAvailability = { emp, av, note ->
                                        viewModel.updateEmployeeAvailability(emp, av, note)
                                    }
                                )
                            }
                            composable(Routes.EQUIPMENTS) {
                                EquipmentsScreen(
                                    state = state,
                                    onSaveEquipment = { viewModel.saveEquipment(it) },
                                    onDeleteEquipment = { viewModel.deleteEquipment(it) },
                                    onAddMaintenance = { eq, type, desc, tech, cost, next, status ->
                                        viewModel.addMaintenanceRecord(eq, type, desc, tech, cost, next, status)
                                    }
                                )
                            }
                            composable(Routes.STOCK) {
                                StockScreen(
                                    state = state,
                                    onSaveStockItem = { viewModel.saveStockItem(it) },
                                    onDeleteStockItem = { viewModel.deleteStockItem(it) },
                                    onRecordMovement = { item, type, qty, reason ->
                                        viewModel.recordStockMovement(item, type, qty, reason)
                                    }
                                )
                            }
                            composable(Routes.COMMERCIAL) {
                                CommercialScreen(
                                    state = state,
                                    onSaveDoc = { viewModel.saveCommercialDoc(it) },
                                    onDeleteDoc = { viewModel.deleteCommercialDoc(it) },
                                    onConvertDevis = { viewModel.convertDevisToFacture(it) },
                                    onRecordPayment = { doc, amt -> viewModel.recordPayment(doc, amt) },
                                    onShowMessage = { viewModel.showFeedback(it) }
                                )
                            }
                            composable(Routes.SAV) {
                                SavScreen(
                                    state = state,
                                    onSaveTicket = { viewModel.saveSavTicket(it) },
                                    onAppendExchange = { ticket, note, status ->
                                        viewModel.appendSavExchange(ticket, note, status)
                                    }
                                )
                            }
                            composable(Routes.TIMETRACKING) {
                                TimeTrackingScreen(
                                    state = state,
                                    onCheckIn = { viewModel.checkInCurrent(it) },
                                    onCheckOut = { entry, note -> viewModel.checkOutEntry(entry, note) }
                                )
                            }
                            composable(Routes.REPORTS) {
                                ReportsScreen(
                                    state = state,
                                    onShowMessage = { viewModel.showFeedback(it) }
                                )
                            }
                            composable(Routes.NOTIFICATIONS) {
                                NotificationsAndAuditScreen(
                                    state = state,
                                    onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                    onMarkRead = { viewModel.markNotificationRead(it) },
                                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                                    onToggleOfflineMode = { viewModel.toggleOfflineMode() },
                                    onSyncNow = { viewModel.syncOfflineData() },
                                    onRetrySyncItem = { viewModel.retrySyncItem(it) },
                                    onResolveSyncConflict = { viewModel.resolveSyncConflictKeepLocal(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
