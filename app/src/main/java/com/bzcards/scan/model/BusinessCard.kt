package com.bzcards.scan.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_cards")
data class BusinessCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    val rawText: String = "",
    val scannedAt: Long = System.currentTimeMillis()
)
