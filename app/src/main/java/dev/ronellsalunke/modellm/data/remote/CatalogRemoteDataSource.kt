package dev.ronellsalunke.modellm.data.remote

import dev.ronellsalunke.modellm.data.remote.dto.ModelDto
import dev.ronellsalunke.modellm.data.remote.dto.ProviderDto
import dev.ronellsalunke.modellm.data.remote.dto.SearchIndexItemDto
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

class CatalogRemoteDataSource(
    private val client: HttpClient,
    private val json: Json,
) {
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun fetchCatalog(): CatalogPayload =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val providers = async {
                    client
                        .prepareGet("https://models.dev/api.json") {
                            parameter("type", "all")
                        }
                        .execute { response ->
                            response.bodyAsChannel().toInputStream().use { input ->
                                json.decodeFromStream<Map<String, ProviderDto>>(input).map {
                                    (id, provider) ->
                                    if (provider.id.isNullOrBlank()) provider.copy(id = id)
                                    else provider
                                }
                            }
                        }
                }
                val models = async {
                    client.prepareGet("https://models.dev/models.json").execute { response ->
                        response.bodyAsChannel().toInputStream().use { input ->
                            json.decodeFromStream<Map<String, ModelDto>>(input)
                        }
                    }
                }
                val searchIndex = async {
                    client.prepareGet("https://models.dev/search-index.json").execute { response ->
                        response.bodyAsChannel().toInputStream().use { input ->
                            json.decodeFromStream<List<SearchIndexItemDto>>(input)
                        }
                    }
                }
                CatalogPayload(providers.await(), models.await(), searchIndex.await())
            }
        }
}
