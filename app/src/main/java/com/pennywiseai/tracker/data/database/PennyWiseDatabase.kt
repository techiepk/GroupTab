package com.pennywiseai.tracker.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pennywiseai.tracker.data.database.converter.Converters
import com.pennywiseai.tracker.data.database.dao.TransactionDao
import com.pennywiseai.tracker.data.database.entity.TransactionEntity

/**
 * The PennyWise Room database.
 * 
 * This database stores all financial transaction data locally on the device.
 * 
 * @property version Current database version. Increment this when making schema changes.
 * @property entities List of all entities (tables) in the database.
 * @property exportSchema Set to true in production to export schema for version control.
 * @property autoMigrations List of automatic migrations between versions.
 */
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true,
    autoMigrations = [
        // Example: AutoMigration(from = 1, to = 2, spec = PennyWiseDatabase.Migration1To2::class)
    ]
)
@TypeConverters(Converters::class)
abstract class PennyWiseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    
    companion object {
        const val DATABASE_NAME = "pennywise_database"
        
        /**
         * Manual migration from version 1 to 2.
         * Example of how to write manual migrations when auto-migration isn't sufficient.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Example: Add a new column
                // db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT")
            }
        }
    }
    
    /**
     * Example AutoMigrationSpec for renaming tables or columns.
     * Uncomment and modify when needed.
     */
    // @RenameTable(fromTableName = "transactions", toTableName = "user_transactions")
    // @RenameColumn(
    //     tableName = "transactions",
    //     fromColumnName = "merchant_name", 
    //     toColumnName = "vendor_name"
    // )
    // class Migration1To2 : AutoMigrationSpec {
    //     override fun onPostMigrate(db: SupportSQLiteDatabase) {
    //         // Perform additional operations after migration if needed
    //         // Example: Update default values, create indexes, etc.
    //     }
    // }
}