package com.m57.hermescontrol.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    content: @Composable (PaddingValues) -> Unit,
) {
    val gestureController = LocalDrawerGestureController.current
    SideEffect { gestureController?.reconcile(drawerGesturesEnabled) }

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
            // Glass-style top bar: a translucent floating pill that holds the
            // title and all chrome. The status bar sits behind it (transparent)
            // so we don't have to draw a solid bar — the glass pill floats over
            // whatever content scrolls behind it.
            GlassTopBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = {
                    if (onRefresh != null) {
                        GlassActionBubble(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.content_desc_refresh),
                                modifier = Modifier.size(20.dp).testTag("refresh_button"),
                            )
                        }
                    }
                    actions()
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        val refreshContent: @Composable () -> Unit = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            start = paddingValues.calculateStartPadding(layoutDirection),
                            end = paddingValues.calculateEndPadding(layoutDirection),
                            bottom = paddingValues.calculateBottomPadding(),
                        ).dynamicTopBarPadding(scrollBehavior, paddingValues.calculateTopPadding()),
            ) {
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
        val heightOffset = scrollBehavior.state.heightOffset.roundToInt()
        val activeTopPadding = (baseTopPaddingPx + heightOffset).coerceAtLeast(0)

        val placeable =
            measurable.measure(
                constraints.copy(
                    maxHeight = (constraints.maxHeight - activeTopPadding).coerceAtLeast(0),
                    minHeight = (constraints.minHeight - activeTopPadding).coerceAtLeast(0),
                ),
            )

        layout(placeable.width, placeable.height + activeTopPadding) {
            placeable.placeRelative(0, activeTopPadding)
        }
    }

/**
 * Glass-style floating top bar.
 *
 * Replaces Material3's [androidx.compose.material3.TopAppBar] with a frosted
 * glass pill that floats over the screen content. The pill shrinks to a
 * tighter shape on scroll (handled by [scrollBehavior] state). Layout:
 *
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │ [bubble]   <floating title text>                  [bubbles]  │
 *  └─────────────────────────────────────────────────────────────┘
 *
 * The icons themselves sit inside small [GlassBubble] "pills" so they read
 * as floating chrome rather than inline buttons. The title is rendered
 * with a thin, light typeface weight so the bar reads as airy even when
 * the underlying content is busy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassTopBar(
    title: @Composable () -> Unit,
    navigationIcon: NavIcon?,
    actions: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val surface = MaterialTheme.colorScheme.surface
    val animatedSurface by animateColorAsState(
        targetValue = surface,
        animationSpec = tween(durationMillis = 200),
        label = "topBarTint",
    )
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("hermes_top_bar"),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 26.dp,
            tint = animatedSurface,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (val icon = navigationIcon) {
                    is NavIcon.Back -> {
                        GlassActionBubble(
                            onClick = icon.onBack,
                            modifier = Modifier.testTag("back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    is NavIcon.Menu -> {
                        GlassActionBubble(
                            onClick = icon.onOpen,
                            modifier = Modifier.testTag("menu_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.content_desc_open_drawer),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    null -> { /* leave room for the title only */ }
                }
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CompositionLocalProvider(
                        androidx.compose.material3.LocalContentColor provides
                            MaterialTheme.colorScheme.onSurface,
                    ) {
                        // Thin, light-weight title — the bar reads as airy.
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalTextStyle provides
                                MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 0.4.sp,
                                ),
                        ) {
                            title()
                        }
                    }
                }
                actions()
            }
        }
    }
}

/**
 * Squircle glass pill used for top-bar / composer chrome (menu, back, send, mic).
 * Same visual language as [GlassSurface] but compact, with built-in click
 * feedback (color flash) and accessibility support.
 */
@Composable
private fun GlassActionBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .testTag("glass_bubble"),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
