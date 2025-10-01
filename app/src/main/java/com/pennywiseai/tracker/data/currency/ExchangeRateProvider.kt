package com.pennywiseai.tracker.data.currency

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Interface for exchange rate providers
 */
interface ExchangeRateProvider {
    suspend fun fetchExchangeRate(fromCurrency: String, toCurrency: String): BigDecimal?
    suspend fun fetchAllExchangeRates(baseCurrency: String): Map<String, BigDecimal>?
    fun getProviderName(): String
    suspend fun getSupportedCurrencies(): List<String>
}

/**
 * Implementation using the free ExchangeRate-API
 * Uses https://open.er-api.com/v6/latest/ endpoint
 */
class FreeExchangeRateProvider @Inject constructor() : ExchangeRateProvider {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
    }

    override suspend fun fetchExchangeRate(fromCurrency: String, toCurrency: String): BigDecimal? {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return BigDecimal.ONE
        }

        return try {
            // For direct conversion, fetch all rates for the base currency
            val rates = fetchAllExchangeRates(fromCurrency)
            rates?.get(toCurrency.uppercase())
        } catch (e: Exception) {
            println("Failed to fetch exchange rate from API: ${e.message}")
            null
        }
    }

    override suspend fun fetchAllExchangeRates(baseCurrency: String): Map<String, BigDecimal>? {
        return try {
            withContext(Dispatchers.IO) {
                val response = client.get("https://open.er-api.com/v6/latest/${baseCurrency.uppercase()}") {
                    header("User-Agent", "PennyWise/1.0")
                }

                val exchangeRateResponse: ExchangeRateApiResponse = response.body()

                if (exchangeRateResponse.result == "success") {
                    exchangeRateResponse.rates.mapValues { (_, rate) ->
                        BigDecimal(rate).setScale(6, RoundingMode.HALF_UP)
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            println("Failed to fetch exchange rates from API: ${e.message}")
            null
        }
    }

    override fun getProviderName(): String {
        return "ExchangeRate-API"
    }

    override suspend fun getSupportedCurrencies(): List<String> {
        return try {
            val rates = fetchAllExchangeRates("USD")
            rates?.keys?.toList() ?: listOf(
                "AED", "USD", "EUR", "GBP", "INR", "THB", "MYR", "SGD", "KWD", "KRW",
                "CAD", "AUD", "JPY", "CNY", "NPR", "ETB"
            )
        } catch (e: Exception) {
            listOf(
                "AED", "USD", "EUR", "GBP", "INR", "THB", "MYR", "SGD", "KWD", "KRW",
                "CAD", "AUD", "JPY", "CNY", "NPR", "ETB"
            )
        }
    }
}

/**
 * Data class for parsing ExchangeRate-API response
 */
@Serializable
data class ExchangeRateApiResponse(
    val result: String,
    val provider: String,
    val documentation: String,
    val terms_of_use: String,
    val time_last_update_unix: Long,
    val time_last_update_utc: String,
    val time_next_update_unix: Long,
    val time_next_update_utc: String,
    val time_eol_unix: Long,
    val base_code: String,
    val rates: Map<String, Double>
)

/**
 * Factory for creating exchange rate providers
 */
object ExchangeRateProviderFactory {
    fun createProvider(): ExchangeRateProvider {
        return FreeExchangeRateProvider()
    }
}