package com.pennywiseai.tracker.database

import androidx.room.*
import com.pennywiseai.tracker.data.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    
    @Query("SELECT * FROM subscriptions WHERE status = 'ACTIVE' ORDER BY nextPaymentDate ASC")
    fun getActiveSubscriptions(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscriptions WHERE status = 'ACTIVE' ORDER BY nextPaymentDate ASC")
    suspend fun getActiveSubscriptionsSync(): List<Subscription>
    
    @Query("SELECT * FROM subscriptions ORDER BY merchantName ASC")
    fun getAllSubscriptions(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscriptions WHERE nextPaymentDate <= :date AND status = 'ACTIVE'")
    suspend fun getUpcomingSubscriptions(date: Long): List<Subscription>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)
    
    @Update
    suspend fun updateSubscription(subscription: Subscription)
    
    @Delete
    suspend fun deleteSubscription(subscription: Subscription)
    
    @Query("SELECT * FROM subscriptions WHERE merchantName = :merchantName LIMIT 1")
    suspend fun getSubscriptionByMerchant(merchantName: String): Subscription?
    
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    fun getSubscriptionById(id: String): Flow<Subscription?>
    
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionByIdSync(id: String): Subscription?
    
    @Query("DELETE FROM subscriptions")
    suspend fun deleteAllSubscriptions()
    
    @Query("SELECT * FROM subscriptions")
    suspend fun getAllSubscriptionsList(): List<Subscription>
}