package dev.ronellsalunke.modellm.ui.util

import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.data.model.Modality
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private const val Thousand = 1_000
private const val Million = 1_000_000
private const val MinimumCompactPrice = 0.01
private const val SecondsPerMinute = 60
private const val SecondsPerHour = 3_600
private const val SecondsPerDay = 86_400

fun groupedNumber(value: Number): String = NumberFormat.getIntegerInstance(Locale.US).format(value)

fun pluralized(count: Int, singular: String, plural: String = "${singular}s"): String =
    "${groupedNumber(count)} ${if (count == 1) singular else plural}"

fun tokenLimit(value: Int?): String =
    when {
        value == null -> "–"
        value >= Million -> "${scaledTokens(value, Million, 2)}M"
        value >= Thousand -> "${scaledTokens(value, Thousand, 1)}K"
        else -> groupedNumber(value)
    }

private fun scaledTokens(value: Int, divisor: Int, scale: Int): String =
    BigDecimal(value)
        .divide(BigDecimal(divisor), scale, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

fun price(value: Double?): String =
    value?.let {
        "$" + BigDecimal.valueOf(it).stripTrailingZeros().toPlainString()
    } ?: "–"

fun compactPrice(value: Double?): String =
    when {
        value == null -> "–"
        value == 0.0 -> "$0.00"
        kotlin.math.abs(value) < MinimumCompactPrice -> "<$0.01"
        else -> "$" + BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

fun compactPricePair(input: Double?, output: Double?): String =
    "${compactPrice(input)} / ${compactPrice(output)}"

fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val seconds = ((now - timestamp) / Thousand).coerceAtLeast(0)
    return when {
        seconds < SecondsPerMinute -> "just now"
        seconds < SecondsPerHour -> "${seconds / SecondsPerMinute}m ago"
        seconds < SecondsPerDay -> "${seconds / SecondsPerHour}h ago"
        else -> "${seconds / SecondsPerDay}d ago"
    }
}

fun modalityLabel(modality: Modality): String =
    when (modality) {
        Modality.Pdf -> "PDF"
        else -> modality.name
    }

fun capabilityLabel(capability: Capability): String =
    when (capability) {
        Capability.Reasoning -> "Reasoning"
        Capability.ToolCall -> "Tools"
        Capability.StructuredOutput -> "Structured output"
        Capability.Attachment -> "Attachments"
        Capability.Temperature -> "Temperature"
    }
