package dev.ronellsalunke.modellm.data.remote

import dev.ronellsalunke.modellm.data.remote.dto.ModelDto
import dev.ronellsalunke.modellm.data.remote.dto.ProviderDto
import dev.ronellsalunke.modellm.data.remote.dto.SearchIndexItemDto

data class CatalogPayload(
    val providers: List<ProviderDto>,
    val canonicalModels: Map<String, ModelDto>,
    val searchIndex: List<SearchIndexItemDto>,
)
