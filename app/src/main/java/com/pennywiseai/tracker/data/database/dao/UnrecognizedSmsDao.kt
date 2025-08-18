package com.pennywiseai.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pennywiseai.tracker.data.database.entity.UnrecognizedSmsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO for managing unrecognized SMS messages from potential financial providers.
 */
@Dao
interface UnrecognizedSmsDao {
    
    @Insert
    suspend fun insert(sms: UnrecognizedSmsEntity): Long
    
    @Query("SELECT * FROM unrecognized_sms WHERE reported = 0 ORDER BY received_at DESC")
    fun getAllUnreported(): Flow<List<UnrecognizedSmsEntity>>
    
    @Query("SELECT * FROM unrecognized_sms WHERE reported = 0 ORDER BY received_at DESC LIMIT 1")
    suspend fun getFirstUnreported(): UnrecognizedSmsEntity?
    
    @Query("SELECT COUNT(*) FROM unrecognized_sms WHERE reported = 0")
    fun getUnreportedCount(): Flow<Int>
    
    @Query("UPDATE unrecognized_sms SET reported = 1 WHERE id IN (:ids)")
    suspend fun markAsReported(ids: List<Long>)
    
    @Query("DELETE FROM unrecognized_sms WHERE received_at < :cutoffDate")
    suspend fun deleteOldEntries(cutoffDate: LocalDateTime)
    
    @Query("DELETE FROM unrecognized_sms")
    suspend fun deleteAll()
    
    @Query("DELETE FROM unrecognized_sms WHERE id = :id")
    suspend fun deleteById(id: Long)
}