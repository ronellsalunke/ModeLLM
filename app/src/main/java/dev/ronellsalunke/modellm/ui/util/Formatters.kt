package dev.ronellsalunke.modellm.ui.util

import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.data.model.Modality
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

fun groupedNumber(value: Number): String = NumberFormat.getIntegerInstance(Locale.US).format(value)

fun pluralized(count: Int, singular: String, plural: String = "${singular}s"): String =
    "${groupedNumber(count)} ${if (count == 1) singular else plural}"

fun tokenLimit(value: Int?): String = when {
    value == null -> "–"
    value >= 1_000_000 -> "${BigDecimal(value).divide(BigDecimal(1_000_000), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}M"
    value >= 1_000 -> "${BigDecimal(value).divide(BigDecimal(1_000), 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}K"
    else -> groupedNumber(value)
}

fun price(value: Double?): String = value?.let {
    "$" + BigDecimal.valueOf(it).stripTrailingZeros().toPlainString()
} ?: "–"

fun pricePair(input: Double?, output: Double?): String = "${price(input)} / ${price(output)}"

fun compactPrice(value: Double?): String {
    if (value == null) return "–"
    if (value == 0.0) return "$0.00"
    if (kotlin.math.abs(value) < 0.01) return "<$0.01"
    return "$" + BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
}

fun compactPricePair(input: Double?, output: Double?): String = "${compactPrice(input)} / ${compactPrice(output)}"

fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val seconds = ((now - timestamp) / 1_000).coerceAtLeast(0)
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}

fun modalityLabel(modality: Modality): String = when (modality) {
    Modality.Pdf -> "PDF"
    else -> modality.name
}

fun capabilityLabel(capability: Capability): String = when (capability) {
    Capability.Reasoning -> "Reasoning"
    Capability.ToolCall -> "Tools"
    Capability.StructuredOutput -> "Structured output"
    Capability.Attachment -> "Attachments"
    Capability.Temperature -> "Temperature"
}
