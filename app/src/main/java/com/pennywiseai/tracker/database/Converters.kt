package com.pennywiseai.tracker.database

import androidx.room.TypeConverter
import com.pennywiseai.tracker.data.SubscriptionFrequency
import com.pennywiseai.tracker.data.SubscriptionStatus
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.TransactionType
import com.pennywiseai.tracker.data.GroupingType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    
    @TypeConverter
    fun fromTransactionCategory(category: TransactionCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toTransactionCategory(category: String): TransactionCategory {
        return TransactionCategory.valueOf(category)
    }
    
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }
    
    @TypeConverter
    fun toTransactionType(type: String): TransactionType {
        return TransactionType.valueOf(type)
    }
    
    @TypeConverter
    fun fromSubscriptionFrequency(frequency: SubscriptionFrequency): String {
        return frequency.name
    }
    
    @TypeConverter
    fun toSubscriptionFrequency(frequency: String): SubscriptionFrequency {
        return SubscriptionFrequency.valueOf(frequency)
    }
    
    @TypeConverter
    fun fromSubscriptionStatus(status: SubscriptionStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toSubscriptionStatus(status: String): SubscriptionStatus {
        return SubscriptionStatus.valueOf(status)
    }
    
    @TypeConverter
    fun fromGroupingType(type: GroupingType): String {
        return type.name
    }
    
    @TypeConverter
    fun toGroupingType(type: String): GroupingType {
        return GroupingType.valueOf(type)
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}