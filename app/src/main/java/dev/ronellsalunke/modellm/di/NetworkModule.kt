package dev.ronellsalunke.modellm.di

import dev.ronellsalunke.modellm.data.mapper.CatalogMapper
import dev.ronellsalunke.modellm.data.remote.CatalogJson
import dev.ronellsalunke.modellm.data.remote.CatalogRemoteDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single<Json> { CatalogJson }

    single {
        HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(get<Json>())
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 180_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
            }
        }
    }

    single { CatalogRemoteDataSource(get(), get()) }
    single { CatalogMapper(get()) }
}
