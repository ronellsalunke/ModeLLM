package dev.ronellsalunke.modellm.ui.models

import dev.ronellsalunke.modellm.data.model.Lab
import dev.ronellsalunke.modellm.data.model.ModelFilters
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.ui.util.capabilityLabel
import dev.ronellsalunke.modellm.ui.util.modalityLabel

private const val YearPrefixLength = 4

data class ActiveFilter(val key: String, val label: String, val remove: ModelFilters)

fun ModelFilters.activeFilters(providers: List<Provider>, labs: List<Lab>): List<ActiveFilter> =
    buildList {
        labIds.forEach { id ->
            add(
                ActiveFilter(
                    "lab:$id",
                    labs.firstOrNull { it.id == id }?.name ?: id,
                    copy(labIds = labIds - id),
                )
            )
        }
        providerIds.forEach { id ->
            add(
                ActiveFilter(
                    "provider:$id",
                    providers.firstOrNull { it.id == id }?.name ?: id,
                    copy(providerIds = providerIds - id),
                )
            )
        }
        inputModalities.forEach {
            add(
                ActiveFilter(
                    "input:$it",
                    "Input: ${modalityLabel(it)}",
                    copy(inputModalities = inputModalities - it),
                )
            )
        }
        capabilities.forEach {
            add(
                ActiveFilter(
                    "capability:$it",
                    capabilityLabel(it),
                    copy(capabilities = capabilities - it),
                )
            )
        }
        if (openWeights == true) {
            add(ActiveFilter("weights", "Open weights", copy(openWeights = null)))
        }
        knowledgeAfter?.let {
            add(
                ActiveFilter(
                    "knowledge",
                    "Knowledge: ${it.take(YearPrefixLength)}+",
                    copy(knowledgeAfter = null),
                )
            )
        }
    }
