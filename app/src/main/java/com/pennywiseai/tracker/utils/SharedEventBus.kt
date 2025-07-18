package com.pennywiseai.tracker.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.util.Log

/**
 * Shared event bus for cross-ViewModel communication
 * Used to notify all ViewModels about global events like SMS scan completion
 */
object SharedEventBus {
    
    sealed class Event {
        object SmsScanCompleted : Event()
        data class TransactionDeleted(val transactionId: String) : Event()
        data class TransactionUpdated(val transactionId: String) : Event()
        object SubscriptionsUpdated : Event()
        object GroupsUpdated : Event()
    }
    
    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<Event> = _events.asSharedFlow()
    
    suspend fun emit(event: Event) {
        _events.emit(event)
    }
}