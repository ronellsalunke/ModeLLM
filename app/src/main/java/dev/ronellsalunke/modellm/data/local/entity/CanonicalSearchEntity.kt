package dev.ronellsalunke.modellm.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "canonical_models_fts")
data class CanonicalSearchEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long,
    val canonicalId: String,
    val name: String,
    val labName: String,
    val family: String,
    val description: String,
    val providerNames: String,
)
