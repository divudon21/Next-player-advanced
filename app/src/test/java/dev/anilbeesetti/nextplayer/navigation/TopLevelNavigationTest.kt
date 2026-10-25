package dev.anilbeesetti.nextplayer.navigation

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.anilbeesetti.nextplayer.core.ui.components.TopLevelFabState
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.playlist.navigation.PlaylistDetailRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelNavigationTest {

    @Test
    fun topLevelDestinationsMatchRequestedTabOrder() {
        assertEquals(
            listOf(
                TopLevelDestination.HOME,
                TopLevelDestination.RADIO,
                TopLevelDestination.LIBRARY,
                TopLevelDestination.EXPLORE,
                TopLevelDestination.SEARCH,
            ),
            TopLevelDestination.entries,
        )
    }

    @Test
    fun playFabReservesSpaceAboveTheFullWidthBottomBar() {
        val playFab = TopLevelFabState(NextIcons.Play, {})

        assertEquals(
            144.dp,
            topLevelNavigationContentHeight(
                showNavigation = true,
                showBottomBar = true,
                showNavRail = false,
                fabState = playFab,
            ),
        )
        assertEquals(
            64.dp,
            topLevelNavigationContentHeight(
                showNavigation = true,
                showBottomBar = true,
                showNavRail = true,
                fabState = playFab,
            ),
        )
    }

    @Test
    fun switchingTabsPreservesPlaylistDetailStack() {
        val stacks = TopLevelDestination.entries.associate { destination ->
            destination.route to NavBackStack<NavKey>(destination.route)
        }
        val state = TopLevelNavState(
            destinations = TopLevelDestination.entries,
            backStacks = stacks,
            selectedIndexState = mutableIntStateOf(0),
        )
        val playlistStack = stacks.getValue(TopLevelDestination.LIBRARY.route)

        state.switchTo(TopLevelDestination.LIBRARY.route)
        playlistStack += PlaylistDetailRoute(7)
        state.switchTo(TopLevelDestination.HOME.route)
        state.switchTo(TopLevelDestination.LIBRARY.route)

        assertEquals(
            listOf(TopLevelDestination.LIBRARY.route, PlaylistDetailRoute(7)),
            state.currentStack,
        )
    }

    @Test
    fun shortcutSelectsPlaylistAndReplacesPreviousPlaylistStack() {
        val stacks = TopLevelDestination.entries.associate { destination ->
            destination.route to NavBackStack<NavKey>(destination.route)
        }
        val state = TopLevelNavState(
            destinations = TopLevelDestination.entries,
            backStacks = stacks,
            selectedIndexState = mutableIntStateOf(0),
        )
        stacks.getValue(TopLevelDestination.LIBRARY.route).add(PlaylistDetailRoute(7))
        state.switchTo(TopLevelDestination.RADIO.route)

        state.openPlaylist(42)
        state.openPlaylist(42)

        assertEquals(TopLevelDestination.LIBRARY.route, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.LIBRARY.route, PlaylistDetailRoute(42)), state.currentStack)
        state.goBack()
        assertEquals(listOf(TopLevelDestination.LIBRARY.route), state.currentStack)
        state.goBack()
        assertEquals(TopLevelDestination.HOME.route, state.topLevelRoute)
    }

    @Test
    fun backFromSearchTabReturnsToHome() {
        val stacks = TopLevelDestination.entries.associate { destination ->
            destination.route to NavBackStack<NavKey>(destination.route)
        }
        val state = TopLevelNavState(
            destinations = TopLevelDestination.entries,
            backStacks = stacks,
            selectedIndexState = mutableIntStateOf(0),
        )

        state.switchTo(TopLevelDestination.SEARCH.route)
        state.goBack()

        assertEquals(TopLevelDestination.HOME.route, state.topLevelRoute)
    }

    @Test
    fun topLevelContentKeysContainsDestinations() {
        val stacks = TopLevelDestination.entries.associate { destination ->
            destination.route to NavBackStack<NavKey>(destination.route)
        }
        val state = TopLevelNavState(
            destinations = TopLevelDestination.entries,
            backStacks = stacks,
            selectedIndexState = mutableIntStateOf(0),
        )
        for (dest in TopLevelDestination.entries) {
            assert(state.topLevelContentKeys.contains(dest.route))
            assert(state.topLevelContentKeys.contains(Pair("${dest.route}", "${dest.route::class}")))
        }
    }
}
