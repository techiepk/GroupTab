package com.pennywiseai.tracker.data

/**
 * Data classes for paginated transaction loading
 */
data class PaginatedTransactionResult(
    val transactions: List<Transaction>,
    val hasMore: Boolean,
    val totalCount: Int,
    val currentPage: Int
)

data class PaginationConfig(
    val pageSize: Int = 20,
    val prefetchDistance: Int = 10,
    val initialLoadSize: Int = 40
)

/**
 * Pagination state for tracking load operations
 */
data class PaginationState(
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 0,
    val error: String? = null
)

/**
 * Transaction filters for queries
 */
data class TransactionFilters(
    val category: TransactionCategory? = null,
    val merchant: String? = null,
    val dateRange: Pair<Long, Long>? = null,
    val searchQuery: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
)

/**
 * Sort options for transactions
 */
enum class TransactionSortOption {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC,
    MERCHANT_ASC,
    CATEGORY_ASC
}

/**
 * Load hint for optimizing database queries
 */
enum class LoadHint {
    INITIAL_LOAD,    // First page load
    NEXT_PAGE,       // Loading next page
    REFRESH,         // Pull to refresh
    FILTER_CHANGE    // Filter applied
}