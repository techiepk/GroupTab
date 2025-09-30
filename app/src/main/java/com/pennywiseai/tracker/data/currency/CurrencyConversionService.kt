package com.pennywiseai.tracker.data.currency

import com.pennywiseai.tracker.data.database.dao.ExchangeRateDao
import com.pennywiseai.tracker.data.database.entity.ExchangeRateEntity
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyConversionService @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val exchangeRateProvider: ExchangeRateProvider,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    // Cache rates for performance
    private val rateCache = mutableMapOf<String, BigDecimal>()
    private var lastCacheUpdate: LocalDateTime = LocalDateTime.MIN

    /**
     * Convert amount from one currency to another
     */
    suspend fun convertAmount(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        forceRefresh: Boolean = false
    ): BigDecimal {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return amount
        }

        val rate = getExchangeRate(fromCurrency, toCurrency, forceRefresh)
        return if (rate != null) {
            amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        } else {
            amount // Return original amount if conversion fails
        }
    }

    /**
     * Get exchange rate between two currencies
     */
    suspend fun getExchangeRate(
        fromCurrency: String,
        toCurrency: String,
        forceRefresh: Boolean = false
    ): BigDecimal? {
        val cacheKey = "${fromCurrency.uppercase()}_${toCurrency.uppercase()}"

        // Check cache first
        if (!forceRefresh && isCacheValid()) {
            rateCache[cacheKey]?.let { return it }
        }

        // Check database
        val currentTime = LocalDateTime.now()
        val dbRate = exchangeRateDao.getExchangeRate(fromCurrency, toCurrency, currentTime)

        if (dbRate != null && !forceRefresh) {
            updateCache(cacheKey, dbRate.rate)
            return dbRate.rate
        }

        // Fetch from API if not found or forced refresh
        return fetchAndCacheRate(fromCurrency, toCurrency)
    }

    /**
     * Check if we have a valid rate for this currency pair
     */
    suspend fun hasValidRate(fromCurrency: String, toCurrency: String): Boolean {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return true
        }

        val cacheKey = "${fromCurrency.uppercase()}_${toCurrency.uppercase()}"
        if (isCacheValid() && rateCache.containsKey(cacheKey)) {
            return true
        }

        return exchangeRateDao.hasValidRate(fromCurrency, toCurrency) > 0
    }

    /**
     * Refresh exchange rates for an account's currencies
     */
    suspend fun refreshExchangeRatesForAccount(currencies: List<String>) {
        if (currencies.size < 2) return // No conversion needed for single currency

        // Get unique currencies and ensure USD is included for API compatibility
        val uniqueCurrencies = currencies.distinct().toMutableList()
        if (!uniqueCurrencies.contains("USD")) {
            uniqueCurrencies.add("USD")
        }

        refreshExchangeRates(uniqueCurrencies)
    }

    /**
     * Refresh exchange rates for specific currencies using USD as base
     */
    suspend fun refreshExchangeRates(currencies: List<String>) {
        // Use USD as the base currency for the API since it's most commonly supported
        val apiBaseCurrency = "USD"

        // Fetch all exchange rates for the base currency at once
        val allRates = exchangeRateProvider.fetchAllExchangeRates(apiBaseCurrency)

        if (allRates != null) {
            // Cache all relevant rates from the API response
            currencies.forEach { fromCurrency ->
                currencies.forEach { toCurrency ->
                    if (fromCurrency != toCurrency) {
                        val rate = if (fromCurrency == apiBaseCurrency) {
                            allRates[toCurrency]
                        } else if (toCurrency == apiBaseCurrency) {
                            BigDecimal.ONE.divide(allRates[fromCurrency]!!, 6, RoundingMode.HALF_UP)
                        } else {
                            // Cross-currency conversion: fromCurrency -> USD -> toCurrency
                            val fromToUsd = allRates[fromCurrency]
                            val usdToTo = allRates[toCurrency]
                            if (fromToUsd != null && usdToTo != null) {
                                usdToTo.divide(fromToUsd, 6, RoundingMode.HALF_UP)
                            } else {
                                null
                            }
                        }

                        if (rate != null) {
                            val entity = ExchangeRateEntity(
                                fromCurrency = fromCurrency,
                                toCurrency = toCurrency,
                                rate = rate,
                                provider = exchangeRateProvider.getProviderName(),
                                updatedAt = LocalDateTime.now(),
                                expiresAt = LocalDateTime.now().plusHours(24) // Rates expire after 24 hours
                            )
                            exchangeRateDao.insertExchangeRate(entity)
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the base currency for the app
     */
    private suspend fun getBaseCurrency(): String {
        return userPreferencesRepository.baseCurrency.first() ?: "INR"
    }

    /**
     * Fetch exchange rate from API and cache it
     */
    private suspend fun fetchAndCacheRate(fromCurrency: String, toCurrency: String): BigDecimal? {
        try {
            val rate = exchangeRateProvider.fetchExchangeRate(fromCurrency, toCurrency)
            if (rate != null) {
                val entity = ExchangeRateEntity(
                    fromCurrency = fromCurrency,
                    toCurrency = toCurrency,
                    rate = rate,
                    provider = exchangeRateProvider.getProviderName(),
                    updatedAt = LocalDateTime.now(),
                    expiresAt = LocalDateTime.now().plusHours(6) // Rates expire after 6 hours
                )

                exchangeRateDao.insertExchangeRate(entity)
                val cacheKey = "${fromCurrency.uppercase()}_${toCurrency.uppercase()}"
                updateCache(cacheKey, rate)
                return rate
            }
        } catch (e: Exception) {
            // Log error but don't crash
            println("Failed to fetch exchange rate for $fromCurrency to $toCurrency: ${e.message}")
        }

        return null
    }

    /**
     * Update cache with new rate
     */
    private fun updateCache(key: String, rate: BigDecimal) {
        rateCache[key] = rate
        lastCacheUpdate = LocalDateTime.now()
    }

    /**
     * Check if cache is still valid (less than 1 hour old)
     */
    private fun isCacheValid(): Boolean {
        return lastCacheUpdate.isAfter(LocalDateTime.now().minusHours(1))
    }

    /**
     * Clear expired rates from database
     */
    suspend fun cleanupExpiredRates() {
        val expiryTime = LocalDateTime.now().minusDays(7) // Keep rates for 7 days
        exchangeRateDao.deleteExpiredRates(expiryTime)
    }

    /**
     * Get all available currencies with exchange rates
     */
    suspend fun getAvailableCurrencies(): List<String> {
        return exchangeRateDao.getAvailableCurrencies()
    }

    /**
     * Convert multiple amounts to base currency
     */
    suspend fun convertToBaseCurrency(
        transactions: List<TransactionData>,
        baseCurrency: String
    ): Map<String, BigDecimal> {
        val convertedAmounts = mutableMapOf<String, BigDecimal>()

        transactions.forEach { transaction ->
            val convertedAmount = convertAmount(
                amount = transaction.amount,
                fromCurrency = transaction.currency,
                toCurrency = baseCurrency
            )
            convertedAmounts[transaction.id] = convertedAmount
        }

        return convertedAmounts
    }

    data class TransactionData(
        val id: String,
        val amount: BigDecimal,
        val currency: String
    )
}