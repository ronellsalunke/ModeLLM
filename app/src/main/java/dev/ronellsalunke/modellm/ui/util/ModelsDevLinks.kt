package dev.ronellsalunke.modellm.ui.util

import android.net.Uri

object ModelsDevLinks {
    const val BaseUrl = "https://models.dev"

    // Canonical ids look like "lab/model", so keep the separator unencoded.
    fun model(canonicalId: String): String =
        "$BaseUrl/models/${Uri.encode(canonicalId, "/")}"

    fun lab(labId: String): String = "$BaseUrl/labs/${Uri.encode(labId)}"

    fun provider(providerId: String): String = "$BaseUrl/providers/${Uri.encode(providerId)}"
}
