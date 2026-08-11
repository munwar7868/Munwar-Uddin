package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.ActionExecutor
import com.example.ai.GeminiClient
import com.example.commands.ParsedCommand
import com.example.speech.SpeechRecognizerManager
import com.example.tts.UrduTtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InteractionHistory(
    val userQuery: String,
    val assistantReply: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AssistantUiState(
    val userSpeech: String = "",
    val assistantResponse: String = "السلام علیکم! میں منور اے آئی ہوں، آپ کی کیا مدد کر سکتی ہوں؟",
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val isSpeaking: Boolean = false,
    val statusText: String = "مائیکرو فون کا بٹن دبا کر بولیں",
    val pendingConfirmationCommand: ParsedCommand? = null,
    val interactionHistory: List<InteractionHistory> = emptyList(),
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val actionExecutor = ActionExecutor(application.applicationContext)
    private var speechRecognizerManager: SpeechRecognizerManager? = null
    private var ttsManager: UrduTtsManager? = null

    init {
        checkNetworkStatus()
        initTts()
    }

    private fun initTts() {
        ttsManager = UrduTtsManager(getApplication())
    }

    fun initSpeechRecognizer(context: Context) {
        speechRecognizerManager = SpeechRecognizerManager(
            context = context,
            onListeningStateChanged = { listening ->
                _uiState.update {
                    it.copy(
                        isListening = listening,
                        statusText = if (listening) "میں سن رہی ہوں..." else "مائیکرو فون کا بٹن دبا کر بولیں"
                    )
                }
            },
            onPartialResult = { partial ->
                _uiState.update { it.copy(userSpeech = partial) }
            },
            onFinalResult = { final ->
                _uiState.update {
                    it.copy(
                        userSpeech = final,
                        isListening = false,
                        isProcessing = true,
                        statusText = "سمجھ رہی ہوں..."
                    )
                }
                processVoiceCommand(final)
            },
            onErrorOccurred = { error ->
                _uiState.update {
                    it.copy(
                        isListening = false,
                        isProcessing = false,
                        errorMessage = error,
                        statusText = error
                    )
                }
                speakOut(error)
            }
        )
    }

    fun checkNetworkStatus() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(network)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _uiState.update { it.copy(isOffline = !hasInternet) }
    }

    fun onMicrophoneClick() {
        checkNetworkStatus()
        val currentState = _uiState.value
        if (currentState.isListening) {
            speechRecognizerManager?.stopListening()
            _uiState.update { it.copy(isListening = false, statusText = "روک دیا گیا") }
        } else {
            ttsManager?.stop()
            _uiState.update {
                it.copy(
                    userSpeech = "",
                    errorMessage = null,
                    pendingConfirmationCommand = null,
                    isListening = true,
                    statusText = "میں سن رہی ہوں..."
                )
            }
            speechRecognizerManager?.startListening()
        }
    }

    fun processVoiceCommand(commandText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val parsed = GeminiClient.parseVoiceCommand(commandText)

            _uiState.update { it.copy(isProcessing = false) }

            if (parsed.confirmationRequired) {
                _uiState.update {
                    it.copy(
                        pendingConfirmationCommand = parsed,
                        assistantResponse = parsed.spokenResponse,
                        statusText = "تصدیق کی ضرورت ہے"
                    )
                }
                speakOut(parsed.spokenResponse)
            } else {
                executeParsedCommand(parsed)
            }
        }
    }

    fun confirmPendingAction() {
        val pending = _uiState.value.pendingConfirmationCommand
        if (pending != null) {
            _uiState.update { it.copy(pendingConfirmationCommand = null) }
            executeParsedCommand(pending)
        }
    }

    fun cancelPendingAction() {
        _uiState.update {
            it.copy(
                pendingConfirmationCommand = null,
                assistantResponse = "جی، منسوخ کر دیا گیا ہے۔",
                statusText = "منسوخ کر دیا گیا"
            )
        }
        speakOut("جی، منسوخ کر دیا گیا ہے۔")
    }

    private fun executeParsedCommand(command: ParsedCommand) {
        viewModelScope.launch {
            val resultMessage = actionExecutor.execute(command)
            val finalSpoken = if (command.spokenResponse.isNotBlank()) command.spokenResponse else resultMessage

            _uiState.update { state ->
                val newHistory = listOf(
                    InteractionHistory(
                        userQuery = state.userSpeech,
                        assistantReply = finalSpoken
                    )
                ) + state.interactionHistory

                state.copy(
                    assistantResponse = finalSpoken,
                    statusText = "کام مکمل ہو گیا",
                    interactionHistory = newHistory.take(15)
                )
            }
            speakOut(finalSpoken)
        }
    }

    private fun speakOut(text: String) {
        _uiState.update { it.copy(isSpeaking = true) }
        ttsManager?.speak(text) {
            _uiState.update { it.copy(isSpeaking = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager?.stopListening()
        ttsManager?.shutdown()
    }
}
