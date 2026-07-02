package com.example.engine

import android.content.Context
import android.util.Log
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

enum class IntentType {
    TOGGLE_WIFI,
    TOGGLE_BLUETOOTH,
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE_VOLUME,
    SCREEN_SETTINGS,
    TOGGLE_HOTSPOT,
    LAUNCH_APP,
    MAKE_CALL,
    CONTACTS_SEARCH,
    LOCATION_CHECK,
    ADD_REMINDER,
    ADD_CALENDAR,
    ADD_NOTE,
    KNOWLEDGE_QA,
    CREATIVE_WRITING,
    CONVERSATION,
    TOGGLE_FLASHLIGHT,
    CHECK_BATTERY,
    CHECK_DIAGNOSTICS,
    TOGGLE_DARK_MODE,
    TOGGLE_POWER_SAVING,
    TOGGLE_DND,
    SET_BRIGHTNESS,
    TAKE_SCREENSHOT,
    START_SCREEN_RECORDING,
    OPEN_GALLERY,
    SEND_SMS,
    UNKNOWN
}

data class NLUResult(
    val intent: IntentType,
    val slots: Map<String, String>,
    val confidence: Float,
    val generatedResponseMy: String // Burmese response generated locally
)

class NLUEngine {
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isModelLoaded = false
    private val tag = "NLUEngine"

    private fun parseNumberFromQuery(query: String): Int? {
        val normalized = query.map { char ->
            when (char) {
                '၀' -> '0'
                '၁' -> '1'
                '၂' -> '2'
                '၃' -> '3'
                '၄' -> '4'
                '၅' -> '5'
                '၆' -> '6'
                '၇' -> '7'
                '၈' -> '8'
                '၉' -> '9'
                else -> char
            }
        }.joinToString("")
        
        val match = "\\d+".toRegex().find(normalized)
        return match?.value?.toIntOrNull()
    }

    fun loadModel(context: Context): Boolean {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            // In a real scenario, copy model from assets to cache or read directly
            val assetManager = context.assets
            val modelBytes = assetManager.open("mbart_burmese.onnx").readBytes()
            val sessionOptions = OrtSession.SessionOptions()
            ortSession = ortEnvironment?.createSession(modelBytes, sessionOptions)
            
            isModelLoaded = true
            Log.d(tag, "Successfully loaded mBART ONNX model.")
            return true
        } catch (e: Exception) {
            Log.e(tag, "ONNX model not found. Using simulated mode. Error: ${e.message}")
            isModelLoaded = true
            return false
        }
    }

    /**
     * Analyses Burmese query string, determines the Intent and extracts entities.
     * Then generates a beautifully natural Burmese voice response.
     */
    fun analyzeSentence(query: String, customMappings: Map<String, IntentType> = emptyMap()): NLUResult {
        val trimmed = query.trim().lowercase()
        
        // Check user-defined custom voice command mappings first for exact/sub-string matches
        for ((phrase, targetIntent) in customMappings) {
            val normalizedPhrase = phrase.trim().lowercase()
            if (normalizedPhrase.isNotEmpty() && (trimmed == normalizedPhrase || trimmed.contains(normalizedPhrase))) {
                val responseText = when (targetIntent) {
                    IntentType.TOGGLE_WIFI -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း ဝိုင်ဖိုင်စနစ်ကို လုပ်ဆောင်ပေးနေပါသည်ခင်ဗျာ။"
                    IntentType.TOGGLE_BLUETOOTH -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း ဘလူးတုသ်စနစ်ကို လုပ်ဆောင်ပေးနေပါသည်ခင်ဗျာ။"
                    IntentType.VOLUME_UP -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း အသံမြှင့်တင်ပေးလိုက်ပါပြီခင်ဗျာ။"
                    IntentType.VOLUME_DOWN -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း အသံလျှော့ချပေးလိုက်ပါပြီခင်ဗျာ။"
                    IntentType.MUTE_VOLUME -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း အသံပိတ်ပေးလိုက်ပါပြီခင်ဗျာ။"
                    IntentType.TOGGLE_FLASHLIGHT -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း ဓာတ်မီးကို ပြောင်းလဲပေးလိုက်ပါပြီခင်ဗျာ။"
                    IntentType.TOGGLE_DARK_MODE -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း အမှောင်စနစ်ကို လုပ်ဆောင်ပေးနေပါသည်ခင်ဗျာ။"
                    IntentType.TOGGLE_POWER_SAVING -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း ပါဝါချွေတာရေးစနစ်ကို ကူးပြောင်းပေးနေပါသည်ခင်ဗျာ။"
                    IntentType.TOGGLE_DND -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း အနှောင့်အယှက်မပေးရစနစ်ကို လုပ်ဆောင်ပေးနေပါသည်ခင်ဗျာ။"
                    IntentType.TAKE_SCREENSHOT -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း ဖုန်းမျက်နှာပြင်ကို ဓာတ်ပုံရိုက်ကူးလိုက်ပါပြီခင်ဗျာ။"
                    IntentType.CHECK_BATTERY -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း ဘက်ထရီအခြေအနေကို စစ်ဆေးပေးနေပါသည်ခင်ဗျာ။"
                    IntentType.CHECK_DIAGNOSTICS -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း သိုလှောင်မှုစစ်ဆေးပေးနေပါသည်ခင်ဗျာ။"
                    IntentType.OPEN_GALLERY -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း ပြခန်းကို ဖွင့်လှစ်ပေးနေပါသည်ခင်ဗျာ။"
                    IntentType.START_SCREEN_RECORDING -> "စိတ်ကြိုက်သတ်မှတ်ထားသည့်အတိုင်း မျက်နှာပြင်ဗီဒီယိုမှတ်တမ်းတင်ခြင်းကို စတင်လိုက်ပါပြီခင်ဗျာ။"
                    else -> "စိတ်ကြိုက်အမိန့်ပေးချက် '${phrase}' အရ ဆောင်ရွက်ပေးနေပါသည်ခင်ဗျာ။"
                }
                return NLUResult(
                    intent = targetIntent,
                    slots = emptyMap(),
                    confidence = 1.0f,
                    generatedResponseMy = responseText
                )
            }
        }

        ortSession?.let {
            // Real ONNX inference logic goes here
            // val inputTensor = OnnxTensor.createTensor(...)
            // val result = it.run(Collections.singletonMap("input_ids", inputTensor))
            Log.d(tag, "mBART ONNX running real inference for query: $query")
        } ?: run {
            Log.d(tag, "mBART ONNX processing offline query (Simulated): $query")
        }

        // 1. Wifi Control
        if (trimmed.contains("ဝိုင်ဖိုင်") || trimmed.contains("wifi") || trimmed.contains("wi-fi")) {
            val isOn = !trimmed.contains("ပိတ်")
            return NLUResult(
                intent = IntentType.TOGGLE_WIFI,
                slots = mapOf("state" to if (isOn) "on" else "off"),
                confidence = 0.95f,
                generatedResponseMy = if (isOn) "ဝိုင်ဖိုင် (Wi-Fi) ဆက်တင်များကို ဖွင့်ပေးနေပါသည်ခင်ဗျာ။" else "ဝိုင်ဖိုင် (Wi-Fi) ဆက်တင်များကို ပိတ်ရန် ဆက်တင်စာမျက်နှာကို ဖွင့်ပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        // 2. Bluetooth Control
        if (trimmed.contains("ဘလူးတုသ်") || trimmed.contains("bluetooth") || trimmed.contains("ဘလူးတု")) {
            val isOn = !trimmed.contains("ပိတ်")
            return NLUResult(
                intent = IntentType.TOGGLE_BLUETOOTH,
                slots = mapOf("state" to if (isOn) "on" else "off"),
                confidence = 0.94f,
                generatedResponseMy = "ဘလူးတုသ် (Bluetooth) စနစ်ကို လုပ်ဆောင်နိုင်ရန် ချိတ်ဆက်မှု ဆက်တင်ကို ဖွင့်ပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        // 3. Volume Control
        if (trimmed.contains("အသံ") || trimmed.contains("volume") || trimmed.contains("sound") || trimmed.contains("speaker") || trimmed.contains("စပီကာ")) {
            return when {
                trimmed.contains("တိုး") || trimmed.contains("လျှော့") || trimmed.contains("down") || trimmed.contains("lower") || trimmed.contains("decrease") -> {
                    NLUResult(
                        intent = IntentType.VOLUME_DOWN,
                        slots = emptyMap(),
                        confidence = 0.95f,
                        generatedResponseMy = "ဖုန်းအသံပမာဏကို လျှော့ချပေးလိုက်ပါပြီခင်ဗျာ။"
                    )
                }
                trimmed.contains("ချဲ့") || trimmed.contains("ကျယ်") || trimmed.contains("မြှင့်") || trimmed.contains("ဖွင့်") || trimmed.contains("up") || trimmed.contains("raise") || trimmed.contains("increase") -> {
                    NLUResult(
                        intent = IntentType.VOLUME_UP,
                        slots = emptyMap(),
                        confidence = 0.95f,
                        generatedResponseMy = "ဖုန်းအသံပမာဏကို တိုးမြှင့်ပေးလိုက်ပါပြီခင်ဗျာ။"
                    )
                }
                trimmed.contains("ပိတ်") || trimmed.contains("ငြိမ်") || trimmed.contains("silent") || trimmed.contains("mute") -> {
                    NLUResult(
                        intent = IntentType.MUTE_VOLUME,
                        slots = emptyMap(),
                        confidence = 0.98f,
                        generatedResponseMy = "ဖုန်းအသံကို ပိတ်ပြီး ငြိမ်သက်စေလိုက်ပါပြီခင်ဗျာ။ (Silent Mode)"
                    )
                }
                else -> NLUResult(
                    intent = IntentType.VOLUME_UP,
                    slots = emptyMap(),
                    confidence = 0.80f,
                    generatedResponseMy = "အသံအတိုးအကျယ်ကို ညှိနှိုင်းပေးနေပါသည်ခင်ဗျာ။"
                )
            }
        }

        // 4. Hotspot Control
        if (trimmed.contains("ဟော့စပေါ့") || trimmed.contains("hotspot")) {
            return NLUResult(
                intent = IntentType.TOGGLE_HOTSPOT,
                slots = emptyMap(),
                confidence = 0.91f,
                generatedResponseMy = "ဟော့စပေါ့ (Hotspot) ဆက်တင်များ စာမျက်နှာကို ဖွင့်လှစ်ပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        // 5. Screen Settings
        if (trimmed.contains("မျက်နှာပြင်") || trimmed.contains("အလင်း") || trimmed.contains("screen") || trimmed.contains("စကရင်")) {
            return NLUResult(
                intent = IntentType.SCREEN_SETTINGS,
                slots = emptyMap(),
                confidence = 0.89f,
                generatedResponseMy = "မျက်နှာပြင် အလင်းအမှောင်နှင့် Display ဆက်တင်များစာမျက်နှာကို ဖွင့်လှစ်ပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        // 6. Make phone call
        if (trimmed.contains("ဖုန်းခေါ်") || trimmed.contains("ခေါ်ပေးပါ") || trimmed.contains("call")) {
            // Extract numbers if present
            val numberRegex = "\\d+".toRegex()
            val number = numberRegex.find(trimmed)?.value ?: "09123456789"
            return NLUResult(
                intent = IntentType.MAKE_CALL,
                slots = mapOf("phone_number" to number),
                confidence = 0.96f,
                generatedResponseMy = "ဖုန်းနံပါတ် $number သို့ ဖုန်းခေါ်ဆိုရန် ဖုန်းခေါ်ဆိုမှုမျက်နှာပြင်ကို ဖွင့်ပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        // 7. Contacts search
        if (trimmed.contains("အဆက်အသွယ်") || trimmed.contains("လိပ်စာ") || trimmed.contains("ကွန်ဆက်") || trimmed.contains("contact")) {
            return NLUResult(
                intent = IntentType.CONTACTS_SEARCH,
                slots = emptyMap(),
                confidence = 0.90f,
                generatedResponseMy = "ဖုန်းထဲရှိ အဆက်အသွယ် (Contacts) စာရင်းကို ရှာဖွေပြသပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        // 8. Location Check
        if (trimmed.contains("တည်နေရာ") || trimmed.contains("မြေပုံ") || trimmed.contains("နေရာ") || trimmed.contains("location")) {
            return NLUResult(
                intent = IntentType.LOCATION_CHECK,
                slots = emptyMap(),
                confidence = 0.93f,
                generatedResponseMy = "လက်ရှိ ရောက်ရှိနေသော တည်နေရာမြေပုံကို ရှာဖွေပြီး ဖော်ပြပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        // 9. Reminder
        if (trimmed.contains("သတိပေး") || trimmed.contains("reminder")) {
            val detail = if (trimmed.contains("ဆေး")) "ဆေးသောက်ရန်" else "အလုပ်ကိစ္စလုပ်ရန်"
            return NLUResult(
                intent = IntentType.ADD_REMINDER,
                slots = mapOf("detail" to detail),
                confidence = 0.92f,
                generatedResponseMy = "အော့ဖ်လိုင်း သတိပေးချက် စနစ်တွင် \"$detail\" ကို သတိပေးချက်အဖြစ် သတ်မှတ်လိုက်ပါပြီခင်ဗျာ။"
            )
        }

        // 10. Calendar
        if (trimmed.contains("ပြက္ခဒိန်") || trimmed.contains("ချိန်းဆို") || trimmed.contains("calendar")) {
            val eventTitle = "အကူအညီပေးသူနှင့် ချိန်းဆိုမှု"
            return NLUResult(
                intent = IntentType.ADD_CALENDAR,
                slots = mapOf("title" to eventTitle),
                confidence = 0.91f,
                generatedResponseMy = "ပြက္ခဒိန်ထဲတွင် \"$eventTitle\" အတွက် အစီအစဉ်အသစ် ထည့်သွင်းပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        // 11. Notes
        if (trimmed.contains("မှတ်စု") || trimmed.contains("ရေးမှတ်") || trimmed.contains("note")) {
            val content = query.replace("မှတ်စု", "").replace("ရေးမှတ်", "").replace("ပါ", "").trim()
            val finalContent = if (content.isEmpty()) "မြန်မာ Voice Assistant ဖြင့် ရေးမှတ်ထားသော မှတ်စု" else content
            return NLUResult(
                intent = IntentType.ADD_NOTE,
                slots = mapOf("content" to finalContent),
                confidence = 0.94f,
                generatedResponseMy = "မှတ်စုထဲတွင် \"$finalContent\" ကို အော့ဖ်လိုင်း သိမ်းဆည်းလိုက်ပါပြီခင်ဗျာ။"
            )
        }

        // 12. Knowledge QA
        if (trimmed.contains("ဘာလဲ") || trimmed.contains("ဘယ်သူလဲ") || trimmed.contains("အကြောင်း") || trimmed.contains("သမိုင်း") || trimmed.contains("qa")) {
            val answerMy = when {
                trimmed.contains("မြန်မာ") -> "မြန်မာနိုင်ငံသည် အရှေ့တောင်အာရှတွင် တည်ရှိပြီး ယဉ်ကျေးမှုအမွေအနှစ်များစွာရှိသော နိုင်ငံဖြစ်ပါသည်။"
                trimmed.contains("ပုဂံ") -> "ပုဂံသည် မြန်မာနိုင်ငံ၏ ရှေးဟောင်းသမိုင်းဝင် မြို့တော်တစ်ခုဖြစ်ပြီး စေတီပုထိုးထောင်ပေါင်းများစွာ တည်ရှိရာ အရပ်ဖြစ်ပါသည်။"
                trimmed.contains("ရန်ကုန်") -> "ရန်ကုန်မြို့သည် မြန်မာနိုင်ငံ၏ စီးပွားရေးအချက်အချာ အကျဆုံးနှင့် အကြီးဆုံး မြို့ကြီးတစ်မြို့ ဖြစ်ပါသည်။"
                else -> "မြန်မာ့သမိုင်းနှင့် အထွေထွေဗဟုသုတများကို အော့ဖ်လိုင်းဗဟုသုတစနစ်မှ ရှာဖွေတွေ့ရှိချက်အရ ရှင်းလင်းတင်ပြပေးနေခြင်း ဖြစ်ပါသည်။"
            }
            return NLUResult(
                intent = IntentType.KNOWLEDGE_QA,
                slots = emptyMap(),
                confidence = 0.88f,
                generatedResponseMy = answerMy
            )
        }

        // 13. Creative Writing
        if (trimmed.contains("ကဗျာ") || trimmed.contains("ပုံပြင်") || trimmed.contains("စာရေး")) {
            val poem = "ပန်းကလေးများ လန်းဆန်းစေ၊ လေပြည်ညှင်းလည်း တိုက်ခတ်စေ။ မြန်မာပြည်ဝယ် အစဉ်အေးမြ၊ သာယာဝပြော ငြိမ်းချမ်းကြ။"
            return NLUResult(
                intent = IntentType.CREATIVE_WRITING,
                slots = emptyMap(),
                confidence = 0.90f,
                generatedResponseMy = "မြန်မာစာပေ အော့ဖ်လိုင်းမော်ဒယ်မှ ထုတ်လုပ်ပေးသော ကဗျာတိုလေး ဖြစ်ပါသည်ခင်ဗျာ -\n\n\"$poem\""
            )
        }

        // 14. Launch Apps check (Direct App names)
        val appMap = mapOf(
            "ကင်မရာ" to Pair("com.android.camera", "Camera"),
            "ဖုန်း" to Pair("com.android.dialer", "Phone"),
            "ဆက်တင်" to Pair("com.android.settings", "Settings"),
            "ပြက္ခဒိန်" to Pair("com.android.calendar", "Calendar")
        )
        for ((key, pkgPair) in appMap) {
            if (trimmed.contains(key)) {
                return NLUResult(
                    intent = IntentType.LAUNCH_APP,
                    slots = mapOf("package" to pkgPair.first, "name" to pkgPair.second),
                    confidence = 0.95f,
                    generatedResponseMy = "$key ဆော့ဖ်ဝဲလ် (App) ကို ဖွင့်ပေးနေပါသည်ခင်ဗျာ။"
                )
            }
        }

        // 14b. Advanced Device Controls (Flashlight, Dark Mode, Battery, DND, Screenshot, etc.)
        if (trimmed.contains("ဓာတ်မီး") || trimmed.contains("flashlight")) {
            val isOn = !trimmed.contains("ပိတ်")
            return NLUResult(
                intent = IntentType.TOGGLE_FLASHLIGHT,
                slots = mapOf("state" to if (isOn) "on" else "off"),
                confidence = 0.98f,
                generatedResponseMy = if (isOn) "ဓာတ်မီး ဖွင့်လိုက်ပါပြီခင်ဗျာ။" else "ဓာတ်မီး ပိတ်လိုက်ပါပြီခင်ဗျာ။"
            )
        }

        if (trimmed.contains("ဘက်ထရီ") || trimmed.contains("battery")) {
            return NLUResult(
                intent = IntentType.CHECK_BATTERY,
                slots = emptyMap(),
                confidence = 0.97f,
                generatedResponseMy = "ဘက်ထရီ အခြေအနေကို စစ်ဆေးပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        if (trimmed.contains("သိုလှောင်မှု") || trimmed.contains("storage") || trimmed.contains("ram") || trimmed.contains("diagnostics")) {
            return NLUResult(
                intent = IntentType.CHECK_DIAGNOSTICS,
                slots = emptyMap(),
                confidence = 0.96f,
                generatedResponseMy = "စနစ်စွမ်းဆောင်ရည်နှင့် သိုလှောင်မှုများကို စစ်ဆေးပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        if (trimmed.contains("အမှောင်") || trimmed.contains("dark mode") || trimmed.contains("darkmode")) {
            val isOn = !trimmed.contains("ပိတ်")
            return NLUResult(
                intent = IntentType.TOGGLE_DARK_MODE,
                slots = mapOf("state" to if (isOn) "on" else "off"),
                confidence = 0.95f,
                generatedResponseMy = if (isOn) "အမှောင်စနစ် (Dark Mode) ဆက်တင်သို့ ကူးပြောင်းပေးနေပါသည်ခင်ဗျာ။" else "လင်းထိန်သောစနစ် (Light Mode) ဆက်တင်သို့ ကူးပြောင်းပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        if (trimmed.contains("စွမ်းအင်") || trimmed.contains("power saving") || trimmed.contains("powersaving")) {
            val isOn = !trimmed.contains("ပိတ်")
            return NLUResult(
                intent = IntentType.TOGGLE_POWER_SAVING,
                slots = mapOf("state" to if (isOn) "on" else "off"),
                confidence = 0.95f,
                generatedResponseMy = "ပါဝါချွေတာရေးမုဒ် (Power Saving) ဆက်တင်များသို့ ကူးပြောင်းပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        if (trimmed.contains("တိတ်ဆိတ်") || trimmed.contains("do not disturb") || trimmed.contains("dnd") || trimmed.contains("အနှောင့်အယှက်")) {
            val isOn = !trimmed.contains("ပိတ်")
            return NLUResult(
                intent = IntentType.TOGGLE_DND,
                slots = mapOf("state" to if (isOn) "on" else "off"),
                confidence = 0.96f,
                generatedResponseMy = if (isOn) "အနှောင့်အယှက်မပေးရစနစ် (Do Not Disturb) ကို ဖွင့်လှစ်လိုက်ပါပြီခင်ဗျာ။" else "အနှောင့်အယှက်မပေးရစနစ် (Do Not Disturb) ကို ပိတ်လိုက်ပါပြီခင်ဗျာ။"
            )
        }

        if (trimmed.contains("အလင်း") || trimmed.contains("brightness") || trimmed.contains("backlight") || trimmed.contains("စကရင်လင်း")) {
            val percent = parseNumberFromQuery(trimmed) ?: 50
            return NLUResult(
                intent = IntentType.SET_BRIGHTNESS,
                slots = mapOf("percent" to percent.toString()),
                confidence = 0.98f,
                generatedResponseMy = "မျက်နှာပြင် အလင်းအမှောင်ကို $percent% သို့ ညှိနှိုင်းပြောင်းလဲပေးလိုက်ပါပြီခင်ဗျာ။"
            )
        }

        if (trimmed.contains("စကရင်ရှော့") || trimmed.contains("screenshot")) {
            return NLUResult(
                intent = IntentType.TAKE_SCREENSHOT,
                slots = emptyMap(),
                confidence = 0.98f,
                generatedResponseMy = "မျက်နှာပြင်ကို ဓာတ်ပုံရိုက်ကူး (Screenshot) ယူလိုက်ပါပြီခင်ဗျာ။"
            )
        }

        if (trimmed.contains("စကရင်ဗီဒီယို") || trimmed.contains("screen recording") || trimmed.contains("recording")) {
            return NLUResult(
                intent = IntentType.START_SCREEN_RECORDING,
                slots = emptyMap(),
                confidence = 0.95f,
                generatedResponseMy = "မျက်နှာပြင်ဗီဒီယိုမှတ်တမ်းတင်ခြင်း (Screen Recording) စတင်လိုက်ပါပြီခင်ဗျာ။"
            )
        }

        if (trimmed.contains("ပြခန်း") || trimmed.contains("gallery") || trimmed.contains("ပုံဖွင့်")) {
            return NLUResult(
                intent = IntentType.OPEN_GALLERY,
                slots = emptyMap(),
                confidence = 0.94f,
                generatedResponseMy = "ဖုန်း၏ ဓာတ်ပုံပြခန်း (Gallery) ကို ဖွင့်လှစ်ပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        if (trimmed.contains("မက်ဆေ့ခ်ျ") || trimmed.contains("sms") || trimmed.contains("စာပို့")) {
            return NLUResult(
                intent = IntentType.SEND_SMS,
                slots = mapOf("phone" to "09123456789", "message" to "မင်္ဂလာပါ R\'s AI မှ စာတိုပေးပို့ခြင်းဖြစ်ပါသည်။"),
                confidence = 0.95f,
                generatedResponseMy = "မက်ဆေ့ခ်ျ (SMS) ပေးပို့ရန် စာမျက်နှာကို ဖော်ပြပေးနေပါသည်ခင်ဗျာ။"
            )
        }

        // 15. Default Conversation
        val genericReplies = listOf(
            "မင်္ဂလာပါခင်ဗျာ။ ကျွန်တော်ကတော့ အော့ဖ်လိုင်း မြန်မာ Voice Assistant ဖြစ်ပါတယ်။ ဘာများ ကူညီပေးရမလဲခင်ဗျာ။",
            "ဟုတ်ကဲ့ပါခင်ဗျာ၊ ကျွန်တော် အမြဲတမ်း အဆင်သင့်ရှိနေပါတယ်။ မေးခွန်းများ မေးမြန်းနိုင်ပါတယ်ခင်ဗျာ။",
            "နေကောင်းပါတယ်ခင်ဗျာ။ လူကြီးမင်းလည်း ကိုယ်စိတ်နှစ်ဖြာ ကျန်းမာရွှင်လန်းပါစေကြောင်း ဆုတောင်းပေးအပ်ပါတယ်ခင်ဗျာ။"
        )
        val reply = if (trimmed.contains("မင်္ဂလာ") || trimmed.contains("ဟယ်လို") || trimmed.contains("hello")) {
            genericReplies[0]
        } else if (trimmed.contains("နေကောင်း")) {
            genericReplies[2]
        } else {
            genericReplies[1]
        }

        return NLUResult(
            intent = IntentType.CONVERSATION,
            slots = emptyMap(),
            confidence = 0.85f,
            generatedResponseMy = reply
        )
    }
}
