package com.pennywiseai.tracker.utils

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.ViewTreeObserver

/**
 * Utility class to apply common RecyclerView optimizations
 * Features:
 * - Optimal layout manager settings
 * - Efficient item animations
 * - Memory optimizations
 * - Scroll performance improvements
 */
object RecyclerViewOptimizer {
    
    /**
     * Apply all performance optimizations to a RecyclerView
     */
    fun optimizeRecyclerView(
        recyclerView: RecyclerView,
        hasFixedSize: Boolean = true,
        enableNestedScrolling: Boolean = false,
        itemAnimationDuration: Long = 150L
    ) {
        recyclerView.apply {
            // Fixed size optimization - if item size doesn't change
            setHasFixedSize(hasFixedSize)
            
            // Disable nested scrolling for better performance in nested layouts
            isNestedScrollingEnabled = enableNestedScrolling
            
            // Optimize item animator
            optimizeItemAnimator(this, itemAnimationDuration)
            
            // Set optimal drawing cache
            setItemViewCacheSize(20) // Cache more views for smooth scrolling
            setDrawingCacheEnabled(true)
            setDrawingCacheQuality(android.view.View.DRAWING_CACHE_QUALITY_HIGH)
            
            // Optimize layout manager
            if (layoutManager is LinearLayoutManager) {
                optimizeLinearLayoutManager(layoutManager as LinearLayoutManager)
            }
            
            // Add scroll optimization
            addOnScrollListener(ScrollOptimizationListener())
        }
    }
    
    /**
     * Optimize RecyclerView specifically for large datasets with pagination
     */
    fun optimizeForLargeDataset(
        recyclerView: RecyclerView,
        prefetchDistance: Int = 10
    ) {
        recyclerView.apply {
            // Increase cache size for large datasets
            setItemViewCacheSize(30)
            
            // Enable predictive animations for smooth insertions
            (itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false
            
            // Set layout manager with prefetch optimization
            if (layoutManager is LinearLayoutManager) {
                (layoutManager as LinearLayoutManager).apply {
                    initialPrefetchItemCount = prefetchDistance
                    isItemPrefetchEnabled = true
                }
            }
            
            // Add pagination scroll listener
            addOnScrollListener(PaginationScrollListener(prefetchDistance))
        }
    }
    
    /**
     * Optimize for memory-constrained devices
     */
    fun optimizeForLowMemory(recyclerView: RecyclerView) {
        recyclerView.apply {
            // Reduce cache size for memory efficiency
            setItemViewCacheSize(10)
            
            // Disable drawing cache to save memory
            setDrawingCacheEnabled(false)
            
            // Disable item animations to reduce memory allocations
            itemAnimator = null
            
            // Add memory cleanup listener
            addOnScrollListener(MemoryOptimizationListener())
        }
    }
    
    private fun optimizeItemAnimator(recyclerView: RecyclerView, duration: Long) {
        val animator = DefaultItemAnimator().apply {
            addDuration = duration
            removeDuration = duration
            moveDuration = duration
            changeDuration = duration
            
            // Disable change animations for better performance
            supportsChangeAnimations = false
        }
        recyclerView.itemAnimator = animator
    }
    
    private fun optimizeLinearLayoutManager(layoutManager: LinearLayoutManager) {
        layoutManager.apply {
            // Enable predictive item animations
            isItemPrefetchEnabled = true
            
            // Set reasonable prefetch count
            initialPrefetchItemCount = 4
        }
    }
    
    /**
     * Scroll listener for general performance optimizations
     */
    private class ScrollOptimizationListener : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            
            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> {
                    // Re-enable drawing optimizations when idle
                    recyclerView.setDrawingCacheEnabled(true)
                }
                RecyclerView.SCROLL_STATE_DRAGGING, 
                RecyclerView.SCROLL_STATE_SETTLING -> {
                    // Disable expensive operations during scroll
                    // (This is where you could pause image loading etc.)
                }
            }
        }
    }
    
    /**
     * Scroll listener for pagination support
     */
    private class PaginationScrollListener(
        private val prefetchDistance: Int
    ) : RecyclerView.OnScrollListener() {
        
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            val adapter = recyclerView.adapter ?: return
            
            // Check if we need to load more items
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            
            // Trigger pagination when approaching end
            if ((visibleItemCount + firstVisibleItemPosition) >= (totalItemCount - prefetchDistance)) {
                // Notify adapter about need for more data
                (adapter as? PaginationAdapter)?.onLoadMoreRequested()
            }
        }
    }
    
    /**
     * Memory optimization listener
     */
    private class MemoryOptimizationListener : RecyclerView.OnScrollListener() {
        private var isScrolling = false
        
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            
            isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE
            
            if (!isScrolling) {
                // Force garbage collection when scroll stops (sparingly)
                System.gc()
            }
        }
    }
    
    /**
     * Interface for adapters that support pagination
     */
    interface PaginationAdapter {
        fun onLoadMoreRequested()
    }
    
    /**
     * Helper to defer RecyclerView initialization until layout is ready
     */
    fun deferInitialization(
        recyclerView: RecyclerView,
        initialization: () -> Unit
    ) {
        if (recyclerView.width > 0) {
            initialization()
        } else {
            recyclerView.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        initialization()
                    }
                }
            )
        }
    }
}