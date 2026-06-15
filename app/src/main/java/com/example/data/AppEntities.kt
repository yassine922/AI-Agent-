package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. Entities ---

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val agentId: String, // "api_agent", "dev_agent", "opp_agent", "news_agent"
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "free_apis")
data class FreeApi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val provider: String,
    val description: String,
    val category: String,
    val url: String,
    val integrationCode: String,
    val isFavorite: Boolean = false,
    val fallbackName: String = "",
    val fallbackUrl: String = "",
    val fallbackDescription: String = "",
    val fallbackIntegrationCode: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "freelance_opportunities")
data class FreelanceOpportunity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val budget: String,
    val platform: String,
    val description: String,
    val requiredSkills: String,
    val matchPercent: Int,
    val sampleProposalText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tech_news")
data class TechNewsItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val summary: String,
    val source: String,
    val url: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// --- 2. DAOs ---

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE agentId = :agentId ORDER BY timestamp ASC")
    fun getChatHistory(agentId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE agentId = :agentId")
    suspend fun clearChatHistory(agentId: String)
}

@Dao
interface FreeApiDao {
    @Query("SELECT * FROM free_apis ORDER BY timestamp DESC")
    fun getAllApisFlow(): Flow<List<FreeApi>>

    @Query("SELECT * FROM free_apis WHERE id = :id LIMIT 1")
    suspend fun getApiById(id: Int): FreeApi?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApi(api: FreeApi)

    @Update
    suspend fun updateApi(api: FreeApi)

    @Query("DELETE FROM free_apis WHERE id = :id")
    suspend fun deleteApi(id: Int)

    @Query("SELECT COUNT(*) FROM free_apis")
    suspend fun getApisCount(): Int
}

@Dao
interface FreelanceOpportunityDao {
    @Query("SELECT * FROM freelance_opportunities ORDER BY timestamp DESC")
    fun getAllOpportunitiesFlow(): Flow<List<FreelanceOpportunity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpportunity(opp: FreelanceOpportunity)

    @Query("DELETE FROM freelance_opportunities WHERE id = :id")
    suspend fun deleteOpportunity(id: Int)

    @Query("SELECT COUNT(*) FROM freelance_opportunities")
    suspend fun getOpportunitiesCount(): Int
}

@Dao
interface TechNewsDao {
    @Query("SELECT * FROM tech_news ORDER BY timestamp DESC")
    fun getAllNewsFlow(): Flow<List<TechNewsItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: TechNewsItem)

    @Query("DELETE FROM tech_news WHERE id = :id")
    suspend fun deleteNews(id: Int)
}

@Entity(tableName = "client_leads")
data class ClientLead(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val businessName: String,
    val industry: String,
    val painPoint: String,
    val mobileAppConcept: String,
    val coldOutreachMessage: String,
    val conversionPotential: String, // "High", "Medium", "Low"
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ClientLeadDao {
    @Query("SELECT * FROM client_leads ORDER BY timestamp DESC")
    fun getAllLeadsFlow(): Flow<List<ClientLead>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: ClientLead)

    @Query("DELETE FROM client_leads WHERE id = :id")
    suspend fun deleteLead(id: Int)

    @Query("SELECT COUNT(*) FROM client_leads")
    suspend fun getLeadsCount(): Int
}

