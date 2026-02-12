package com.bzcards.scan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bzcards.scan.model.BusinessCard

@Database(entities = [BusinessCard::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessCardDao(): BusinessCardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bzcards_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
