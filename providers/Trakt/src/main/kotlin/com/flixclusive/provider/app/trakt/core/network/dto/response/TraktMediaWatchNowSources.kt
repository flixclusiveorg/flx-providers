package com.flixclusive.provider.app.trakt.core.network.dto.response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Currency

@Serializable
internal data class TraktMediaWatchNowSources(
    @SerialName("cable") val cable: List<WatchNowItem>,
    @SerialName("cinema") val cinema: List<WatchNowItem>,
    @SerialName("free") val free: List<WatchNowItem>,
    @SerialName("purchase") val purchase: List<WatchNowItem>,
    @SerialName("subscription") val subscription: List<WatchNowItem>
) {
    val isEmpty get() = cable.isEmpty()
        && cinema.isEmpty()
        && free.isEmpty()
        && purchase.isEmpty()
        && subscription.isEmpty()

    val all = listOf(cable, cinema, free, purchase, subscription).flatten()
}

@Serializable
internal data class WatchNowItem(
    @SerialName("source") val id: String,
    @SerialName("currency") val currency: String,
    @SerialName("link") val link: String,
    @SerialName("uhd") val uhd: Boolean,
    @SerialName("prices") val prices: Prices,
    @SerialName("link_direct") val linkDirect: String?,
    @SerialName("link_android") val linkAndroid: String?,
) {
    /**
     * A user-friendly description that should include most properties of the source, such as:
     * - Whether it's UHD or not (e.g., "UHD")
     * - The price if available (e.g., "Rent for $3.99", "Buy for $12.99")
     * */
    val description: String
        get() {
            val priceInfo = when {
                prices.rent != null -> "Rent for ${getCurrencySymbol(currency)}${prices.rent}"
                prices.buy != null -> "Buy for ${getCurrencySymbol(currency)}${prices.buy}"
                else -> "Stream"
            }

            return listOfNotNull(
                if (uhd) "UHD" else null,
                priceInfo
            ).joinToString(" | ")
        }

    private fun getCurrencySymbol(currencyCode: String): String {
        return try {
            Currency.getInstance(currencyCode).symbol
        } catch (e: IllegalArgumentException) {
            currencyCode // fallback to code itself
        }
    }
}

@Serializable
internal data class Prices(
    @SerialName("rent") val rent: Double?,
    @SerialName("purchase") val buy: Double?
)