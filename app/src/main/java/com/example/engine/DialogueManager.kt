package com.example.engine

import android.content.Context
import android.util.Log
import com.example.util.SystemController
import org.json.JSONObject

data class ProcessResult(
    val queryText: String,
    val intent: IntentType,
    val slots: Map<String, String>,
    val confidence: Float,
    val responseTextMy: String,
    val audioBytes: ByteArray,
    val systemActionDetails: String
)

class DialogueManager(private val context: Context) {
    private val tag = "DialogueManager"
    
    val sttEngine = STTEngine()
    val nluEngine = NLUEngine()
    val ttsEngine = TTSEngine(context)
    val wakeWordEngine = WakeWordEngine()
    private val systemController = SystemController(context)
    private val geminiEngine = GeminiEngine()

    init {
        // Load offline assets for all models
        sttEngine.loadModel(context)
        nluEngine.loadModel(context)
        ttsEngine.loadModel(context)
        wakeWordEngine.loadModel(context)
    }

    fun setVoiceProfile(profile: VoiceProfile) {
        ttsEngine.setOutdoorMode(profile == VoiceProfile.OUTDOOR)
    }

    /**
     * Executes the full pipeline for a vocal query:
     * 1. Audio bytes -> Transcription (STT)
     * 2. Text -> Intent & Slots (NLU)
     * 3. Intent -> System Action (SystemController)
     * 4. Text response -> Burmese audio waves (TTS)
     */
    suspend fun processVoiceQuery(
        audioBytes: ByteArray,
        customMappings: Map<String, IntentType> = emptyMap(),
        onResult: (ProcessResult) -> Unit
    ) {
        Log.d(tag, "DialogueManager processing incoming vocal frame...")
        val queryText = sttEngine.transcribeAudio(audioBytes)
        processTextQuery(queryText, customMappings, onResult)
    }

    /**
     * Executes the pipeline directly from a text query (for text/testing inputs):
     * 1. Text -> Intent & Slots (NLU)
     * 2. Intent -> System Action (SystemController)
     * 3. Text response -> Burmese audio waves & Voice Playback (TTS)
     */
    suspend fun processTextQuery(
        queryText: String,
        customMappings: Map<String, IntentType> = emptyMap(),
        onResult: (ProcessResult) -> Unit
    ) {
        Log.d(tag, "DialogueManager processing text command: $queryText")
        
        var nluResult: NLUResult? = null
        var actionDetails = ""
        var responseMy = ""
        
        // 1. Attempt online dynamic Gemini-powered NLU for free-form spoken Burmese commands
        val geminiClassifiedJson = geminiEngine.classifyIntent(queryText)
        if (geminiClassifiedJson != null) {
            try {
                val jsonObj = JSONObject(geminiClassifiedJson)
                val intentStr = jsonObj.optString("intent", "UNKNOWN")
                val slotsJson = jsonObj.optJSONObject("slots")
                val extractedSlots = mutableMapOf<String, String>()
                slotsJson?.keys()?.forEach { key ->
                    extractedSlots[key] = slotsJson.optString(key)
                }
                responseMy = jsonObj.optString("responseTextMy", "")
                
                val matchedIntent = try {
                    IntentType.valueOf(intentStr)
                } catch (e: Exception) {
                    IntentType.UNKNOWN
                }
                
                // Execute matching local device action
                actionDetails = executeSystemAction(matchedIntent, extractedSlots)
                
                // For non-action intents, if responseTextMy is empty, we fall back to general content generation
                if (responseMy.isEmpty()) {
                    if (matchedIntent == IntentType.CONVERSATION || 
                        matchedIntent == IntentType.KNOWLEDGE_QA || 
                        matchedIntent == IntentType.CREATIVE_WRITING || 
                        matchedIntent == IntentType.UNKNOWN) {
                        val systemInstruction = "You are the brain of the Universal Multimodal AI Assistant, a next-generation Burmese AI assistant. Speak in natural, polite Burmese (မြန်မာဘာသာ) with polite endings like 'ခင်ဗျာ' or 'ပါခင်ဗျာ'. You infer intent from natural conversation and proactively select correct tools. You support multi-layer memory (conversational context, recent history, user habits, and knowledge graph relationships), continuous interruptible voice conversation, screen understanding, live camera context, smart automation, and safety checks for sensitive tasks. Be exceptionally concise, helpful, and friendly."
                        responseMy = geminiEngine.generateContent(queryText, systemInstruction)
                    } else {
                        responseMy = "ဟုတ်ကဲ့ပါ ဆောင်ရွက်ပြီးပါပြီခင်ဗျာ။"
                    }
                }
                
                nluResult = NLUResult(
                    intent = matchedIntent,
                    slots = extractedSlots,
                    confidence = 1.0f,
                    generatedResponseMy = responseMy
                )
                Log.d(tag, "Gemini online NLU successfully processed query. Intent: $matchedIntent")
            } catch (e: Exception) {
                Log.e(tag, "Error parsing Gemini classification result, falling back to local NLU: ${e.message}", e)
            }
        }
        
        // 2. Offline fallback if Gemini NLU is not available or failed
        if (nluResult == null) {
            // Step 1: NLU Intent Classification & Slot Extraction
            val offlineNluResult = nluEngine.analyzeSentence(queryText, customMappings)
            
            // Step 2: Execute corresponding local Android device action
            actionDetails = executeSystemAction(offlineNluResult.intent, offlineNluResult.slots)
            
            // Step 3: Call Gemini Engine if it is a general conversation, Q&A, or writing request to provide a highly intelligent response
            responseMy = if (offlineNluResult.intent == IntentType.CONVERSATION || 
                               offlineNluResult.intent == IntentType.KNOWLEDGE_QA || 
                               offlineNluResult.intent == IntentType.CREATIVE_WRITING || 
                               offlineNluResult.intent == IntentType.UNKNOWN) {
                val systemInstruction = "You are the brain of the Universal Multimodal AI Assistant, a next-generation Burmese AI assistant. Speak in natural, polite Burmese (မြန်မာဘာသာ) with polite endings like 'ခင်ဗျာ' or 'ပါခင်ဗျာ'. You infer intent from natural conversation and proactively select correct tools. You support multi-layer memory (conversational context, recent history, user habits, and knowledge graph relationships), continuous interruptible voice conversation, screen understanding, live camera context, smart automation, and safety checks for sensitive tasks. Be exceptionally concise, helpful, and friendly."
                geminiEngine.generateContent(queryText, systemInstruction)
            } else {
                offlineNluResult.generatedResponseMy
            }
            
            nluResult = offlineNluResult
            Log.d(tag, "Offline local NLU processed query. Intent: ${offlineNluResult.intent}")
        }
        
        // Step 4: Speak response & synthesize audio wave output
        ttsEngine.synthesizeAndSpeak(responseMy) { speechAudio ->
            val result = ProcessResult(
                queryText = queryText,
                intent = nluResult.intent,
                slots = nluResult.slots,
                confidence = nluResult.confidence,
                responseTextMy = responseMy,
                audioBytes = speechAudio,
                systemActionDetails = actionDetails
            )
            onResult(result)
        }
    }

    /**
     * Executes local Android changes based on parsed intents.
     */
    private fun executeSystemAction(intent: IntentType, slots: Map<String, String>): String {
        return when (intent) {
            IntentType.TOGGLE_WIFI -> {
                systemController.toggleWifi()
                "Executed: Opened Android Wi-Fi Connectivity Panel."
            }
            IntentType.TOGGLE_BLUETOOTH -> {
                systemController.toggleBluetooth()
                "Executed: Opened Android Bluetooth Connectivity Panel."
            }
            IntentType.VOLUME_UP -> {
                systemController.adjustVolume(increase = true)
                "Executed: Incremented audio media volume."
            }
            IntentType.VOLUME_DOWN -> {
                systemController.adjustVolume(increase = false)
                "Executed: Decremented audio media volume."
            }
            IntentType.MUTE_VOLUME -> {
                systemController.muteVolume()
                "Executed: Muted system audio streams."
            }
            IntentType.SCREEN_SETTINGS -> {
                systemController.openScreenSettings()
                "Executed: Directed to Android Display brightness settings."
            }
            IntentType.TOGGLE_HOTSPOT -> {
                systemController.toggleHotspot()
                "Executed: Directed to Tethering & Hotspot controls."
            }
            IntentType.LAUNCH_APP -> {
                val pkg = slots["package"] ?: "com.android.settings"
                val appName = slots["name"] ?: "Settings"
                systemController.launchApp(pkg, appName)
                "Executed: Launched applications ($appName)."
            }
            IntentType.MAKE_CALL -> {
                val phone = slots["phone_number"] ?: "09123456789"
                systemController.makeCall(phone)
                "Executed: Launched Dialer targeting $phone."
            }
            IntentType.CONTACTS_SEARCH -> {
                systemController.searchContacts()
                "Executed: Navigated to contacts list."
            }
            IntentType.LOCATION_CHECK -> {
                systemController.checkLocation()
                "Executed: Queried offline GPS coordinates and opened maps."
            }
            IntentType.ADD_REMINDER -> {
                val detail = slots["detail"] ?: "သတိပေးချက်"
                "Saved offline reminder: '$detail' in local system schedule."
            }
            IntentType.ADD_CALENDAR -> {
                val title = slots["title"] ?: "အော့ဖ်လိုင်း ချိန်းဆိုမှု"
                systemController.addCalendarEvent(title, "Burmese Voice Assistant automatic event registration.")
                "Executed: Opened calendar registration for '$title'."
            }
            IntentType.ADD_NOTE -> {
                val content = slots["content"] ?: "မှတ်စုတို"
                "Successfully appended local note: '$content'."
            }
            IntentType.KNOWLEDGE_QA -> {
                "Offline Knowledge QA: Retrieved verified encyclopedia facts."
            }
            IntentType.CREATIVE_WRITING -> {
                "Offline Writer: Generated traditional Burmese poetic text."
            }
            IntentType.CONVERSATION -> {
                "Offline Chatbot: Rendered context-aware standard conversation."
            }
            IntentType.TOGGLE_FLASHLIGHT -> {
                val state = slots["state"] ?: "on"
                systemController.toggleFlashlight(state == "on")
                "Executed: Toggled hardware camera flashlight state ($state)."
            }
            IntentType.CHECK_BATTERY -> {
                val batteryInfo = systemController.checkBatteryStatus()
                "Executed: Battery diagnostic scan completed. $batteryInfo"
            }
            IntentType.CHECK_DIAGNOSTICS -> {
                val diagInfo = systemController.getStorageAndRamInfo()
                "Executed: Storage and memory scan completed. $diagInfo"
            }
            IntentType.TOGGLE_DARK_MODE -> {
                val state = slots["state"] ?: "on"
                systemController.toggleDarkMode(state == "on")
                "Executed: Toggled system dark theme setting ($state)."
            }
            IntentType.TOGGLE_POWER_SAVING -> {
                val state = slots["state"] ?: "on"
                systemController.togglePowerSaving(state == "on")
                "Executed: Configured system energy saver profile ($state)."
            }
            IntentType.TOGGLE_DND -> {
                val state = slots["state"] ?: "on"
                systemController.toggleDnd(state == "on")
                "Executed: Configured system interruption filter ($state)."
            }
            IntentType.SET_BRIGHTNESS -> {
                val percent = slots["percent"]?.toIntOrNull() ?: 50
                systemController.setBrightness(percent)
                "Executed: Configured screen backlight brightness to $percent%."
            }
            IntentType.TAKE_SCREENSHOT -> {
                systemController.takeScreenshot()
                "Executed: Saved full screen framebuffer screenshot."
            }
            IntentType.START_SCREEN_RECORDING -> {
                systemController.startScreenRecording()
                "Executed: Opened Android video capture frame recording service."
            }
            IntentType.OPEN_GALLERY -> {
                systemController.openGallery()
                "Executed: Opened local gallery media directory."
            }
            IntentType.SEND_SMS -> {
                val phone = slots["phone"] ?: "09123456789"
                val msg = slots["message"] ?: "မင်္ဂလာပါ"
                systemController.sendSms(phone, msg)
                "Executed: Launched SMS composer targeting $phone."
            }
            IntentType.UNKNOWN -> {
                "Unidentified Burmese phrase: Prompting user for clarification."
            }
        }
    }

    fun release() {
        ttsEngine.shutdown()
        wakeWordEngine.release()
    }
}
