package dev.ronellsalunke.modellm.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json

    @TypeConverter
    fun fromStringList(value: List<String>?): String = json.encodeToString(value.orEmpty())

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
    }
}
