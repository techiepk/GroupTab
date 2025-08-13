package com.pennywiseai.tracker.di

import android.content.Context
import androidx.room.Room
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.dao.ChatDao
import com.pennywiseai.tracker.data.database.dao.MerchantMappingDao
import com.pennywiseai.tracker.data.database.dao.SubscriptionDao
import com.pennywiseai.tracker.data.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides the singleton instance of PennyWiseDatabase.
     * 
     * @param context Application context
     * @return Configured Room database instance
     */
    @Provides
    @Singleton
    fun providePennyWiseDatabase(
        @ApplicationContext context: Context
    ): PennyWiseDatabase {
        return Room.databaseBuilder(
            context,
            PennyWiseDatabase::class.java,
            PennyWiseDatabase.DATABASE_NAME
        )
            // Add manual migrations here when needed
            // .addMigrations(PennyWiseDatabase.MIGRATION_1_2)
            
            // Enable auto-migrations
            // Room will automatically detect schema changes between versions
            
            // Fallback strategy for development
            // In production, remove this and handle all migrations properly
            .fallbackToDestructiveMigration(dropAllTables = true)
            
            // Allow queries on main thread for debugging (remove in production)
            // .allowMainThreadQueries()
            
            .build()
    }
    
    /**
     * Provides the TransactionDao from the database.
     * 
     * @param database The PennyWiseDatabase instance
     * @return TransactionDao for accessing transaction data
     */
    @Provides
    @Singleton
    fun provideTransactionDao(database: PennyWiseDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    /**
     * Provides the SubscriptionDao from the database.
     * 
     * @param database The PennyWiseDatabase instance
     * @return SubscriptionDao for accessing subscription data
     */
    @Provides
    @Singleton
    fun provideSubscriptionDao(database: PennyWiseDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }
    
    /**
     * Provides the ChatDao from the database.
     * 
     * @param database The PennyWiseDatabase instance
     * @return ChatDao for accessing chat message data
     */
    @Provides
    @Singleton
    fun provideChatDao(database: PennyWiseDatabase): ChatDao {
        return database.chatDao()
    }
    
    /**
     * Provides the MerchantMappingDao from the database.
     * 
     * @param database The PennyWiseDatabase instance
     * @return MerchantMappingDao for accessing merchant mapping data
     */
    @Provides
    @Singleton
    fun provideMerchantMappingDao(database: PennyWiseDatabase): MerchantMappingDao {
        return database.merchantMappingDao()
    }
}