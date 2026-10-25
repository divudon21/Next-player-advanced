package dev.anilbeesetti.nextplayer

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.common.service.system.SystemService
import dev.anilbeesetti.nextplayer.core.media.services.MediaOperationsService
import dev.anilbeesetti.nextplayer.core.model.ThemeConfig
import dev.anilbeesetti.nextplayer.core.ui.components.LocalNavigationBottomPadding
import dev.anilbeesetti.nextplayer.core.ui.components.LocalTopLevelBottomBarVisibleSetter
import dev.anilbeesetti.nextplayer.core.ui.components.LocalTopLevelFabSetter
import dev.anilbeesetti.nextplayer.core.ui.components.TopLevelFabState
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.MediaPickerRoute
import dev.anilbeesetti.nextplayer.navigation.NextNavigationBar
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.backdrops.layerBackdrop
import dev.anilbeesetti.nextplayer.navigation.convx.backdrop.backdrops.rememberLayerBackdrop
import dev.anilbeesetti.nextplayer.navigation.convx.floatingtabbar.FloatingTabBarInlineBehavior
import dev.anilbeesetti.nextplayer.navigation.convx.floatingtabbar.rememberFloatingTabBarScrollConnection
import dev.anilbeesetti.nextplayer.navigation.NextNavigationRail
import dev.anilbeesetti.nextplayer.navigation.TopLevelDestination
import dev.anilbeesetti.nextplayer.navigation.TopLevelNavState
import dev.anilbeesetti.nextplayer.navigation.mediaNavGraph
import dev.anilbeesetti.nextplayer.navigation.moreNavGraph
import dev.anilbeesetti.nextplayer.navigation.navigationTransition
import dev.anilbeesetti.nextplayer.navigation.networkNavGraph
import dev.anilbeesetti.nextplayer.navigation.openPlaylist
import dev.anilbeesetti.nextplayer.navigation.playlistNavGraph
import dev.anilbeesetti.nextplayer.navigation.rememberTopLevelNavState
import dev.anilbeesetti.nextplayer.navigation.settingsNavGraph
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {

    private val mediaOperationsService: MediaOperationsService by inject()

    private val systemService: SystemService by inject()

    private val viewModel: MainViewModel by viewModel()

    private var pendingPlaylistId by mutableStateOf<Long?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPlaylistId = PlaylistShortcut.playlistId(intent)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            pendingPlaylistId = PlaylistShortcut.playlistId(intent)
        }
        systemService.initialize(this@MainActivity)
        mediaOperationsService.initialize(this@MainActivity)

        installSplashScreen().setKeepOnScreenCondition {
            viewModel.state.value is MainActivityUiState.Loading
        }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val shouldUseDarkTheme = shouldUseDarkTheme(state = state)

            LaunchedEffect(shouldUseDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { shouldUseDarkTheme },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { shouldUseDarkTheme },
                    ),
                )
            }

            NextPlayerTheme(
                darkTheme = shouldUseDarkTheme,
                highContrastDarkTheme = shouldUseHighContrastDarkTheme(state = state),
                dynamicColor = shouldUseDynamicTheming(state = state),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    val navState = rememberTopLevelNavState()

                    LaunchedEffect(pendingPlaylistId) {
                        pendingPlaylistId?.let { playlistId ->
                            navState.openPlaylist(playlistId)
                            pendingPlaylistId = null
                        }
                    }

                    val mediaStack = navState.backStacks.getValue(TopLevelDestination.HOME.route)
                    val playlistStack = navState.backStacks.getValue(TopLevelDestination.LIBRARY.route)
                    val networkStack = navState.backStacks.getValue(TopLevelDestination.RADIO.route)
                    val moreStack = navState.backStacks.getValue(TopLevelDestination.EXPLORE.route)

                    val provider = entryProvider {
                        mediaNavGraph(
                            context = this@MainActivity,
                            backStack = mediaStack,
                            onSearchClick = { navState.switchTo(TopLevelDestination.SEARCH.route) },
                            onSearchNavigateUp = navState::goBack,
                            onSearchFolderClick = { folderPath ->
                                mediaStack.clear()
                                mediaStack.add(TopLevelDestination.HOME.route)
                                mediaStack.add(
                                    MediaPickerRoute(folderPath),
                                )
                                navState.switchTo(TopLevelDestination.HOME.route)
                            },
                        )
                        playlistNavGraph(context = this@MainActivity, backStack = playlistStack)
                        networkNavGraph(context = this@MainActivity, backStack = networkStack)
                        moreNavGraph(context = this@MainActivity, backStack = moreStack)
                        settingsNavGraph(backStack = navState.currentStack)
                    }

                    val topLevelFabStates = remember { mutableStateMapOf<String, TopLevelFabState>() }
                    var showTopLevelBottomBar by remember { mutableStateOf(true) }
                    NavigationLayout(
                        state = navState,
                        fabStates = topLevelFabStates,
                        showBottomBar = showTopLevelBottomBar,
                    ) { layoutPaddingValues ->
                        val railPadding = layoutPaddingValues.calculateStartPadding(LocalLayoutDirection.current)
                        val navigationInsetsDecorator = remember(navState, railPadding) {
                            NavEntryDecorator<NavKey> { entry ->
                                // Reserve rail space per entry without resizing the animated display.
                                Box(
                                    Modifier.thenIf(navState.topLevelContentKeys.contains(entry.contentKey)) {
                                        padding(start = railPadding)
                                            .consumeWindowInsets(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                                    },
                                ) {
                                    entry.Content()
                                }
                            }
                        }
                        CompositionLocalProvider(
                            LocalNavigationBottomPadding provides layoutPaddingValues.calculateBottomPadding(),
                            LocalTopLevelBottomBarVisibleSetter provides { showTopLevelBottomBar = it },
                            LocalTopLevelFabSetter provides { key, state ->
                                if (state == null) {
                                    topLevelFabStates.remove(key)
                                } else {
                                    topLevelFabStates[key] = state
                                }
                            },
                        ) {
                            NavDisplay(
                                entries = navState.rememberEntries(provider, navigationInsetsDecorator),
                                onBack = { navState.goBack() },
                                transitionSpec = { navState.navigationTransition(initialState, targetState) },
                                popTransitionSpec = { navState.navigationTransition(initialState, targetState) },
                                predictivePopTransitionSpec = { navState.navigationTransition(initialState, targetState) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationLayout(
    modifier: Modifier = Modifier,
    state: TopLevelNavState,
    fabStates: SnapshotStateMap<String, TopLevelFabState>,
    showBottomBar: Boolean,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
    content: @Composable (PaddingValues) -> Unit,
) {
    val isTv = LocalContext.current.isTelevision
    val density = LocalDensity.current
    var railWidth by remember(density) { mutableStateOf(0.dp) }
    val contentFocusRequester = remember { FocusRequester() }
    val fabFocusRequester = remember { FocusRequester() }
    val showNavRail = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val showNavigation = state.currentStack.lastOrNull()?.let { state.topLevelContentKeys.contains(it) } == true
    val scrollConnection = rememberFloatingTabBarScrollConnection(
        inlineBehavior = if (showNavRail || isTv) {
            FloatingTabBarInlineBehavior.Never
        } else {
            FloatingTabBarInlineBehavior.OnScrollDown
        },
    )
    LaunchedEffect(state.topLevelRoute, showNavigation, showBottomBar) {
        scrollConnection.expand()
    }

    val selectedFabState = state.destinations[state.selectedIndex].fabKey?.let(fabStates::get)
    var displayedFabState by remember { mutableStateOf<TopLevelFabState?>(null) }
    LaunchedEffect(selectedFabState) {
        selectedFabState?.let { displayedFabState = it }
    }

    val bottomBarContentHeight = when {
        !showNavigation || !showBottomBar -> 0.dp
        showNavRail && displayedFabState != null -> 56.dp
        showNavRail -> 0.dp
        else -> 68.dp
    }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val targetBottomPadding = if (showNavigation && showBottomBar) {
        bottomBarContentHeight + 12.dp + bottomInset
    } else {
        0.dp
    }
    val contentBottomPadding by animateDpAsState(
        targetValue = targetBottomPadding,
        animationSpec = tween(durationMillis = 220),
        label = "navigation_content_bottom_padding",
    )

    val shouldCaptureBackdrop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        showNavigation && showBottomBar && !showNavRail
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val appBackdrop = rememberLayerBackdrop(
        onDraw = remember(backgroundColor) {
            val backdropColor = backgroundColor
            { drawRect(backdropColor); drawContent() }
        },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollConnection)
            .background(backgroundColor),
    ) {
        // Capture only app content, not the navigation chrome that samples it. The source spans
        // the full window so the liquid-glass bar can refract content all the way to the bottom edge.
        Box(
            Modifier
                .fillMaxSize()
                .thenIf(shouldCaptureBackdrop) { layerBackdrop(appBackdrop) },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .thenIf(isTv) {
                        focusRequester(contentFocusRequester)
                            .focusProperties {
                                onExit = {
                                    if (requestedFocusDirection == FocusDirection.Down &&
                                        showNavigation && showBottomBar && displayedFabState != null
                                    ) {
                                        fabFocusRequester.requestFocus()
                                    }
                                }
                            }
                            .focusGroup()
                    },
            ) {
                content(
                    PaddingValues(
                        start = if (showNavRail) railWidth else 0.dp,
                        bottom = contentBottomPadding,
                    ),
                )
            }
        }

        AnimatedVisibility(
            visible = showNavigation && showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() +
                slideInVertically { it / 2 } +
                expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() +
                slideOutVertically { it / 2 } +
                shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            NextNavigationBar(
                state = state,
                fabState = displayedFabState,
                contentFocusRequester = contentFocusRequester,
                fabFocusRequester = fabFocusRequester,
                scrollConnection = scrollConnection,
                appBackdrop = appBackdrop,
                showFabOnly = showNavRail,
            )
        }

        AnimatedVisibility(
            visible = showNavRail && showNavigation,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            NextNavigationRail(
                state = state,
                modifier = Modifier.onSizeChanged { railWidth = with(density) { it.width.toDp() } },
            )
        }
    }
}

/**
 * Returns `true` if dark theme should be used, as a function of the [state] and the
 * current system context.
 */
@Composable
fun shouldUseDarkTheme(
    state: MainActivityUiState,
): Boolean = when (state) {
    MainActivityUiState.Loading -> isSystemInDarkTheme()
    is MainActivityUiState.Success -> when (state.preferences.themeConfig) {
        ThemeConfig.SYSTEM -> isSystemInDarkTheme()
        ThemeConfig.OFF -> false
        ThemeConfig.ON -> true
    }
}

@Composable
fun shouldUseHighContrastDarkTheme(
    state: MainActivityUiState,
): Boolean = when (state) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> state.preferences.useHighContrastDarkTheme
}

/**
 * Returns `true` if the dynamic color is disabled, as a function of the [state].
 */
@Composable
fun shouldUseDynamicTheming(
    state: MainActivityUiState,
): Boolean = when (state) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> state.preferences.useDynamicColors
}
