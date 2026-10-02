package com.planecatcher.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.planecatcher.ui.collection.CollectionScreen
import com.planecatcher.ui.collection.PlaneDetailScreen
import com.planecatcher.ui.home.HomeScreen
import com.planecatcher.ui.onboarding.OnboardingScreen
import com.planecatcher.ui.quiz.QuizScreen
import com.planecatcher.ui.settings.SettingsScreen

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val COLLECTION = "collection"
    const val SETTINGS = "settings"
    const val QUIZ = "quiz/{hex}"
    const val DETAIL = "detail/{hex}"
    fun quiz(hex: String) = "quiz/$hex"
    fun detail(hex: String) = "detail/$hex"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Radar", Icons.Filled.Radar),
    Tab(Routes.COLLECTION, "Collection", Icons.Filled.Collections),
    Tab(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun AppNav(
    onboardingDone: Boolean,
    onOnboardingFinished: () -> Unit,
    requestedHex: String?,
    onRequestHandled: () -> Unit,
) {
    val nav = rememberNavController()
    // Fixed on first composition so finishing onboarding doesn't rebuild the graph.
    val startDestination = remember { if (onboardingDone) Routes.HOME else Routes.ONBOARDING }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }

    // A notification tap brings the user back to the radar, which shows that plane.
    LaunchedEffect(requestedHex) {
        if (requestedHex != null && onboardingDone && route != Routes.HOME) nav.switchTab(Routes.HOME)
    }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onFinished = {
                    onOnboardingFinished()
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    requestedHex = requestedHex,
                    onRequestHandled = onRequestHandled,
                    onStartCatch = { hex -> nav.navigate(Routes.quiz(hex)) },
                )
            }
            composable(Routes.COLLECTION) {
                CollectionScreen(onOpen = { hex -> nav.navigate(Routes.detail(hex)) })
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.QUIZ, arguments = listOf(navArgument("hex") { type = NavType.StringType })) {
                QuizScreen(
                    onDone = { nav.popBackStack() },
                    onOpenCollection = { hex ->
                        nav.switchTab(Routes.COLLECTION)
                        nav.navigate(Routes.detail(hex))
                    },
                )
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("hex") { type = NavType.StringType })) {
                PlaneDetailScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
