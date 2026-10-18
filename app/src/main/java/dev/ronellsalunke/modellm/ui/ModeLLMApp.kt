package dev.ronellsalunke.modellm.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.models.*
import dev.ronellsalunke.modellm.ui.navigation.*
import dev.ronellsalunke.modellm.ui.providers.*
import dev.ronellsalunke.modellm.ui.labs.*
import kotlinx.coroutines.launch
import dev.ronellsalunke.modellm.ui.settings.SettingsScreen
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.LocalMaterialYouEnabled

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
                val palette = LocalAppPalette.current
                val materialYou = LocalMaterialYouEnabled.current
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (materialYou) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = if (materialYou) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        palette.brand
                    },
                    unselectedIconColor = palette.textSecondary,
                    unselectedTextColor = palette.textSecondary,
                )
                Column(Modifier.fillMaxSize().background(palette.background)) {
                    HorizontalPager(
                        pagerState,
                        modifier = Modifier.weight(1f),
                        beyondViewportPageCount = 2,
                        key = { it },
                    ) { page ->
                        when (page) {
                            0 -> ModelsListScreen(onModelClick = { navController.navigate(CanonicalModelDetailRoute(it.id)) }, onSettings = { navController.navigate(SettingsRoute) })
                            1 -> ProvidersListScreen(onProviderClick = { navController.navigate(ProviderDetailRoute(it)) })
                            2 -> LabsScreen(onLabClick = { navController.navigate(LabDetailRoute(it)) })
                        }
                    }
                    CatalogDivider()
                    NavigationBar(
                        containerColor = palette.background,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                        NavigationBarItem(
                            selected = pagerState.currentPage == 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                            label = { Text("Models") },
                            colors = navItemColors,
                        )
                        NavigationBarItem(
                            selected = pagerState.currentPage == 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                            label = { Text("Providers") },
                            colors = navItemColors,
                        )
                        NavigationBarItem(
                            selected = pagerState.currentPage == 2,
                            onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                            icon = { Icon(Icons.Default.Science, contentDescription = null) },
                            label = { Text("Labs") },
                            colors = navItemColors,
                        )
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
