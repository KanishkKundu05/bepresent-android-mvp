package com.bepresent.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bepresent.android.data.analytics.AnalyticsEvents
import com.bepresent.android.data.analytics.AnalyticsManager
import com.bepresent.android.data.datastore.PreferencesManager
import com.bepresent.android.ui.dev.DevScreen
import com.bepresent.android.ui.homev2.HomeV2Screen
import com.bepresent.android.ui.homev2.HomeV2Tokens
import com.bepresent.android.ui.homev2.HomeV2ViewModel
import com.bepresent.android.ui.leaderboard.LeaderboardScreen
import com.bepresent.android.ui.onboarding.v2.OnboardingV2Screen
import com.bepresent.android.ui.partner.PartnerScreen
import com.bepresent.android.ui.profile.ProfileScreen
import com.bepresent.android.ui.schedules.SchedulesScreen
import com.bepresent.android.ui.schedules.SchedulesViewModel
import com.bepresent.android.ui.screentime.ScreenTimeScreen
import com.bepresent.android.ui.screentime.ScreenTimeViewModel
import com.bepresent.android.ui.social.SocialScreen
import com.bepresent.android.ui.social.SocialViewModel
import com.bepresent.android.ui.theme.BePresentTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Tab definitions matching iOS tab order
enum class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val analyticsEvent: String
) {
    Home("home", "Home", Icons.Default.Home, AnalyticsEvents.CLICKED_HOME),
    Schedules("schedules", "Schedules", Icons.Default.CalendarMonth, AnalyticsEvents.CLICKED_SCHEDULES),
    LeaderboardTab("leaderboard", "Leaderboard", Icons.Default.Leaderboard, AnalyticsEvents.CLICKED_LEADERBOARD),
    ScreenTime("screentime", "Screen Time", Icons.Default.PhoneAndroid, AnalyticsEvents.CLICKED_SCREEN_TIME),
    Social("social", "Social", Icons.Default.Groups, AnalyticsEvents.CLICKED_SOCIAL)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash visible until navigation destination is resolved
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Track app foreground/background via ProcessLifecycleOwner
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                analyticsManager.track(AnalyticsEvents.APPLICATION_FOREGROUNDED)
            }

            override fun onStop(owner: LifecycleOwner) {
                analyticsManager.track(AnalyticsEvents.APPLICATION_BACKGROUNDED)
                analyticsManager.flush()
            }
        })

        setContent {
            BePresentTheme {
                MainAppContent(analyticsManager, preferencesManager, onReady = { isReady = true })
            }
        }
    }
}

@Composable
private fun MainAppContent(analyticsManager: AnalyticsManager, preferencesManager: PreferencesManager, onReady: () -> Unit) {
    // Check onboarding status before rendering NavHost to avoid flash
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val completed = preferencesManager.onboardingCompleted.first()
        startDestination = if (completed) BottomTab.Home.route else "onboarding"
        onReady()
    }
    val resolvedStart = startDestination ?: return // Don't render until resolved

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine if we should show bottom nav (hide on detail screens)
    val bottomTabs = BottomTab.entries
    val showBottomBar = currentRoute == null && bottomTabs.any { it.route == resolvedStart }
        || bottomTabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = currentRoute == tab.route,
                            onClick = {
                                analyticsManager.track(tab.analyticsEvent)
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HomeV2Tokens.BrandPrimary,
                                selectedTextColor = HomeV2Tokens.BrandPrimary,
                                indicatorColor = HomeV2Tokens.Brand100
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = resolvedStart,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            // Home tab — new HomeV2
            composable(BottomTab.Home.route) {
                val viewModel: HomeV2ViewModel = hiltViewModel()
                Box(modifier = Modifier.padding(innerPadding)) {
                    HomeV2Screen(
                        viewModel = viewModel,
                        onLeaderboardClick = { navController.navigate(BottomTab.LeaderboardTab.route) },
                        onDevClick = { navController.navigate("dev") }
                    )
                }
            }

            // Schedules tab
            composable(BottomTab.Schedules.route) {
                val viewModel: SchedulesViewModel = hiltViewModel()
                Box(modifier = Modifier.padding(innerPadding)) {
                    SchedulesScreen(viewModel = viewModel)
                }
            }

            // Leaderboard tab
            composable(BottomTab.LeaderboardTab.route) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    LeaderboardScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Screen Time tab
            composable(BottomTab.ScreenTime.route) {
                val viewModel: ScreenTimeViewModel = hiltViewModel()
                Box(modifier = Modifier.padding(innerPadding)) {
                    ScreenTimeScreen(viewModel = viewModel)
                }
            }

            // Social tab — accountability partners
            composable(BottomTab.Social.route) {
                val viewModel: SocialViewModel = hiltViewModel()
                Box(modifier = Modifier.padding(innerPadding)) {
                    SocialScreen(viewModel = viewModel)
                }
            }

            // Detail screens (no bottom bar)
            composable("dev") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    DevScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToOnboarding = { navController.navigate("onboarding") }
                    )
                }
            }
            // Onboarding: no innerPadding so gradient extends behind status bar
            composable("onboarding") {
                OnboardingV2Screen(
                    onComplete = {
                        navController.navigate(BottomTab.Home.route) {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable("profile") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onPartnerClick = { partnerId ->
                            navController.navigate("partner/$partnerId")
                        }
                    )
                }
            }
            composable(
                "partner/{partnerId}",
                arguments = listOf(navArgument("partnerId") { type = NavType.StringType })
            ) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    PartnerScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderTab(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Gray
        )
    }
}
