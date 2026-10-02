package uz.hangulfriend.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.vocab.VocabScreen
import uz.hangulfriend.ui.vocab.VocabViewModel
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import uz.hangulfriend.ui.game.AchievementsScreen
import uz.hangulfriend.ui.game.AchievementsViewModel
import uz.hangulfriend.ui.game.GameId
import uz.hangulfriend.ui.game.GameScreen
import uz.hangulfriend.ui.game.GamesScreen
import uz.hangulfriend.ui.game.GamesViewModel
import uz.hangulfriend.ui.game.MistakesScreen
import uz.hangulfriend.ui.game.MistakesViewModel
import uz.hangulfriend.ui.home.HomeScreen
import uz.hangulfriend.ui.home.HomeViewModel
import uz.hangulfriend.ui.home.ShareCardButton
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
import uz.hangulfriend.ui.story.EpisodeScreen
import uz.hangulfriend.ui.story.EpisodeViewModel
import uz.hangulfriend.ui.story.StoryListScreen
import uz.hangulfriend.ui.story.StoryListViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val MAP = "map"
    const val SETTINGS = "settings"
    const val GAMES = "games"
    const val GAME = "game/{id}"
    const val ACHIEVEMENTS = "achievements"
    const val MISTAKES = "mistakes"
    const val STORIES = "stories"
    const val VOCAB = "vocab"
    const val EPISODE = "story/{lessonId}"
    const val LESSON = "lesson/{lessonId}"
    const val SESSION = "session/{mode}?lessonId={lessonId}"

    fun lesson(id: String) = "lesson/$id"

    fun episode(id: String) = "story/$id"

    fun session(mode: SessionMode, lessonId: String? = null) =
        if (lessonId == null) "session/${mode.route}" else "session/${mode.route}?lessonId=$lessonId"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    Tab(Routes.MAP, R.string.nav_book, Icons.Filled.Map),
    Tab(Routes.STORIES, R.string.nav_stories, Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
fun HangulFriendNav(container: AppContainer, openReview: Boolean = false) {
    val settings: Settings? by container.settings.settings.collectAsState(initial = null)
    val loaded = settings ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    // A tapped reminder opens the review session once, on top of Home.
    LaunchedEffect(Unit) {
        if (openReview && loaded.onboarded) nav.navigate(Routes.session(SessionMode.REVIEW))
    }
    val route = backStack?.destination?.route
    Scaffold(
        containerColor = Color.Transparent,
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
                val vm = viewModel {
                    HomeViewModel(
                        container.content, container.study, container.settings, container.game, container.progress,
                        container.story, container.shareStats, container.clock,
                    )
                }
                HomeScreen(
                    vm,
                    onStartReview = { nav.navigate(Routes.session(SessionMode.REVIEW)) },
                    onContinueLesson = { nav.navigate(Routes.lesson(it)) },
                    onOpenMap = {
                        nav.navigate(Routes.MAP) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGames = { nav.navigate(Routes.GAMES) },
                    onMistakes = { nav.navigate(Routes.MISTAKES) },
                    onAchievements = { nav.navigate(Routes.ACHIEVEMENTS) },
                    onVocab = { nav.navigate(Routes.VOCAB) },
                    headerAction = { ShareCardButton(container.shareStats) },
                )
            }
            composable(Routes.MAP) {
                val vm = viewModel { BookMapViewModel(container.content, container.progress, container.settings, container.game) }
                BookMapScreen(
                    vm,
                    onOpenLesson = { nav.navigate(Routes.lesson(it)) },
                    onQuickCheck = { nav.navigate(Routes.session(SessionMode.QUICK_CHECK, it)) },
                    onBoss = { nav.navigate(Routes.session(SessionMode.BOSS, it.toString())) },
                    onFinal = { nav.navigate(Routes.session(SessionMode.FINAL)) },
                )
            }
            composable(Routes.SETTINGS) {
                val vm = viewModel { SettingsViewModel(container.content, container.settings, container.onboarding, container.backup, container.flags) }
                SettingsScreen(vm)
            }
            composable(Routes.GAMES) { entry ->
                val vm = viewModel(entry) { GamesViewModel(container.content, container.game, container.settings) }
                GamesScreen(vm, onOpen = { nav.navigate("game/${it.route}") })
            }
            composable(Routes.GAME) { entry ->
                val id = GameId.fromRoute(entry.arguments?.getString("id").orEmpty())
                val vm = viewModel { GamesViewModel(container.content, container.game, container.settings) }
                GameScreen(vm, id)
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen(viewModel { AchievementsViewModel(container.game) })
            }
            composable(Routes.MISTAKES) {
                val vm = viewModel { MistakesViewModel(container.lessons, container.game) }
                MistakesScreen(vm, onPractice = { nav.navigate(Routes.session(SessionMode.MISTAKES)) })
            }
            composable(Routes.VOCAB) {
                val vm = viewModel { VocabViewModel(container.content, container.userWords, container.db) }
                VocabScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(Routes.STORIES) {
                val vm = viewModel { StoryListViewModel(container.content, container.progress, container.story) }
                StoryListScreen(vm, onOpen = { nav.navigate(Routes.episode(it)) })
            }
            composable(Routes.EPISODE) { entry ->
                val lessonId = entry.arguments?.getString("lessonId").orEmpty()
                val vm = viewModel { EpisodeViewModel(lessonId, container.content, container.story, container.settings, container.game) }
                EpisodeScreen(vm, onClose = { nav.popBackStack() })
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
                            container.settings, container.sessionBuilder, container.grader, container.speechAvailable, container.game,
                            container.lessons::lesson,
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
    val t = LocalGameTokens.current
    NavigationBar(containerColor = t.background, contentColor = t.muted) {
        tabs.forEach { tab ->
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = t.accent,
                    selectedTextColor = t.accent,
                    indicatorColor = t.accent.copy(alpha = 0.15f),
                    unselectedIconColor = t.muted,
                    unselectedTextColor = t.muted,
                ),
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
