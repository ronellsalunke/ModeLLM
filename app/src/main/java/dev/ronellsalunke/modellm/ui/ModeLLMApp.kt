package dev.ronellsalunke.modellm.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.ronellsalunke.modellm.ui.components.CatalogShortcutState
import dev.ronellsalunke.modellm.ui.components.FloatingTabBar
import dev.ronellsalunke.modellm.ui.components.FloatingTabBarClearance
import dev.ronellsalunke.modellm.ui.components.FloatingTabItem
import dev.ronellsalunke.modellm.ui.components.FloatingTabRail
import dev.ronellsalunke.modellm.ui.components.LocalCatalogShortcuts
import dev.ronellsalunke.modellm.ui.components.LocalDetailShowsBackButton
import dev.ronellsalunke.modellm.ui.components.LocalNavigationAreaBottomPadding
import dev.ronellsalunke.modellm.ui.components.LocalTabPageActive
import dev.ronellsalunke.modellm.ui.components.LocalWideLayout
import dev.ronellsalunke.modellm.ui.components.MediumWidthBreakpoint
import dev.ronellsalunke.modellm.ui.components.isControlShortcut
import dev.ronellsalunke.modellm.ui.labs.LabDetailScreen
import dev.ronellsalunke.modellm.ui.labs.LabsScreen
import dev.ronellsalunke.modellm.ui.models.CanonicalModelDetailScreen
import dev.ronellsalunke.modellm.ui.models.ModelsListScreen
import dev.ronellsalunke.modellm.ui.models.ProviderModelDetailScreen
import dev.ronellsalunke.modellm.ui.navigation.AppNavigator
import dev.ronellsalunke.modellm.ui.navigation.CanonicalModelDetailRoute
import dev.ronellsalunke.modellm.ui.navigation.LabDetailRoute
import dev.ronellsalunke.modellm.ui.navigation.ProviderDetailRoute
import dev.ronellsalunke.modellm.ui.navigation.ProviderModelDetailRoute
import dev.ronellsalunke.modellm.ui.navigation.SettingsRoute
import dev.ronellsalunke.modellm.ui.navigation.TabsRoute
import dev.ronellsalunke.modellm.ui.navigation.pushedContentTransform
import dev.ronellsalunke.modellm.ui.navigation.pushedPopContentTransform
import dev.ronellsalunke.modellm.ui.navigation.pushedPredictivePopContentTransform
import dev.ronellsalunke.modellm.ui.providers.ProviderDetailScreen
import dev.ronellsalunke.modellm.ui.providers.ProvidersListScreen
import dev.ronellsalunke.modellm.ui.settings.SettingsScreen
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import kotlinx.coroutines.launch

private val TabItems = listOf(
    FloatingTabItem("Models", Icons.AutoMirrored.Filled.List),
    FloatingTabItem("Providers", Icons.Default.Storage),
    FloatingTabItem("Labs", Icons.Default.Science),
)

private val TabPlaceholders = listOf("Select a model", "Select a provider", "Select a lab")

private val BackSwipeMaxShift = 24.dp
private const val BackSwipeMinScale = 0.92f

private const val ModelsPage = 0
private const val ProvidersPage = 1
private const val LabsPage = 2

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ModeLLMApp() {
    val backStack = rememberNavBackStack(TabsRoute)
    val navigator = remember(backStack) { AppNavigator(backStack) }
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(adaptiveInfo) {
        calculatePaneScaffoldDirective(adaptiveInfo).copy(horizontalPartitionSpacerSize = 0.dp)
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LocalAppPalette.current.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        AppShell(backStack, navigator, directive)
    }
}

@Composable
private fun AppShell(
    backStack: NavBackStack<NavKey>,
    navigator: AppNavigator,
    directive: PaneScaffoldDirective,
) {
    val pagerState = rememberPagerState(pageCount = { TabItems.size })
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val shortcuts = remember { CatalogShortcutState() }
    val onSelectTab: (Int) -> Unit = { page ->
        navigator.popToRoot()
        scope.launch { pagerState.animateScrollToPage(page) }
    }
    // Restore root focus after navigation so app-wide shortcuts keep firing even when the
    // pushed screen holds no focus target of its own.
    LaunchedEffect(backStack.size) { focusRequester.requestFocus() }
    BoxWithConstraints(
        Modifier.fillMaxSize().focusRequester(focusRequester)
            .onPreviewKeyEvent { event -> event.tabShortcut(shortcuts, onSelectTab) }
            .focusable(),
    ) {
        val useRail = maxWidth >= MediumWidthBreakpoint
        // Compact detail screens stay full screen, so the bar would cover their content.
        val showBottomBar = !useRail && backStack.size == 1
        // A detail opened from another detail keeps its back button so the parent stays reachable.
        val showsDetailPane = directive.maxHorizontalPartitions >= 2 && backStack.size <= 2
        CompositionLocalProvider(
            LocalCatalogShortcuts provides shortcuts,
            LocalNavigationAreaBottomPadding provides
                if (showBottomBar) FloatingTabBarClearance else 0.dp,
            LocalDetailShowsBackButton provides showsDetailPane.not(),
            LocalWideLayout provides useRail,
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                if (useRail) {
                    FloatingTabRail(
                        items = TabItems,
                        selectedIndex = pagerState.targetPage,
                        onSelect = onSelectTab,
                        modifier = Modifier.fillMaxHeight(),
                    )
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    AppNavDisplay(
                        backStack = backStack,
                        navigator = navigator,
                        pagerState = pagerState,
                        // The rail is one tap away, and swiping a narrow list pane would
                        // change the tab under an open detail.
                        swipeEnabled = !useRail,
                        directive = directive,
                    )
                    if (showBottomBar) {
                        FloatingTabBar(
                            items = TabItems,
                            selectedIndex = pagerState.targetPage,
                            onSelect = onSelectTab,
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .offset(y = -FloatingToolbarDefaults.ScreenOffset).zIndex(1f),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    navigator: AppNavigator,
    pagerState: PagerState,
    swipeEnabled: Boolean,
    directive: PaneScaffoldDirective,
) {
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = directive,
        // Pop one entry at a time so a detail opened from another detail returns to its parent.
        backNavigationBehavior = BackNavigationBehavior.PopUntilContentChange,
    )
    val scope = rememberCoroutineScope()
    val backEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backEventState,
        isBackEnabled = backStack.size == 1 && pagerState.currentPage != ModelsPage,
        onBackCompleted = { scope.launch { pagerState.animateScrollToPage(ModelsPage) } },
    )
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            val event =
                (backEventState.transitionState as? NavigationEventTransitionState.InProgress)
                    ?.latestEvent
            val progress = event?.progress ?: 0f
            val scale = 1f - progress * (1f - BackSwipeMinScale)
            val direction = if (event?.swipeEdge == NavigationEvent.EDGE_RIGHT) -1f else 1f
            scaleX = scale
            scaleY = scale
            translationX = direction * progress * BackSwipeMaxShift.toPx()
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { navigator.goBack() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            sceneStrategies = listOf(listDetailStrategy),
            transitionSpec = { pushedContentTransform() },
            popTransitionSpec = { pushedPopContentTransform() },
            predictivePopTransitionSpec = { edge -> pushedPredictivePopContentTransform(edge) },
            entryProvider = appEntries(navigator, pagerState, swipeEnabled),
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun appEntries(
    navigator: AppNavigator,
    pagerState: PagerState,
    swipeEnabled: Boolean,
) = entryProvider {
    entry(
        key = TabsRoute,
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                DetailPlaceholder(TabPlaceholders.getOrElse(pagerState.currentPage) { "" })
            },
        ),
    ) {
        TabsPager(pagerState, navigator, swipeEnabled)
    }
    entry<SettingsRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
        SettingsScreen(onBack = { navigator.goBack() })
    }
    entry<CanonicalModelDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        CanonicalModelDetailScreen(
            route.canonicalId,
            onBack = { navigator.goBack() },
            onProviderClick = { navigator.navigate(ProviderDetailRoute(it)) },
        )
    }
    entry<ProviderModelDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        ProviderModelDetailScreen(
            route.providerId, route.modelId, onBack = { navigator.goBack() },
        )
    }
    entry<LabDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        LabDetailScreen(
            route.labId,
            onBack = { navigator.goBack() },
            onModelClick = { navigator.navigate(CanonicalModelDetailRoute(it)) },
        )
    }
    entry<ProviderDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        ProviderDetailScreen(
            route.providerId,
            onBack = { navigator.goBack() },
            onModelClick = {
                navigator.navigate(ProviderModelDetailRoute(it.providerId, it.id))
            },
        )
    }
}

@Composable
private fun TabsPager(
    pagerState: PagerState,
    navigator: AppNavigator,
    swipeEnabled: Boolean,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = TabItems.size - 1,
        userScrollEnabled = swipeEnabled,
        key = { it },
    ) { page ->
        // Composed neighbouring pages must not claim focus or shortcuts.
        CompositionLocalProvider(LocalTabPageActive provides (page == pagerState.currentPage)) {
            TabPage(page = page, navigator = navigator)
        }
    }
}

@Composable
private fun TabPage(page: Int, navigator: AppNavigator) {
    when (page) {
        ModelsPage ->
            ModelsListScreen(
                onModelClick = { navigator.navigate(CanonicalModelDetailRoute(it.id)) },
                onSettings = { navigator.navigate(SettingsRoute) },
            )
        ProvidersPage ->
            ProvidersListScreen(
                onProviderClick = { navigator.navigate(ProviderDetailRoute(it)) },
            )
        LabsPage ->
            LabsScreen(onLabClick = { navigator.navigate(LabDetailRoute(it)) })
        else -> throw IllegalArgumentException("Invalid tab page index: $page")
    }
}

@Composable
private fun DetailPlaceholder(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Switches tabs with Ctrl+1, Ctrl+2, and Ctrl+3; forwards other Ctrl shortcuts. */
private fun KeyEvent.tabShortcut(
    shortcuts: CatalogShortcutState,
    onSelectTab: (Int) -> Unit,
): Boolean {
    val page =
        when (key) {
            Key.One -> ModelsPage
            Key.Two -> ProvidersPage
            Key.Three -> LabsPage
            else -> null
        }
    return when {
        !isControlShortcut() -> false
        page == null -> shortcuts.handle(this)
        else -> {
            onSelectTab(page)
            true
        }
    }
}
