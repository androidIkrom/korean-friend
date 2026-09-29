package uz.hangulfriend.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uz.hangulfriend.AppContainer
import uz.hangulfriend.R
import uz.hangulfriend.data.Settings
import uz.hangulfriend.ui.home.HomeScreen
import uz.hangulfriend.ui.home.HomeViewModel
import uz.hangulfriend.ui.map.BookMapScreen
import uz.hangulfriend.ui.map.BookMapViewModel
import uz.hangulfriend.ui.onboarding.OnboardingScreen
import uz.hangulfriend.ui.onboarding.OnboardingViewModel
import uz.hangulfriend.ui.settings.SettingsScreen
import uz.hangulfriend.ui.lesson.LessonController
import uz.hangulfriend.ui.lesson.LessonScreen
import uz.hangulfriend.ui.lesson.LessonViewModel
import uz.hangulfriend.ui.session.SessionController
import uz.hangulfriend.ui.session.SessionMode
import uz.hangulfriend.ui.session.SessionScreen
import uz.hangulfriend.ui.session.SessionViewModel
import uz.hangulfriend.ui.settings.SettingsViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val MAP = "map"
    const val SETTINGS = "settings"
    const val LESSON = "lesson/{lessonId}"
    const val SESSION = "session/{mode}?lessonId={lessonId}"

    fun lesson(id: String) = "lesson/$id"

    fun session(mode: SessionMode, lessonId: String? = null) =
        if (lessonId == null) "session/${mode.route}" else "session/${mode.route}?lessonId=$lessonId"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Filled.Today),
    Tab(Routes.MAP, R.string.nav_book, Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
fun HangulFriendNav(container: AppContainer) {
    val settings: Settings? by container.settings.settings.collectAsState(initial = null)
    val loaded = settings ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == route }) BottomBar(nav, route)
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (loaded.onboarded) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                val vm = viewModel { OnboardingViewModel(container.content, container.onboarding) }
                OnboardingScreen(vm) {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            }
            composable(Routes.HOME) {
                val vm = viewModel { HomeViewModel(container.content, container.study, container.settings) }
                HomeScreen(
                    vm,
                    onStartReview = { nav.navigate(Routes.session(SessionMode.REVIEW)) },
                    onContinueLesson = { nav.navigate(Routes.lesson(it)) },
                )
            }
            composable(Routes.MAP) {
                val vm = viewModel { BookMapViewModel(container.content, container.progress) }
                BookMapScreen(vm, onOpenLesson = { nav.navigate(Routes.lesson(it)) })
            }
            composable(Routes.SETTINGS) {
                val vm = viewModel { SettingsViewModel(container.content, container.settings, container.onboarding) }
                SettingsScreen(vm)
            }
            composable(Routes.LESSON) { entry ->
                val lessonId = entry.arguments?.getString("lessonId").orEmpty()
                val vm = viewModel {
                    LessonViewModel(
                        LessonController(lessonId, container.content, container.study, container.progress),
                        container.sessionBuilder,
                        container.grader,
                    )
                }
                LessonScreen(
                    vm,
                    onBack = { nav.popBackStack() },
                    onStartPractice = { nav.navigate(Routes.session(SessionMode.PRACTICE, it)) },
                    onStartTest = { nav.navigate(Routes.session(SessionMode.TEST, it)) },
                    onStartLessonReview = { nav.navigate(Routes.session(SessionMode.LESSON_REVIEW, it)) },
                )
            }
            composable(
                Routes.SESSION,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("lessonId") { type = NavType.StringType; nullable = true },
                ),
            ) { entry ->
                val mode = SessionMode.fromRoute(entry.arguments?.getString("mode").orEmpty())
                val lessonId = entry.arguments?.getString("lessonId")
                val vm = viewModel {
                    SessionViewModel(
                        SessionController(
                            mode, lessonId, container.content, container.study, container.progress,
                            container.settings, container.sessionBuilder, container.grader, container.speechAvailable,
                        ),
                    )
                }
                SessionScreen(vm, mode, onClose = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, current: String?) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

@Composable
private fun Placeholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.placeholder_screen)) }
}
