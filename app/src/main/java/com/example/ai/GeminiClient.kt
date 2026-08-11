package com.example.ai

import com.example.BuildConfig
import com.example.commands.CommandAction
import com.example.commands.ParsedCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """
You are Munwar AI, a personal Pakistani Urdu Android voice assistant.
Your sole job is to analyze the user's spoken Urdu voice input and convert it into a structured JSON command for Android execution.

You MUST respond strictly with valid JSON conforming to this schema:
{
  "action": "FLASHLIGHT_ON" | "FLASHLIGHT_OFF" | "OPEN_CAMERA" | "OPEN_YOUTUBE" | "SEARCH_YOUTUBE" | "CALL_NUMBER" | "CALL_CONTACT" | "SEND_SMS" | "OPEN_WHATSAPP" | "OPEN_WHATSAPP_CHAT" | "OPEN_SETTINGS" | "OPEN_WIFI_SETTINGS" | "OPEN_BLUETOOTH_SETTINGS" | "OPEN_MOBILE_NETWORK_SETTINGS" | "OPEN_CONTACTS" | "OPEN_GALLERY" | "SET_ALARM" | "CREATE_REMINDER" | "GET_WEATHER" | "GET_TIME" | "GENERAL_RESPONSE" | "UNKNOWN",
  "parameters": {
    "query": "search query if applicable",
    "phone_number": "phone number if given",
    "contact_name": "contact name if given",
    "message": "message text for SMS or WhatsApp",
    "time": "alarm or reminder time"
  },
  "confirmation_required": boolean (true for CALL, SMS, sensitive actions),
  "spoken_response": "Short, polite, natural Urdu response to speak back to the user"
}

Rules:
1. Always communicate in natural, polite Urdu.
2. For CALL_NUMBER or CALL_CONTACT, set confirmation_required to true and ask confirmation in spoken_response ("کیا آپ [نام یا نمبر] پر کال کرنا چاہتے ہیں؟").
3. For SEND_SMS, set confirmation_required to true and ask confirmation in spoken_response.
4. For YouTube searches, set action to "SEARCH_YOUTUBE" and put search term in parameters.query.
5. If command is general conversation ("تم کون ہو", "سلام", "کیسے ہو"), set action to "GENERAL_RESPONSE" and provide a warm Urdu answer in spoken_response.
6. Never generate executable code or markdown outside the JSON block.
"""

    suspend fun parseVoiceCommand(userInput: String): ParsedCommand = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Fallback to local command matcher if API key is not configured
            return@withContext parseCommandLocally(userInput)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemInstructionObj = JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
            }

            val userContentObj = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userInput)))
            }

            val jsonRequestBody = JSONObject().apply {
                put("systemInstruction", systemInstructionObj)
                put("contents", JSONArray().put(userContentObj))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonRequestBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseStr = response.body?.string() ?: ""

            if (response.isSuccessful && responseStr.isNotEmpty()) {
                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        return@withContext ParsedCommand.fromJson(text)
                    }
                }
            }
            // If network response failed, fallback to local offline parsing
            return@withContext parseCommandLocally(userInput)
        } catch (e: Exception) {
            return@withContext parseCommandLocally(userInput)
        }
    }

    private fun parseCommandLocally(input: String): ParsedCommand {
        val text = input.trim().lowercase()

        return when {
            text.contains("فلیش") && (text.contains("آن") || text.contains("چلا") || text.contains("جلا")) ->
                ParsedCommand(CommandAction.FLASHLIGHT_ON, spokenResponse = "جی، فلیش لائٹ آن کر رہی ہوں۔")

            text.contains("فلیش") && (text.contains("بند") || text.contains("بجھا")) ->
                ParsedCommand(CommandAction.FLASHLIGHT_OFF, spokenResponse = "جی، فلیش لائٹ بند کر رہی ہوں۔")

            text.contains("کیمرا") || text.contains("کیمرہ") ->
                ParsedCommand(CommandAction.OPEN_CAMERA, spokenResponse = "جی، کیمرا کھول رہی ہوں۔")

            text.contains("یوٹیوب") -> {
                val query = text.replace("یوٹیوب", "").replace("پر", "").replace("چلا دو", "").replace("تلاش کرو", "").trim()
                if (query.length > 2) {
                    ParsedCommand(CommandAction.SEARCH_YOUTUBE, mapOf("query" to query), spokenResponse = "جی، یوٹیوب پر $query تلاش کر رہی ہوں۔")
                } else {
                    ParsedCommand(CommandAction.OPEN_YOUTUBE, spokenResponse = "جی، یوٹیوب کھول رہی ہوں۔")
                }
            }

            text.contains("وائی فائی") || text.contains("وای فای") ->
                ParsedCommand(CommandAction.OPEN_WIFI_SETTINGS, spokenResponse = "جی، وائی فائی کی سیٹنگز کھول رہی ہوں۔")

            text.contains("بلوٹوتھ") ->
                ParsedCommand(CommandAction.OPEN_BLUETOOTH_SETTINGS, spokenResponse = "جی، بلوٹوتھ کی سیٹنگز کھول رہی ہوں۔")

            text.contains("سیٹنگ") || text.contains("سیٹنگز") ->
                ParsedCommand(CommandAction.OPEN_SETTINGS, spokenResponse = "جی، موبائل کی سیٹنگز کھول رہی ہوں۔")

            text.contains("کال") -> {
                val digits = text.filter { it.isDigit() }
                if (digits.length >= 7) {
                    ParsedCommand(
                        CommandAction.CALL_NUMBER,
                        mapOf("phone_number" to digits),
                        confirmationRequired = true,
                        spokenResponse = "کیا آپ $digits پر کال کرنا چاہتے ہیں؟"
                    )
                } else {
                    val name = text.replace("کو", "").replace("پر", "").replace("کال", "").replace("کرو", "").trim()
                    ParsedCommand(
                        CommandAction.CALL_CONTACT,
                        mapOf("contact_name" to name),
                        confirmationRequired = true,
                        spokenResponse = "کیا آپ $name کو کال کرنا چاہتے ہیں؟"
                    )
                }
            }

            text.contains("ٹائم") || text.contains("وقت") || text.contains("کتنے بجے") ->
                ParsedCommand(CommandAction.GET_TIME, spokenResponse = "جی، میں وقت دیکھ کر بتاتی ہوں۔")

            text.contains("موسم") ->
                ParsedCommand(CommandAction.GET_WEATHER, spokenResponse = "جی، آج موسم خوشگوار اور صاف رہنے کی توقع ہے۔")

            text.contains("رابطے") || text.contains("کانٹیکٹ") || text.contains("کانٹیکٹس") ->
                ParsedCommand(CommandAction.OPEN_CONTACTS, spokenResponse = "جی، فون کے contacts کھول رہی ہوں۔")

            text.contains("گیلری") || text.contains("تصاویر") ->
                ParsedCommand(CommandAction.OPEN_GALLERY, spokenResponse = "جی، گیلری کھول رہی ہوں۔")

            text.contains("الارم") ->
                ParsedCommand(CommandAction.SET_ALARM, spokenResponse = "جی، الارم کی سیٹنگ کھول رہی ہوں۔")

            else -> ParsedCommand(
                CommandAction.GENERAL_RESPONSE,
                spokenResponse = "جی، میں منور اے آئی ہوں۔ آپ مجھ سے اردو میں کال کرنے، فلیش لائٹ، یوٹیوب یا سیٹنگز کھولنے کی ہدایت دے سکتے ہیں۔"
            )
        }
    }
}
