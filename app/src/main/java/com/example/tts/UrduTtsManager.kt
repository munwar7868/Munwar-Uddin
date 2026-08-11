package com.example.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class UrduTtsManager(
    context: Context,
    private val onInitResult: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val urduLocale = Locale("ur", "PK")
            val result = tts?.setLanguage(urduLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to generic Urdu or default language
                tts?.setLanguage(Locale("ur"))
            }
            tts?.setSpeechRate(0.9f)
            tts?.setPitch(1.0f)
            isInitialized = true
            onInitResult(true)
        } else {
            isInitialized = false
            onInitResult(false)
        }
    }

    fun speak(text: String, onDone: () -> Unit = {}) {
        if (!isInitialized || tts == null) {
            onDone()
            return
        }

        try {
            tts?.stop()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MunwarTtsId")
        } catch (e: Exception) {
            onDone()
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {}
    }
}
