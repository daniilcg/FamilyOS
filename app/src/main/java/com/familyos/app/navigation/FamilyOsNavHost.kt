package com.familyos.app.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.familyos.app.permissions.RequestCorePermissionsOnStart
import com.familyos.app.ui.MainViewModel
import com.familyos.feature.ai.navigation.AiRoutes
import com.familyos.feature.ai.navigation.aiGraph
import com.familyos.feature.auth.google.GoogleSignInHelper
import com.familyos.feature.auth.navigation.AuthRoutes
import com.familyos.feature.auth.navigation.authNavGraph
import com.familyos.feature.billing.navigation.BillingRoutes
import com.familyos.feature.billing.navigation.billingGraph
import com.familyos.feature.budget.navigation.BudgetRoutes
import com.familyos.feature.budget.navigation.budgetNavGraph
import com.familyos.feature.calendar.navigation.CalendarRoutes
import com.familyos.feature.calendar.navigation.calendarNavGraph
import com.familyos.feature.chat.navigation.ChatRoutes
import com.familyos.feature.chat.navigation.chatGraph
import com.familyos.feature.documents.navigation.DocumentsRoutes
import com.familyos.feature.documents.navigation.documentsGraph
import com.familyos.feature.family.navigation.FamilyRoutes
import com.familyos.feature.family.navigation.familyNavGraph
import com.familyos.feature.home.navigation.HomeRoutes
import com.familyos.feature.home.navigation.homeNavGraph
import com.familyos.feature.notes.navigation.NotesRoutes
import com.familyos.feature.notes.navigation.notesGraph
import com.familyos.feature.notifications.navigation.NotificationsRoutes
import com.familyos.feature.notifications.navigation.notificationsGraph
import com.familyos.feature.profile.navigation.ProfileRoutes
import com.familyos.feature.profile.navigation.profileNavGraph
import com.familyos.feature.settings.navigation.SettingsRoutes
import com.familyos.feature.settings.navigation.settingsNavGraph
import com.familyos.feature.shopping.navigation.ShoppingRoutes
import com.familyos.feature.shopping.navigation.shoppingNavGraph
import com.familyos.feature.tasks.navigation.TaskRoutes
import com.familyos.feature.tasks.navigation.tasksNavGraph

/**
 * Top-level route identifiers used by the bottom bar / More hub.
 */
object AppRoutes {
    const val MORE = "more"
}

private data class BottomDest(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Root Compose navigation host with auth gate and bottom bar:
 * Home, Shopping, Tasks, Calendar, More.
 */
@Composable
fun FamilyOsNavHost(
    googleSignInHelper: GoogleSignInHelper,
    mainViewModel: MainViewModel,
) {
    val navController = rememberNavController()
    val user by mainViewModel.currentUser.collectAsStateWithLifecycle()
    val startDestination = if (user == null) AuthRoutes.GRAPH else HomeRoutes.HOME

    RequestCorePermissionsOnStart(enabled = user != null)

    LaunchedEffect(user?.id) {
        if (user == null) {
            val route = navController.currentDestination?.route
            if (route != null && route != AuthRoutes.LOGIN && !route.startsWith("auth")) {
                navController.navigate(AuthRoutes.GRAPH) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val bottomItems = listOf(
        BottomDest(HomeRoutes.HOME, "Home", Icons.Outlined.Home),
        BottomDest(ShoppingRoutes.GRAPH, "Shopping", Icons.Outlined.ShoppingCart),
        BottomDest(TaskRoutes.GRAPH, "Tasks", Icons.Outlined.TaskAlt),
        BottomDest(CalendarRoutes.GRAPH, "Calendar", Icons.Outlined.CalendarMonth),
        BottomDest(AppRoutes.MORE, "More", Icons.Outlined.MoreHoriz),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = user != null && (
        currentRoute == HomeRoutes.HOME ||
            currentRoute?.startsWith("shopping") == true ||
            currentRoute?.startsWith("tasks") == true ||
            currentRoute?.startsWith("calendar") == true ||
            currentRoute == AppRoutes.MORE ||
            currentRoute in moreChildRoutes
        )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { dest ->
                        val selected = when (dest.route) {
                            ShoppingRoutes.GRAPH -> currentRoute?.startsWith("shopping") == true
                            TaskRoutes.GRAPH -> currentRoute?.startsWith("tasks") == true
                            CalendarRoutes.GRAPH -> currentRoute?.startsWith("calendar") == true
                            AppRoutes.MORE -> currentRoute == AppRoutes.MORE || currentRoute in moreChildRoutes
                            else -> currentRoute == dest.route
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            authNavGraph(
                navController = navController,
                googleSignInHelper = googleSignInHelper,
                onAuthenticated = {
                    navController.navigate(HomeRoutes.HOME) {
                        popUpTo(AuthRoutes.GRAPH) { inclusive = true }
                    }
                },
            )

            homeNavGraph(
                onOpenTasks = { navController.navigate(TaskRoutes.GRAPH) },
                onOpenShopping = { navController.navigate(ShoppingRoutes.GRAPH) },
                onOpenCalendar = { navController.navigate(CalendarRoutes.GRAPH) },
                onOpenBudget = { navController.navigate(BudgetRoutes.GRAPH) },
                onCreateOrJoinFamily = { navController.navigate(FamilyRoutes.CREATE) },
            )

            shoppingNavGraph(navController = navController)
            tasksNavGraph(navController = navController)
            calendarNavGraph(navController = navController)
            budgetNavGraph(navController = navController)
            aiGraph()
            billingGraph()
            notificationsGraph()

            composable(AppRoutes.MORE) {
                MoreScreen(
                    onOpenNotes = { navController.navigate(NotesRoutes.LIST) },
                    onOpenDocuments = { navController.navigate(DocumentsRoutes.ROOT) },
                    onOpenBudget = { navController.navigate(BudgetRoutes.GRAPH) },
                    onOpenChat = { navController.navigate(ChatRoutes.ROOT) },
                    onOpenAi = { navController.navigate(AiRoutes.ROOT) },
                    onOpenNotifications = { navController.navigate(NotificationsRoutes.ROOT) },
                    onOpenBilling = { navController.navigate(BillingRoutes.PAYWALL) },
                    onOpenFamily = { navController.navigate(FamilyRoutes.MEMBERS) },
                    onOpenSettings = { navController.navigate(SettingsRoutes.SETTINGS) },
                    onOpenProfile = { navController.navigate(ProfileRoutes.PROFILE) },
                    onCreateFamily = { navController.navigate(FamilyRoutes.CREATE) },
                    onJoinFamily = { navController.navigate(FamilyRoutes.JOIN) },
                )
            }

            notesGraph(navController)
            documentsGraph(navController)
            chatGraph()
            familyNavGraph(navController)
            profileNavGraph(
                onNavigateBack = { navController.popBackStack() },
                onAccountDeleted = {
                    navController.navigate(AuthRoutes.GRAPH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
            settingsNavGraph(
                onNavigateBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(AuthRoutes.GRAPH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenProfile = { navController.navigate(ProfileRoutes.PROFILE) },
            )
        }
    }
}

private val moreChildRoutes = setOf(
    NotesRoutes.LIST,
    DocumentsRoutes.ROOT,
    BudgetRoutes.GRAPH,
    BudgetRoutes.HOME,
    ChatRoutes.ROOT,
    AiRoutes.ROOT,
    NotificationsRoutes.ROOT,
    BillingRoutes.PAYWALL,
    FamilyRoutes.MEMBERS,
    FamilyRoutes.CREATE,
    FamilyRoutes.JOIN,
    FamilyRoutes.INVITE,
    SettingsRoutes.SETTINGS,
    ProfileRoutes.PROFILE,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreScreen(
    onOpenNotes: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBilling: () -> Unit,
    onOpenFamily: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onCreateFamily: () -> Unit,
    onJoinFamily: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("More") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            MoreRow(Icons.AutoMirrored.Outlined.Note, "Notes", onOpenNotes)
            MoreRow(Icons.Outlined.Folder, "Documents", onOpenDocuments)
            MoreRow(Icons.Outlined.Payments, "Budget", onOpenBudget)
            MoreRow(Icons.AutoMirrored.Outlined.Chat, "Chat", onOpenChat)
            MoreRow(Icons.Outlined.AutoAwesome, "Family AI", onOpenAi)
            MoreRow(Icons.Outlined.Notifications, "Notifications", onOpenNotifications)
            MoreRow(Icons.Outlined.WorkspacePremium, "Premium", onOpenBilling)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MoreRow(Icons.Outlined.AccountTree, "Family members", onOpenFamily)
            MoreRow(Icons.Outlined.AccountTree, "Create family", onCreateFamily)
            MoreRow(Icons.Outlined.AccountTree, "Join family", onJoinFamily)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MoreRow(Icons.Outlined.Person, "Profile", onOpenProfile)
            MoreRow(Icons.Outlined.Settings, "Settings", onOpenSettings)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
