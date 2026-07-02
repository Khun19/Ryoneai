# ပြီးပြည့်စုံသော အော့ဖ်လိုင်း မြန်မာ Voice Assistant တည်ဆောက်ခြင်း လမ်းညွှန်

ဤလမ်းညွှန်သည် အင်တာနက်မလိုဘဲ အလုပ်လုပ်နိုင်သော မြန်မာဘာသာစကားဖြင့် အမိန့်ပေးနိုင်သည့် Voice Assistant (TFLite နှင့် ONNX အသုံးပြု) တည်ဆောက်ခြင်း အဆင့် (၁၀) ဆင့်ကို အသေးစိတ် ဖော်ပြထားပါသည်။

## Phase 1: Data Preparation & Preprocessing
* **လိုအပ်သော ဒေတာ (Dataset)**: မြန်မာအသံဖိုင်များနှင့် စာသားများ (ဥပမာ - `myanmar-language-dataset-collection`)
* **ကိရိယာများ**: Python, Librosa, Pandas
* **လုပ်ဆောင်ချက်**: 
  - အသံဖိုင်များကို 16kHz, 16-bit Mono WAV format သို့ပြောင်းပါ။
  - Noise reduction နှင့် Silence trimming ပြုလုပ်ပါ။
  - Transcript များကို သန့်စင်ပြီး Tokenization ပြုလုပ်ပါ။

## Phase 2: Core Model Training (STT, NLU, TTS)
* **STT (Speech-to-Text)**: OpenAI Whisper (Tiny/Base) ကို မြန်မာအသံဖြင့် Fine-tune လုပ်ပါ။ (HuggingFace `transformers` အသုံးပြု)
* **NLU (Natural Language Understanding)**: XLM-R သို့မဟုတ် mBART ကို အသုံးပြု၍ Intent Classification နှင့် Slot Filling အတွက် Train ပါ။
* **TTS (Text-to-Speech)**: Coqui TTS (VITS သို့မဟုတ် Tacotron2) ကို မြန်မာအသံ ဒေတာဖြင့် Train ပါ။

## Phase 3: Model Conversion & Optimization (TFLite & ONNX)
* **TFLite (STT)**: 
  ```python
  import tensorflow as tf
  # Convert STT to TFLite
  converter = tf.lite.TFLiteConverter.from_saved_model('stt_model_dir')
  converter.optimizations = [tf.lite.Optimize.DEFAULT]
  tflite_model = converter.convert()
  with open('whisper_mm.tflite', 'wb') as f: f.write(tflite_model)
  ```
* **ONNX (NLU & TTS)**:
  ```python
  import torch
  # Export NLU to ONNX
  torch.onnx.export(nlu_model, dummy_input, "nlu_mm.onnx", opset_version=14)
  ```
* **Quantization**: ဖုန်းပေါ်တွင် လျင်မြန်စွာ အလုပ်လုပ်ရန် Float16 သို့မဟုတ် INT8 Quantization ပြုလုပ်ပါ။

## Phase 4: Android Project Setup & Engine Implementation
* **Dependencies**: `tensorflow-lite`, `onnxruntime-android` ကို `build.gradle.kts` တွင်ထည့်ပါ။
* **Engines**: 
  - `STTEngine`: TFLite Interpreter ကိုသုံး၍ အသံလှိုင်း (PCM) မှ စာသား (Text) သို့ပြောင်းပါ။
  - `NLUEngine`: OrtEnvironment နှင့် OrtSession သုံး၍ စာသားမှ Intent ခွဲခြားပါ။
  - `TTSEngine`: Android ၏ Native `TextToSpeech` (သို့) ပြင်ပ ONNX TTS Model ကို သုံး၍ အသံပြန်ထုတ်ပါ။
  - `WakeWordEngine`: "Hey Bro" သို့မဟုတ် "ဟယ်လို" ကဲ့သို့ စကားလုံးအတွက် Micro KWS (Keyword Spotting) မော်ဒယ်အသေးကို သုံးပါ။

## Phase 5: Privacy & Permissions Management
* **Permissions**: `RECORD_AUDIO` ကို `AndroidManifest.xml` တွင် ကြေငြာပါ။
* **Runtime Request**: App စတင်ချိန်တွင် အသံဖမ်းယူခွင့် (Microphone Permission) တောင်းခံပါ။

## Phase 6: Core Logic Workflow
1. **Wake Word Detection**: Background တွင် အမြဲနားထောင်နေပြီး "Hey Bro" ဟုကြားလျှင် စတင်အလုပ်လုပ်သည်။
2. **Audio Capture**: အသုံးပြုသူ၏ အသံကို ဖမ်းယူပြီး TFLite STT Engine သို့ ပေးပို့သည်။
3. **Intent Recognition**: ရရှိလာသော မြန်မာစာသားကို ONNX NLU Engine က နားလည်ပြီး လုပ်ဆောင်ရမည့်အရာ (ဥပမာ - မီးဖွင့်ရန်၊ သီချင်းဖွင့်ရန်) ကို ခွဲခြားသည်။
4. **Action Execution**: `SystemController` သို့မဟုတ် `DialogueManager` က သတ်မှတ်ထားသော လုပ်ဆောင်ချက်ကို လုပ်ဆောင်သည်။
5. **Voice Feedback**: ပြီးစီးကြောင်းကို မြန်မာလို အသံဖြင့် (TTS) ပြန်လည်ပြောကြားသည်။

## Phase 7: Model Size & Performance Footprint
* **STT Model (Quantized)**: ~40 MB
* **NLU Model (Quantized)**: ~80 MB
* **TTS Model (Quantized/Native)**: ~50 MB (Native သုံးလျှင် 0 MB)
* **RAM Usage**: inference အချိန်တွင် ~200-300 MB အခြေအနေထိ ရှိသည်။

## Phase 8: Optimization & Edge Deployment Tips
* **Audio Buffering**: အသံဖမ်းယူရာတွင် Circular Buffer ကို သုံး၍ Memory ကို ချွေတာပါ။
* **Hardware Acceleration**: NNAPI သို့မဟုတ် XNNPACK ကို ဖွင့်ထားပါ။
* **Asynchronous Execution**: Model Inference ကို Main Thread တွင် မလုပ်ဘဲ Coroutines ဖြင့် Background တွင် သီးသန့်လုပ်ပါ။

## Phase 9: Quick Start Developer Checklist
- [x] Model များကို TFLite/ONNX ပြောင်းပြီး `assets` folder တွင် ထည့်ပြီးပြီလား?
- [x] Microphone Permission ကို Runtime တွင် တောင်းခံထားသလား?
- [x] `SystemController` တွင် System အမိန့်များ (Wifi, Bluetooth) ရေးထားသလား?
- [x] UI တွင် Status နှင့် Chat Log များကို ပြသနိုင်ပြီလား?

## Phase 10: Learning Resources & Links
* [TensorFlow Lite Android Guide](https://www.tensorflow.org/lite/android)
* [ONNX Runtime Android API](https://onnxruntime.ai/docs/execution-providers/NNAPI-ExecutionProvider.html)
* [HuggingFace Transformers](https://huggingface.co/docs/transformers/index)
* [Android AudioRecord Documentation](https://developer.android.com/reference/android/media/AudioRecord)

---

# 🚀 Gemini Live Clone (Burmese Voice Assistant) Full Build Roadmap

Gemini Live သို့မဟုတ် ခေတ်မီအဆင့်မြင့် AI Voice Assistant ကို အခြေခံမှစတင်ပြီး Production Level အထိ တည်ဆောက်နိုင်ရန် ပြည့်စုံသော လမ်းညွှန်ပြေစာ ဖြစ်ပါသည်။

### Project Directory Layout (Gemini-Live-Clone)
```text
Gemini-Live-Clone/
│
├── app/
│   ├── ui/ (Screens, Theme, Widgets, Navigation)
│   ├── ai/ (Prompt, Reasoning, Memory, Tools, Model)
│   ├── voice/ (STT, TTS, Wakeword, Streaming, Audio)
│   ├── vision/ (Camera integration, OCR, Objects)
│   ├── files/ (Document parser, PDFs)
│   ├── browser/ (Search integration, Web Scraper)
│   ├── plugins/ (Integration services)
│   ├── database/ (Room DB, SharedPreferences)
│   └── settings/ (User Preferences, API Keys)
│
├── backend/
│   ├── api/ (REST endpoints)
│   ├── auth/ (User authentication)
│   ├── websocket/ (Streaming sockets)
│   └── vector-db/ (RAG semantic memory)
│
└── docs/ (API schemas and system design docs)
```

---

## 📚 Phase 1 — Foundation
* **Android Studio & Tooling**: Kotlin & Jetpack Compose ကို အသုံးပြုပါ။
* **Architecture**: MVVM (Model-View-ViewModel) Architecture ကို အသုံးပြုပြီး Dynamic Theme ဖြင့် တည်ဆောက်ပါ။
* **Asynchronous Flow**: Kotlin Coroutines & Flow ကို အသုံးပြု၍ Concurrent processes များကို စီမံပါ။
* **Persistence**: Room Database (Local History, Custom Commands) နှင့် SharedPreferences (Settings) ကို သုံးပါ။

## 🧠 Phase 2 — AI Brain (AI Engine)
* **Workflow**: `User` -> `Speech` -> `Speech-To-Text` -> `Prompt Builder` -> `Gemini API` -> `Reasoning` -> `Response` -> `Text-To-Speech`
* **Modules**:
  - `Prompt Manager`: System Instructions များနှင့် Personality prompts များကို စီမံသည်။
  - `Conversation Manager`: မေးခွန်းနှင့် အဖြေများကို Context တွဲစပ်သိမ်းဆည်းသည်။
  - `Token Manager & Error Handler`: Gemini API ခေါ်ဆိုမှု တုံ့ပြန်နှုန်းနှင့် သုံးစွဲမှုကို ထိန်းညှိသည်။

## 🧠 Phase 3 — Memory System
* **Memory Tiers**:
  - `Short Memory (Session Context)`: လက်ရှိပြောဆိုနေသော စကားစုများ၏ Context။
  - `Long Memory (User Profile DB)`: User ၏ နာမည်၊ စိတ်ကြိုက်ဘာသာစကား၊ စိတ်ကြိုက်အမိန့်များနှင့် Preferences များကို သိမ်းဆည်းမှတ်သားခြင်း။

## 🎤 Phase 4 — Voice Pipeline
* **Flow**: `Mic` -> `Noise Reduction` -> `Voice Activity Detection (VAD)` -> `Wake Word` -> `Speech Recognition` -> `AI` -> `TTS` -> `Speaker`
* **Features**:
  - **Interrupt Speaking**: AI စကားပြောနေစဉ် အသုံးပြုသူမှ ကြားဖြတ်ပြောဆိုပါက ရပ်တန့်ပေးခြင်း။
  - **Low Latency Streaming**: latency နည်းပါးစွာဖြင့် အချိန်နှင့်တပြေးညီ တုံ့ပြန်နိုင်စွမ်း။

## 👀 Phase 5 — Vision (Camera Live)
* **Flow**: `Camera Capture` -> `OCR / Object Detection` -> `Image Understanding` -> `Gemini Vision API` -> `Voice Answer`
* **Capabilities**: စာရွက်စာတမ်းဖတ်ခြင်း (OCR)၊ ပြေစာများ၊ သစ်ပင်၊ တိရိစ္ဆာန်များနှင့် ပတ်ဝန်းကျင်ပစ္စည်းများကို မြင်ရုံဖြင့် ရှင်းပြနိုင်ခြင်း။

## 📄 Phase 6 — Files Analyzer
* **Support formats**: PDF, DOCX, TXT, PPT, Excel, Images
* **Workflow**: ဖိုင်ကိုဖွင့်ပါ -> စာသားကိုထုတ်ယူပါ -> Gemini AI ဖြင့်ဆန်းစစ်ပါ -> အကျဉ်းချုပ်ပြီး အသံဖြင့်ရှင်းပြပါ။

## 🌐 Phase 7 — Browser Agent
* **RAG & Live Web Search**: Gemini Search Grounding သို့မဟုတ် Google Search APIs နှင့် ချိတ်ဆက်ပြီး ဝက်ဘ်ဆိုက်များမှ သတင်းအသစ်များကို ရှာဖွေဆန်းစစ်ကာ အဖြေထုတ်ပေးခြင်း။

## 🔌 Phase 8 — Tool Calling (Function Calling)
* **Tool Router**: AI က အသုံးပြုသူ ပြောလိုက်သောအမိန့်ကို ဆန်းစစ်ပြီး Dynamic အနေဖြင့် Calendar, Google Maps, Weather, Contacts သို့မဟုတ် Alarm APIs များကို အလိုအလျောက် ရွေးချယ်ခေါ်ယူစေခြင်း။

## 📱 Phase 9 — Android System Control
* **OS Actions**: Phone Calls, SMS, Open Apps, Flashlight, Bluetooth, Wi-Fi, Volume & Brightness, Screenshots, Alarms များကို voice commands ဖြင့် တိုက်ရိုက်ထိန်းချုပ်ခြင်း။

## 🌍 Phase 10 — Live Conversation
* **Bidirectional Streaming**: Audio streaming input/output socket သို့မဟုတ် REST channel များသုံးပြီး ဖုန်းပြောသကဲ့သို့ အဆက်မပြတ် စကားပြောနိုင်ခြင်း။

## 🔒 Phase 11-15 — Advanced, Security & Local AI
* **Phase 11 (Security)**: API Key Encyption, Secure Biometric Authentication, Device data encryption။
* **Phase 12 (Cloud Sync)**: Cloud back up with Google Drive/Firebase sync။
* **Phase 13 (Automation)**: Trigger tasks based on scheduled voice alarms (e.g., "မနက် ၆ နာရီ Alarm ဖွင့်")။
* **Phase 14 (Plugin System)**: Gmail, YouTube, Maps, Translate plugin interfaces။
* **Phase 15 (Local AI)**: Offline Small Language Models (e.g., Gemma 2B via Mediapipe), Offline STT & TTS fully local execution။
