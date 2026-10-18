package dev.ronellsalunke.modellm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
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

@Composable
fun ModeLLMApp() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val showBottomBar = destination == null || destination.hasRoute<TabsRoute>()
    Scaffold(
        modifier = Modifier.fillMaxSize(), containerColor = LocalAppPalette.current.background,
        bottomBar = {
            if (showBottomBar) androidx.compose.foundation.layout.Column {
                CatalogDivider()
                Row(Modifier.fillMaxWidth().navigationBarsPadding().height(56.dp).selectableGroup()) {
                    FlatTab("Models", Icons.AutoMirrored.Filled.List, pagerState.currentPage == 0, { scope.launch { pagerState.animateScrollToPage(0) } }, Modifier.weight(1f))
                    FlatTab("Providers", Icons.Default.Storage, pagerState.currentPage == 1, { scope.launch { pagerState.animateScrollToPage(1) } }, Modifier.weight(1f))
                    FlatTab("Labs", Icons.Default.Science, pagerState.currentPage == 2, { scope.launch { pagerState.animateScrollToPage(2) } }, Modifier.weight(1f))
                }
            }
        },
    ) { innerPadding ->
        NavHost(navController, startDestination = TabsRoute, modifier = Modifier.padding(innerPadding)) {
            composable<TabsRoute> {
                HorizontalPager(pagerState, modifier = Modifier.fillMaxSize().background(LocalAppPalette.current.background), beyondViewportPageCount = 2, key = { it }) { page ->
                    when (page) {
                        0 -> ModelsListScreen(onModelClick = { navController.navigate(CanonicalModelDetailRoute(it.id)) }, onSettings = { navController.navigate(SettingsRoute) })
                        1 -> ProvidersListScreen(onProviderClick = { navController.navigate(ProviderDetailRoute(it)) })
                        2 -> LabsScreen(onLabClick = { navController.navigate(LabDetailRoute(it)) })
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

@Composable
private fun FlatTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val color = if (selected) LocalAppPalette.current.brandText else LocalAppPalette.current.textSecondary
    Column(modifier.fillMaxHeight().selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
    }
}
