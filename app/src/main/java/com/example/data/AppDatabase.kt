package com.example.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        ChatMessage::class,
        FreeApi::class,
        FreelanceOpportunity::class,
        TechNewsItem::class,
        ClientLead::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun freeApiDao(): FreeApiDao
    abstract fun freelanceOpportunityDao(): FreelanceOpportunityDao
    abstract fun techNewsDao(): TechNewsDao
    abstract fun clientLeadDao(): ClientLeadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agent_hub_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
