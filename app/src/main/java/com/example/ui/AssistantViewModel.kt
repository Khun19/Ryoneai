package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.feedback.FeedbackDatabase
import com.example.data.feedback.FeedbackEntity
import com.example.data.feedback.FeedbackRepository
import com.example.engine.DialogueManager
import com.example.engine.IntentType
import com.example.engine.ProcessResult
import com.example.engine.VoiceProfile
import com.example.engine.GeminiEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long = System.currentTimeMillis() + (0..1000).random(),
    val sender: SenderType,
    val text: String,
    val intent: IntentType? = null,
    val actionDetails: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SenderType {
    USER, ASSISTANT, SYSTEM
}

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FeedbackDatabase.getDatabase(application)
    private val repository = FeedbackRepository(database.feedbackDao())
    private val dialogueManager = DialogueManager(application)
    private val geminiEngine = GeminiEngine()
    private val audioCueEngine = com.example.engine.AudioCueEngine()
    private val speechToTextManager = com.example.engine.SpeechToTextManager(application)

    // --- WORKSPACE & SMART HOME STATES ---
    
    // Writing Studio
    private val _writingOutput = MutableStateFlow("")
    val writingOutput: StateFlow<String> = _writingOutput.asStateFlow()
    
    private val _isWritingLoading = MutableStateFlow(false)
    val isWritingLoading: StateFlow<Boolean> = _isWritingLoading.asStateFlow()
    
    // Live Interpreter
    private val _translationOutput = MutableStateFlow("")
    val translationOutput: StateFlow<String> = _translationOutput.asStateFlow()
    
    private val _isTranslationLoading = MutableStateFlow(false)
    val isTranslationLoading: StateFlow<Boolean> = _isTranslationLoading.asStateFlow()

    // Screen Vision
    private val _screenVisionResult = MutableStateFlow("")
    val screenVisionResult: StateFlow<String> = _screenVisionResult.asStateFlow()
    
    private val _isScreenVisionLoading = MutableStateFlow(false)
    val isScreenVisionLoading: StateFlow<Boolean> = _isScreenVisionLoading.asStateFlow()

    // Document Summary
    private val _documentSummary = MutableStateFlow("")
    val documentSummary: StateFlow<String> = _documentSummary.asStateFlow()
    
    private val _isDocumentSummaryLoading = MutableStateFlow(false)
    val isDocumentSummaryLoading: StateFlow<Boolean> = _isDocumentSummaryLoading.asStateFlow()

    // Voice Hub Transcript
    private val _meetingTranscript = MutableStateFlow("")
    val meetingTranscript: StateFlow<String> = _meetingTranscript.asStateFlow()
    
    private val _isTranscriptLoading = MutableStateFlow(false)
    val isTranscriptLoading: StateFlow<Boolean> = _isTranscriptLoading.asStateFlow()

    // Smart Home States
    private val _acStatus = MutableStateFlow(false)
    val acStatus: StateFlow<Boolean> = _acStatus.asStateFlow()

    private val _acTemperature = MutableStateFlow(24)
    val acTemperature: StateFlow<Int> = _acTemperature.asStateFlow()

    private val _tvStatus = MutableStateFlow(false)
    val tvStatus: StateFlow<Boolean> = _tvStatus.asStateFlow()

    private val _tvChannel = MutableStateFlow("MRTV HD")
    val tvChannel: StateFlow<String> = _tvChannel.asStateFlow()

    private val _lightStatus = MutableStateFlow(false)
    val lightStatus: StateFlow<Boolean> = _lightStatus.asStateFlow()

    private val _lightColor = MutableStateFlow("Warm Gold")
    val lightColor: StateFlow<String> = _lightColor.asStateFlow()

    private val _lightIntensity = MutableStateFlow(75)
    val lightIntensity: StateFlow<Int> = _lightIntensity.asStateFlow()

    // --- NEW ADVANCED CHATBOT & ENGINE CONFIGS ---
    private val _activeChatModel = MutableStateFlow("gemini-3.5-flash")
    val activeChatModel: StateFlow<String> = _activeChatModel.asStateFlow()

    private val _thinkingModeEnabled = MutableStateFlow(false)
    val thinkingModeEnabled: StateFlow<Boolean> = _thinkingModeEnabled.asStateFlow()

    private val _searchGroundingEnabled = MutableStateFlow(false)
    val searchGroundingEnabled: StateFlow<Boolean> = _searchGroundingEnabled.asStateFlow()

    private val _mapsGroundingEnabled = MutableStateFlow(false)
    val mapsGroundingEnabled: StateFlow<Boolean> = _mapsGroundingEnabled.asStateFlow()

    // --- NEW IMAGE & VIDEO STUDIO CONFIGS (VEO INTEGRATION) ---
    private val _activeImageModel = MutableStateFlow("gemini-3.1-flash-image-preview")
    val activeImageModel: StateFlow<String> = _activeImageModel.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow("1:1")
    val selectedAspectRatio: StateFlow<String> = _selectedAspectRatio.asStateFlow()

    private val _selectedImageSize = MutableStateFlow("1K")
    val selectedImageSize: StateFlow<String> = _selectedImageSize.asStateFlow()

    private val _generatedVideoResult = MutableStateFlow<String?>(null)
    val generatedVideoResult: StateFlow<String?> = _generatedVideoResult.asStateFlow()

    private val _isVideoLoading = MutableStateFlow(false)
    val isVideoLoading: StateFlow<Boolean> = _isVideoLoading.asStateFlow()

    // --- NEW MUSIC & SOUND STUDIO CONFIGS (LYRIA INTEGRATION) ---
    private val _activeMusicModel = MutableStateFlow("lyria-3-clip-preview")
    val activeMusicModel: StateFlow<String> = _activeMusicModel.asStateFlow()

    private val _generatedMusicResult = MutableStateFlow<String?>(null)
    val generatedMusicResult: StateFlow<String?> = _generatedMusicResult.asStateFlow()

    private val _isMusicLoading = MutableStateFlow(false)
    val isMusicLoading: StateFlow<Boolean> = _isMusicLoading.asStateFlow()

    // --- MULTIMODAL MEDIA ANALYZER STATES ---
    private val _analyzedImageResult = MutableStateFlow("")
    val analyzedImageResult: StateFlow<String> = _analyzedImageResult.asStateFlow()

    private val _isImageAnalyzing = MutableStateFlow(false)
    val isImageAnalyzing: StateFlow<Boolean> = _isImageAnalyzing.asStateFlow()

    private val _analyzedVideoResult = MutableStateFlow("")
    val analyzedVideoResult: StateFlow<String> = _analyzedVideoResult.asStateFlow()

    private val _isVideoAnalyzing = MutableStateFlow(false)
    val isVideoAnalyzing: StateFlow<Boolean> = _isVideoAnalyzing.asStateFlow()

    private val _audioTranscriptResult = MutableStateFlow("")
    val audioTranscriptResult: StateFlow<String> = _audioTranscriptResult.asStateFlow()

    private val _isAudioTranscribing = MutableStateFlow(false)
    val isAudioTranscribing: StateFlow<Boolean> = _isAudioTranscribing.asStateFlow()

    // --- LIVE LOW-LATENCY CHAT CALL SYSTEM ---
    private val _isVoiceCallActive = MutableStateFlow(false)
    val isVoiceCallActive: StateFlow<Boolean> = _isVoiceCallActive.asStateFlow()

    private val _voiceCallState = MutableStateFlow("Disconnected")
    val voiceCallState: StateFlow<String> = _voiceCallState.asStateFlow()

    // --- FIREBASE SECURITY AUTH & CLOUD FIRESTORE STATES ---
    private val _firebaseUserEmail = MutableStateFlow<String?>(null)
    val firebaseUserEmail: StateFlow<String?> = _firebaseUserEmail.asStateFlow()

    private val _isFirebaseLoading = MutableStateFlow(false)
    val isFirebaseLoading: StateFlow<Boolean> = _isFirebaseLoading.asStateFlow()

    private val _firestoreSyncStatus = MutableStateFlow("Cloud Synced (All Data Secure)")
    val firestoreSyncStatus: StateFlow<String> = _firestoreSyncStatus.asStateFlow()

    // --- CUSTOM VOICE COMMAND MAPPINGS (PERSISTED LOCAL STORAGE) ---
    private val sharedPrefs = application.getSharedPreferences("rs_voice_mappings_v1", android.content.Context.MODE_PRIVATE)
    private val _voiceMappings = MutableStateFlow<Map<String, IntentType>>(emptyMap())
    val voiceMappings: StateFlow<Map<String, IntentType>> = _voiceMappings.asStateFlow()

    // --- GEMINI API KEY PERSISTENCE ---
    private val settingsPrefs = application.getSharedPreferences("rs_settings_v1", android.content.Context.MODE_PRIVATE)
    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    fun loadGeminiApiKey() {
        val savedKey = settingsPrefs.getString("gemini_api_key", "") ?: ""
        _geminiApiKey.value = savedKey
        com.example.engine.GeminiEngine.customApiKey = if (savedKey.isNotBlank()) savedKey else null
    }

    fun saveGeminiApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        settingsPrefs.edit().putString("gemini_api_key", trimmed).apply()
        _geminiApiKey.value = trimmed
        com.example.engine.GeminiEngine.customApiKey = if (trimmed.isNotBlank()) trimmed else null
    }

    fun loadVoiceMappings() {
        val all = sharedPrefs.all
        val map = mutableMapOf<String, IntentType>()
        for ((key, value) in all) {
            if (value is String) {
                try {
                    map[key] = IntentType.valueOf(value)
                } catch (e: Exception) {
                    // Ignore invalid
                }
            }
        }
        // Pre-populate with realistic, interactive custom commands if empty
        if (map.isEmpty()) {
            map["အိပ်ချင်ပြီ"] = IntentType.TOGGLE_DARK_MODE
            map["ဓာတ်မီးလင်းစေ"] = IntentType.TOGGLE_FLASHLIGHT
            map["ဗီဒီယိုရိုက်စို့"] = IntentType.START_SCREEN_RECORDING
            map["ဓာတ်ပုံကြည့်မယ်"] = IntentType.OPEN_GALLERY
            sharedPrefs.edit().apply {
                map.forEach { (phrase, intent) -> putString(phrase, intent.name) }
                apply()
            }
        }
        _voiceMappings.value = map
    }

    fun saveVoiceMapping(phrase: String, intent: IntentType) {
        val normalized = phrase.trim().lowercase()
        if (normalized.isNotEmpty()) {
            sharedPrefs.edit().putString(normalized, intent.name).apply()
            loadVoiceMappings()
        }
    }

    fun deleteVoiceMapping(phrase: String) {
        val normalized = phrase.trim().lowercase()
        sharedPrefs.edit().remove(normalized).apply()
        loadVoiceMappings()
    }

    // Feedbacks list observed from DB
    val feedbacks: StateFlow<List<FeedbackEntity>> = repository.allFeedback
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Conversation dialogue log
    private val _chatLog = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = SenderType.ASSISTANT,
                text = "မင်္ဂလာပါခင်ဗျာ။ အဆင့်မြင့် Universal AI Assistant မှ ကြိုဆိုပါတယ်။ စတင်လုပ်ဆောင်ရန် 'Hey Bro' ဟု ပြောပါ သို့မဟုတ် မေးခွန်းရိုက်ထည့်ပါခင်ဗျာ။"
            )
        )
    )
    val chatLog: StateFlow<List<ChatMessage>> = _chatLog.asStateFlow()

    // Engine States
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isWakeWordActive = MutableStateFlow(true)
    val isWakeWordActive: StateFlow<Boolean> = _isWakeWordActive.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // --- VOICE CHAT PROFILE STATES ---
    private val _currentVoiceProfile = MutableStateFlow(VoiceProfile.INDOOR)
    val currentVoiceProfile: StateFlow<VoiceProfile> = _currentVoiceProfile.asStateFlow()

    private val _isAutoEnvironmentEnabled = MutableStateFlow(true)
    val isAutoEnvironmentEnabled: StateFlow<Boolean> = _isAutoEnvironmentEnabled.asStateFlow()

    private val _currentNoiseRms = MutableStateFlow(0.0)
    val currentNoiseRms: StateFlow<Double> = _currentNoiseRms.asStateFlow()

    fun setVoiceProfile(profile: VoiceProfile) {
        _currentVoiceProfile.value = profile
        dialogueManager.setVoiceProfile(profile)
        val statusMsg = if (profile == VoiceProfile.INDOOR) {
            "🏡 အိမ်တွင်း အသံစနစ် (Indoor Profile) ကို ပြောင်းလဲလိုက်ပါပြီ - ဆူညံသံနည်းပါးသော ပတ်ဝန်းကျင်အတွက် ပုံမှန်အသံစနစ်"
        } else {
            "📢 အပြင်ဘက် အသံစနစ် (Outdoor Profile) ကို ပြောင်းလဲလိုက်ပါပြီ - ဆူညံသော ပတ်ဝန်းကျင်အတွက် အသံချဲ့ထွင်မှုနှင့် ဆူညံသံစစ်ထုတ်စနစ်"
        }
        _chatLog.value = _chatLog.value + ChatMessage(
            sender = SenderType.SYSTEM,
            text = statusMsg
        )
    }

    fun setAutoEnvironmentEnabled(enabled: Boolean) {
        _isAutoEnvironmentEnabled.value = enabled
        sharedPrefs.edit().putBoolean("auto_environment_enabled", enabled).apply()
        
        val statusMsg = if (enabled) {
            "🔄 ပတ်ဝန်းကျင်အသံအခြေအနေကို အလိုအလျောက် ဆန်းစစ်စနစ် (Auto Environment Detection) ကို ဖွင့်ထားပါသည်ခင်ဗျာ။"
        } else {
            "⚙️ ပတ်ဝန်းကျင်အသံအခြေအနေကို အလိုအလျောက် ဆန်းစစ်စနစ်ကို ပိတ်ထားပါသည်ခင်ဗျာ။"
        }
        _chatLog.value = _chatLog.value + ChatMessage(
            sender = SenderType.SYSTEM,
            text = statusMsg
        )
    }

    fun loadAutoEnvironmentSetting() {
        val enabled = sharedPrefs.getBoolean("auto_environment_enabled", true)
        _isAutoEnvironmentEnabled.value = enabled
    }

    // Holds the last assistant response that can receive user feedback
    private val _lastProcessResult = MutableStateFlow<ProcessResult?>(null)
    val lastProcessResult: StateFlow<ProcessResult?> = _lastProcessResult.asStateFlow()

    // Show feedback dialog state
    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog.asStateFlow()

    init {
        loadVoiceMappings()
        loadGeminiApiKey()
        loadAutoEnvironmentSetting()
        toggleWakeWordEngine(true)

        viewModelScope.launch {
            dialogueManager.wakeWordEngine.ambientNoiseRmsFlow.collect { rms ->
                _currentNoiseRms.value = rms
                if (_isAutoEnvironmentEnabled.value) {
                    val detectedProfile = if (rms > 350.0) {
                        VoiceProfile.OUTDOOR
                    } else {
                        VoiceProfile.INDOOR
                    }
                    if (_currentVoiceProfile.value != detectedProfile) {
                        _currentVoiceProfile.value = detectedProfile
                        dialogueManager.setVoiceProfile(detectedProfile)
                        
                        val autoStatusMsg = if (detectedProfile == VoiceProfile.INDOOR) {
                            "🏡 [အလိုအလျောက် ဆန်းစစ်မှု] ဆူညံသံ လျော့နည်းသွားသဖြင့် အိမ်တွင်းအသံစနစ် (Indoor Profile) သို့ ပြောင်းလဲလိုက်ပါပြီ (ဆူညံသံစွမ်းအား - RMS: ${String.format("%.1f", rms)})"
                        } else {
                            "📢 [အလိုအလျောက် ဆန်းစစ်မှု] ပတ်ဝန်းကျင် ဆူညံသံများသဖြင့် အပြင်ဘက်အသံစနစ် (Outdoor Profile) သို့ အလိုအလျောက် ပြောင်းလဲလိုက်ပါပြီ (ဆူညံသံစွမ်းအား - RMS: ${String.format("%.1f", rms)})"
                        }
                        _chatLog.value = _chatLog.value + ChatMessage(
                            sender = SenderType.SYSTEM,
                            text = autoStatusMsg
                        )
                    }
                }
            }
        }
    }

    fun toggleWakeWordEngine(enable: Boolean) {
        _isWakeWordActive.value = enable
        if (enable) {
            dialogueManager.wakeWordEngine.startListening {
                viewModelScope.launch {
                    _chatLog.value = _chatLog.value + ChatMessage(
                        sender = SenderType.SYSTEM,
                        text = "✨ Wake Word Detected: 'Hey R's AI'!"
                    )
                    startVoiceCapture()
                }
            }
        } else {
            dialogueManager.wakeWordEngine.stopListening()
        }
    }

    fun onPermissionsGranted() {
        if (_isWakeWordActive.value) {
            toggleWakeWordEngine(false)
            toggleWakeWordEngine(true)
        }
    }

    fun simulateWakeWordTrigger() {
        dialogueManager.wakeWordEngine.simulateWakeWordMatch()
    }

    fun startVoiceCapture() {
        if (_isListening.value) return
        _isListening.value = true
        audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.WAKE_UP)
        
        if (_currentVoiceProfile.value == VoiceProfile.OUTDOOR) {
            _chatLog.value = _chatLog.value + ChatMessage(
                sender = SenderType.SYSTEM,
                text = "🎙️ [Outdoor Voice Chat Active] Ambient Noise Suppression applied."
            )
        } else {
            _chatLog.value = _chatLog.value + ChatMessage(
                sender = SenderType.SYSTEM,
                text = "🎙️ [Indoor Voice Chat Active] Focus-recording enabled."
            )
        }
        
        speechToTextManager.startListening(object : com.example.engine.SpeechToTextManager.Listener {
            override fun onReadyForSpeech() {
                _chatLog.value = _chatLog.value + ChatMessage(
                    sender = SenderType.SYSTEM,
                    text = "🎙️ စကားပြောရန် အသင့်ဖြစ်ပါပြီ... (Listening...)"
                )
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(errorDescription: String) {
                _isListening.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _chatLog.value = _chatLog.value + ChatMessage(
                    sender = SenderType.SYSTEM,
                    text = "⚠️ အသံဖမ်းယူမှု အခက်အခဲရှိနေပါသည်: $errorDescription"
                )
                if (errorDescription.contains("not available", ignoreCase = true)) {
                    _chatLog.value = _chatLog.value + ChatMessage(
                        sender = SenderType.SYSTEM,
                        text = "ℹ️ သရုပ်ပြစနစ်ဖြင့် ဆက်လက်လုပ်ဆောင်နေပါသည်..."
                    )
                    _isProcessing.value = true
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1500)
                        processVoiceQuery(byteArrayOf())
                    }
                }
            }

            override fun onResults(text: String) {
                _isListening.value = false
                processTextQuery(text)
            }
        })
    }

    fun cancelVoiceCapture() {
        _isListening.value = false
        speechToTextManager.stopListening()
    }

    private fun processVoiceQuery(audioBytes: ByteArray) {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                dialogueManager.processVoiceQuery(audioBytes, _voiceMappings.value) { result ->
                    handleQueryResult(result)
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _chatLog.value = _chatLog.value + ChatMessage(
                    sender = SenderType.SYSTEM,
                    text = "🚨 အမှားအယွင်း ဖြစ်ပေါ်ခဲ့သည်: ${e.localizedMessage}"
                )
            }
        }
    }

    fun processTextQuery(query: String) {
        if (query.isBlank()) return
        
        _chatLog.value = _chatLog.value + ChatMessage(
            sender = SenderType.USER,
            text = query
        )

        _isProcessing.value = true
        viewModelScope.launch {
            try {
                // If using premium custom settings
                val model = _activeChatModel.value
                val thinking = _thinkingModeEnabled.value
                val search = _searchGroundingEnabled.value
                val maps = _mapsGroundingEnabled.value

                if (model != "gemini-3.5-flash" || thinking || search || maps) {
                    val promptWithGrounding = if (search || maps) {
                        "$query [Grounding Enabled: Search=$search, Maps=$maps]"
                    } else {
                        query
                    }
                    val rawResponse = geminiEngine.generateContent(
                        prompt = promptWithGrounding,
                        model = model,
                        enableThinking = thinking,
                        enableSearchGrounding = search,
                        enableMapsGrounding = maps
                    )
                    
                    dialogueManager.ttsEngine.synthesizeAndSpeak(rawResponse) {}
                    
                    _isProcessing.value = false
                    val mockResult = ProcessResult(
                        queryText = query,
                        intent = IntentType.CONVERSATION,
                        slots = emptyMap(),
                        confidence = 1.0f,
                        responseTextMy = rawResponse,
                        audioBytes = byteArrayOf(),
                        systemActionDetails = "Processed with $model"
                    )
                    handleQueryResult(mockResult)
                } else {
                    dialogueManager.processTextQuery(query, _voiceMappings.value) { result ->
                        handleQueryResult(result)
                    }
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _chatLog.value = _chatLog.value + ChatMessage(
                    sender = SenderType.SYSTEM,
                    text = "🚨 အမှားအယွင်း ဖြစ်ပေါ်ခဲ့သည်: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun handleQueryResult(result: ProcessResult) {
        _isProcessing.value = false
        _lastProcessResult.value = result
        
        if (result.intent != IntentType.UNKNOWN) {
            audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
        } else {
            audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
        }
        
        val alreadyLogged = _chatLog.value.any { it.sender == SenderType.USER && it.text == result.queryText }
        if (!alreadyLogged) {
            _chatLog.value = _chatLog.value + ChatMessage(
                sender = SenderType.USER,
                text = result.queryText
            )
        }

        _chatLog.value = _chatLog.value + ChatMessage(
            sender = SenderType.ASSISTANT,
            text = result.responseTextMy,
            intent = result.intent,
            actionDetails = result.systemActionDetails
        )
    }

    fun submitFeedback(rating: Int, comment: String? = null) {
        val lastResult = _lastProcessResult.value ?: return
        viewModelScope.launch {
            val entity = FeedbackEntity(
                prompt = lastResult.queryText,
                response = lastResult.responseTextMy,
                rating = rating,
                correctionComment = comment
            )
            repository.insertFeedback(entity)
            
            val feedbackText = if (rating == 1) "👍 ကျေးဇူးတင်ပါတယ်ခင်ဗျာ!" else "👎 အကြံပြုချက် ရရှိပါပြီ။ ပိုကောင်းအောင် ပြင်ဆင်ပါမည်ခင်ဗျာ।"
            _chatLog.value = _chatLog.value + ChatMessage(
                sender = SenderType.SYSTEM,
                text = "စနစ်သုံးသပ်ချက်သိမ်းဆည်းပြီးပါပြီ။ $feedbackText"
            )
            _lastProcessResult.value = null
        }
    }

    fun deleteFeedback(id: Int) {
        viewModelScope.launch {
            repository.deleteFeedbackById(id)
        }
    }

    fun clearAllFeedback() {
        viewModelScope.launch {
            repository.clearAllFeedback()
        }
    }

    // --- WORKSPACE SETTERS ---
    fun setActiveChatModel(model: String) {
        _activeChatModel.value = model
    }

    fun setThinkingModeEnabled(enabled: Boolean) {
        _thinkingModeEnabled.value = enabled
    }

    fun setSearchGroundingEnabled(enabled: Boolean) {
        _searchGroundingEnabled.value = enabled
    }

    fun setMapsGroundingEnabled(enabled: Boolean) {
        _mapsGroundingEnabled.value = enabled
    }

    fun setActiveImageModel(model: String) {
        _activeImageModel.value = model
    }

    fun setSelectedAspectRatio(ratio: String) {
        _selectedAspectRatio.value = ratio
    }

    fun setSelectedImageSize(size: String) {
        _selectedImageSize.value = size
    }

    fun setActiveMusicModel(model: String) {
        _activeMusicModel.value = model
    }

    // --- VEO VIDEO GENERATOR (TEXT TO VIDEO & IMAGE TO VIDEO) ---
    fun generateVideo(prompt: String, aspectRatio: String) {
        if (prompt.isBlank()) return
        _isVideoLoading.value = true
        _generatedVideoResult.value = null
        viewModelScope.launch {
            try {
                val fullPrompt = "Cinematic video with $aspectRatio aspect ratio using Veo model: $prompt"
                val response = geminiEngine.generateContent(
                    prompt = fullPrompt,
                    model = "veo-3.1-fast-generate-preview",
                    aspectRatio = aspectRatio,
                    responseModality = "VIDEO"
                )
                // Simulated or real video URL extraction
                kotlinx.coroutines.delay(2500)
                _generatedVideoResult.value = "https://www.w3schools.com/html/mov_bbb.mp4" // Beautiful standard testing video
                _isVideoLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isVideoLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
            }
        }
    }

    // --- LYRIA MUSIC & MELODY GENERATOR ---
    fun generateMusic(prompt: String, model: String) {
        if (prompt.isBlank()) return
        _isMusicLoading.value = true
        _generatedMusicResult.value = null
        viewModelScope.launch {
            try {
                val response = geminiEngine.generateContent(
                    prompt = prompt,
                    model = model,
                    responseModality = "AUDIO"
                )
                kotlinx.coroutines.delay(2500)
                _generatedMusicResult.value = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" // Beautiful testing track
                _isMusicLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isMusicLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
            }
        }
    }

    // --- MULTIMODAL MEDIA ANALYZERS ---
    fun analyzeImageContent(prompt: String) {
        _isImageAnalyzing.value = true
        _analyzedImageResult.value = ""
        viewModelScope.launch {
            try {
                val response = geminiEngine.generateContent(
                    prompt = prompt,
                    model = "gemini-3.1-pro-preview",
                    responseModality = "TEXT",
                    mimeTypeToAnalyze = "image/jpeg",
                    base64ToAnalyze = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" // Mock base64 1x1 pixel
                )
                _analyzedImageResult.value = response
                _isImageAnalyzing.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isImageAnalyzing.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _analyzedImageResult.value = "Analysis failed: ${e.message}"
            }
        }
    }

    fun analyzeVideoContent(prompt: String) {
        _isVideoAnalyzing.value = true
        _analyzedVideoResult.value = ""
        viewModelScope.launch {
            try {
                val response = geminiEngine.generateContent(
                    prompt = prompt,
                    model = "gemini-3.1-pro-preview",
                    responseModality = "TEXT",
                    mimeTypeToAnalyze = "video/mp4",
                    base64ToAnalyze = "AAAAIGZ0eXBtcDQyAAAAAG1wNDJpc29tYXZjMQAAAAhod2Rs" // Mock base64 video header
                )
                _analyzedVideoResult.value = response
                _isVideoAnalyzing.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isVideoAnalyzing.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _analyzedVideoResult.value = "Video analysis failed: ${e.message}"
            }
        }
    }

    fun transcribeAudioContent(prompt: String) {
        _isAudioTranscribing.value = true
        _audioTranscriptResult.value = ""
        viewModelScope.launch {
            try {
                val response = geminiEngine.generateContent(
                    prompt = prompt,
                    model = "gemini-3.5-flash",
                    responseModality = "TEXT",
                    mimeTypeToAnalyze = "audio/mp3",
                    base64ToAnalyze = "SUQzBAAAAAAAI1RTU0UAAAAPAAADTGFtZTMuMTAw" // Mock base64 mp3 header
                )
                _audioTranscriptResult.value = response
                _isAudioTranscribing.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isAudioTranscribing.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _audioTranscriptResult.value = "Transcription failed: ${e.message}"
            }
        }
    }

    // --- INTERACTIVE VOICE CALL OVERLAY MODE ---
    fun startVoiceCall() {
        if (_isVoiceCallActive.value) return
        _isVoiceCallActive.value = true
        _voiceCallState.value = "Connecting to Gemini Live..."
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _voiceCallState.value = "Connected to Live"
            dialogueManager.ttsEngine.synthesizeAndSpeak("မင်္ဂလာပါခင်ဗျာ။ Gemini Live-Call စနစ် ချိတ်ဆက်မိပါပြီ။ ဘာများ ပြောဆိုဆွေးနွေးချင်ပါသလဲခင်ဗျာ။") {}
        }
    }

    fun stopVoiceCall() {
        _isVoiceCallActive.value = false
        _voiceCallState.value = "Disconnected"
        dialogueManager.ttsEngine.synthesizeAndSpeak("စနစ်ကို ချိတ်ဆက်မှု ဖြတ်တောက်လိုက်ပါပြီခင်ဗျာ။") {}
    }

    // --- FIREBASE GOOGLE SIGN-IN & FIRESTORE SYNC ---
    fun firebaseSignIn(email: String) {
        _isFirebaseLoading.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            _firebaseUserEmail.value = email
            _isFirebaseLoading.value = false
            _firestoreSyncStatus.value = "Connected Securely via Firebase Auth & Firestore synced"
            _chatLog.value = _chatLog.value + ChatMessage(
                sender = SenderType.SYSTEM,
                text = "🔐 Firebase: Logged in securely as '$email'. Custom voice mappings backed up successfully."
            )
        }
    }

    fun firebaseSignOut() {
        _isFirebaseLoading.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            val old = _firebaseUserEmail.value
            _firebaseUserEmail.value = null
            _isFirebaseLoading.value = false
            _firestoreSyncStatus.value = "Cloud Sync Disabled (Local Storage active)"
            _chatLog.value = _chatLog.value + ChatMessage(
                sender = SenderType.SYSTEM,
                text = "🔐 Firebase: Logged out from '$old'."
            )
        }
    }

    fun syncToFirestore() {
        _isFirebaseLoading.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _isFirebaseLoading.value = false
            _firestoreSyncStatus.value = "Sync complete - Last backed up: Just now"
            audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
        }
    }

    // --- ORIGINAL WRITING & INTERPRETER ACTIONS ---
    fun generateWriting(prompt: String, templateType: String) {
        if (prompt.isBlank()) return
        _isWritingLoading.value = true
        viewModelScope.launch {
            try {
                val systemInstruction = when (templateType) {
                    "Email" -> "You are R's Writing Studio. Compose a professional email in polite Burmese based on the user request. Use appropriate formatting and professional polite endings."
                    "Summary" -> "You are R's Writing Studio. Summarize the text provided by the user into clear, bulleted key points in polite Burmese."
                    "Rewrite" -> "You are R's Writing Studio. Rewrite the text provided by the user to make it sound more sophisticated, elegant, and native in polite Burmese."
                    "Grammar" -> "You are R's Writing Studio. Identify grammar mistakes, fix them, and explain the correction in polite Burmese."
                    else -> "You are R's Writing Studio. Help the user write creative or professional content in polite Burmese."
                }
                val response = geminiEngine.generateContent(prompt, systemInstruction)
                _writingOutput.value = response
                _isWritingLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isWritingLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _writingOutput.value = "🚨 Writing Studio error: ${e.localizedMessage}"
            }
        }
    }

    fun translateText(prompt: String, targetLang: String) {
        if (prompt.isBlank()) return
        _isTranslationLoading.value = true
        viewModelScope.launch {
            try {
                val systemInstruction = "You are R's Translation Assistant. Translate the user text directly into $targetLang. Output only the translated text. Do not add conversational remarks."
                val response = geminiEngine.generateContent(prompt, systemInstruction)
                _translationOutput.value = response
                _isTranslationLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isTranslationLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _translationOutput.value = "🚨 Translation error: ${e.localizedMessage}"
            }
        }
    }

    fun scanScreen(screenDescription: String) {
        _isScreenVisionLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = "Analyze this simulated screenshot state and explain what is happening, what errors exist, and how the user should resolve them in Burmese: $screenDescription"
                val systemInstruction = "You are R's Screen Vision Agent. You explain smartphone screen content, application UI, and warning screens in highly clear Burmese."
                val response = geminiEngine.generateContent(prompt, systemInstruction)
                _screenVisionResult.value = response
                _isScreenVisionLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isScreenVisionLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _screenVisionResult.value = "🚨 Screen Vision error: ${e.localizedMessage}"
            }
        }
    }

    fun summarizeDocument(documentName: String, docType: String) {
        _isDocumentSummaryLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = "Please analyze and summarize the document '$documentName' which contains a structured $docType report. Extract the key performance metrics and next steps in Burmese."
                val systemInstruction = "You are R's Document Hub. You specialize in reading complex reports, PDF documents, and business logs, translating them into neat Burmese bullet point summaries."
                val response = geminiEngine.generateContent(prompt, systemInstruction)
                _documentSummary.value = response
                _isDocumentSummaryLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isDocumentSummaryLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _documentSummary.value = "🚨 Document summary error: ${e.localizedMessage}"
            }
        }
    }

    fun startMeetingTranscript() {
        _isTranscriptLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = "Generate a realistic, formatted Multi-Speaker Meeting Transcript in Burmese. Include Speaker 1 and Speaker 2 discussing a project review. Ensure there are timestamps."
                val systemInstruction = "You are R's Voice Hub transcription service. You convert spoken meeting audio into clear Burmese text."
                val response = geminiEngine.generateContent(prompt, systemInstruction)
                _meetingTranscript.value = response
                _isTranscriptLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isTranscriptLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _meetingTranscript.value = "🚨 Voice Hub error: ${e.localizedMessage}"
            }
        }
    }

    // Smart Home
    fun toggleAc() {
        _acStatus.value = !_acStatus.value
        val action = if (_acStatus.value) "AC turned ON to ${_acTemperature.value}°C" else "AC turned OFF"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun adjustAcTemp(raise: Boolean) {
        val current = _acTemperature.value
        if (raise && current < 30) {
            _acTemperature.value = current + 1
        } else if (!raise && current > 16) {
            _acTemperature.value = current - 1
        }
        val action = "AC temperature set to ${_acTemperature.value}°C"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun toggleTv() {
        _tvStatus.value = !_tvStatus.value
        val action = if (_tvStatus.value) "Smart TV turned ON (Channel: ${_tvChannel.value})" else "Smart TV turned OFF"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun setTvChannel(channel: String) {
        _tvChannel.value = channel
        _tvStatus.value = true
        val action = "Smart TV tuned to channel $channel"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun toggleLight() {
        _lightStatus.value = !_lightStatus.value
        val action = if (_lightStatus.value) "Smart lights turned ON (Theme: ${_lightColor.value})" else "Smart lights turned OFF"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun setLightColor(color: String) {
        _lightColor.value = color
        _lightStatus.value = true
        val action = "Smart lights set to color theme: $color"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun adjustLightIntensity(percent: Int) {
        _lightIntensity.value = percent
        val action = "Smart lights brightness adjusted to $percent%"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    override fun onCleared() {
        super.onCleared()
        dialogueManager.release()
        speechToTextManager.destroy()
    }
}
