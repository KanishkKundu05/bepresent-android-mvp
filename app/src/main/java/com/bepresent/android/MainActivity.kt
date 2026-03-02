package com.bepresent.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val icon: ImageVector
) {
    Home("home", "Home", Icons.Default.Home),
    Schedules("schedules", "Schedules", Icons.Default.CalendarMonth),
    LeaderboardTab("leaderboard", "Leaderboard", Icons.Default.Leaderboard),
    ScreenTime("screentime", "Screen Time", Icons.Default.PhoneAndroid),
    Social("social", "Social", Icons.Default.Groups)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BePresentTheme {
                // DEV: skip onboarding, go straight to home
                MainAppContent()
            }
        }
    }
}

@Composable
private fun MainAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine if we should show bottom nav (hide on detail screens)
    val bottomTabs = BottomTab.entries
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

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
            startDestination = BottomTab.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            // Home tab — new HomeV2
            composable(BottomTab.Home.route) {
                val viewModel: HomeV2ViewModel = hiltViewModel()
                HomeV2Screen(
                    viewModel = viewModel,
                    onLeaderboardClick = { navController.navigate(BottomTab.LeaderboardTab.route) },
                    onDevClick = { navController.navigate("dev") }
                )
            }

            // Schedules tab
            composable(BottomTab.Schedules.route) {
                val viewModel: SchedulesViewModel = hiltViewModel()
                SchedulesScreen(viewModel = viewModel)
            }

            // Leaderboard tab
            composable(BottomTab.LeaderboardTab.route) {
                LeaderboardScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Screen Time tab
            composable(BottomTab.ScreenTime.route) {
                val viewModel: ScreenTimeViewModel = hiltViewModel()
                ScreenTimeScreen(viewModel = viewModel)
            }

            // Social tab — accountability partners
            composable(BottomTab.Social.route) {
                val viewModel: SocialViewModel = hiltViewModel()
                SocialScreen(viewModel = viewModel)
            }

            // Detail screens (no bottom bar)
            composable("dev") {
                DevScreen(onBack = { navController.popBackStack() })
            }
            composable("profile") {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onPartnerClick = { partnerId ->
                        navController.navigate("partner/$partnerId")
                    }
                )
            }
            composable(
                "partner/{partnerId}",
                arguments = listOf(navArgument("partnerId") { type = NavType.StringType })
            ) {
                PartnerScreen(
                    onBack = { navController.popBackStack() }
                )
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
