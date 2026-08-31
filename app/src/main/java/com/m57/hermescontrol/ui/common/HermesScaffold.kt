package com.m57.hermescontrol.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.m57.hermescontrol.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Sealed interface for navigation icon types — eliminates boolean-flag anti-pattern. */
sealed interface NavIcon {
    /** Hamburger menu icon that opens the drawer. */
    data class Menu(
        val onOpen: () -> Unit,
    ) : NavIcon

    /** Back arrow icon. */
    data class Back(
        val onBack: () -> Unit,
    ) : NavIcon
}

/**
 * Single source of truth for whether the modal navigation drawer's swipe gestures
 * should be enabled for the currently-composed screen.
 *
 * Each [HermesScaffold] (or [DisableDrawerGestures]) reconciles its preference into
 * the singleton instance via a [SideEffect]; `ModalNavigationDrawer` in `Navigation.kt`
 * reads [enabled] and passes it straight to Material's `gesturesEnabled` parameter.
 *
 * This replaces the old `DRAWER_GESTURE_SCREENS` set + `LaunchedEffect(snapTo(Closed))`
 * workaround documented in issue #619: gesture state now flows from the rendered
 * screen automatically, and the drawer controller closes the drawer itself when a
 * screen opts out — no global allow-list, no defensive close callbacks.
 */
class DrawerGestureController(
    private val drawerState: DrawerState,
    private val scope: CoroutineScope,
) {
    var enabled by mutableStateOf(false)
        private set

    /**
     * Reconcile a screen's desired gesture state.
     *
     * Called from a [SideEffect] on every screen composition. If a screen opts out
     * of gestures while the drawer is open, we close it synchronously so the scrim
     * cannot intercept the next tap (the bug class from PRs #445 / #454 / #455).
     */
    fun reconcile(desired: Boolean) {
        if (desired == enabled) return
        enabled = desired
        if (!desired && drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }
    }
}

/**
 * CompositionLocal that exposes the active [DrawerGestureController].
 *
 * Provided by `MainNavigation` and read both by [HermesScaffold] (to reconcile a
 * screen's preference) and by `ModalNavigationDrawer` (to gate gestures).
 */
val LocalDrawerGestureController =
    compositionLocalOf<DrawerGestureController?> {
        null
    }

/**
 * Opt out of drawer gestures for a screen that does NOT use [HermesScaffold].
 *
 * Used by full-bleed entry screens (Landing, AuthLogin) that
 * render their own layout. Scaffold-based screens should instead pass
 * `drawerGesturesEnabled = false` to [HermesScaffold].
 */
@Composable
fun DisableDrawerGestures() {
    val controller = LocalDrawerGestureController.current ?: return
    SideEffect { controller.reconcile(false) }
}

/**
 * Shared Scaffold wrapper that standardizes the TopAppBar.
 *
 * Improvements (v2):
 *  - Scroll-aware top bar (pinned collapse on scroll)
 *  - Uses Spacing tokens internally
 *  - Simplified API: [navigationIcon] replaces the old showBack/onBack/onOpenDrawer trio
 *  - Safe-area aware via Scaffold insets
 *  - Drawer-gesture aware via [drawerGesturesEnabled] (issue #619)
 *
 * Screens that still need a Back arrow instead of a Menu icon can set
 * `navigationIcon = NavIcon.Back { … }` instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermesScaffold(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: NavIcon? = null,
    /**
     * Whether the modal navigation drawer's swipe-open/swipe-close gestures should
     * be active while this screen is visible. Defaults to `true` (primary screens).
     *
     * Pass `false` for drill-down sub-pages (e.g. Settings sub-pages) where the
     * drawer should be closed and locked. The [DrawerGestureController] reconciles
     * this preference and closes the drawer automatically if a screen opts out
     * while the drawer is open — see issue #619.
     */
    drawerGesturesEnabled: Boolean = true,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    pinTopBar: Boolean = false,
    snackbarHost: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    /**
     * When true, the top bar renders as a floating glass pill that sits
     * slightly below the system status bar. When false, the original
     * Material3 TopAppBar is used (pinned / enterAlways, scrolls with
     * the content).
     *
     * Default: false (reverted to the original TopAppBar — the floating
     * glass pill on top of every screen was too tall and ate a third of
     * the viewport on phones, so we're back to the standard top app bar
     * that respects Material3's enterAlways/pinned scroll behavior).
     */
    floatingTopBar: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val gestureController = LocalDrawerGestureController.current
    SideEffect { gestureController?.reconcile(drawerGesturesEnabled) }

    // Standard Material3 TopAppBar scroll behavior:
    //   - pinnedScrollBehavior:   bar stays at full size, never collapses
    //   - enterAlwaysScrollBehavior: bar collapses when scrolling down,
    //                                expands when scrolling up
    val scrollBehavior =
        if (pinTopBar) {
            TopAppBarDefaults.pinnedScrollBehavior()
        } else {
            TopAppBarDefaults.enterAlwaysScrollBehavior()
        }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { snackbarHost() },
        topBar = {
            if (floatingTopBar) {
                // Floating bar (no background): just a transparent row
                // sitting at the top of the screen so the title + nav
                // icon + actions stay readable. We *deliberately* do
                // not paint a scrim here — a dark glass strip would
                // hover over the content and eat a third of the
                // viewport on tall screens. The user can opt in to a
                // glass surface via the new `topBarSurface` param.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Navigation icon column
                        when (val icon = navigationIcon) {
                            is NavIcon.Back -> {
                                IconButton(onClick = icon.onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.content_desc_back),
                                        modifier = Modifier.testTag("back_button"),
                                    )
                                }
                            }

                            is NavIcon.Menu -> {
                                IconButton(onClick = icon.onOpen) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = stringResource(R.string.content_desc_open_drawer),
                                        modifier = Modifier.testTag("menu_button"),
                                    )
                                }
                            }

                            null -> { /* no navigation icon */ }
                        }
                        // Title (weight 1f)
                        Box(modifier = Modifier.weight(1f)) {
                            title()
                        }
                        // Trailing actions
                        if (onRefresh != null) {
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.content_desc_refresh),
                                    modifier = Modifier.testTag("refresh_button"),
                                )
                            }
                        }
                        actions()
                    }
                }
            } else {
                TopAppBar(
                    title = title,
                    navigationIcon = {
                        when (val icon = navigationIcon) {
                            is NavIcon.Back -> {
                                IconButton(onClick = icon.onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.content_desc_back),
                                        modifier = Modifier.testTag("back_button"),
                                    )
                                }
                            }

                            is NavIcon.Menu -> {
                                IconButton(onClick = icon.onOpen) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = stringResource(R.string.content_desc_open_drawer),
                                        modifier = Modifier.testTag("menu_button"),
                                    )
                                }
                            }

                            null -> { /* no navigation icon */ }
                        }
                    },
                    actions = {
                        if (onRefresh != null) {
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.content_desc_refresh),
                                    modifier = Modifier.testTag("refresh_button"),
                                )
                            }
                        }
                        actions()
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        // Reserve room for the floating top bar at the top of the content
        // (status bar + 8dp gap + ~48dp bar + 8dp breathing room). Without
        // this, the content would scroll under the floating bar — the bar
        // is rendered as the first item inside the Box above, so the
        // content area has to start below it.
        val floatingTopGap =
            if (floatingTopBar) {
                // Only when a custom floating bar is in use do we need
                // to manually reserve space for it. With the default
                // TopAppBar path, Scaffold already handles the insets.
                56.dp
            } else {
                0.dp
            }
        val refreshContent: @Composable () -> Unit = {
            val baseModifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        top = paddingValues.calculateTopPadding() + floatingTopGap,
                        bottom = paddingValues.calculateBottomPadding(),
                    )
            val withScroll =
                if (scrollBehavior != null) {
                    baseModifier.dynamicTopBarPadding(
                        scrollBehavior,
                        paddingValues.calculateTopPadding(),
                    )
                } else {
                    baseModifier
                }
            Box(modifier = withScroll) {
                content(paddingValues)
            }
        }

        if (onRefresh != null) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                refreshContent()
            }
        } else {
            refreshContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.dynamicTopBarPadding(
    scrollBehavior: TopAppBarScrollBehavior,
    baseTopPadding: Dp,
): Modifier =
    this.layout { measurable, constraints ->
        val baseTopPaddingPx = baseTopPadding.roundToPx()
        // heightOffset goes from 0 (expanded) to a negative value as the
        // user scrolls down (the bar collapses). We want the *extra*
        // padding we add to shrink accordingly so the content can move
        // up and the user can reach the very bottom of the list.
        val heightOffset = scrollBehavior.state.heightOffset.roundToInt()
        val extraTopPadding = (baseTopPaddingPx + heightOffset).coerceAtLeast(0)
        // Total top padding = what the Scaffold gave us + the extra
        // we're reserving here. Place content at that offset and use
        // the remaining height for measurement so the LazyColumn never
        // gets more vertical space than fits on screen.
        val placeable =
            measurable.measure(
                constraints.copy(
                    maxHeight = (constraints.maxHeight - extraTopPadding).coerceAtLeast(0),
                    minHeight = (constraints.minHeight - extraTopPadding).coerceAtLeast(0),
                ),
            )
        // The reported layout size is what the placeable actually
        // measured, plus the extra top padding we reserved — this keeps
        // the parent layout from giving us more height than the
        // visible area, which is the bug that made LazyColumn stop
        // mid-list on every screen.
        layout(constraints.maxWidth, placeable.height + extraTopPadding) {
            placeable.placeRelative(0, extraTopPadding)
        }
    }
