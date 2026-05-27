package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Random
import java.util.concurrent.TimeUnit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// --- Gemini request model definitions using Moshi for simplicity and AGP integration ---
data class Part(val text: String?)
data class Content(val parts: List<Part>)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

data class Candidate(val content: Content)
data class GenerateContentResponse(val candidates: List<Candidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiService {
    private const val TAG = "GeminiService"
    
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    // --- Hardcoded Backup Content ---
    private val BACKUP_DEATH_TAUNTS = listOf(
        "Did you really think that platform was solid? How adorable. 💀",
        "Oh look, gravity works. And so does that trap. 💀",
        "Don't trust the floor, don't trust the doors, don't trust ME! 💀",
        "I'm not saying you're bad, but even a brick falls slower than you. 💀",
        "That 'SAFE' button was totally safe... for me to watch you die! 💀",
        "You walked right into that. Literally. 💀",
        "Oops! Was that a fake wall? My bad... not! 💀",
        "Have you tried NOT falling into spikes? It usually helps. 💀",
        "Your dedication to dying is truly inspiring! 💀",
        "That checkpoint was 100% real. If 'real' means sending you back! 💀",
        "Congratulations on finding another way to lose! 💀",
        "The arrow said right, so you went right. Do you always listen to signs? 💀",
        "Gravity is a harsh mistress, especially when I flip it! 💀",
        "That floor vanished faster than your hopes of winning. 💀",
        "Is your jump button broken, or are you just testing the spikes? 💀",
        "I've seen slugs with better reaction times. Keep trying though! 💀",
        "The exit door is laughing at you from the other side. 💀",
        "Don't cry. The platform only wanted to shrink away from you! 💀",
        "Two identical paths, and you chose the one with spikes. Classic. 💀",
        "You died again. I'd say I'm sorry, but I'd be lying! 💀"
    )

    private val BACKUP_FAKE_HINTS = listOf(
        "Stand on the left side of the block for 2 seconds to unlock safety in levels. 😈",
        "The red spikes are actually trampoline cushions in this level, jump on them! 😈",
        "The exit door wants a hug, walk slowly towards it without jumping. 😈",
        "Press JUMP exactly twice while falling to enable flying mode! 😈",
        "Wait at the start line for 10 seconds to get an invincibility shield. 😈",
        "If you jump directly into the fake wall, it gives you a secret speed boost! 😈"
    )

    private val BACKUP_REAL_HINTS = listOf(
        "Avoid the central platform entirely, leap directly from the start block. 💡",
        "That green button is a trap! Go left and drop through the fake wall tiles. 💡",
        "When gravity flips, do not move! Let yourself land on the ceiling safely. 💡",
        "Step briefly on the disappearing floor, then immediately jump backward! 💡",
        "The checkpoint in the middle is fake. Jump cleanly over it to stay safe. 💡",
        "Walk right up to the door so it moves, then immediately double-back! 💡"
    )

    private val BACKUP_LEVEL_INTROS = listOf(
        "Don't worry, this level is completely straightforward. Honest! 😏",
        "Everything here has been double-checked for your ultimate safety. 😌",
        "A charming little stage with zero tricks. Believe me! 😉",
        "Some floors might feel soft, but that's just premium design. 🤫",
        "Follow the arrows. They have never guided anyone into a pit. 😬"
    )

    private val BACKUP_DIFFICULTY_WARNINGS = listOf(
        "Oh, a showoff! Let's see you survive this next batch of pain! 😡",
        "Flawless run so far? Cute. Prepare for immediate devastation! 🌋",
        "I was going easy on you. Now, the kid gloves are coming off! 😈",
        "Three clean levels? You must be cheating. Time to flip the heat! 🔥"
    )

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun fetchDeathTaunt(deathCount: Int, levelNumber: Int): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "No valid api key. Using static fallback taunt.")
            return@withContext BACKUP_DEATH_TAUNTS[Random().nextInt(BACKUP_DEATH_TAUNTS.size)]
        }

        val prompt = "You are the evil narrator of a trap platformer game called Trust Nobody. The player just died for the $deathCount time on level $levelNumber. Write exactly ONE short funny evil taunt message. Maximum 12 words. Be sarcastic. Mock the player for trusting the game. Use one emoji at the end. Return only the taunt message and nothing else. No explanation. No punctuation besides the emoji."
        
        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(prompt)))),
                systemInstruction = Content(parts = listOf(Part("You are an evil sarcastic narrator in a video game.")))
            )
            val response = apiService.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                return@withContext text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed, using fallback: ${e.message}")
        }
        return@withContext BACKUP_DEATH_TAUNTS[Random().nextInt(BACKUP_DEATH_TAUNTS.size)]
    }

    suspend fun fetchFakeHint(levelNumber: Int, trapsList: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext BACKUP_FAKE_HINTS[Random().nextInt(BACKUP_FAKE_HINTS.size)]
        }

        val prompt = "You are the evil narrator of Trust Nobody game. The player is on level $levelNumber which contains these traps: $trapsList. Write ONE fake hint that sounds completely helpful and convincing but is actually wrong and will trick them into dying. Maximum 15 words. Sound very confident and helpful. Return only the hint and nothing else."
        
        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(prompt))))
            )
            val response = apiService.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                return@withContext text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed for fake hint: ${e.message}")
        }
        return@withContext BACKUP_FAKE_HINTS[Random().nextInt(BACKUP_FAKE_HINTS.size)]
    }

    suspend fun fetchRealHint(levelNumber: Int, safePathDescription: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext BACKUP_REAL_HINTS[Random().nextInt(BACKUP_REAL_HINTS.size)]
        }

        val prompt = "You are a helpful game assistant for Trust Nobody. Player is stuck on level $levelNumber. The real safe path is $safePathDescription. Write ONE genuine helpful hint that guides them without giving the full answer. Maximum 20 words. Be kind and direct. Return only the hint and nothing else."
        
        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(prompt))))
            )
            val response = apiService.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                return@withContext text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed for real hint: ${e.message}")
        }
        return@withContext BACKUP_REAL_HINTS[Random().nextInt(BACKUP_REAL_HINTS.size)]
    }

    suspend fun fetchLevelIntroduction(levelNumber: Int): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext BACKUP_LEVEL_INTROS[levelNumber % BACKUP_LEVEL_INTROS.size]
        }

        val prompt = "You are the evil narrator of Trust Nobody. Write a 1 sentence evil introduction for level $levelNumber. Make it sound like a reassuring warning but be completely misleading. Maximum 15 words. Use one emoji. Return only the sentence and nothing else."
        
        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(prompt))))
            )
            val response = apiService.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                return@withContext text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed for level intro: ${e.message}")
        }
        return@withContext BACKUP_LEVEL_INTROS[levelNumber % BACKUP_LEVEL_INTROS.size]
    }

    suspend fun fetchDynamicDifficultyWarning(streakCount: Int): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext BACKUP_DIFFICULTY_WARNINGS[Random().nextInt(BACKUP_DIFFICULTY_WARNINGS.size)]
        }

        val prompt = "The player of Trust Nobody has completed $streakCount levels without dying. Write ONE short threatening message warning that the difficulty is about to dramatically increase. Evil and funny tone. Maximum 12 words. One emoji. Return only the message."
        
        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(prompt))))
            )
            val response = apiService.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                return@withContext text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed for difficulty warning: ${e.message}")
        }
        return@withContext BACKUP_DIFFICULTY_WARNINGS[Random().nextInt(BACKUP_DIFFICULTY_WARNINGS.size)]
    }
}
