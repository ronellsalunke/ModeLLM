package dev.ronellsalunke.modellm.ui.util

import android.net.Uri

object ModelsDevLinks {
    const val BASEURL = "https://models.dev"

    // Canonical ids look like "lab/model", so keep the separator unencoded.
    fun model(canonicalId: String): String =
        "$BASEURL/models/${Uri.encode(canonicalId, "/")}"

    fun lab(labId: String): String = "$BASEURL/labs/${Uri.encode(labId)}"

    fun provider(providerId: String): String = "$BASEURL/providers/${Uri.encode(providerId)}"
}
