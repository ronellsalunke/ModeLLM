package dev.ronellsalunke.modellm.di

import dev.ronellsalunke.modellm.ui.models.ProviderModelDetailViewModel
import dev.ronellsalunke.modellm.ui.models.ModelsViewModel
import dev.ronellsalunke.modellm.ui.models.CanonicalModelDetailViewModel
import dev.ronellsalunke.modellm.ui.labs.LabsViewModel
import dev.ronellsalunke.modellm.ui.labs.LabDetailViewModel
import dev.ronellsalunke.modellm.ui.providers.ProviderDetailViewModel
import dev.ronellsalunke.modellm.ui.providers.ProvidersViewModel
import dev.ronellsalunke.modellm.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// The specified Android ViewModel DSL is deprecated in Koin 4.2.
@Suppress("DEPRECATION")
val viewModelModule = module {
    viewModel { ModelsViewModel(get(), get()) }
    viewModel { LabsViewModel(get(), get()) }
    viewModel { (labId: String) -> LabDetailViewModel(labId, get(), get()) }
    viewModel { (canonicalId: String) -> CanonicalModelDetailViewModel(canonicalId, get(), get()) }
    viewModel { (providerId: String, modelId: String) -> ProviderModelDetailViewModel(providerId, modelId, get(), get()) }
    viewModel { ProvidersViewModel(get(), get()) }
    viewModel { (providerId: String) -> ProviderDetailViewModel(providerId, get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
