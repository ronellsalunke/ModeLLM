package dev.ronellsalunke.modellm.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

/**
 * Standalone FTS4 index so search covers model, family, description, provider name,
 * and the provider-qualified model id. Rows are rebuilt during each catalogue sync.
 */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "models_fts")
data class ModelSearchEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long,
    val providerId: String,
    val modelId: String,
    val name: String,
    val family: String,
    val description: String,
    val providerName: String,
)
