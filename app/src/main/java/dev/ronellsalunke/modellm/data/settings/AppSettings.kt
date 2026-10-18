package dev.ronellsalunke.modellm.data.settings

enum class ThemeMode {
    System,
    Light,
    Dark,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
)
