package dev.ronellsalunke.modellm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import dev.ronellsalunke.modellm.ui.components.FloatingTabBar
import dev.ronellsalunke.modellm.ui.components.FloatingTabItem
import dev.ronellsalunke.modellm.ui.models.*
import dev.ronellsalunke.modellm.ui.navigation.*
import dev.ronellsalunke.modellm.ui.providers.*
import dev.ronellsalunke.modellm.ui.labs.*
import kotlinx.coroutines.launch
import dev.ronellsalunke.modellm.ui.settings.SettingsScreen
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette

private val TabItems = listOf(
    FloatingTabItem("Models", Icons.AutoMirrored.Filled.List),
    FloatingTabItem("Providers", Icons.Default.Storage),
    FloatingTabItem("Labs", Icons.Default.Science),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModeLLMApp() {
    val navController = rememberNavController()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier.fillMaxSize(), containerColor = LocalAppPalette.current.background,
    ) { innerPadding ->
        NavHost(navController, startDestination = TabsRoute, modifier = Modifier.padding(innerPadding)) {
            composable<TabsRoute> {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val onTabs = backStackEntry?.destination?.hasRoute<TabsRoute>() == true
                BackHandler(enabled = onTabs && pagerState.currentPage != 0) {
                    scope.launch { pagerState.animateScrollToPage(0) }
                }
                val palette = LocalAppPalette.current
                Box(Modifier.fillMaxSize().background(palette.background)) {
                    FloatingTabBar(
                        items = TabItems,
                        selectedIndex = pagerState.targetPage,
                        onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                            .zIndex(1f),
                    )
                    HorizontalPager(
                        pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 2,
                        key = { it },
                    ) { page ->
                        when (page) {
                            0 -> ModelsListScreen(onModelClick = { navController.navigate(CanonicalModelDetailRoute(it.id)) }, onSettings = { navController.navigate(SettingsRoute) })
                            1 -> ProvidersListScreen(onProviderClick = { navController.navigate(ProviderDetailRoute(it)) })
                            2 -> LabsScreen(onLabClick = { navController.navigate(LabDetailRoute(it)) })
                        }
                    }
                }
            }
            composable<SettingsRoute> { SettingsScreen(onBack = { navController.navigateUp() }) }
            composable<CanonicalModelDetailRoute> { entry ->
                val route = entry.toRoute<CanonicalModelDetailRoute>()
                CanonicalModelDetailScreen(route.canonicalId, onBack = { navController.navigateUp() }, onProviderClick = { navController.navigate(ProviderDetailRoute(it)) })
            }
            composable<ProviderModelDetailRoute> { entry ->
                val route = entry.toRoute<ProviderModelDetailRoute>()
                ProviderModelDetailScreen(route.providerId, route.modelId, onBack = { navController.navigateUp() })
            }
            composable<LabDetailRoute> { entry ->
                val route = entry.toRoute<LabDetailRoute>()
                LabDetailScreen(route.labId, onBack = { navController.navigateUp() }, onModelClick = { navController.navigate(CanonicalModelDetailRoute(it)) })
            }
            composable<ProviderDetailRoute> { entry ->
                val route = entry.toRoute<ProviderDetailRoute>()
                ProviderDetailScreen(route.providerId, onBack = { navController.popBackStack() }, onModelClick = {
                    navController.navigate(ProviderModelDetailRoute(it.providerId, it.id))
                })
            }
        }
    }
}
