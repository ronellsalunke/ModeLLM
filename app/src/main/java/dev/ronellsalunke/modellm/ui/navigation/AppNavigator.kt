package dev.ronellsalunke.modellm.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** Pushes and pops destinations, never removing the root tab entry. */
class AppNavigator(private val backStack: NavBackStack<NavKey>) {
    fun navigate(route: NavKey) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }

    fun goBack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    /** Returns to the tab root before switching tabs, so a pushed screen never stays on top. */
    fun popToRoot() {
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
}
