package dev.ronellsalunke.modellm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.Icons
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.ronellsalunke.modellm.ui.components.FloatingTabBar
import dev.ronellsalunke.modellm.ui.components.FloatingTabItem
import dev.ronellsalunke.modellm.ui.labs.LabDetailScreen
import dev.ronellsalunke.modellm.ui.labs.LabsScreen
import dev.ronellsalunke.modellm.ui.models.CanonicalModelDetailScreen
import dev.ronellsalunke.modellm.ui.models.ModelsListScreen
import dev.ronellsalunke.modellm.ui.models.ProviderModelDetailScreen
import dev.ronellsalunke.modellm.ui.navigation.CanonicalModelDetailRoute
import dev.ronellsalunke.modellm.ui.navigation.LabDetailRoute
import dev.ronellsalunke.modellm.ui.navigation.ProviderDetailRoute
import dev.ronellsalunke.modellm.ui.navigation.ProviderModelDetailRoute
import dev.ronellsalunke.modellm.ui.navigation.SettingsRoute
import dev.ronellsalunke.modellm.ui.navigation.TabsRoute
import dev.ronellsalunke.modellm.ui.navigation.pushedEnter
import dev.ronellsalunke.modellm.ui.navigation.pushedExit
import dev.ronellsalunke.modellm.ui.navigation.pushedPopEnter
import dev.ronellsalunke.modellm.ui.navigation.pushedPopExit
import dev.ronellsalunke.modellm.ui.navigation.pushedPredictivePopEnter
import dev.ronellsalunke.modellm.ui.navigation.pushedPredictivePopExit
import dev.ronellsalunke.modellm.ui.providers.ProviderDetailScreen
import dev.ronellsalunke.modellm.ui.providers.ProvidersListScreen
import dev.ronellsalunke.modellm.ui.settings.SettingsScreen
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import kotlinx.coroutines.launch

private val TabItems =
    listOf(
        FloatingTabItem("Models", Icons.AutoMirrored.Filled.List),
        FloatingTabItem("Providers", Icons.Default.Storage),
        FloatingTabItem("Labs", Icons.Default.Science),
    )

private val BackSwipeMaxShift = 24.dp
private const val BackSwipeMinScale = 0.92f

@Composable
fun ModeLLMApp() {
    val navController = rememberNavController()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LocalAppPalette.current.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        AppNavHost(
            navController = navController,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TabsRoute,
        modifier = modifier,
        enterTransition = { pushedEnter() },
        exitTransition = { pushedExit() },
        popEnterTransition = { pushedPopEnter() },
        popExitTransition = { pushedPopExit() },
        predictivePopEnterTransition = { swipeEdge -> pushedPredictivePopEnter(swipeEdge) },
        predictivePopExitTransition = { swipeEdge -> pushedPredictivePopExit(swipeEdge) },
    ) {
        composable<TabsRoute> {
            TabsScreen(navController = navController)
        }
        composable<SettingsRoute> { SettingsScreen(onBack = { navController.navigateUp() }) }
        composable<CanonicalModelDetailRoute> { entry ->
            val route = entry.toRoute<CanonicalModelDetailRoute>()
            CanonicalModelDetailScreen(
                route.canonicalId,
                onBack = { navController.navigateUp() },
                onProviderClick = { navController.navigate(ProviderDetailRoute(it)) },
            )
        }
        composable<ProviderModelDetailRoute> { entry ->
            val route = entry.toRoute<ProviderModelDetailRoute>()
            ProviderModelDetailScreen(
                route.providerId,
                route.modelId,
                onBack = { navController.navigateUp() },
            )
        }
        composable<LabDetailRoute> { entry ->
            val route = entry.toRoute<LabDetailRoute>()
            LabDetailScreen(
                route.labId,
                onBack = { navController.navigateUp() },
                onModelClick = { navController.navigate(CanonicalModelDetailRoute(it)) },
            )
        }
        composable<ProviderDetailRoute> { entry ->
            val route = entry.toRoute<ProviderDetailRoute>()
            ProviderDetailScreen(
                route.providerId,
                onBack = { navController.popBackStack() },
                onModelClick = {
                    navController.navigate(ProviderModelDetailRoute(it.providerId, it.id))
                },
            )
        }
    }
}

@Composable
private fun TabsScreen(navController: NavHostController) {
    val pagerState = rememberPagerState(pageCount = { TabItems.size })
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val onTabs = backStackEntry?.destination?.hasRoute<TabsRoute>() == true
    val tabsDispatcherOwner = rememberNavigationEventDispatcherOwner(enabled = onTabs)
    val backEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val palette = LocalAppPalette.current
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides tabsDispatcherOwner) {
        NavigationBackHandler(
            state = backEventState,
            isBackEnabled = pagerState.currentPage != 0,
            onBackCompleted = { scope.launch { pagerState.animateScrollToPage(0) } },
        )
    }
    Box(
        Modifier.fillMaxSize()
            .background(palette.background)
            .graphicsLayer {
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
        FloatingTabBar(
            items = TabItems,
            selectedIndex = pagerState.targetPage,
            onSelect = { page ->
                scope.launch { pagerState.animateScrollToPage(page) }
            },
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                    .zIndex(1f),
        )
        HorizontalPager(
            pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 2,
            key = { it },
        ) { page ->
            TabPagerContent(page = page, navController = navController)
        }
    }
}

@Composable
private fun TabPagerContent(page: Int, navController: NavHostController) {
    when (page) {
        0 ->
            ModelsListScreen(
                onModelClick = {
                    navController.navigate(CanonicalModelDetailRoute(it.id))
                },
                onSettings = { navController.navigate(SettingsRoute) },
            )
        1 ->
            ProvidersListScreen(
                onProviderClick = {
                    navController.navigate(ProviderDetailRoute(it))
                },
            )
        2 ->
            LabsScreen(
                onLabClick = { navController.navigate(LabDetailRoute(it)) },
            )
        else -> throw IllegalArgumentException("Invalid tab page index: $page")
    }
}
