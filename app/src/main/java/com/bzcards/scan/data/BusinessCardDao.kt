package com.bzcards.scan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bzcards.scan.model.BusinessCard
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessCardDao {
    @Query("SELECT * FROM business_cards ORDER BY scannedAt DESC")
    fun getAllCards(): Flow<List<BusinessCard>>

    @Query("SELECT * FROM business_cards WHERE id = :id")
    suspend fun getCardById(id: Long): BusinessCard?

    @Insert
    suspend fun insert(card: BusinessCard): Long

    @Update
    suspend fun update(card: BusinessCard)

    @Delete
    suspend fun delete(card: BusinessCard)
}
