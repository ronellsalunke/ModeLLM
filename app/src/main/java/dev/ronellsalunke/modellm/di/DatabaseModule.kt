package dev.ronellsalunke.modellm.di

import androidx.room.Room
import dev.ronellsalunke.modellm.data.local.ModeLLMDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            ModeLLMDatabase::class.java,
            "modellm.db",
        ).build()
    }

    single { get<ModeLLMDatabase>().providerDao() }
    single { get<ModeLLMDatabase>().modelDao() }
    single { get<ModeLLMDatabase>().catalogDao() }
}
