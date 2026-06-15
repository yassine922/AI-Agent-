package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// --- Gemini Request Data Classes ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiConfig(
    @Json(name = "temperature") val temperature: Double? = 0.5,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 2048,
    @Json(name = "responseMimeType") val responseMimeType: String? = null
)

// --- Gemini Response Data Classes ---

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

// --- Retrofit API Service Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Singleton Client API ---

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Executes content generation against Gemini 3.5 Flash
     */
    suspend fun getGeminiResponse(
        prompt: String,
        systemInstruction: String? = null,
        isJsonResult: Boolean = false
    ): String {
        // Fallback API Key check
        val rawKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        val apiKey = rawKey.ifBlank { "MOCK_API_KEY_OR_MISSING" }

        if (apiKey.contains("MY_GEMINI_API_KEY") || apiKey == "MOCK_API_KEY_OR_MISSING") {
            return "تنبيـــه: لم يتم إدخال مفتاح الـ GEMINI_API_KEY بشكل صحيح من خلال لوحة أسرار AI Studio (Secrets Panel).\nيرجى فتح لوحة الأسرار وكتابة المفتاح، أو تعديل ملف .env لتشغيل المحادثات التفاعلية.\n\n[رد تجريبي محاكى للمساعد المستقل]: استفسرت عن: \"$prompt\" - كوكيل ذكي مخصص للعمل الحر، يمكنني مساعدتك في هذه المهمة بمجرد تكوين المفتاح لربط الذكاء الاصطناعي مباشرة!"
        }

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = systemInstruction?.let { GeminiContent(parts = listOf(GeminiPart(text = it))) },
            generationConfig = if (isJsonResult) GeminiConfig(responseMimeType = "application/json") else null
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "خطأ: لم يتم تلقي أي رد من نموذج الذكاء الاصطناعي."
        } catch (e: Exception) {
            "عذراً، حدث خطأ أثناء الاتصال بالذكاء الاصطناعي: ${e.localizedMessage}\nيرجى التأكد من اتصال الإنترنت وإعداد مفتاح API بشكل صحيح."
        }
    }
}
