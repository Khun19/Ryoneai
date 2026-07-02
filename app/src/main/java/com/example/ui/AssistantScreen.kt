package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.feedback.FeedbackEntity
import com.example.engine.IntentType
import kotlinx.coroutines.launch

enum class WorkspaceType(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ASSISTANT("R's AI", Icons.Default.Hearing),
    WRITING("Writing Suite", Icons.Default.Create),
    IMAGE("Image Studio", Icons.Default.Palette),
    DOCUMENTS("Doc & Voice", Icons.Default.Description),
    SMART_HOME("Smart Home", Icons.Default.Home),
    TRANSLATOR("Vision & Trans", Icons.Default.Translate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel, modifier: Modifier = Modifier) {
    val chatLog by viewModel.chatLog.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isWakeWordActive by viewModel.isWakeWordActive.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastProcessResult.collectAsStateWithLifecycle()
    val feedbacks by viewModel.feedbacks.collectAsStateWithLifecycle()
    val isAutoEnvironmentEnabled by viewModel.isAutoEnvironmentEnabled.collectAsStateWithLifecycle()
    val currentVoiceProfile by viewModel.currentVoiceProfile.collectAsStateWithLifecycle()
    val currentNoiseRms by viewModel.currentNoiseRms.collectAsStateWithLifecycle()

    // Workspace and Tab management
    var currentTab by remember { mutableStateOf(0) } // 0 = Assistant, 1 = Feedback Log, 2 = Guide, 3 = Config
    var activeWorkspace by remember { mutableStateOf(WorkspaceType.ASSISTANT) }

    // Screen input states
    var textInput by remember { mutableStateOf("") }
    var writingInput by remember { mutableStateOf("") }
    var translateInput by remember { mutableStateOf("") }
    var targetLanguage by remember { mutableStateOf("English") }
    var imagePromptInput by remember { mutableStateOf("") }

    // Selected items for simulations
    var selectedDocument by remember { mutableStateOf("business_report.pdf") }
    var selectedScreenState by remember { mutableStateOf("System Error Dialog on Main Home launcher") }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Auto-scroll chat log on update
    LaunchedEffect(chatLog.size) {
        if (chatLog.isNotEmpty()) {
            listState.animateScrollToItem(chatLog.size - 1)
        }
    }

    // Modern High-Contrast Palette: Deep space with ultraviolet and cyan accents
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF030712), Color(0xFF0F172A), Color(0xFF1E1B4B))
    )
    val neonPurple = Color(0xFF8B5CF6)
    val neonCyan = Color(0xFF06B6D4)
    val neonPink = Color(0xFFEC4899)
    val glassBg = Color(0x1F334155)
    val glassBorder = Color(0x3394A3B8)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_rs_ai_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, neonCyan, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "R's AI",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Myanmar HyperAI OS Assistant",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.simulateWakeWordTrigger()
                        },
                        modifier = Modifier.testTag("simulate_wake_button")
                    ) {
                        Icon(Icons.Default.Bolt, "Simulate Wake word", tint = neonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF030712)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF030712)
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Chat, "Chat assistant") },
                    label = { Text("စကားပြောခန်း") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = neonCyan,
                        selectedTextColor = neonCyan,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray,
                        indicatorColor = neonCyan.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Storage, "Feedback local storage database viewer") },
                    label = { Text("ဒေတာဘေ့စ်") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = neonPurple,
                        selectedTextColor = neonPurple,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray,
                        indicatorColor = neonPurple.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.LibraryBooks, "Architecture roadmap builder guide") },
                    label = { Text("နည်းပညာ") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = neonPink,
                        selectedTextColor = neonPink,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray,
                        indicatorColor = neonPink.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Settings, "Config voice custom mapping settings") },
                    label = { Text("ပြင်ဆင်ချက်") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = neonCyan,
                        selectedTextColor = neonCyan,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray,
                        indicatorColor = neonCyan.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(bgGradient)
        ) {
            when (currentTab) {
                0 -> {
                    // MAIN INTERACTIVE HUB (WORKSPACES)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Horizontal Workspace tabs selector row
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF030712))
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(WorkspaceType.values().toList()) { workspace ->
                                val isSelected = activeWorkspace == workspace
                                val activeTabColor = when (workspace) {
                                    WorkspaceType.ASSISTANT -> neonCyan
                                    WorkspaceType.WRITING -> neonPurple
                                    WorkspaceType.IMAGE -> neonPink
                                    WorkspaceType.DOCUMENTS -> neonCyan
                                    WorkspaceType.SMART_HOME -> neonPurple
                                    WorkspaceType.TRANSLATOR -> neonPink
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) activeTabColor.copy(alpha = 0.2f) else glassBg)
                                        .border(
                                            1.dp,
                                            if (isSelected) activeTabColor else glassBorder,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { activeWorkspace = workspace }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("workspace_tab_${workspace.name}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = workspace.icon,
                                            contentDescription = workspace.displayName,
                                            tint = if (isSelected) activeTabColor else Color.LightGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = workspace.displayName,
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Render selected workspace panel contents
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            when (activeWorkspace) {
                                WorkspaceType.ASSISTANT -> {
                                    // 1. DYNAMIC BOT CHAT & SYSTEM INTENT CLASSIFICATION ENGINE
                                    val activeModel by viewModel.activeChatModel.collectAsStateWithLifecycle()
                                    val thinkingEnabled by viewModel.thinkingModeEnabled.collectAsStateWithLifecycle()
                                    val searchGrounding by viewModel.searchGroundingEnabled.collectAsStateWithLifecycle()
                                    val mapsGrounding by viewModel.mapsGroundingEnabled.collectAsStateWithLifecycle()

                                    Column(modifier = Modifier.fillMaxSize()) {
                                        
                                        // UPPER DYNAMIC CONTROL BAR
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0x33334155)),
                                            border = BorderStroke(1.dp, glassBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Model Selection
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Model: ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        var modelExpanded by remember { mutableStateOf(false) }
                                                        Box {
                                                            Text(
                                                                text = activeModel,
                                                                color = neonCyan,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                modifier = Modifier
                                                                    .clickable { modelExpanded = true }
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                            DropdownMenu(
                                                                expanded = modelExpanded,
                                                                onDismissRequest = { modelExpanded = false },
                                                                modifier = Modifier.background(Color(0xFF1E293B))
                                                            ) {
                                                                listOf("gemini-3.5-flash", "gemini-3.1-flash-lite-preview", "gemini-3.1-pro-preview").forEach { m ->
                                                                    DropdownMenuItem(
                                                                        text = { Text(m, color = Color.White, fontSize = 11.sp) },
                                                                        onClick = {
                                                                            viewModel.setActiveChatModel(m)
                                                                            modelExpanded = false
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // High-Speed Voice Call Button
                                                    Button(
                                                        onClick = { viewModel.startVoiceCall() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonPurple),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Phone, "Call Live", tint = Color.White, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Gemini Live", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                Spacer(Modifier.height(6.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Grounding Toggles
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Checkbox(
                                                            checked = searchGrounding,
                                                            onCheckedChange = { viewModel.setSearchGroundingEnabled(it) },
                                                            colors = CheckboxDefaults.colors(checkedColor = neonCyan)
                                                        )
                                                        Text("🌐 Search", color = Color.LightGray, fontSize = 9.sp)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Checkbox(
                                                            checked = mapsGrounding,
                                                            onCheckedChange = { viewModel.setMapsGroundingEnabled(it) },
                                                            colors = CheckboxDefaults.colors(checkedColor = neonCyan)
                                                        )
                                                        Text("🗺️ Maps", color = Color.LightGray, fontSize = 9.sp)
                                                    }
                                                    if (activeModel.contains("pro")) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
                                                            Switch(
                                                                checked = thinkingEnabled,
                                                                onCheckedChange = { viewModel.setThinkingModeEnabled(it) },
                                                                colors = SwitchDefaults.colors(checkedThumbColor = neonPurple)
                                                            )
                                                            Spacer(Modifier.width(2.dp))
                                                            Text("🧠 Thinking", color = Color.LightGray, fontSize = 9.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Ambient Noise & Micro Speech Diagnostics monitor bar
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0x2210B981)),
                                            border = BorderStroke(1.dp, Color(0x3310B981))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isWakeWordActive) Color(0xFF10B981) else Color.Gray)
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = if (isWakeWordActive) "KWS Listening continuous ('Hey R's AI' active)" else "KWS Inactive",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Row {
                                                        Text(
                                                            text = "ဆူညံသံ: ",
                                                            color = Color.Gray,
                                                            fontSize = 11.sp
                                                        )
                                                        Text(
                                                            text = "${String.format("%.1f", currentNoiseRms)} RMS",
                                                            color = if (currentNoiseRms > 350.0) Color(0xFFEF4444) else Color(0xFF10B981),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.height(6.dp))

                                                // Dynamic visual level meter progress bar
                                                val progressNormalized = (currentNoiseRms.toFloat() / 1000f).coerceIn(0f, 1f)
                                                val meterColor = if (currentNoiseRms > 350.0) Color(0xFFF59E0B) else Color(0xFF10B981)
                                                LinearProgressIndicator(
                                                    progress = { progressNormalized },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp)),
                                                    color = meterColor,
                                                    trackColor = Color.Gray.copy(alpha = 0.2f)
                                                )
                                            }
                                        }

                                        // Chat messages
                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(chatLog) { msg ->
                                                ChatBubble(message = msg)
                                            }
                                        }

                                        // Feedback dialog proposal bar
                                        lastResult?.let { res ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                                border = BorderStroke(1.dp, neonPurple)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text("အခုအဖြေကို ကျေနပ်မှုရှိရဲ့လားခင်ဗျာ။ (Provide Feedback)", color = Color.White, fontSize = 11.sp)
                                                    Spacer(Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceEvenly
                                                    ) {
                                                        Button(
                                                            onClick = { viewModel.submitFeedback(1) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                                        ) {
                                                            Icon(Icons.Default.ThumbUp, "Up", modifier = Modifier.size(14.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("ကျေနပ်တယ်", fontSize = 10.sp)
                                                        }
                                                        Button(
                                                            onClick = { viewModel.submitFeedback(0, "မမှန်ကန်ပါ") },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                                        ) {
                                                            Icon(Icons.Default.ThumbDown, "Down", modifier = Modifier.size(14.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("မကျေနပ်ပါ", fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Suggestions Shortcuts Row
                                        HorizontalShortcutRow(
                                            onSelect = { query -> viewModel.processTextQuery(query) }
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        // Text Input and Microphone Button Bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .navigationBarsPadding(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = textInput,
                                                onValueChange = { textInput = it },
                                                placeholder = { Text("အမိန့်ပေးချက်များ သို့မဟုတ် မေးခွန်းများ...", color = Color.Gray, fontSize = 13.sp) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("chat_text_input")
                                                    .clip(RoundedCornerShape(24.dp)),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color(0xFF1E293B),
                                                    unfocusedContainerColor = Color(0xFF1E293B),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent
                                                ),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                                keyboardActions = KeyboardActions(
                                                    onSend = {
                                                        if (textInput.isNotBlank()) {
                                                            viewModel.processTextQuery(textInput)
                                                            textInput = ""
                                                            focusManager.clearFocus()
                                                            keyboardController?.hide()
                                                        }
                                                    }
                                                )
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            FloatingActionButton(
                                                onClick = {
                                                    if (isListening) viewModel.cancelVoiceCapture() else viewModel.startVoiceCapture()
                                                },
                                                containerColor = if (isListening) Color.Red else neonCyan,
                                                contentColor = Color.White,
                                                shape = CircleShape,
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .testTag("voice_record_fab")
                                            ) {
                                                Icon(
                                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Outlined.Mic,
                                                    contentDescription = "Voice Assistant Capture Button"
                                                )
                                            }
                                        }
                                    }
                                }
                                WorkspaceType.WRITING -> {
                                    // 2. AI WRITING WORKSPACE
                                    val writingOutput by viewModel.writingOutput.collectAsStateWithLifecycle()
                                    val isWritingLoading by viewModel.isWritingLoading.collectAsStateWithLifecycle()

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(110.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.img_home_banner),
                                                    contentDescription = "Creative Banner",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.5f))
                                                        .padding(12.dp),
                                                    contentAlignment = Alignment.BottomStart
                                                ) {
                                                    Column {
                                                        Text("R's Writing Studio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                                        Text("AI စနစ်ဖြင့် စာတို/အီးမေးလ်များ ဖန်တီးတည်ဆောက်ပေးခြင်း", color = Color.LightGray, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = writingInput,
                                                onValueChange = { writingInput = it },
                                                label = { Text("စာရေးသားရန် အကြောင်းအရာ ရေးသားပါ", color = Color.LightGray) },
                                                placeholder = { Text("ဥပမာ- 'ခွင့်တိုင်စာတစ်စောင် ရေးသားပေးပါ'", color = Color.DarkGray) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = neonPurple,
                                                    unfocusedBorderColor = glassBorder,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                )
                                            )
                                        }

                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val templates = listOf(
                                                    "Email" to "📧 Email",
                                                    "Summary" to "📝 Summary",
                                                    "Rewrite" to "✍️ Rewrite",
                                                    "Grammar" to "🔍 Grammar"
                                                )
                                                templates.forEach { (type, label) ->
                                                    Button(
                                                        onClick = {
                                                            viewModel.generateWriting(writingInput, type)
                                                            focusManager.clearFocus()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonPurple.copy(alpha = 0.8f)),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (isWritingLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonPurple)
                                                }
                                            }
                                        }

                                        if (writingOutput.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("AI Generated Content", fontWeight = FontWeight.Bold, color = neonPurple, fontSize = 13.sp)
                                                            IconButton(
                                                                onClick = {
                                                                    clipboardManager.setText(AnnotatedString(writingOutput))
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.ContentCopy, "Copy", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(
                                                            text = writingOutput,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            lineHeight = 20.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                WorkspaceType.IMAGE -> {
                                    // 3. AI IMAGE STUDIO (WALLPAPER, RESOLUTIONS & VEO VIDEO CREATION)
                                    val activeImgModel by viewModel.activeImageModel.collectAsStateWithLifecycle()
                                    val selectedRatio by viewModel.selectedAspectRatio.collectAsStateWithLifecycle()
                                    val selectedSize by viewModel.selectedImageSize.collectAsStateWithLifecycle()
                                    val generatedVideoUrl by viewModel.generatedVideoResult.collectAsStateWithLifecycle()
                                    val isVideoLoading by viewModel.isVideoLoading.collectAsStateWithLifecycle()

                                    var generatedImageResult by remember { mutableStateOf<Int?>(null) }
                                    var isImageLoading by remember { mutableStateOf(false) }
                                    var videoPromptInput by remember { mutableStateOf("") }
                                    var selectedVideoRatio by remember { mutableStateOf("16:9") }

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Text(
                                                "R's Image & Veo Video Studio",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                "Wallpaper Generator, Image Resolutions & Cinematic Video",
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                        }

                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x22EC4899)),
                                                border = BorderStroke(1.dp, glassBorder)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text("Image Resolution Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Spacer(Modifier.height(6.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        // Model Choice
                                                        Column(modifier = Modifier.weight(1.5f)) {
                                                            Text("Model", color = Color.LightGray, fontSize = 9.sp)
                                                            var imgModelExpanded by remember { mutableStateOf(false) }
                                                            Box {
                                                                Text(activeImgModel, color = neonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { imgModelExpanded = true })
                                                                DropdownMenu(expanded = imgModelExpanded, onDismissRequest = { imgModelExpanded = false }) {
                                                                    listOf("gemini-3.1-flash-image-preview", "gemini-3-pro-image-preview").forEach { m ->
                                                                        DropdownMenuItem(text = { Text(m) }, onClick = { viewModel.setActiveImageModel(m); imgModelExpanded = false })
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        // Aspect Ratio Choice
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text("Aspect Ratio", color = Color.LightGray, fontSize = 9.sp)
                                                            var ratioExpanded by remember { mutableStateOf(false) }
                                                            Box {
                                                                Text(selectedRatio, color = neonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { ratioExpanded = true })
                                                                DropdownMenu(expanded = ratioExpanded, onDismissRequest = { ratioExpanded = false }) {
                                                                    listOf("1:1", "3:4", "4:3", "9:16", "16:9", "21:9").forEach { r ->
                                                                        DropdownMenuItem(text = { Text(r) }, onClick = { viewModel.setSelectedAspectRatio(r); ratioExpanded = false })
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        // Size/Quality Choice
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text("Quality", color = Color.LightGray, fontSize = 9.sp)
                                                            var sizeExpanded by remember { mutableStateOf(false) }
                                                            Box {
                                                                Text(selectedSize, color = neonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { sizeExpanded = true })
                                                                DropdownMenu(expanded = sizeExpanded, onDismissRequest = { sizeExpanded = false }) {
                                                                    listOf("1K", "2K", "4K").forEach { s ->
                                                                        DropdownMenuItem(text = { Text(s) }, onClick = { viewModel.setSelectedImageSize(s); sizeExpanded = false })
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = imagePromptInput,
                                                onValueChange = { imagePromptInput = it },
                                                label = { Text("ဖန်တီးချင်သော ဓာတ်ပုံ ဖော်ပြချက် ရေးသားပါ", color = Color.LightGray) },
                                                placeholder = { Text("ဥပမာ- 'လှပသော မြန်မာ့ရွှေတိဂုံစေတီတော် ရှုခင်း Wallpaper'", color = Color.DarkGray) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = neonPink,
                                                    unfocusedBorderColor = glassBorder,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                )
                                            )
                                        }

                                        item {
                                            Button(
                                                onClick = {
                                                    isImageLoading = true
                                                    generatedImageResult = null
                                                    scope.launch {
                                                        kotlinx.coroutines.delay(2000)
                                                        isImageLoading = false
                                                        generatedImageResult = R.drawable.img_home_banner
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = neonPink),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Generate Image (Studio Quality)", fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (isImageLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        CircularProgressIndicator(color = neonPink)
                                                        Spacer(Modifier.height(4.dp))
                                                        Text("Studio Rendering active...", color = Color.LightGray, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }

                                        generatedImageResult?.let { imgRes ->
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonPink.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text("Generated Image ($selectedSize, Ratio $selectedRatio)", fontWeight = FontWeight.Bold, color = neonPink, fontSize = 11.sp)
                                                        Spacer(Modifier.height(10.dp))
                                                        Image(
                                                            painter = painterResource(id = imgRes),
                                                            contentDescription = "Simulated Image Result",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(180.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // VEO VIDEO GENERATOR SECTION
                                        item {
                                            Spacer(Modifier.height(8.dp))
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x331E1B4B)),
                                                border = BorderStroke(1.dp, neonPurple)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("🎬 Veo Cinematic 3.1 Video Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("Create ultra high-definition videos from plain text descriptions", color = Color.LightGray, fontSize = 11.sp)
                                                    Spacer(Modifier.height(10.dp))

                                                    OutlinedTextField(
                                                        value = videoPromptInput,
                                                        onValueChange = { videoPromptInput = it },
                                                        placeholder = { Text("Describe the cinematic video scene...", color = Color.DarkGray) },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = neonPurple,
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White
                                                        )
                                                    )

                                                    Spacer(Modifier.height(8.dp))

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row {
                                                            Text("Video Aspect Ratio: ", color = Color.White, fontSize = 11.sp)
                                                            Text(
                                                                text = selectedVideoRatio,
                                                                color = neonCyan,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.clickable {
                                                                    selectedVideoRatio = if (selectedVideoRatio == "16:9") "9:16" else "16:9"
                                                                }
                                                            )
                                                        }

                                                        Button(
                                                            onClick = { viewModel.generateVideo(videoPromptInput, selectedVideoRatio) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = neonPurple),
                                                            enabled = !isVideoLoading
                                                        ) {
                                                            Text("Animate (Veo 3.1)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (isVideoLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonPurple)
                                                }
                                            }
                                        }

                                        generatedVideoUrl?.let { videoUrl ->
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text("Veo Generated Video", fontWeight = FontWeight.Bold, color = neonPurple, fontSize = 11.sp)
                                                        Spacer(Modifier.height(8.dp))
                                                        // Real Android native video stream view inside Compose!
                                                        AndroidView(
                                                            factory = { ctx ->
                                                                VideoView(ctx).apply {
                                                                    setVideoURI(Uri.parse(videoUrl))
                                                                    start()
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(200.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                WorkspaceType.DOCUMENTS -> {
                                    // 4. DOCUMENT HUB & LYRIA MUSIC STUDIO & MULTIMODAL MEDIA ANALYZER
                                    val documentSummary by viewModel.documentSummary.collectAsStateWithLifecycle()
                                    val isDocLoading by viewModel.isDocumentSummaryLoading.collectAsStateWithLifecycle()
                                    val meetingTranscript by viewModel.meetingTranscript.collectAsStateWithLifecycle()
                                    val isTranscriptLoading by viewModel.isTranscriptLoading.collectAsStateWithLifecycle()

                                    // Lyria States
                                    val isMusicLoading by viewModel.isMusicLoading.collectAsStateWithLifecycle()
                                    val musicModel by viewModel.activeMusicModel.collectAsStateWithLifecycle()
                                    val musicResultUrl by viewModel.generatedMusicResult.collectAsStateWithLifecycle()
                                    var musicPromptInput by remember { mutableStateOf("") }

                                    // Multimodal states
                                    var imageAnalysisPrompt by remember { mutableStateOf("") }
                                    val analyzedResult by viewModel.analyzedImageResult.collectAsStateWithLifecycle()
                                    val isImageAnalyzing by viewModel.isImageAnalyzing.collectAsStateWithLifecycle()

                                    var audioTranscriptPrompt by remember { mutableStateOf("") }
                                    val audioTranscriptResult by viewModel.audioTranscriptResult.collectAsStateWithLifecycle()
                                    val isAudioTranscribing by viewModel.isAudioTranscribing.collectAsStateWithLifecycle()

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Text("R's Doc, Voice & Sound Hub", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                            Text("Lyria Music Studio, PDF Summarizers & Audio Transcribers", color = Color.LightGray, fontSize = 11.sp)
                                        }

                                        // LYRIA MUSIC GENERATOR
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x3306B6D4)),
                                                border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("🎵 Lyria AI Music Studio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                    Text("Create melodic tracks or clips with deep music modeling", color = Color.LightGray, fontSize = 11.sp)
                                                    Spacer(Modifier.height(10.dp))

                                                    OutlinedTextField(
                                                        value = musicPromptInput,
                                                        onValueChange = { musicPromptInput = it },
                                                        placeholder = { Text("An ambient lofi track with traditional saung elements...", color = Color.DarkGray) },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = neonCyan,
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White
                                                        )
                                                    )

                                                    Spacer(Modifier.height(8.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        var musicModelExpanded by remember { mutableStateOf(false) }
                                                        Box {
                                                            Text("Model: $musicModel", color = neonCyan, fontSize = 11.sp, modifier = Modifier.clickable { musicModelExpanded = true })
                                                            DropdownMenu(expanded = musicModelExpanded, onDismissRequest = { musicModelExpanded = false }) {
                                                                listOf("lyria-3-clip-preview", "lyria-3-pro-preview").forEach { m ->
                                                                    DropdownMenuItem(text = { Text(m) }, onClick = { viewModel.setActiveMusicModel(m); musicModelExpanded = false })
                                                                }
                                                            }
                                                        }

                                                        Button(
                                                            onClick = { viewModel.generateMusic(musicPromptInput, musicModel) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                                            enabled = !isMusicLoading
                                                        ) {
                                                            Text("Compose Track", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (isMusicLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonCyan)
                                                }
                                            }
                                        }

                                        musicResultUrl?.let { mUrl ->
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text("Lyria Audio Output", fontWeight = FontWeight.Bold, color = neonCyan, fontSize = 12.sp)
                                                        Spacer(Modifier.height(6.dp))
                                                        
                                                        // Custom Interactive Media Player Controls
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            IconButton(onClick = {}) {
                                                                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White)
                                                            }
                                                            Spacer(Modifier.width(8.dp))
                                                            Slider(
                                                                value = 0.35f,
                                                                onValueChange = {},
                                                                modifier = Modifier.weight(1f),
                                                                colors = SliderDefaults.colors(thumbColor = neonCyan, activeTrackColor = neonCyan)
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                            Text("0:12 / 0:30", color = Color.LightGray, fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // MULTIMODAL AUDIO TRANSCRIPTION
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x33EC4899)),
                                                border = BorderStroke(1.dp, glassBorder)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("🎤 Gemini Speech Transcription (Transcriber)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Spacer(Modifier.height(6.dp))
                                                    OutlinedTextField(
                                                        value = audioTranscriptPrompt,
                                                        onValueChange = { audioTranscriptPrompt = it },
                                                        placeholder = { Text("Translate or transcribe spoken audio files...", color = Color.DarkGray) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    Spacer(Modifier.height(8.dp))
                                                    Button(
                                                        onClick = { viewModel.transcribeAudioContent(audioTranscriptPrompt) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonPink),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Transcribe Audio Content (using 3.5-flash)")
                                                    }
                                                }
                                            }
                                        }

                                        if (isAudioTranscribing) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonPink)
                                                }
                                            }
                                        }

                                        if (audioTranscriptResult.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text("Spoken Audio Transcript", color = neonPink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(audioTranscriptResult, color = Color.White, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }

                                        // IMAGE MULTIMODAL ANALYZER
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x338B5CF6)),
                                                border = BorderStroke(1.dp, glassBorder)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("🖼️ Visual Multimodal Analyzer", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Spacer(Modifier.height(6.dp))
                                                    OutlinedTextField(
                                                        value = imageAnalysisPrompt,
                                                        onValueChange = { imageAnalysisPrompt = it },
                                                        placeholder = { Text("What information do you want to extract from image?", color = Color.DarkGray) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    Spacer(Modifier.height(8.dp))
                                                    Button(
                                                        onClick = { viewModel.analyzeImageContent(imageAnalysisPrompt) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonPurple),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Analyze Image content (using 3.1-Pro)")
                                                    }
                                                }
                                            }
                                        }

                                        if (isImageAnalyzing) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonPurple)
                                                }
                                            }
                                        }

                                        if (analyzedResult.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text("Media Analysis Output", color = neonPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(analyzedResult, color = Color.White, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }

                                        // ORIGINAL PDF DOC SUMMARIZER
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x3394A3B8)),
                                                border = BorderStroke(1.dp, glassBorder)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("Select PDF Document to Analyze", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Spacer(Modifier.height(8.dp))
                                                    val docs = listOf("business_report.pdf", "study_guide.pdf", "trip_itinerary.pdf")
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        docs.forEach { doc ->
                                                            val isDocSelected = selectedDocument == doc
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isDocSelected) neonCyan.copy(alpha = 0.3f) else Color.DarkGray)
                                                                    .clickable { selectedDocument = doc }
                                                                    .padding(8.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(doc, color = if (isDocSelected) neonCyan else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                    Spacer(Modifier.height(10.dp))
                                                    Button(
                                                        onClick = {
                                                            viewModel.summarizeDocument(selectedDocument, "PDF report")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Summarize Document (အနှစ်ချုပ် ထုတ်ပါ)", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (isDocLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonCyan)
                                                }
                                            }
                                        }

                                        if (documentSummary.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text("Document Insights Summary", fontWeight = FontWeight.Bold, color = neonCyan, fontSize = 13.sp)
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(documentSummary, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                WorkspaceType.SMART_HOME -> {
                                    // 5. SMART HOME CONTROLLER
                                    val acStatus by viewModel.acStatus.collectAsStateWithLifecycle()
                                    val acTemperature by viewModel.acTemperature.collectAsStateWithLifecycle()
                                    val tvStatus by viewModel.tvStatus.collectAsStateWithLifecycle()
                                    val tvChannel by viewModel.tvChannel.collectAsStateWithLifecycle()
                                    val lightStatus by viewModel.lightStatus.collectAsStateWithLifecycle()
                                    val lightColor by viewModel.lightColor.collectAsStateWithLifecycle()
                                    val lightIntensity by viewModel.lightIntensity.collectAsStateWithLifecycle()

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Text("R's Smart Home Control Center", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                            Text("အိမ်သုံးပစ္စည်းများကို AI ဖြင့် အော့ဖ်လိုင်း အဝေးမှ ထိန်းချုပ်ရန်", color = Color.LightGray, fontSize = 11.sp)
                                        }

                                        // Air Conditioner Card
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x3306B6D4)),
                                                border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("❄️ Air Conditioner (လေအေးပေးစက်)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                            Text(if (acStatus) "Active - $acTemperature°C" else "OFF", color = Color.LightGray, fontSize = 11.sp)
                                                        }
                                                        Switch(
                                                            checked = acStatus,
                                                            onCheckedChange = { viewModel.toggleAc() },
                                                            colors = SwitchDefaults.colors(checkedThumbColor = neonCyan)
                                                        )
                                                    }
                                                    if (acStatus) {
                                                        Spacer(Modifier.height(10.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Button(
                                                                onClick = { viewModel.adjustAcTemp(false) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                                            ) {
                                                                Text("- Temp")
                                                            }
                                                            Text("$acTemperature°C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                            Button(
                                                                onClick = { viewModel.adjustAcTemp(true) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                                            ) {
                                                                Text("+ Temp")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Smart TV Card
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x338B5CF6)),
                                                border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("📺 Smart TV (တီဗွီ)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                            Text(if (tvStatus) "ON - Channel: $tvChannel" else "OFF", color = Color.LightGray, fontSize = 11.sp)
                                                        }
                                                        Switch(
                                                            checked = tvStatus,
                                                            onCheckedChange = { viewModel.toggleTv() },
                                                            colors = SwitchDefaults.colors(checkedThumbColor = neonPurple)
                                                        )
                                                    }
                                                    if (tvStatus) {
                                                        Spacer(Modifier.height(10.dp))
                                                        Text("Select Channel", color = Color.LightGray, fontSize = 11.sp)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            val channels = listOf("MRTV HD", "CHANNEL 7", "MESSENGER", "FORTUNE")
                                                            channels.forEach { ch ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(if (tvChannel == ch) neonPurple.copy(alpha = 0.3f) else Color.DarkGray)
                                                                        .clickable { viewModel.setTvChannel(ch) }
                                                                        .padding(8.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(ch, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Smart Lights Card
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x33EC4899)),
                                                border = BorderStroke(1.dp, neonPink.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("💡 Smart Light Color themes", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                            Text(if (lightStatus) "ON - $lightColor ($lightIntensity%)" else "OFF", color = Color.LightGray, fontSize = 11.sp)
                                                        }
                                                        Switch(
                                                            checked = lightStatus,
                                                            onCheckedChange = { viewModel.toggleLight() },
                                                            colors = SwitchDefaults.colors(checkedThumbColor = neonPink)
                                                        )
                                                    }
                                                    if (lightStatus) {
                                                        Spacer(Modifier.height(10.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            val lightColors = listOf("Warm Gold", "Ocean Blue", "Electric Purple", "Sunset Red")
                                                            lightColors.forEach { color ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(if (lightColor == color) neonPink.copy(alpha = 0.3f) else Color.DarkGray)
                                                                        .clickable { viewModel.setLightColor(color) }
                                                                        .padding(8.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(color, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                        Spacer(Modifier.height(10.dp))
                                                        Text("Brightness Intensity", color = Color.LightGray, fontSize = 11.sp)
                                                        Slider(
                                                            value = lightIntensity.toFloat(),
                                                            onValueChange = { viewModel.adjustLightIntensity(it.toInt()) },
                                                            valueRange = 10f..100f,
                                                            colors = SliderDefaults.colors(
                                                                thumbColor = neonPink,
                                                                activeTrackColor = neonPink
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                WorkspaceType.TRANSLATOR -> {
                                    // 6. SCREEN VISION & TRANSLATION INTERPRETER
                                    val translationOutput by viewModel.translationOutput.collectAsStateWithLifecycle()
                                    val isTransLoading by viewModel.isTranslationLoading.collectAsStateWithLifecycle()
                                    val screenVisionResult by viewModel.screenVisionResult.collectAsStateWithLifecycle()
                                    val isVisionLoading by viewModel.isScreenVisionLoading.collectAsStateWithLifecycle()

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Text("R's Screen Vision & Interpreter", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                            Text("Real-time Gemini Translation & Screen error analysis", color = Color.LightGray, fontSize = 11.sp)
                                        }

                                        // Translation Panel
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x338B5CF6)),
                                                border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("AI Translator & Interpreter", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Spacer(Modifier.height(8.dp))
                                                    OutlinedTextField(
                                                        value = translateInput,
                                                        onValueChange = { translateInput = it },
                                                        label = { Text("စာသား သို့မဟုတ် စကားစု ရေးသားပါ", color = Color.LightGray) },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = neonPurple,
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White
                                                        )
                                                    )
                                                    Spacer(Modifier.height(10.dp))
                                                    Text("Select Target Language", color = Color.LightGray, fontSize = 11.sp)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        val langs = listOf("English", "Chinese", "Japanese", "Korean", "Thai")
                                                        langs.forEach { lang ->
                                                            val isLangSelected = targetLanguage == lang
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isLangSelected) neonPurple.copy(alpha = 0.3f) else Color.DarkGray)
                                                                    .clickable { targetLanguage = lang }
                                                                    .padding(8.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(lang, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                    Spacer(Modifier.height(12.dp))
                                                    Button(
                                                        onClick = { viewModel.translateText(translateInput, targetLanguage) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonPurple),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Translate (ဘာသာပြန်ဆိုမည်)", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (isTransLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonPurple)
                                                }
                                            }
                                        }

                                        if (translationOutput.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text("Translation Result", fontWeight = FontWeight.Bold, color = neonPurple, fontSize = 13.sp)
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(translationOutput, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                                                    }
                                                }
                                            }
                                        }

                                        // Screen Vision Panel
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x3306B6D4)),
                                                border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("AI Screen Understanding (Vision)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Text("ဖုန်းမျက်နှာပြင်တွင် ဖြစ်ပျက်နေသည်များကို ဆန်းစစ်သုံးသပ်ပေးခြင်း", color = Color.LightGray, fontSize = 11.sp)
                                                    Spacer(Modifier.height(10.dp))
                                                    val screenStates = listOf(
                                                        "System Error Dialog on Main Home launcher",
                                                        "Play Store download stalled on Telegram App",
                                                        "Myanmar fonts configuration page mismatch error"
                                                    )
                                                    screenStates.forEach { state ->
                                                        val isStateSelected = selectedScreenState == state
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (isStateSelected) neonCyan.copy(alpha = 0.2f) else Color.DarkGray)
                                                                .clickable { selectedScreenState = state }
                                                                .padding(10.dp)
                                                        ) {
                                                            Text(state, color = if (isStateSelected) neonCyan else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    Spacer(Modifier.height(12.dp))
                                                    Button(
                                                        onClick = { viewModel.scanScreen(selectedScreenState) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Analyze Screen content (စကရင်ကို ခွဲခြမ်းစိတ်ဖြာပါ)", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (isVisionLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonCyan)
                                                }
                                            }
                                        }

                                        if (screenVisionResult.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text("Screen Intelligence Output", fontWeight = FontWeight.Bold, color = neonCyan, fontSize = 13.sp)
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(screenVisionResult, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: Local Offline Feedback Database Log Viewer
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Local Feedback Logs",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                               )
                                Text(
                                    "ဒေတာဘေ့စ်ရှိ သုံးသပ်ချက်နှင့် ပြင်ဆင်ချက်များ",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                            if (feedbacks.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearAllFeedback() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                    modifier = Modifier.testTag("clear_feedback_button")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, "Clear database")
                                    Spacer(Modifier.width(4.dp))
                                    Text("အားလုံးဖျက်ပါ")
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (feedbacks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Storage,
                                        contentDescription = "Empty Database",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "အော့ဖ်လိုင်း သုံးသပ်ချက်များ မရှိသေးပါ",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "အကူအညီပေးသူ၏ အမေးအဖြေများကို 👍 သို့မဟုတ် 👎 ပေးပြီး စမ်းသပ်သိမ်းဆည်းနိုင်ပါသည်ခင်ဗျာ။",
                                        color = Color.DarkGray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(feedbacks) { item ->
                                    FeedbackCard(item = item, onDelete = { viewModel.deleteFeedback(item.id) })
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: Offline Technical Architecture & Build Guide
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "R's AI (Myanmar HyperAI)",
                                fontWeight = FontWeight.Bold,
                                color = neonCyan,
                                fontSize = 18.sp
                            )
                            Text(
                                "အော့ဖ်လိုင်းနှင့် အွန်လိုင်းစနစ်များ၏ နည်းပညာ တည်ဆောက်ပုံ အနှစ်ချုပ်",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        }
                        item {
                            GuideCard(
                                title = "STT (Speech-to-Text)",
                                description = "အသံလှိုင်းများကို စာသားအဖြစ် ပြောင်းလဲခြင်း",
                                details = "Model: Whisper-Tiny (Fine-tuned on Burmese)\nFormat: TensorFlow Lite (FP16/INT8 Quantized)\nProcessing: Offline inference via TFLite runtime C++ wrapper."
                            )
                        }
                        item {
                            GuideCard(
                                title = "NLU (Natural Language Understanding)",
                                description = "စာသား၏ ရည်ရွယ်ချက်နှင့် စကားလုံးများ ခွဲထုတ်ခြင်း",
                                details = "Model: mBART or XLM-RoBERTa (Burmese Instruction dataset)\nFormat: ONNX Runtime (Mobile Optimized)\nCapabilities: Wifi, Bluetooth, Volume, Flashlight, Brightness, DND, Calling, Notes, Knowledge QA, Reminders."
                            )
                        }
                        item {
                            GuideCard(
                                title = "TTS (Text-to-Speech)",
                                description = "စာသားမှ သဘာဝကျသော မြန်မာအသံ ထွက်ပေါ်ခြင်း",
                                details = "Model: Tacotron2 or VITS (Myanmar Male/Female voice audio dataset)\nFormat: ONNX Model / native engine bridge\nSpeed: Real-time RTF < 0.6 on average Android SoC hardware acceleration."
                            )
                        }
                        item {
                            GuideCard(
                                title = "Continuous Wake Word (KWS)",
                                description = "အမြဲတမ်း နားထောင်ပြီး အမိန့်ပေးရန် အဆင်သင့်ဖြစ်စေခြင်း",
                                details = "Model: MicroSpeech CNN keyword spotter (110 KB)\nFormat: TensorFlow Lite Micro (Continuous audio circular buffer pooling)\nKeyword: \"Hey Bro\" (Hello Assistant)."
                            )
                        }

                        // 🚀 User's Gemini Live Clone Build Roadmap
                        item {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "🚀 Gemini Live Clone Build Roadmap",
                                fontWeight = FontWeight.Bold,
                                color = neonPurple,
                                fontSize = 18.sp
                            )
                            Text(
                                "Gemini Live ကဲ့သို့ အဆင့်မြင့် AI Voice Assistant ကို အခြေခံမှ Production Level အထိ တည်ဆောက်နိုင်မည့် ပြည့်စုံသော လမ်းညွှန်",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        }

                        // Project Structure Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x1A8B5CF6)),
                                border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "📂 Production Architecture Layout",
                                        fontWeight = FontWeight.Bold,
                                        color = neonPurple,
                                        fontSize = 13.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Gemini-Live-Clone/\n" +
                                                "├── app/\n" +
                                                "│   ├── ui/ (Screens, Theme, Navigation)\n" +
                                                "│   ├── ai/ (Prompt, Reasoning, Memory, Tools, Model)\n" +
                                                "│   ├── voice/ (STT, TTS, Wakeword, Streaming, Audio)\n" +
                                                "│   ├── vision/ (Camera, OCR, Object Detection)\n" +
                                                "│   ├── database/ (Room DB, SharedPreferences)\n" +
                                                "│   └── settings/ (User Preferences, API Keys)\n" +
                                                "└── backend/\n" +
                                                "    ├── api/ & websocket/ (Realtime Audio Socket)\n" +
                                                "    └── vector-db/ (Long-term RAG semantic memory)",
                                        color = Color(0xFFE0E7FF),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        val phases = listOf(
                            Triple("📚 Phase 1 — Foundation", "Android Studio, Kotlin, Jetpack Compose, MVVM Architecture, Coroutines, Room Database, Retrofit, Dependency Injection (Hilt/Koin), Git/GitHub", Color(0xFF60A5FA)),
                            Triple("🧠 Phase 2 — AI Brain (AI Engine)", "Speech -> Speech-To-Text -> Prompt Builder -> Gemini API -> Reasoning -> Response -> TTS. Includes Prompt Manager, Conversation Context, Token Manager, and Error Handling.", Color(0xFF34D399)),
                            Triple("🧠 Phase 3 — Memory System", "Short Memory (Session active chat logs) and Long Memory (User profile DB storing User name, Favorite language, Custom commands, Preferences).", Color(0xFFFBBF24)),
                            Triple("🎤 Phase 4 — Voice Pipeline", "Mic -> Noise Reduction -> VAD -> Wake Word -> Speech Recognition -> AI -> TTS -> Speaker. Features include Interrupt speaking and Low latency audio streaming.", Color(0xFFF87171)),
                            Triple("👀 Phase 5 — Vision (Camera Live)", "Camera frame capture -> OCR / Object detection -> Image understanding -> Gemini Vision. Analyze documents, receipts, plants, or animals live.", Color(0xFFC084FC)),
                            Triple("📄 Phase 6 — Files Analyzer", "Support formats: PDF, DOCX, TXT, PPT, Excel, Images. Flow: Open file -> Extract text -> Gemini AI Analysis -> Voice synthesis summary.", Color(0xFF22D3EE)),
                            Triple("🌐 Phase 7 — Browser Agent", "Live Web search grounding, Scrapers & RAG (Retrieval-Augmented Generation) to read online news, weather, or real-time topics.", Color(0xFFFB7185)),
                            Triple("🔌 Phase 8 — Tool Calling", "AI Tool Router: Automatically translate natural language into function calls for Google Calendar, Maps, Weather, Phone, and Alarms.", Color(0xFF818CF8)),
                            Triple("📱 Phase 9 — Android OS Control", "Voice Commands to trigger physical system actions: call, SMS, open apps, toggle flashlight, Bluetooth, Wi-Fi, volume, brightness, and screenshot.", Color(0xFF34D399)),
                            Triple("🌍 Phase 10 — Live Conversation", "Bidirectional low-latency audio streaming channel (using WebSockets or gRPC) for hands-free voice chats like real phone conversations.", Color(0xFF60A5FA)),
                            Triple("🔒 Phase 11 — Security & Privacy", "API Key Protection, Local Data Encryption, Secure Storage (EncryptedSharedPreferences), Biometric authentication.", Color(0xFFF87171)),
                            Triple("☁️ Phase 12 — Cloud Sync", "Firebase / Google Drive synchronization for cross-device conversation histories, user settings, and personalized memories.", Color(0xFFC084FC)),
                            Triple("🤖 Phase 13 — Automation Tasks", "Voice automation loops (e.g. \"မနက် ၆ နာရီ Alarm ဖွင့်ပေးပြီး ရာသီဥတုသတင်း ဖတ်ပြပါ\") automatically scheduled via Voice Intent.", Color(0xFF22D3EE)),
                            Triple("🧩 Phase 14 — Plugin System", "Integration plugins for third-party services: Gmail, Google Calendar, YouTube, Google Maps, Spotify, and Translate APIs.", Color(0xFFFB7185)),
                            Triple("🧠 Phase 15 — Local AI (Offline SLM)", "Offline Voice Assistant: local Whisper STT, Gemma 2B (On-device LLM via MediaPipe GenAI SDK), and local Sherpa-ONNX TTS.", Color(0xFF818CF8))
                        )

                        phases.forEach { (title, desc, color) ->
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = title,
                                            fontWeight = FontWeight.Bold,
                                            color = color,
                                            fontSize = 13.sp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = desc,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 3: Custom Voice Command Mappings & FIREBASE AUTH SECURE SYNC
                    val voiceMappings by viewModel.voiceMappings.collectAsStateWithLifecycle()
                    val savedApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
                    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }
                    var newPhrase by remember { mutableStateOf("") }
                    var selectedIntent by remember { mutableStateOf(IntentType.TOGGLE_DARK_MODE) }
                    var dropdownExpanded by remember { mutableStateOf(false) }

                    // Firebase states
                    val fbUserEmail by viewModel.firebaseUserEmail.collectAsStateWithLifecycle()
                    val isFbLoading by viewModel.isFirebaseLoading.collectAsStateWithLifecycle()
                    val syncStatus by viewModel.firestoreSyncStatus.collectAsStateWithLifecycle()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "R's AI Settings & Security",
                                fontWeight = FontWeight.Bold,
                                color = neonCyan,
                                fontSize = 18.sp
                            )
                            Text(
                                "စိတ်ကြိုက် ဆက်တင်များနှင့် အမိန့်ပေးစနစ်များကို ဤနေရာတွင် စီမံခန့်ခွဲနိုင်ပါသည်ခင်ဗျာ။",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        }

                        // FIREBASE CLOUD SYNC CARD
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x331E1B4B)),
                                border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudSync, "Sync", tint = neonPurple)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Firebase Authentication & Sync", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text("Back up your voice command profiles, personal settings, and conversational learning history to Firestore securely.", color = Color.LightGray, fontSize = 11.sp)
                                    Spacer(Modifier.height(12.dp))

                                    if (isFbLoading) {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = neonPurple)
                                        }
                                    } else {
                                        if (fbUserEmail == null) {
                                            var emailInput by remember { mutableStateOf("") }
                                            OutlinedTextField(
                                                value = emailInput,
                                                onValueChange = { emailInput = it },
                                                label = { Text("Google Email account", color = Color.Gray) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Button(
                                                onClick = { if (emailInput.isNotBlank()) viewModel.firebaseSignIn(emailInput) },
                                                colors = ButtonDefaults.buttonColors(containerColor = neonPurple),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Sign In with Google Account")
                                            }
                                        } else {
                                            Column {
                                                Text("Logged in as: $fbUserEmail", color = neonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("Status: $syncStatus", color = Color.LightGray, fontSize = 11.sp)
                                                Spacer(Modifier.height(10.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = { viewModel.syncToFirestore() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Sync Now", fontSize = 11.sp)
                                                    }
                                                    Button(
                                                        onClick = { viewModel.firebaseSignOut() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Sign Out", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Gemini API Key Config Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x3306B6D4)),
                                border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Gemini AI API Key (အွန်လိုင်း အသိဉာဏ်တု)",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "အွန်လိုင်းမှတဆင့် Gemini AI ကဲ့သို့ ပိုမိုစမတ်ကျစွာ တုန့်ပြန်နိုင်ရန် သင်၏ Gemini API Key ကို ဤနေရာတွင် ထည့်သွင်းပါခင်ဗျာ။ API key မရှိပါက simulated model ဖြင့် ဖြေကြားပေးပါမည်။",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = apiKeyInput,
                                        onValueChange = { apiKeyInput = it },
                                        label = { Text("Gemini API Key", color = Color.Gray) },
                                        placeholder = { Text("AIzaSy...", color = Color.DarkGray) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("gemini_api_key_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = neonCyan,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            viewModel.saveGeminiApiKey(apiKeyInput)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("save_api_key_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
                                    ) {
                                        Icon(Icons.Default.Save, "Save API Key", tint = Color.Black)
                                        Spacer(Modifier.width(6.dp))
                                        Text("API Key သိမ်းဆည်းရန် (Save API Key)", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }

                        // UI Accessibility Service Settings Card
                        item {
                            val isServiceActive = com.example.service.AssistantAccessibilityService.isServiceRunning
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x22EC4899)),
                                border = BorderStroke(1.dp, neonPink.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Visibility, "Screen content", tint = neonPink)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "UI Accessibility (မျက်နှာပြင်နှင့် အဝေးထိန်း)",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                        
                                        // Status badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isServiceActive) Color(0xFF10B981) else Color(0xFFEF4444))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                if (isServiceActive) "ACTIVE" else "INACTIVE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "ယခုစနစ်သည် အက်စစ်စတန့်အား သင့်ဖုန်းမျက်နှာပြင်မှ စာသားများကို ဖတ်ရှုနားလည်စေရန်နှင့် သင့်ကိုယ်စား ခလုတ်နှိပ်ခြင်း၊ စာရိုက်ခြင်း စသည့် အဝေးထိန်းလုပ်ဆောင်ချက်များကို လုပ်ဆောင်ပေးနိုင်ရန် ကူညီပေးပါသည်ခင်ဗျာ။",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(Modifier.height(12.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                    android.widget.Toast.makeText(context, "Accessibility settings opened. Locate \"R's AI\" to activate.", android.widget.Toast.LENGTH_LONG).show()
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Error opening settings", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = neonPink),
                                            modifier = Modifier.weight(1.5f)
                                        ) {
                                            Icon(Icons.Default.Settings, "Configure", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("ခွင့်ပြုချက်ပေးရန်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Button(
                                            onClick = {
                                                if (isServiceActive) {
                                                    val service = com.example.service.AssistantAccessibilityService.instance
                                                    val summary = service?.getScreenTextSummary() ?: "No active nodes detected."
                                                    android.widget.Toast.makeText(context, "မျက်နှာပြင်ဖတ်မှု:\n${summary.take(200)}...", android.widget.Toast.LENGTH_LONG).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "မျက်နှာပြင်ဖတ်စနစ် စမ်းသပ်ရန် ပထမဦးစွာ ခွင့်ပြုချက်ပေးပါခင်ဗျာ။", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, "Test", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("စမ်းသပ်ရန်", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "Custom Voice Commands (စိတ်ကြိုက် အမိန့်ပေးစနစ်များ)",
                                fontWeight = FontWeight.Bold,
                                color = neonCyan,
                                fontSize = 14.sp
                            )
                            Text(
                                "ကိုယ်ပိုင် Burmese voice commands များကို သတ်မှတ်ပြီး ဖုန်းလုပ်ဆောင်ချက်များနှင့် ချိတ်ဆက်သိမ်းဆည်းပါ",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        }

                        // Form card to add new mappings
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x33334155)),
                                border = BorderStroke(1.dp, glassBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "အမိန့်အသစ် ထည့်သွင်းရန် (Add Custom Command Mapping)",
                                        fontWeight = FontWeight.Bold,
                                        color = neonPurple,
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = newPhrase,
                                        onValueChange = { newPhrase = it },
                                        label = { Text("ပြောမည့် စကားစု (e.g. အိပ်ချင်ပြီ)", color = Color.Gray) },
                                        placeholder = { Text("အမိန့်ပေး စကားလုံး ရေးပါ...", color = Color.DarkGray) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("custom_phrase_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = neonCyan,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { dropdownExpanded = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("intent_dropdown_trigger"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("ခေါ်ယူမည့် လုပ်ဆောင်ချက်: ${selectedIntent.name}")
                                                Icon(Icons.Default.ArrowDropDown, "Expand dropdown")
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false },
                                            modifier = Modifier
                                                .background(Color(0xFF1E293B))
                                                .width(300.dp)
                                        ) {
                                            val validIntents = listOf(
                                                IntentType.TOGGLE_DARK_MODE,
                                                IntentType.TOGGLE_FLASHLIGHT,
                                                IntentType.TOGGLE_WIFI,
                                                IntentType.TOGGLE_BLUETOOTH,
                                                IntentType.VOLUME_UP,
                                                IntentType.VOLUME_DOWN,
                                                IntentType.MUTE_VOLUME,
                                                IntentType.TAKE_SCREENSHOT,
                                                IntentType.START_SCREEN_RECORDING,
                                                IntentType.OPEN_GALLERY,
                                                IntentType.CHECK_BATTERY,
                                                IntentType.CHECK_DIAGNOSTICS,
                                                IntentType.TOGGLE_POWER_SAVING,
                                                IntentType.TOGGLE_DND
                                            )
                                            validIntents.forEach { intent ->
                                                DropdownMenuItem(
                                                    text = { Text(intent.name, color = Color.White, fontSize = 13.sp) },
                                                    onClick = {
                                                        selectedIntent = intent
                                                        dropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (newPhrase.isNotBlank()) {
                                                viewModel.saveVoiceMapping(newPhrase, selectedIntent)
                                                newPhrase = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("save_mapping_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
                                    ) {
                                        Icon(Icons.Default.Add, "Add")
                                        Spacer(Modifier.width(6.dp))
                                        Text("အမိန့်အဖြစ် သိမ်းဆည်းမည် (Save Command)", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "လက်ရှိ သိမ်းဆည်းထားသော အမိန့်များ (Saved voice mappings)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        if (voiceMappings.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("စိတ်ကြိုက် သတ်မှတ်ထားသော အမိန့်များ မရှိသေးပါခင်ဗျာ။", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(voiceMappings.toList()) { (phrase, intent) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("mapping_item_${phrase}"),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val icon = when (intent) {
                                                IntentType.TOGGLE_WIFI -> Icons.Default.Settings
                                                IntentType.TOGGLE_BLUETOOTH -> Icons.Default.Settings
                                                IntentType.VOLUME_UP -> Icons.Default.Notifications
                                                IntentType.VOLUME_DOWN -> Icons.Default.Notifications
                                                IntentType.MUTE_VOLUME -> Icons.Default.Notifications
                                                IntentType.TOGGLE_FLASHLIGHT -> Icons.Default.Build
                                                IntentType.TOGGLE_DARK_MODE -> Icons.Default.Settings
                                                IntentType.TAKE_SCREENSHOT -> Icons.Default.Create
                                                IntentType.START_SCREEN_RECORDING -> Icons.Default.Create
                                                IntentType.OPEN_GALLERY -> Icons.Default.Palette
                                                IntentType.CHECK_BATTERY -> Icons.Default.Info
                                                IntentType.CHECK_DIAGNOSTICS -> Icons.Default.Info
                                                IntentType.TOGGLE_POWER_SAVING -> Icons.Default.Info
                                                IntentType.TOGGLE_DND -> Icons.Default.Settings
                                                else -> Icons.Default.Settings
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(neonCyan.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(icon, contentDescription = intent.name, tint = neonCyan, modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "\"$phrase\"",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "လုပ်ဆောင်ချက်: ${intent.name}",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteVoiceMapping(phrase) },
                                            modifier = Modifier.testTag("delete_mapping_${phrase}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete mapping", tint = Color.Red.copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // GEMINI LIVE LOW-LATENCY FULL-SCREEN CALL OVERLAY
            val isVoiceCallActive by viewModel.isVoiceCallActive.collectAsStateWithLifecycle()
            val voiceCallState by viewModel.voiceCallState.collectAsStateWithLifecycle()

            if (isVoiceCallActive) {
                Dialog(onDismissRequest = { viewModel.stopVoiceCall() }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
                        border = BorderStroke(2.dp, neonCyan),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Gemini Live Call", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(voiceCallState, color = Color.LightGray, fontSize = 12.sp)
                            }

                            // Rotating Voice Waveform graphic
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, neonCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                VoiceWaveform(color = neonCyan)
                            }

                            Button(
                                onClick = { viewModel.stopVoiceCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.CallEnd, "Hang up", tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("End Session", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == SenderType.USER
    val isSystem = message.sender == SenderType.SYSTEM

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = when {
            isSystem -> Alignment.Center
            isUser -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        if (isSystem) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF334155))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 0.dp,
                                bottomEnd = if (isUser) 0.dp else 16.dp
                            )
                        )
                        .background(
                            if (isUser) Color(0xFF8B5CF6) else Color(0xFF1E293B)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )

                        // Show classification badge for assistant messages
                        message.intent?.let { int ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Intent: ${int.name}",
                                color = Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Show physical action execution note
                message.actionDetails?.let { act ->
                    Text(
                        text = act,
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeedbackCard(item: FeedbackEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, if (item.rating == 1) Color(0xFF059669) else Color(0xFFDC2626))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.rating == 1) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                        contentDescription = "Rating status",
                        tint = if (item.rating == 1) Color(0xFF34D399) else Color(0xFFF87171),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (item.rating == 1) "ကျေနပ်မှု ရှိသည်" else "ပြန်လည်ပြင်ဆင်ရန်",
                        color = if (item.rating == 1) Color(0xFF34D399) else Color(0xFFF87171),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, "Delete item", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("User Speech (Prompt):", color = Color.Gray, fontSize = 10.sp)
            Text(item.prompt, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(4.dp))
            Text("Assistant Answer (Response):", color = Color.Gray, fontSize = 10.sp)
            Text(item.response, color = Color.LightGray, fontSize = 13.sp)

            item.correctionComment?.let { comm ->
                if (comm.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("ညွှန်ကြားပြင်ဆင်ချက် (Correction Comments):", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(comm, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideCard(title: String, description: String, details: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 14.sp)
            Text(description, color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A))
                    .padding(8.dp)
            ) {
                Text(
                    text = details,
                    color = Color(0xFFA5B4FC),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun HorizontalShortcutRow(onSelect: (String) -> Unit) {
    val shortcuts = listOf(
        "ဝိုင်ဖိုင် ဖွင့်ပါ" to "🌐 Wifi ဖွင့်",
        "ဘလူးတုသ် ဖွင့်ပါ" to "📡 Bluetooth",
        "ဓာတ်မီး ဖွင့်ပေးပါ" to "🔦 Flashlight ဖွင့်",
        "ဓာတ်မီး ပိတ်ပါ" to "🔦 Flashlight ပိတ်",
        "ဘက်ထရီ စစ်ပေးပါ" to "🔋 Battery စစ်",
        "အမှောင်မုဒ် ပြောင်းပါ" to "🌒 Dark Mode",
        "စကရင်ရှော့ ရိုက်ပါ" to "📸 Screenshot",
        "စကရင်ဗီဒီယို ရိုက်ပါ" to "🎥 Record Screen",
        "ပုံများ ဖွင့်ပါ" to "🖼️ Gallery",
        "မက်ဆေ့ခ်ျ ပို့ပါ" to "💬 SMS စာပို့",
        "09123456789 ကို ဖုန်းခေါ်ပေးပါ" to "📞 Call",
        "တည်နေရာ ပြပါဦး" to "🗺️ Maps",
        "ချိန်းဆိုမှု ပြက္ခဒိန်ထဲ ထည့်ပါ" to "🗓️ Calendar",
        "မှတ်စု ရေးမှတ်ပါ" to "📝 Note",
        "ပုဂံအကြောင်း ပြောပြပါ" to "🏛️ Pagan QA",
        "ကဗျာတစ်ပုဒ် ရေးပြပါ" to "✍️ Poem"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(shortcuts) { (query, label) ->
            SuggestionChip(
                onClick = { onSelect(query) },
                label = { Text(label, color = Color.White) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFF1E293B)
                )
            )
        }
    }
}

@Composable
fun VoiceWaveform(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2
        val path = Path()

        path.moveTo(0f, midY)
        for (x in 0..width.toInt() step 5) {
            val relativeX = x / width
            val sine = Math.sin((relativeX * 3 * Math.PI) + waveOffset).toFloat()
            val dampening = (1f - Math.abs(relativeX - 0.5f) * 2).coerceIn(0f, 1f)
            val y = midY + sine * 15f * dampening
            path.lineTo(x.toFloat(), y)
        }

        drawPath(
            path = path,
            color = color.copy(alpha = 0.5f),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
