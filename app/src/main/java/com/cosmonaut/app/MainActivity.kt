package com.cosmonaut.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cosmonaut.app.analytics.CosmoAnalytics
import com.cosmonaut.app.analytics.TrackScreenViews
import com.cosmonaut.app.auth.AuthManager
import com.cosmonaut.app.auth.AuthState
import com.cosmonaut.app.data.local.CosmoPreferences
import com.cosmonaut.app.data.repository.UserRepository
import com.cosmonaut.app.navigation.BottomNavItem
import com.cosmonaut.app.navigation.CosmoNavHost
import com.cosmonaut.app.navigation.CosmoRoute
import com.cosmonaut.app.ui.screens.audio.AudioNarrationViewModel
import com.cosmonaut.app.ui.screens.audio.ExpandedPlayer
import com.cosmonaut.app.ui.screens.audio.MiniPlayer
import com.cosmonaut.app.ui.theme.CosmoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

enum class DeepLinkTarget {
    WORLD,
    SESSION,
}

data class DeepLinkData(
    val target: DeepLinkTarget,
    val id: String,
    val invite: String? = null,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authManager: AuthManager

    @Inject lateinit var preferences: CosmoPreferences

    @Inject lateinit var userRepository: UserRepository

    @Inject lateinit var analytics: CosmoAnalytics

    private val _pendingDeepLink = MutableStateFlow<DeepLinkData?>(null)
    val pendingDeepLink: StateFlow<DeepLinkData?> = _pendingDeepLink.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            authManager.authState.value is AuthState.Unknown ||
                authManager.authState.value is AuthState.Loading
        }

        handleDeepLink(intent)

        setContent {
            CosmoTheme {
                CosmoAppContent(
                    authManager = authManager,
                    preferences = preferences,
                    userRepository = userRepository,
                    pendingDeepLink = _pendingDeepLink,
                    analytics = analytics,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val deepLink = parseCosmonautDeepLink(uri) ?: return
        Timber.d("Deep link received: target=${deepLink.target}, id=${deepLink.id}, invite=${deepLink.invite}")
        _pendingDeepLink.value = deepLink
    }

    fun consumeDeepLink() {
        _pendingDeepLink.value = null
    }
}

private fun parseCosmonautDeepLink(uri: Uri): DeepLinkData? {
    val pathSegments = uri.pathSegments ?: return null
    if (pathSegments.size < 2) return null
    val id = pathSegments[1]
    if (id.isBlank()) return null
    val invite = uri.getQueryParameter("invite")
    return when (pathSegments[0]) {
        "worlds" -> DeepLinkData(target = DeepLinkTarget.WORLD, id = id, invite = invite)
        "sessions" -> DeepLinkData(target = DeepLinkTarget.SESSION, id = id)
        else -> null
    }
}

@Composable
private fun CosmoAppContent(
    authManager: AuthManager,
    preferences: CosmoPreferences,
    userRepository: UserRepository,
    pendingDeepLink: MutableStateFlow<DeepLinkData?>,
    analytics: CosmoAnalytics,
) {
    val authState by authManager.authState.collectAsState()
    val hasSeenCarousel by preferences.hasSeenCarousel.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        authManager.initialize()
    }

    val isReady = authState !is AuthState.Unknown &&
        authState !is AuthState.Loading &&
        hasSeenCarousel != null

    if (!isReady) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = CosmoTheme.colors.primary)
        }
        return
    }

    when (authState) {
        is AuthState.Unknown, is AuthState.Loading -> { /* handled above */ }

        is AuthState.Unauthenticated -> {
            val startDestination: CosmoRoute = if (!hasSeenCarousel!!) {
                CosmoRoute.OnboardingCarousel
            } else {
                CosmoRoute.Login
            }

            UnauthenticatedShell(
                startDestination = startDestination,
                onCarouselSeen = { scope.launch { preferences.setCarouselSeen(true) } },
            )
        }

        is AuthState.Authenticated -> {
            AuthenticatedShell(
                pendingDeepLink = pendingDeepLink,
                analytics = analytics,
                userRepository = userRepository,
                preferences = preferences,
            )
        }
    }
}

@Composable
private fun UnauthenticatedShell(startDestination: CosmoRoute, onCarouselSeen: () -> Unit,) {
    val navController = rememberNavController()

    LaunchedEffect(startDestination) {
        if (startDestination is CosmoRoute.Login) {
            onCarouselSeen()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CosmoTheme.colors.background,
    ) { innerPadding ->
        CosmoNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun AuthenticatedShell(
    pendingDeepLink: MutableStateFlow<DeepLinkData?>,
    analytics: CosmoAnalytics,
    userRepository: UserRepository,
    preferences: CosmoPreferences,
) {
    val navController = rememberNavController()
    TrackScreenViews(navController = navController, analytics = analytics)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val audioViewModel: AudioNarrationViewModel = hiltViewModel()
    val deepLink by pendingDeepLink.collectAsState()

    LaunchedEffect(Unit) {
        try {
            val profile = userRepository.fetchFresh()
            preferences.setOnboardingCompleted(profile.isOnboarded)
            if (!profile.isOnboarded) {
                navController.navigate(CosmoRoute.Onboarding) {
                    popUpTo(CosmoRoute.Home) { inclusive = true }
                }
            }
        } catch (_: Exception) {
            Timber.d("Failed to fetch user profile for onboarding check")
        }
    }

    LaunchedEffect(deepLink) {
        val link = deepLink ?: return@LaunchedEffect
        when (link.target) {
            DeepLinkTarget.WORLD -> navController.navigate(CosmoRoute.WorldHome(link.id, link.invite))
            DeepLinkTarget.SESSION -> navController.navigate(CosmoRoute.SessionHome(link.id))
        }
        pendingDeepLink.value = null
    }

    val playbackState by audioViewModel.playbackState.collectAsState()
    val trackInfo by audioViewModel.trackInfo.collectAsState()
    val isPlayerVisible by audioViewModel.isPlayerVisible.collectAsState()
    val isGenerating by audioViewModel.isGenerating.collectAsState()
    var showExpandedPlayer by remember { mutableStateOf(false) }
    val voices by audioViewModel.voices.collectAsState()
    val selectedVoiceId by audioViewModel.selectedVoiceId.collectAsState()

    val currentNavItem = BottomNavItem.entries.find { item ->
        currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
    }

    val showBottomBar = currentDestination?.let { dest ->
        !dest.hasRoute(CosmoRoute.StoryNode::class) &&
            !dest.hasRoute(CosmoRoute.StoryMap::class) &&
            !dest.hasRoute(CosmoRoute.Feedback::class) &&
            !dest.hasRoute(CosmoRoute.Onboarding::class)
    } ?: true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CosmoTheme.colors.background,
        bottomBar = {
            Column {
                MiniPlayer(
                    isVisible = isPlayerVisible,
                    playbackState = playbackState,
                    trackInfo = trackInfo,
                    isGenerating = isGenerating,
                    onTogglePlayPause = audioViewModel::togglePlayPause,
                    onSkipBack = audioViewModel::skipBack,
                    onSkipForward = audioViewModel::skipForward,
                    onClose = audioViewModel::closePlayer,
                    onExpand = { showExpandedPlayer = true },
                )
                if (showBottomBar) {
                    CosmoBottomBar(
                        currentNavItem = currentNavItem,
                        onItemSelected = { item ->
                            if (item == BottomNavItem.HOME) {
                                if (currentNavItem == BottomNavItem.HOME) return@CosmoBottomBar
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        CosmoNavHost(
            navController = navController,
            startDestination = CosmoRoute.Home,
            modifier = Modifier.padding(innerPadding),
            audioViewModel = audioViewModel,
        )
    }

    if (showExpandedPlayer) {
        ExpandedPlayer(
            playbackState = playbackState,
            trackInfo = trackInfo,
            voices = voices,
            selectedVoiceId = selectedVoiceId,
            isGenerating = isGenerating,
            onTogglePlayPause = audioViewModel::togglePlayPause,
            onSeek = audioViewModel::seekToFraction,
            onVolumeChange = audioViewModel::setVolume,
            onSpeedChange = audioViewModel::setPlaybackSpeed,
            onVoiceSelect = audioViewModel::selectVoice,
            onPauseMainAudio = audioViewModel::pauseForSample,
            onResumeMainAudio = audioViewModel::resumeAfterSample,
            onClose = { showExpandedPlayer = false },
            onDismiss = { showExpandedPlayer = false },
        )
    }
}

@Composable
private fun CosmoBottomBar(
    currentNavItem: BottomNavItem?,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = CosmoTheme.colors.card,
        contentColor = CosmoTheme.colors.cardForeground,
    ) {
        BottomNavItem.entries.forEach { item ->
            val selected = item == currentNavItem
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CosmoTheme.colors.primary,
                    selectedTextColor = CosmoTheme.colors.primary,
                    unselectedIconColor = CosmoTheme.colors.mutedForeground,
                    unselectedTextColor = CosmoTheme.colors.mutedForeground,
                    indicatorColor = CosmoTheme.colors.primary.copy(alpha = 0.12f),
                ),
            )
        }
    }
}
