package dev.ronellsalunke.modellm.data.remote

import dev.ronellsalunke.modellm.data.remote.dto.ProviderDto
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

class CatalogRemoteDataSource(
    private val client: HttpClient,
    private val json: Json,
) {
    /**
     * Streams and decodes the single `api.json` document. The root object is keyed by
     * provider id; the provider id is injected when the payload omits the duplicate field.
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun fetchCatalog(includeAllTypes: Boolean = false): List<ProviderDto> =
        withContext(Dispatchers.IO) {
            client.prepareGet(CATALOG_URL) {
                if (includeAllTypes) {
                    parameter("type", "all")
                }
            }.execute { response ->
                response.bodyAsChannel().toInputStream().use { input ->
                    json.decodeFromStream<Map<String, ProviderDto>>(input)
                        .map { (providerId, provider) ->
                            if (provider.id.isNullOrBlank()) provider.copy(id = providerId) else provider
                        }
                }
            }
        }

    private companion object {
        const val CATALOG_URL = "https://models.dev/api.json"
    }
}
