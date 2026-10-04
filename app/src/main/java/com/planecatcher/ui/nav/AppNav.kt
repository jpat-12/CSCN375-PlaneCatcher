package com.planecatcher.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.planecatcher.ui.home.CameraScreen
import com.planecatcher.ui.home.MapScreen
import com.planecatcher.ui.onboarding.OnboardingScreen
import com.planecatcher.ui.onboarding.OnboardingViewModel
import com.planecatcher.ui.onboarding.TutorialCatch
import com.planecatcher.ui.profile.ProfileScreen
import com.planecatcher.ui.quiz.QuizScreen
import com.planecatcher.ui.theme.InkDark
import com.planecatcher.ui.theme.PillBlue

private object Routes {
    const val ONBOARDING = "onboarding"
    const val COLLECTION = "collection"
    const val CAMERA = "camera"
    const val MAP = "map"
    const val PROFILE = "profile"
    const val TUTORIAL = "tutorial"
    const val QUIZ = "quiz/{hex}"
    const val DETAIL = "detail/{hex}"
    fun quiz(hex: String) = "quiz/$hex"
    fun detail(hex: String) = "detail/$hex"
}

private data class Tab(val route: String, val label: String)

/** The three tabs of the mockup's top pill switcher. */
private val tabs = listOf(
    Tab(Routes.COLLECTION, "Collection"),
    Tab(Routes.CAMERA, "Camera"),
    Tab(Routes.MAP, "Map"),
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
    val startDestination = remember { if (onboardingDone) Routes.CAMERA else Routes.ONBOARDING }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onTab = tabs.any { it.route == route }

    // A notification tap opens the Camera tab, which shows that plane.
    LaunchedEffect(requestedHex) {
        if (requestedHex != null && onboardingDone && route != Routes.CAMERA && route != Routes.MAP) nav.switchTab(Routes.CAMERA)
    }

    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = startDestination, modifier = Modifier.fillMaxSize()) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onFinished = {
                        onOnboardingFinished()
                        nav.navigate(Routes.CAMERA) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    })
                }
                composable(Routes.COLLECTION) {
                    CollectionScreen(onOpen = { hex -> nav.navigate(Routes.detail(hex)) })
                }
                composable(Routes.CAMERA) {
                    CameraScreen(
                        requestedHex = requestedHex,
                        onRequestHandled = onRequestHandled,
                        onStartCatch = { hex -> nav.navigate(Routes.quiz(hex)) },
                    )
                }
                composable(Routes.MAP) {
                    MapScreen(
                        requestedHex = requestedHex,
                        onRequestHandled = onRequestHandled,
                        onStartCatch = { hex -> nav.navigate(Routes.quiz(hex)) },
                    )
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(onBack = { nav.popBackStack() }, onReplayTutorial = { nav.navigate(Routes.TUTORIAL) })
                }
                composable(Routes.TUTORIAL) {
                    val vm: OnboardingViewModel = hiltViewModel()
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                        TutorialCatch(onPlay = vm::play, onDone = { nav.popBackStack() })
                    }
                }
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

            if (onTab) {
                Row(
                    Modifier.align(Alignment.TopCenter).padding(top = 10.dp, start = 12.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PillSwitcher(selected = route, onSelect = { nav.switchTab(it) })
                    // Profile button (the mockup's person-and-gear icon): progress and settings.
                    Box(
                        Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { nav.navigate(Routes.PROFILE) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.ManageAccounts, contentDescription = "Profile and settings", tint = InkDark)
                    }
                }
            }
        }
    }
}

/** White pill with the selected tab in light blue, as in the mockup. */
@Composable
private fun PillSwitcher(selected: String?, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .shadow(4.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(4.dp),
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.route == selected
            Text(
                tab.label,
                style = MaterialTheme.typography.titleSmall,
                color = InkDark,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) PillBlue else Color.Transparent)
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    }
                    .clickable { onSelect(tab.route) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
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
