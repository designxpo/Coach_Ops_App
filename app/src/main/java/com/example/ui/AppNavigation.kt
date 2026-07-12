package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppControlState
import com.example.data.AppDatabase
import com.example.data.AuthRepository
import com.example.data.CoachRepository
import com.example.data.FeatureFlagManager
import com.example.data.UserPreferences
import com.example.data.WeeklyMealPlan
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary

/**
 * Detail-screen push guarded against double-tap: launchSingleTop prevents two
 * copies of the same screen stacking when a card is tapped twice quickly
 * (which made the back button need two presses).
 */
private fun androidx.navigation.NavHostController.push(route: String) =
    navigate(route) { launchSingleTop = true }

sealed class Screen(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Gym : Screen("gym", "Gym", Icons.Filled.Storefront)
    object Clients : Screen("clients", "Members", Icons.Filled.SupervisorAccount)
    object Programs : Screen("programs", "Programs", Icons.Filled.Checklist)
    object Billing : Screen("billing", "Revenue", Icons.Filled.AttachMoney)
    object Bookings : Screen("bookings", "Bookings", Icons.Filled.DateRange)
    object Library : Screen("coach_library", "Library", Icons.Filled.FitnessCenter)
}

sealed class ClientScreen(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Discover : ClientScreen("discover", "Discover", Icons.Filled.Search)
    object Fitness : ClientScreen("fitness", "Fitness", Icons.Filled.FitnessCenter)
    object MyBookings : ClientScreen("my_bookings", "Bookings", Icons.Filled.BookOnline)
    object Messages : ClientScreen("member_chat", "Messages", Icons.Filled.ChatBubbleOutline)
    object Profile : ClientScreen("client_profile", "Profile", Icons.Filled.Person)
}

@Composable
fun CoachOpsApp(
    repository: CoachRepository,
    userPreferences: UserPreferences,
    featureFlagManager: FeatureFlagManager
) {
    val viewModel: MainViewModel = viewModel(
        factory = CoachViewModelFactory(repository, userPreferences, featureFlagManager)
    )
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(userPreferences)
    )
    MainAppScreen(viewModel, userPreferences, chatViewModel)
}

@Composable
fun MainAppScreen(viewModel: MainViewModel, userPreferences: UserPreferences, chatViewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = ChatViewModelFactory(userPreferences))) {
    val appControl by viewModel.appControl.collectAsState()

    // ── Maintenance mode: replace entire app with message screen ──────────────
    if (appControl.maintenanceMode) {
        MaintenanceScreen(appControl.maintenanceMessage)
        return
    }

    // ── Force update: replace app with update-required screen ─────────────────
    if (appControl.forceUpdate) {
        ForceUpdateScreen(appControl.updateMessage)
        return
    }

    // ── Account suspended by admin ────────────────────────────────────────────
    val isSuspended by viewModel.isSuspended.collectAsState()
    if (isSuspended) {
        SuspendedScreen()
        return
    }

    // ── Compulsory update gate (version-count based) ──────────────────────────
    // Applies to EVERYONE — sits before the coach/member fork below. When a user
    // is more than `compulsoryUpdateAfter` versions behind the latest release
    // (set in the admin App Control page), they must update to keep using the app.
    val versionsBehind = if (appControl.latestVersionCode > 0)
        appControl.latestVersionCode - com.example.BuildConfig.VERSION_CODE else 0
    if (versionsBehind > appControl.compulsoryUpdateAfter) {
        UpdateGateSheet(appControl.updateMessage)
        return
    }

    // ── Role self-heal ────────────────────────────────────────────────────────
    // Older builds never persisted role for members who signed in via Google on
    // the login screen, so the admin panel showed them as coaches (its default
    // for role-less docs). Backfill Firestore from local prefs exactly once per
    // launch — runs BEFORE the client early-return so members are healed too.
    LaunchedEffect(Unit) {
        val healUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val localRole = userPreferences.userRole
        if (healUid != null && localRole.isNotEmpty() &&
            com.example.data.FirestoreSync.getUserRole(healUid) == null
        ) {
            com.example.data.FirestoreSync.setUserRole(localRole)
        }
    }

    // ── Role fork: clients see the discovery/booking app ─────────────────────
    // Requires a live Firebase session — a signed-out user must never be able
    // to sit inside the member app on stale prefs.
    if (userPreferences.userRole == "client" && userPreferences.onboardingComplete &&
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
        val activityContext = LocalContext.current
        ClientNavScreen(
            userPreferences   = userPreferences,
            onNavigateToLogin = {
                // The NavHost doesn't exist yet on this path — recreate the Activity
                // so the SplashScreen routes to login fresh
                (activityContext as? android.app.Activity)?.recreate()
            }
        )
        return
    }

    // Entitlements drive tier-differentiated UI (Gym tab, locked features)
    val billingContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        com.example.data.EntitlementManager.start(userPreferences)
        com.example.data.BillingManager.init(billingContext)   // restores active subscriptions on launch
    }
    val entitlements by com.example.data.EntitlementManager.entitlements.collectAsState()
    val isGymOwner = userPreferences.userRole == "gym_owner"

    // Soft "update available" nudge — dismissible for this session
    var updateNudgeDismissed by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    // Always 5 tabs (Instagram-style) — Gym lives in the Home top bar + quick access
    val items = listOf(Screen.Home, Screen.Clients, Screen.Programs, Screen.Billing, Screen.Bookings)
    val gymAvailable = isGymOwner || entitlements.gymUnlocked

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Screens where nav bar should be hidden (fullscreen flows: auth, chat, editors, paywall)
    val hideNavRoutes = setOf(
        "login", "register", "onboarding", "role_picker", "client_onboarding", "client_nav", "splash",
        "upgrade_plans",
        "coach_chat/{chatId}/{memberName}/{memberPhone}",
        "coach_chat_new/{memberId}/{memberName}/{memberPhone}",
        "diet_editor/{clientId}"
    )
    val showBottomBar = currentRoute != null && currentRoute !in hideNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        containerColor = CyberBgPrimary,
        bottomBar = {
            if (showBottomBar) {
                AppBottomNav(
                    items = items.map { Triple(it.route, it.title, it.icon) },
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Don't save/restore state — prevents nested routes (e.g. Library)
                            // being restored when returning to a tab
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // ── Announcement banner (shown when logged in and on main tabs) ───
            if (showBottomBar && appControl.announcementEnabled && appControl.announcementText.isNotEmpty()) {
                AnnouncementBanner(appControl)
            }
            // ── Soft update nudge (behind, but within the compulsory threshold) ─
            if (showBottomBar && appControl.updateNudgeEnabled && !updateNudgeDismissed &&
                versionsBehind in 1..appControl.compulsoryUpdateAfter) {
                UpdateNudgeBanner(appControl.updateMessage) { updateNudgeDismissed = true }
            }
            NavHost(
                navController,
                startDestination = "splash",
                modifier = Modifier.weight(1f)
            ) {
                composable("splash") {
                    val dest = remember {
                        val loggedIn = AuthRepository.currentUser != null
                        val role = userPreferences.userRole
                        when {
                            loggedIn && role.isEmpty()                                          -> "role_picker"
                            loggedIn && userPreferences.onboardingComplete && role == "client" -> "client_nav"
                            loggedIn && userPreferences.onboardingComplete                     -> Screen.Home.route
                            loggedIn && role == "client"                                       -> "client_onboarding"
                            loggedIn                                                           -> "onboarding"
                            else                                                               -> "login"
                        }
                    }
                    SplashScreen {
                        navController.navigate(dest) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
                composable("role_picker") {
                    RolePickerScreen(
                        onRoleSelected = { role ->
                            userPreferences.userRole = role
                            val dest = if (role == "client") "client_onboarding" else "onboarding"
                            navController.navigate(dest) {
                                popUpTo("role_picker") { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.navigate("login") {
                                popUpTo("role_picker") { inclusive = true }
                            }
                        }
                    )
                }
                composable("login") {
                    LoginScreen(
                        userPreferences = userPreferences,
                        viewModel = viewModel,
                        onLoginSuccess = {
                            // Bind to the authenticated UID — wipes prefs if a different user logged in
                            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            userPreferences.bindToUser(uid)
                            com.example.data.EntitlementManager.start(userPreferences)
                            viewModel.syncFromCloud()
                            val role = userPreferences.userRole
                            val dest = when {
                                role.isEmpty()                                          -> "role_picker"
                                role == "client" && userPreferences.onboardingComplete -> "client_nav"
                                role == "client"                                       -> "client_onboarding"
                                userPreferences.onboardingComplete                     -> Screen.Home.route
                                else                                                   -> "onboarding"
                            }
                            navController.navigate(dest) {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onNavigateToRegister = { navController.push("register") }
                    )
                }
                composable("register") {
                    RegisterScreen(
                        userPreferences = userPreferences,
                        onRegisterSuccess = { isNewUser ->
                            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            userPreferences.bindToUser(uid)
                            com.example.data.EntitlementManager.start(userPreferences)
                            viewModel.syncFromCloud()
                            val role = userPreferences.userRole
                            val dest = when {
                                isNewUser && role == "client" -> "client_onboarding"
                                isNewUser -> "onboarding"
                                role == "client" -> "client_nav"
                                else -> { userPreferences.onboardingComplete = true; Screen.Home.route }
                            }
                            navController.navigate(dest) {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onNavigateToLogin = { navController.popBackStack() }
                    )
                }
                composable("client_onboarding") {
                    ClientOnboardingScreen(
                        userPreferences = userPreferences,
                        onComplete = {
                            navController.navigate("client_nav") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    )
                }
                composable("client_nav") {
                    ClientNavScreen(
                        userPreferences   = userPreferences,
                        onNavigateToLogin = {
                            navController.navigate("login") { popUpTo(0) { inclusive = true } }
                        }
                    )
                }
                composable("onboarding") {
                    OnboardingScreen(
                        viewModel = viewModel,
                        initialName = userPreferences.coachName,
                        initialPhone = userPreferences.coachPhone
                    ) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
                composable("profile") {
                    val profileContext = androidx.compose.ui.platform.LocalContext.current
                    ProfileScreen(
                        viewModel = viewModel,
                        userPreferences = userPreferences,
                        onBack = { navController.popBackStack() },
                        onLogout = {
                            com.example.data.AuthRepository.signOut(profileContext)
                            viewModel.logout(com.example.data.GymRepository.getInstance(profileContext))
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onPrivacyPolicyClick = { navController.push("privacy_policy") },
                        onTermsClick = { navController.push("privacy_policy") },
                        onDeleteAccountClick = { /* TODO: show delete account dialog */ },
                        onManagePlanClick = { navController.push("upgrade_plans") },
                        onEditPortfolioClick = { navController.push("portfolio_builder") },
                    )
                }
                composable("portfolio_builder") {
                    PortfolioBuilderScreen(
                        viewModel = viewModel,
                        userPreferences = userPreferences,
                        onBack = { navController.popBackStack() },
                        onUpgradeClick = { navController.push("upgrade_plans") }
                    )
                }
                composable("privacy_policy") {
                    PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Home.route) {
                    val chatThreads by chatViewModel.threads.collectAsState()
                    val totalUnread = chatThreads.sumOf { it.unreadCoach }
                    HomeScreen(
                        viewModel       = viewModel,
                        onProfileClick  = { navController.push("profile") },
                        onClientClick   = { clientId -> navController.push("client/$clientId") },
                        onChatClick     = { navController.push("coach_chat_list") },
                        onLibraryClick  = { navController.navigate(Screen.Library.route) },
                        chatUnreadCount = totalUnread,
                        showGym         = gymAvailable,
                        onGymClick      = { navController.navigate(Screen.Gym.route) }
                    )
                }
                composable(Screen.Clients.route) {
                    ClientsScreen(
                        viewModel = viewModel,
                        onClientClick = { clientId -> navController.push("client/$clientId") },
                        onChatClick = { memberId, memberName, memberPhone ->
                            navController.push("coach_chat_new/$memberId/${Uri.encode(memberName)}/${Uri.encode(memberPhone)}")
                        },
                        onUpgradeClick = { navController.push("upgrade_plans") }
                    )
                }
                composable(Screen.Programs.route) {
                    ProgramsScreen(viewModel) { programId -> navController.push("program/$programId") }
                }
                composable("program/{programId}") { backStack ->
                    val programId = backStack.arguments?.getString("programId") ?: return@composable
                    ProgramDetailScreen(
                        viewModel = viewModel,
                        programId = programId,
                        onBack = { navController.popBackStack() },
                        onClientClick = { clientId -> navController.push("client/$clientId") }
                    )
                }
                composable(Screen.Billing.route) {
                    BillingScreen(viewModel, onUpgradeClick = { navController.push("upgrade_plans") })
                }
                composable(Screen.Bookings.route) { CoachBookingsScreen(viewModel) }

                // ─── Gym Suite (gym owners + Business plan) ────────────────────
                composable(Screen.Gym.route) {
                    val gymContext = LocalContext.current
                    val gymVm: GymViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = GymViewModelFactory(
                            com.example.data.GymRepository.getInstance(gymContext), userPreferences,
                            CoachRepository.getInstance(gymContext))
                    )
                    GymDashboardScreen(
                        viewModel = gymVm,
                        onMembersClick = { navController.push("gym_members") },
                        onMemberClick = { id -> navController.push("gym_member/$id") },
                        onPlansClick = { navController.push("gym_plans") },
                        onAttendanceClick = { navController.push("gym_attendance") },
                        onUpgradeClick = { navController.push("upgrade_plans") }
                    )
                }
                composable("gym_members") {
                    val gymContext = LocalContext.current
                    val gymVm: GymViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = GymViewModelFactory(
                            com.example.data.GymRepository.getInstance(gymContext), userPreferences,
                            CoachRepository.getInstance(gymContext))
                    )
                    GymMembersScreen(
                        viewModel = gymVm,
                        onBack = { navController.popBackStack() },
                        onMemberClick = { id -> navController.push("gym_member/$id") }
                    )
                }
                composable("gym_member/{memberId}") { backStack ->
                    val memberId = backStack.arguments?.getString("memberId") ?: return@composable
                    val gymContext = LocalContext.current
                    val gymVm: GymViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = GymViewModelFactory(
                            com.example.data.GymRepository.getInstance(gymContext), userPreferences,
                            CoachRepository.getInstance(gymContext))
                    )
                    GymMemberDetailScreen(
                        viewModel = gymVm,
                        memberId = memberId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("gym_plans") {
                    val gymContext = LocalContext.current
                    val gymVm: GymViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = GymViewModelFactory(
                            com.example.data.GymRepository.getInstance(gymContext), userPreferences,
                            CoachRepository.getInstance(gymContext))
                    )
                    GymPlansScreen(viewModel = gymVm, onBack = { navController.popBackStack() })
                }
                composable("gym_attendance") {
                    val gymContext = LocalContext.current
                    val gymVm: GymViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = GymViewModelFactory(
                            com.example.data.GymRepository.getInstance(gymContext), userPreferences,
                            CoachRepository.getInstance(gymContext))
                    )
                    GymAttendanceScreen(viewModel = gymVm, onBack = { navController.popBackStack() })
                }

                // ─── Plans & pricing ───────────────────────────────────────────
                composable("upgrade_plans") {
                    CoachUpgradeScreen(
                        userPreferences = userPreferences,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("client/{clientId}") { backStack ->
                    val clientId = backStack.arguments?.getString("clientId") ?: return@composable
                    ClientDetailScreen(
                        viewModel      = viewModel,
                        clientId       = clientId,
                        onBack         = { navController.popBackStack() },
                        onDietPlanEdit = { cId -> navController.push("diet_editor/$cId") }
                    )
                }
                composable("diet_editor/{clientId}") { backStack ->
                    val cId = backStack.arguments?.getString("clientId") ?: return@composable
                    val clients by viewModel.clients.collectAsStateWithLifecycle()
                    val clientName = clients.find { it.id == cId }?.name ?: ""
                    DietPlanEditorScreen(
                        viewModel  = viewModel,
                        clientId   = cId,
                        clientName = clientName,
                        onBack     = { navController.popBackStack() }
                    )
                }
                composable(Screen.Library.route) {
                    val libVm: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "coach_lib", factory = FitnessViewModelFactory(userPreferences)
                    )
                    CoachLibraryScreen(
                        viewModel = libVm,
                        onCategoryClick = { cat ->
                            navController.push("coach_ex_category/${cat.name}")
                        },
                        onExerciseClick = { id ->
                            navController.push("coach_ex_detail/$id")
                        },
                        onNutritionClick = {
                            navController.push("coach_nutrition_goals")
                        },
                        onHealthMetricsClick = {
                            navController.push("coach_health_metrics")
                        }
                    )
                }
                composable("coach_chat_list") {
                    CoachChatListScreen(
                        viewModel = chatViewModel,
                        onOpenChat = { chatId, memberName, memberPhone ->
                            navController.push("coach_chat/$chatId/${Uri.encode(memberName)}/${Uri.encode(memberPhone)}")
                        }
                    )
                }
                composable("coach_chat/{chatId}/{memberName}/{memberPhone}") { back ->
                    val chatId      = back.arguments?.getString("chatId") ?: return@composable
                    val memberName  = Uri.decode(back.arguments?.getString("memberName") ?: "")
                    val memberPhone = Uri.decode(back.arguments?.getString("memberPhone") ?: "")
                    ChatScreen(
                        viewModel   = chatViewModel,
                        chatId      = chatId,
                        otherName   = memberName,
                        otherPhone  = memberPhone,
                        onBack      = { navController.popBackStack() }
                    )
                }
                composable("coach_chat_new/{memberId}/{memberName}/{memberPhone}") { back ->
                    val memberId    = back.arguments?.getString("memberId") ?: return@composable
                    val memberName  = Uri.decode(back.arguments?.getString("memberName") ?: "")
                    val memberPhone = Uri.decode(back.arguments?.getString("memberPhone") ?: "")
                    var chatId by remember { mutableStateOf("") }
                    LaunchedEffect(memberId) {
                        chatViewModel.openOrCreateChat(memberId, memberName, memberPhone) { id ->
                            chatId = id
                        }
                    }
                    if (chatId.isNotEmpty()) {
                        ChatScreen(
                            viewModel   = chatViewModel,
                            chatId      = chatId,
                            otherName   = memberName,
                            otherPhone  = memberPhone,
                            onBack      = { navController.popBackStack() }
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(CyberBgPrimary), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator(color = CyberAccent)
                        }
                    }
                }
                composable("coach_health_metrics") {
                    val libVm: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "coach_lib", factory = FitnessViewModelFactory(userPreferences)
                    )
                    HealthMetricsScreen(
                        viewModel = libVm,
                        onBack = { navController.popBackStack() },
                        onExerciseClick = { id -> navController.push("coach_ex_detail/$id") }
                    )
                }
                composable("coach_nutrition_goals") {
                    val libVm: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "coach_lib", factory = FitnessViewModelFactory(userPreferences)
                    )
                    NutritionGoalScreen(
                        viewModel = libVm,
                        onGoalClick = { goal -> navController.push("coach_nutrition_plan/${goal.name}") },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("coach_nutrition_plan/{goal}") { backStack ->
                    val goalName = backStack.arguments?.getString("goal") ?: return@composable
                    val goal = com.example.data.ClientGoal.valueOf(goalName)
                    val libVm: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "coach_lib", factory = FitnessViewModelFactory(userPreferences)
                    )
                    NutritionScreen(
                        viewModel = libVm,
                        goal = goal,
                        onBack = { navController.popBackStack() },
                        isCoachMode = true
                    )
                }
                composable("coach_ex_category/{category}") { backStack ->
                    val catName = backStack.arguments?.getString("category") ?: return@composable
                    val cat = com.example.data.ExerciseCategory.valueOf(catName)
                    val libVm: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "coach_lib", factory = FitnessViewModelFactory(userPreferences)
                    )
                    ExerciseCategoryScreen(
                        viewModel = libVm,
                        category  = cat,
                        onExerciseClick = { id -> navController.push("coach_ex_detail/$id") },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("coach_ex_detail/{exerciseId}") { backStack ->
                    val id = backStack.arguments?.getString("exerciseId") ?: return@composable
                    val libVm: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "coach_lib", factory = FitnessViewModelFactory(userPreferences)
                    )
                    ExerciseDetailScreen(
                        viewModel  = libVm,
                        exerciseId = id,
                        onBack     = { navController.popBackStack() },
                        isCoachMode = true,
                        onNavigate = { relId -> navController.push("coach_ex_detail/$relId") }
                    )
                }
            }
        }
    }
}

// ─── Client Navigation Shell ─────────────────────────────────────────────────

@Composable
fun ClientNavScreen(userPreferences: UserPreferences, onNavigateToLogin: () -> Unit = {}) {
    val viewModel: ClientViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ClientViewModelFactory(userPreferences)
    )
    val memberChatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "member_chat_global", factory = ChatViewModelFactory(userPreferences)
    )
    LaunchedEffect(Unit) {
        memberChatVm.restartListeningThreads()
        com.example.data.EntitlementManager.start(userPreferences)
    }
    val entitlements by com.example.data.EntitlementManager.entitlements.collectAsState()
    val chatThreads by memberChatVm.threads.collectAsState()
    val memberUnread = chatThreads.sumOf { it.unreadMember }

    val context = LocalContext.current
    val navController = rememberNavController()
    val clientItems = listOf(ClientScreen.Discover, ClientScreen.Fitness, ClientScreen.MyBookings, ClientScreen.Messages, ClientScreen.Profile)
    var sharedMealPlan by remember { mutableStateOf<WeeklyMealPlan?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Hide bottom nav on fullscreen experiences: camera, chats, AI coach (keyboard-heavy)
    val hideNavRoutes = setOf(
        "food_scanner",
        "nutrition_coach",
        "member_chat_open/{chatId}/{coachName}",
        "member_chat_new/{coachId}/{coachName}"
    )
    val showBottomBar = currentRoute != null && currentRoute !in hideNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        containerColor = CyberBgPrimary,
        bottomBar = {
            if (showBottomBar) {
                AppBottomNav(
                    items = clientItems.map { Triple(it.route, it.title, it.icon) },
                    currentRoute = currentRoute,
                    badges = mapOf(ClientScreen.Messages.route to memberUnread),
                    onNavigate = { route ->
                        // Pop-first: if the target tab is already in the back stack
                        // (we're on it, or on one of its detail screens), pop back to
                        // it — so a tab tap ALWAYS visibly responds. Otherwise do a
                        // state-preserving tab switch.
                        val popped = navController.popBackStack(route, false)
                        if (!popped) {
                            navController.navigate(route) {
                                popUpTo(ClientScreen.Discover.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = ClientScreen.Discover.route,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable(ClientScreen.Discover.route) {
                DiscoverScreen(
                    viewModel = viewModel,
                    onTrainerClick = { uid -> navController.push("trainer_detail/$uid") },
                    // Avatar = tab switch to Profile (NOT a push onto this tab's
                    // stack — a push contaminates the saved state and makes the
                    // origin tab look dead when re-tapped)
                    onAvatarClick  = {
                        navController.navigate(ClientScreen.Profile.route) {
                            popUpTo(ClientScreen.Discover.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("trainer_detail/{trainerId}") { back ->
                val id = back.arguments?.getString("trainerId") ?: return@composable
                TrainerDetailScreen(viewModel = viewModel, trainerId = id, onBack = { navController.popBackStack() })
            }
            composable(ClientScreen.Fitness.route) {
                val fitnessViewModel: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = FitnessViewModelFactory(userPreferences)
                )
                val healthViewModel: HealthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = HealthViewModelFactory(userPreferences, context)
                )
                FitnessHubScreen(
                    viewModel            = fitnessViewModel,
                    healthViewModel      = healthViewModel,
                    onCategoryClick      = { cat -> navController.push("exercise_category/${cat.name}") },
                    onNutritionClick     = { navController.push("nutrition_goals") },
                    onProgressClick      = { navController.push("fitness_progress") },
                    onHealthMetricsClick = { navController.push("health_metrics") },
                    onMyDietClick        = { navController.push("my_diet") },
                    onFoodScanClick         = { navController.push("food_scanner") },
                    onFoodDiaryClick        = { navController.push("food_diary") },
                    onBodyMeasurementsClick = { navController.push("body_measurements") },
                    onProgressPhotosClick   = { navController.push("progress_photos") },
                    onCycleTrackerClick     = { navController.push("cycle_tracker") },
                    onNutritionCoachClick   = { navController.push("nutrition_coach") },
                    onMealPlannerClick      = { navController.push("meal_planner") },
                    onHealthConnectClick    = { navController.push("health_connect") },
                    onAwardsClick           = { navController.push("awards") },
                    // Tab-switch semantics — see Discover's onAvatarClick
                    onAvatarClick           = {
                        navController.navigate(ClientScreen.Profile.route) {
                            popUpTo(ClientScreen.Discover.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("health_metrics") {
                val fitnessViewModel: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = FitnessViewModelFactory(userPreferences)
                )
                HealthMetricsScreen(
                    viewModel = fitnessViewModel,
                    onBack = { navController.popBackStack() },
                    onExerciseClick = { id -> navController.push("exercise_detail/$id") }
                )
            }
            composable("exercise_category/{category}") { back ->
                val catName = back.arguments?.getString("category") ?: return@composable
                val cat = com.example.data.ExerciseCategory.valueOf(catName)
                val fitnessViewModel: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = FitnessViewModelFactory(userPreferences)
                )
                ExerciseCategoryScreen(
                    viewModel = fitnessViewModel,
                    category  = cat,
                    onExerciseClick = { id -> navController.push("exercise_detail/$id") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("exercise_detail/{exerciseId}") { back ->
                val id = back.arguments?.getString("exerciseId") ?: return@composable
                val fitnessViewModel: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = FitnessViewModelFactory(userPreferences)
                )
                ExerciseDetailScreen(
                    viewModel = fitnessViewModel,
                    exerciseId = id,
                    onBack = { navController.popBackStack() },
                    onNavigate = { relId -> navController.push("exercise_detail/$relId") }
                )
            }
            composable("nutrition_goals") {
                val fitnessViewModel: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = FitnessViewModelFactory(userPreferences)
                )
                NutritionGoalScreen(
                    viewModel = fitnessViewModel,
                    onGoalClick = { goal -> navController.push("nutrition_plan/${goal.name}") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("nutrition_plan/{goal}") { back ->
                val goalName = back.arguments?.getString("goal") ?: return@composable
                val goal = com.example.data.ClientGoal.valueOf(goalName)
                val fitnessViewModel: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = FitnessViewModelFactory(userPreferences)
                )
                NutritionScreen(
                    viewModel = fitnessViewModel,
                    goal   = goal,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("my_diet") {
                val fitnessViewModel: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = FitnessViewModelFactory(userPreferences)
                )
                MyDietScreen(
                    viewModel = fitnessViewModel,
                    onBack    = { navController.popBackStack() }
                )
            }
            composable("fitness_progress") {
                val fitnessViewModel: FitnessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = FitnessViewModelFactory(userPreferences)
                )
                ProgressScreen(
                    viewModel = fitnessViewModel,
                    onBack    = { navController.popBackStack() }
                )
            }
            composable("body_measurements") {
                val healthViewModel: HealthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = HealthViewModelFactory(userPreferences, context)
                )
                BodyMeasurementsScreen(viewModel = healthViewModel, onBack = { navController.popBackStack() })
            }
            composable("progress_photos") {
                val healthViewModel: HealthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = HealthViewModelFactory(userPreferences, context)
                )
                ProgressPhotosScreen(viewModel = healthViewModel, onBack = { navController.popBackStack() })
            }
            composable("cycle_tracker") {
                val healthViewModel: HealthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = HealthViewModelFactory(userPreferences, context)
                )
                CycleTrackerScreen(viewModel = healthViewModel, onBack = { navController.popBackStack() })
            }
            composable("food_scanner") {
                FoodScannerScreen(
                    onBack      = { navController.popBackStack() },
                    onOpenDiary = {
                        navController.navigate("food_diary") {
                            popUpTo("food_scanner") { inclusive = true }
                        }
                    }
                )
            }
            composable("food_diary") {
                val db = remember { AppDatabase.getInstance(context) }
                FoodDiaryScreen(
                    onBack          = { navController.popBackStack() },
                    userPreferences = userPreferences,
                    db              = db,
                    onAddFood       = { navController.push("food_scanner") },
                    onOpenInsights  = { navController.push("insights") }
                )
            }
            composable("insights") {
                val db = remember { AppDatabase.getInstance(context) }
                NutritionInsightsScreen(
                    onBack          = { navController.popBackStack() },
                    userPreferences = userPreferences,
                    db              = db
                )
            }
            composable("nutrition_coach") {
                if (entitlements.aiNutritionCoachUnlocked) {
                    NutritionCoachScreen(
                        onBack          = { navController.popBackStack() },
                        userPreferences = userPreferences
                    )
                } else {
                    MemberPremiumScreen(
                        userPreferences = userPreferences,
                        featureName = "AI Nutrition Coach",
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable("meal_planner") {
                if (entitlements.aiMealPlannerUnlocked) {
                    MealPlannerScreen(
                        onBack            = { navController.popBackStack() },
                        userPreferences   = userPreferences,
                        onViewGroceryList = { plan ->
                            sharedMealPlan = plan
                            navController.push("grocery_list")
                        }
                    )
                } else {
                    MemberPremiumScreen(
                        userPreferences = userPreferences,
                        featureName = "AI Meal Planner",
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable("grocery_list") {
                val plan = sharedMealPlan
                if (plan != null) {
                    GroceryListScreen(onBack = { navController.popBackStack() }, plan = plan)
                }
            }
            composable("health_connect") {
                HealthConnectScreen(onBack = { navController.popBackStack() }, context = context)
            }
            composable("awards") {
                AwardsScreen(
                    userPreferences = userPreferences,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(ClientScreen.MyBookings.route) {
                ClientDashboardScreen(viewModel = viewModel)
            }
            composable(ClientScreen.Messages.route) {
                val memberChatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    key = "member_chat", factory = ChatViewModelFactory(userPreferences)
                )
                val bookings by viewModel.myBookings.collectAsState()
                MemberMessagesScreen(
                    chatViewModel = memberChatVm,
                    bookings      = bookings,
                    onOpenChat    = { chatId, coachName ->
                        navController.push("member_chat_open/$chatId/${android.net.Uri.encode(coachName)}")
                    },
                    onStartChat   = { coachId, coachName ->
                        navController.push("member_chat_new/$coachId/${android.net.Uri.encode(coachName)}")
                    }
                )
            }
            composable("member_chat_open/{chatId}/{coachName}") { back ->
                val chatId    = back.arguments?.getString("chatId") ?: return@composable
                val coachName = android.net.Uri.decode(back.arguments?.getString("coachName") ?: "")
                val memberChatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    key = "member_chat", factory = ChatViewModelFactory(userPreferences)
                )
                ChatScreen(
                    viewModel  = memberChatVm,
                    chatId     = chatId,
                    otherName  = coachName,
                    otherPhone = "",
                    onBack     = { navController.popBackStack() }
                )
            }
            composable("member_chat_new/{coachId}/{coachName}") { back ->
                val coachId   = back.arguments?.getString("coachId") ?: return@composable
                val coachName = android.net.Uri.decode(back.arguments?.getString("coachName") ?: "")
                val memberChatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    key = "member_chat", factory = ChatViewModelFactory(userPreferences)
                )
                var chatId by remember { mutableStateOf("") }
                LaunchedEffect(coachId) {
                    memberChatVm.openOrCreateChatAsMember(coachId, coachName) { id -> chatId = id }
                }
                if (chatId.isNotEmpty()) {
                    ChatScreen(
                        viewModel  = memberChatVm,
                        chatId     = chatId,
                        otherName  = coachName,
                        otherPhone = "",
                        onBack     = { navController.popBackStack() }
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(CyberBgPrimary), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyberAccent)
                    }
                }
            }
            composable(ClientScreen.Profile.route) {
                ClientProfileScreen(
                    viewModel = viewModel,
                    onLogout = {
                        // Navigate only AFTER the session is torn down — navigating first
                        // let the relaunch see stale auth and bounce back into the app
                        viewModel.logout(context) { onNavigateToLogin() }
                    }
                )
            }
        }
    }
}

// ─── Maintenance Screen ───────────────────────────────────────────────────────

@Composable
fun MaintenanceScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🔧", fontSize = 56.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Under Maintenance",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = CyberTextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

// ─── Suspended Screen ─────────────────────────────────────────────────────────

@Composable
fun SuspendedScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🚫", fontSize = 56.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Account Suspended",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Your account has been suspended by the administrator. Please contact support for assistance.",
                color = CyberTextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

// ─── Force Update Screen ──────────────────────────────────────────────────────

@Composable
fun ForceUpdateScreen(message: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🚀", fontSize = 56.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Update Required",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = CyberTextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(28.dp))
            androidx.compose.material3.Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.aistudio.coachops.abxyzm"))
                    context.startActivity(intent)
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = CyberAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update on Play Store", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

// ─── Update gate & nudge (version-count based) ────────────────────────────────

private fun openPlayStore(context: android.content.Context) {
    val id = "com.aistudio.coachops.abxyzm"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")))
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$id")))
    }
}

/**
 * Compulsory update — a non-dismissable sheet anchored to the bottom, shown when
 * a user is too many versions behind. Replaces the app until they update.
 */
@Composable
fun UpdateGateSheet(message: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgPrimary.copy(alpha = 0.97f))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(CyberBgCard)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🚀", fontSize = 44.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                "Time to update ProCoach India",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center, lineHeight = 26.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                message.ifBlank { "You're several versions behind. Update now for the best experience and the latest features." },
                color = CyberTextMuted, fontSize = 14.sp,
                textAlign = TextAlign.Center, lineHeight = 22.sp
            )
            Spacer(Modifier.height(22.dp))
            androidx.compose.material3.Button(
                onClick = { openPlayStore(context) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CyberAccent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Now", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

/**
 * Soft update nudge — a dismissible banner shown to users who are behind but
 * still within the compulsory threshold.
 */
@Composable
fun UpdateNudgeBanner(message: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberAccent.copy(alpha = 0.14f))
            .clickable { openPlayStore(context) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message.ifBlank { "A new version is available — tap to update for the best experience." },
            color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = 17.sp, modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(CyberAccent)
                .clickable { openPlayStore(context) }
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text("Update", color = Color(0xFF1A1A1A), fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Text(
            "✕", color = CyberTextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// ─── Announcement Banner ──────────────────────────────────────────────────────

@Composable
fun AnnouncementBanner(appControl: AppControlState) {
    val (bgColor, textColor) = when (appControl.announcementType) {
        "warning" -> Color(0xFFF59E0B) to Color(0xFF1A1A1A)
        "success" -> Color(0xFF10B981) to Color(0xFF1A1A1A)
        else      -> CyberAccent.copy(alpha = 0.85f) to Color.White
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = appControl.announcementText,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ─── Custom bottom navigation bar ────────────────────────────────────────────

@Composable
fun AppBottomNav(
    items: List<Triple<String, String, ImageVector>>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    badges: Map<String, Int> = emptyMap()
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    // Compact layout when 6+ items OR narrow screen (< 390dp covers most budget/small phones)
    val compact = items.size >= 6 || screenWidthDp < 390

    val outerPadH = if (compact) 8.dp  else 16.dp
    val outerPadV = if (compact) 8.dp  else 10.dp
    val innerPadH = if (compact) 4.dp  else 8.dp
    val innerPadV = if (compact) 4.dp  else 6.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = outerPadH, vertical = outerPadV)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(CyberBgCard)
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(999.dp))
                .padding(horizontal = innerPadH, vertical = innerPadV),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (route, title, icon) ->
                NavTabItem(
                    icon       = icon,
                    label      = title,
                    selected   = currentRoute == route,
                    badgeCount = badges[route] ?: 0,
                    onClick    = { onNavigate(route) },
                    modifier   = Modifier.weight(1f),
                    compact    = compact
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Scale dimensions based on compact flag
    val pillHeight   = if (compact) 30.dp  else 36.dp
    val boxHeight    = if (compact) 34.dp  else 40.dp
    val pillSelW     = if (compact) 40.dp  else 52.dp
    val pillUnselW   = if (compact) 28.dp  else 38.dp
    val iconSize     = if (compact) 17.dp  else 20.dp
    val labelSize    = if (compact) 9.sp   else 10.sp
    val badgeSize    = if (compact) 15.dp  else 18.dp
    val badgeFont    = if (compact) 8.sp   else 9.sp

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = if (compact) 2.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 3.dp)
    ) {
        // Icon pill + badge
        Box(
            modifier = Modifier
                .height(boxHeight)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(pillHeight)
                    .width(if (selected) pillSelW else pillUnselW)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) CyberAccent else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) CyberAccentDark else CyberTextMuted,
                    modifier = Modifier.size(iconSize)
                )
            }
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(badgeSize)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .border(1.5.dp, Color.Black.copy(0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (badgeCount > 9) "9+" else "$badgeCount",
                        fontSize = badgeFont,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = badgeFont
                    )
                }
            }
        }
        // Label — always single line, ellipsis if it still can't fit
        Text(
            text     = label,
            fontSize = labelSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color    = if (selected) CyberAccent else CyberTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

