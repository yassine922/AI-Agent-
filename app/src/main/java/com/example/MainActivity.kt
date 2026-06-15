package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AgentViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Wrap everything in RTL to ensure gorgeous Arabic typography and flow
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun DownloadIcon(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = (w * 0.1f).coerceIn(4f, 12f)
        
        // Vertical stem of the arrow
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.12f),
            end = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.62f),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // Arrowhead sides
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.62f),
            end = androidx.compose.ui.geometry.Offset(w / 2f - w * 0.22f, h * 0.42f),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.62f),
            end = androidx.compose.ui.geometry.Offset(w / 2f + w * 0.22f, h * 0.42f),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // Base flat bar
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.82f),
            end = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.82f),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AgentViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeAgentId by viewModel.activeAgentId.collectAsStateWithLifecycle()

    val apisList by viewModel.apisList.collectAsStateWithLifecycle()
    val opportunitiesList by viewModel.opportunitiesList.collectAsStateWithLifecycle()
    val clientLeadsList by viewModel.clientLeadsList.collectAsStateWithLifecycle()
    val newsList by viewModel.newsList.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showDownloadDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getTabTitle(currentTab),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                ),
                actions = {
                    IconButton(onClick = { showDownloadDialog = true }) {
                        DownloadIcon(
                            modifier = Modifier.size(26.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (currentTab == "chat") {
                        IconButton(onClick = {
                            viewModel.clearChat(activeAgentId)
                            Toast.makeText(context, "تم مسح المحادثة", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "مسح المحادثة",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "dashboard",
                    onClick = { viewModel.setTab("dashboard") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "الوكلاء") },
                    label = { Text("الوكلاء", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == "chat",
                    onClick = { viewModel.setTab("chat") },
                    icon = { Icon(Icons.Default.Share, contentDescription = "الدردشة") }, // share used as connection hub
                    label = { Text("الدردشة", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == "apis",
                    onClick = { viewModel.setTab("apis") },
                    icon = { Icon(Icons.Default.List, contentDescription = "مكتبة APIs") },
                    label = { Text("APIs", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == "gigs",
                    onClick = { viewModel.setTab("gigs") },
                    icon = { Icon(Icons.Default.Star, contentDescription = "الفرص") },
                    label = { Text("الفرص", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == "project_builder",
                    onClick = { viewModel.setTab("project_builder") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "باني الكود") },
                    label = { Text("باني التطبيقات", fontSize = 11.sp) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        apisCount = apisList.size,
                        gigsCount = opportunitiesList.size,
                        newsCount = newsList.size
                    )
                    "chat" -> ChatScreen(viewModel = viewModel)
                    "apis" -> ApisLibraryScreen(viewModel = viewModel, apisList = apisList)
                    "gigs" -> GigsScreen(viewModel = viewModel, opportunitiesList = opportunitiesList, clientLeadsList = clientLeadsList)
                    "project_builder" -> ProjectBuilderScreen(viewModel = viewModel)
                    "news" -> NewsBrowserScreen(viewModel = viewModel, newsList = newsList)
                }
            }
        }
    }

    if (showDownloadDialog) {
        Dialog(onDismissRequest = { showDownloadDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        DownloadIcon(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "تنزيل وتثبيت التطبيق 📱",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "لتثبيت التطبيق الفعلي على هاتفك الأندرويد والاستمتاع بكامل سرعته وثباته دون الحاجة للمتصفح، يرجى اتباع الخطوات الموضحة بالأسفل لتنزيل الـ APK الآن من المنصة:",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 خطوات تحميل الـ APK بكل سهولة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("1.", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "انظر إلى الهامش السفلي الأيمن لواجهة المنصة (خارج شاشة التطبيق الزرقاء)، واضغط على زر النقاط الثلاث (•••) المتواجد بجانب زر Preview.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("2.", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "اضغط على خيار \"Settings\" (الإعدادات) لتفتح قائمة خيارات النظام الجانبية.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("3.", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "ابحث عن خيار \"Versions\" (إصدارات البناء) واضغط عليه لعرض ملفات البناء المجمعة.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("4.", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "اضغط على أحدث إصدار ناجح (Latest Compile Row) المكتوب في القائمة، ومن هناك مباشرة قم بالضغط على زر \"Download APK\" لبدء التحميل فوراً على هاتفك وتثبيته!",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showDownloadDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مفهوم، دعنا نبدأ! 👍")
                    }
                }
            }
        }
    }
}

private fun getTabTitle(tab: String): String {
    return when (tab) {
        "dashboard" -> "لوحة تحكم الوكيل المستقل"
        "chat" -> "الدردشة التفاعلية مع الوكلاء"
        "apis" -> "مكتشف الـ APIs المجانية"
        "gigs" -> "الفرص ومولد العروض"
        "project_builder" -> "باني التطبيقات للعمل الحر"
        "news" -> "متصفح الأخبار وعالم التقنية"
        else -> "الوكيل المستقل"
    }
}

// --- SCREEN 1: DASHBOARD ---

@Composable
fun DashboardScreen(
    viewModel: AgentViewModel,
    apisCount: Int,
    gigsCount: Int,
    newsCount: Int
) {
    val activeAgentId by viewModel.activeAgentId.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Welcome Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مرحباً بك في مركز الوكيل المستقل! 🚀",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "منظومة عمل ذكية ومتكاملة لتسهيل مسيرتك في العمل الحر كمهندس تطبيقات أندرويد. اختر أحد الوكلاء المتخصصين المتاحين أدناه، وتناقش معهم لتنمية وتجربة أفكارك البرمجية وحصد العروض الناجحة.",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Active AI Systems Pulse Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                    Text(
                        text = "كافة منظومات الذكاء المتقدمة المكونة من سبعة وكلاء (7) متصلة بالكامل ومستعدة لاقتناص فرصك واستقطاب عملائك ⚡",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "APIs مجانية",
                    value = apisCount.toString(),
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "فرص متاحة",
                    value = gigsCount.toString(),
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "الجريدة التقنية",
                    value = if (newsCount == 0) "اقرأ" else newsCount.toString(),
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab("news") }
                )
            }
        }

        // Selection Section
        item {
            Text(
                text = "اختر الوكيل النشط للمحادثة وللعمل معه:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        // The 4 Agents list
        items(getAgentsList()) { agent ->
            val isSelected = activeAgentId == agent.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.setActiveAgent(agent.id)
                        viewModel.setTab("chat")
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) agent.accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(agent.accentColor, agent.accentColor))) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(agent.accentColor.copy(alpha = 0.2f), circleShapeForAgent(agent.id))
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = agent.icon,
                            contentDescription = agent.name,
                            tint = agent.accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = agent.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isSelected) agent.accentColor else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = agent.description,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "نشط",
                            tint = agent.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(95.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )

        }
    }
}

// --- SCREEN 2: CHAT SCREEN ---

@Composable
fun ChatScreen(viewModel: AgentViewModel) {
    val activeAgentId by viewModel.activeAgentId.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val chatLoading by viewModel.chatLoading.collectAsStateWithLifecycle()

    val currentAgent = remember(activeAgentId) {
        getAgentsList().find { it.id == activeAgentId } ?: getAgentsList().first()
    }

    var messageText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Agent Quick Switcher Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الوكيل النشط:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(getAgentsList()) { ag ->
                    val isAct = ag.id == activeAgentId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isAct) ag.accentColor else MaterialTheme.colorScheme.surface)
                            .clickable { viewModel.setActiveAgent(ag.id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = ag.icon,
                                contentDescription = ag.name,
                                tint = if (isAct) Color.White else ag.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ag.chatTabName,
                                color = if (isAct) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Chat Space
        Box(modifier = Modifier.weight(1f)) {
            if (chatHistory.isEmpty()) {
                // Intro Greeting Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(currentAgent.accentColor.copy(alpha = 0.2f), CircleShape)
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = currentAgent.icon,
                            contentDescription = currentAgent.name,
                            tint = currentAgent.accentColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "مرحباً! أنا ${currentAgent.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentAgent.accentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "أنا جاهز تماماً لمساعدتك اليوم في هدفنا المشترك للعمل المستقل وبناء أعمالك! هذه بعض الاقتراحات السريعة التي يمكنك سؤالي عنها:",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    currentAgent.suggestions.forEach { suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { messageText = suggestion },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            } else {
                // Messages List Flow
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                ) {
                    items(chatHistory) { msg ->
                        ChatBubble(
                            message = msg,
                            agentColor = currentAgent.accentColor,
                            onCopyClick = {
                                clipboardManager.setText(AnnotatedString(msg.content))
                                Toast.makeText(context, "تم تحديد الكود ونسخه للمحافظة", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (chatLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = currentAgent.accentColor.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = currentAgent.accentColor
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "الوكيل يستجمع قواه الذكية للتفكير والكتابة...",
                                            fontSize = 12.sp,
                                            color = currentAgent.accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input Field Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("اكتب رسالتك للوكيل البرمجي هنا...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier
                        .background(currentAgent.accentColor, CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    agentColor: Color,
    onCopyClick: () -> Unit
) {
    val bubbleColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.61f)
    }

    val arrangement = if (message.isUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 295.dp)
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = if (message.isUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
            } else {
                RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
            }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header of sender
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (message.isUser) "أنت" else "الوكيل الذكي",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (message.isUser) MaterialTheme.colorScheme.primary else agentColor
                    )

                    // Quick Copy for code/chats
                    if (!message.isUser) {
                        IconButton(
                            onClick = onCopyClick,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "نسخ",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// --- SCREEN 3: APIs LIBRARY SCREEN ---

@Composable
fun ApisLibraryScreen(viewModel: AgentViewModel, apisList: List<FreeApi>) {
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog state controllers for primary API
    var nameState by remember { mutableStateOf("") }
    var providerState by remember { mutableStateOf("") }
    var categoryState by remember { mutableStateOf("") }
    var urlState by remember { mutableStateOf("") }
    var descriptionState by remember { mutableStateOf("") }
    var integrationCodeState by remember { mutableStateOf("") }

    // Dialog state controllers for backup API
    var fallbackNameState by remember { mutableStateOf("") }
    var fallbackUrlState by remember { mutableStateOf("") }
    var fallbackDescriptionState by remember { mutableStateOf("") }
    var fallbackIntegrationCodeState by remember { mutableStateOf("") }

    val apiStatuses by viewModel.apiStatuses.collectAsState()

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (apisList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "مكتبة واجهات الـ APIs فارغة!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "يمكنك الطلب من الوكيل فحص APIs جديدة من الشركات بالدردشة، أو إضافة عنوان يدويًا بالزر أدناه.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🛡️ نظام حماية استقرار العمل الحر والاتصال التلقائي (Auto-Fallback)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تتضمن قاعدة البيانات حالياً 15 واجهة API مجانية متكاملة بفحوصات حية. إذا انقطع الاتصال بأي خادم أو فشل الـ API الرئيسي، يقوم النظام تلقائياً بالتمويه والتحويل إلى واجهة برمجة تطبيقات بديلة وجلب كود الربط الفوري الخاص بها للحفاظ على تطبيقات عملائك قوية ودائمة الاستقرار!",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "الـ APIs المجانية والاحتياطية المتوفرة بالنظام:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(apisList) { api ->
                    var isExpanded by remember { mutableStateOf(false) }
                    val status = apiStatuses[api.id] ?: "idle"
                    val isFallbackActive = status == "fallback"

                    // Reactive values swapped dynamically if fallback is active
                    val activeName = if (isFallbackActive && api.fallbackName.isNotBlank()) api.fallbackName else api.name
                    val activeProvider = if (isFallbackActive) "مُفَعّل تلقائياً (رابط بديل)" else "مقدم الخدمة: ${api.provider}"
                    val activeUrl = if (isFallbackActive && api.fallbackUrl.isNotBlank()) api.fallbackUrl else api.url
                    val activeDescription = if (isFallbackActive && api.fallbackDescription.isNotBlank()) api.fallbackDescription else api.description
                    val activeCode = if (isFallbackActive && api.fallbackIntegrationCode.isNotBlank()) api.fallbackIntegrationCode else api.integrationCode

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFallbackActive) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isFallbackActive) {
                            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800))
                        } else {
                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { isExpanded = !isExpanded }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = activeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isFallbackActive) Color(0xFFE65100) else Color(0xFF2196F3)
                                        )
                                        if (isFallbackActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFF9800).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("نظام الطوارئ 🚨", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                            }
                                        }
                                    }
                                    Text(
                                        text = activeProvider,
                                        fontSize = 11.sp,
                                        color = if (isFallbackActive) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2196F3).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = api.category,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2196F3)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = activeDescription,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { isExpanded = !isExpanded }
                            )

                            // High technology state panel
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (status) {
                                    "online" -> {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("🟢 مستجيب ويعمل", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    "fallback" -> {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFFE0B2), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("💥 معطل - حُوِّل تلقائياً", fontSize = 10.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    "checking" -> {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE3F2FD), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp, color = Color(0xFF1976D2))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("جاري الاتصال والطلب...", fontSize = 10.sp, color = Color(0xFF1976D2))
                                            }
                                        }
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("⚪ جاهز للفحص والربط", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // Controller operations row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.testAndAutoSwitchApi(api.id, api.url) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.12f), contentColor = Color(0xFF1976D2)),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("فحص الاتصال والتبديل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                if (isFallbackActive) {
                                    Button(
                                        onClick = { viewModel.resetApiStatus(api.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("استعادة الخادم الأصلي", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.simulateFallback(api.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFC62828)),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("قطع اتصال الخادم الأصلي", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (isExpanded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "الرابط النشط حالياً بالتطبيق:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF2196F3)
                                            )
                                            if (isFallbackActive) {
                                                Text(
                                                    text = "(الرابط البديل للطوارئ)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF9800)
                                                )
                                            }
                                        }
                                        
                                        Text(
                                            text = activeUrl,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "كود كوتلن للربط الفوري (تحديث تلقائي):",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF2196F3)
                                        )
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = activeCode.ifBlank { "// لم يُولّد كود ربط بعد لهذا الـ API." },
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .fillMaxWidth()
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    clipboard.setText(AnnotatedString(activeCode))
                                                    Toast.makeText(context, "تم نسخ الكود النشط بنجاح", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.height(34.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isFallbackActive) Color(0xFFFF9800) else Color(0xFF2196F3)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp)
                                            ) {
                                                Text("نسخ كود الربط الفعّال", fontSize = 10.sp, color = Color.White)
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteApi(api.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف الـ API",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        if (api.fallbackName.isNotBlank() && !isFallbackActive) {
                                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                                            Text(
                                                text = "ℹ️ تفاصيل الـ API الاحتياطي المدمج للطوارئ:",
                                                fontSize = 10.sp,
                                                color = Color(0xFFFF9800),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${api.fallbackName} (${api.fallbackUrl}) - ${api.fallbackDescription}",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to add APIs
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة API")
        }

        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "سجل واجهة API مع خيار البديل التلقائي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF2196F3)
                            )
                        }

                        item {
                            Text(
                                text = "تفاصيل الـ API الرئيسي:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = nameState,
                                onValueChange = { nameState = it },
                                label = { Text("اسم الـ API الرئيسي", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = providerState,
                                onValueChange = { providerState = it },
                                label = { Text("اسم الشركة / المزود الرئيسي", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = categoryState,
                                onValueChange = { categoryState = it },
                                label = { Text("التصنيف (طقس، مالية، ذكاء اصطناعي)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = urlState,
                                onValueChange = { urlState = it },
                                label = { Text("الرابط الأساسي الرئيسي (Endpoint URL)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = descriptionState,
                                onValueChange = { descriptionState = it },
                                label = { Text("وصف خدمة الـ API الرئيسي", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = integrationCodeState,
                                onValueChange = { integrationCodeState = it },
                                label = { Text("أكواد ربط كوتلن للـ الرئيسي", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "تفاصيل الـ API الاحتياطي البديل (اختياري للطوارئ):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = fallbackNameState,
                                onValueChange = { fallbackNameState = it },
                                label = { Text("اسم الـ API الاحتياطي", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = fallbackUrlState,
                                onValueChange = { fallbackUrlState = it },
                                label = { Text("الرابط الأساسي الاحتياطي (Fallback URL)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = fallbackDescriptionState,
                                onValueChange = { fallbackDescriptionState = it },
                                label = { Text("وصف خدمة الـ API الاحتياطي", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = fallbackIntegrationCodeState,
                                onValueChange = { fallbackIntegrationCodeState = it },
                                label = { Text("أكواد ربط كوتلن للـ احتياطي", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAddDialog = false }) {
                                    Text("إلغاء", color = Color.Gray)
                                }
                                Box(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (nameState.isNotBlank() && providerState.isNotBlank()) {
                                            viewModel.addManualApi(
                                                name = nameState,
                                                provider = providerState,
                                                desc = descriptionState,
                                                cat = categoryState,
                                                url = urlState,
                                                code = integrationCodeState,
                                                fallbackName = fallbackNameState,
                                                fallbackUrl = fallbackUrlState,
                                                fallbackDesc = fallbackDescriptionState,
                                                fallbackCode = fallbackIntegrationCodeState
                                            )
                                            // Reset States
                                            nameState = ""
                                            providerState = ""
                                            categoryState = ""
                                            urlState = ""
                                            descriptionState = ""
                                            integrationCodeState = ""
                                            fallbackNameState = ""
                                            fallbackUrlState = ""
                                            fallbackDescriptionState = ""
                                            fallbackIntegrationCodeState = ""
                                            showAddDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                                ) {
                                    Text("حفظ بقاعدة البيانات", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 4: FREELANCE GIGS SCREEN ---

@Composable
fun GigsScreen(
    viewModel: AgentViewModel,
    opportunitiesList: List<FreelanceOpportunity>,
    clientLeadsList: List<ClientLead>
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Platforms Opportunities, 1 = Clients Hunt/CRM
    var searchKeyword by remember { mutableStateOf("") }
    var clientKeyword by remember { mutableStateOf("") }

    val proposalLoading by viewModel.proposalLoading.collectAsStateWithLifecycle()
    val generatedProposal by viewModel.generatedProposal.collectAsStateWithLifecycle()

    val leadLoading by viewModel.leadLoading.collectAsStateWithLifecycle()
    val generatedLeadPitch by viewModel.generatedLeadPitch.collectAsStateWithLifecycle()

    var activeOpportunitySelected by remember { mutableStateOf<FreelanceOpportunity?>(null) }
    var activeLeadSelected by remember { mutableStateOf<ClientLead?>(null) }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row for switching between platform opportunities and local client hunting
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("منصات العمل الحر 💼", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("صائد العملاء المستهدفين 🎯", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (activeSubTab == 0) {
                // Existing opportunities screen layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Header Search Input
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "البحث والجمع من شركات ومواقع العمل المستقل:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchKeyword,
                                    onValueChange = { searchKeyword = it },
                                    placeholder = { Text("اكتب مهارة للبحث عن فرص عمل حر..", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (searchKeyword.isNotBlank()) {
                                            viewModel.searchAndAddOpportunity(searchKeyword)
                                            searchKeyword = ""
                                        }
                                    },
                                    enabled = !proposalLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                                ) {
                                    if (proposalLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.dp, color = Color.White)
                                    } else {
                                        Text("جمع الفرص", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (opportunitiesList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لم نجد أي فرص مسجلة. جرب الضغط على 'جمع الفرص' بالذكاء الاصطناعي أعلاه!",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(opportunitiesList) { opp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = opp.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFFFF9800)
                                                )
                                                Text(
                                                    text = "الموقع: ${opp.platform} | الميزانية المتوقعة: ${opp.budget}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = if (opp.matchPercent >= 90) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF9800).copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "المطابقة: ${opp.matchPercent}%",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (opp.matchPercent >= 90) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = opp.description,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "المهارات المستهدفة: ${opp.requiredSkills}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    activeOpportunitySelected = opp
                                                    viewModel.generateCustomBidProposal(opp)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                                            ) {
                                                Text("كتابة عرض احترافي ⚡", fontSize = 11.sp, color = Color.White)
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteOpportunity(opp.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف فرصة",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Client Hunter & Outreach CRM sub-tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "البحث وتتنقيب الشركات والمحلات عن طريق الذكاء الاصطناعي:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFFF5722)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = clientKeyword,
                                    onValueChange = { clientKeyword = it },
                                    placeholder = { Text("مثال: عيادات أسنان، مطابخ، نوادي رياضية..", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (clientKeyword.isNotBlank()) {
                                            viewModel.searchAndAddClientLead(clientKeyword)
                                            clientKeyword = ""
                                        }
                                    },
                                    enabled = !leadLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                                ) {
                                    if (leadLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.dp, color = Color.White)
                                    } else {
                                        Text("البحث عن عملاء", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (clientLeadsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "قاعدة بيانات العملاء فارغة. جرب كتابة نشاط تجاري في البحث مثل 'عيادة' أو 'مقهى' للبدء في صيد العملاء التقنيين!",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(clientLeadsList) { lead ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = lead.businessName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFFFF5722)
                                                )
                                                Text(
                                                    text = lead.industry,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            val potentialColor = when (lead.conversionPotential) {
                                                "High" -> Color(0xFF4CAF50)
                                                "Medium" -> Color(0xFFFF9800)
                                                else -> Color.Gray
                                            }
                                            val potentialLabel = when (lead.conversionPotential) {
                                                "High" -> "فرصة ممتازة 🔥"
                                                "Medium" -> "مناسبة 👍"
                                                else -> "صعبة 👀"
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = potentialColor.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = potentialLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = potentialColor
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Callout Box for Pain Points
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                                                .padding(10.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "⚠️ الفجوة ونقطة الألم لدى العميل:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = lead.painPoint,
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // App Concept Suggestion
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                                .padding(10.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "💡 فكرة التطبيق والحل المقترح:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = lead.mobileAppConcept,
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    activeLeadSelected = lead
                                                    viewModel.generateOutreachPitch(lead)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                                            ) {
                                                Text("توليد العرض وحملة التواصل 📝", fontSize = 11.sp, color = Color.White)
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteClientLead(lead.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف العميل",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
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

        // Dialog for Freelance proposal
        if (generatedProposal.isNotBlank() && activeOpportunitySelected != null) {
            Dialog(onDismissRequest = { viewModel.clearProposal(); activeOpportunitySelected = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "عرض العمل المستهدف الفائز بقوة الذكاء الاصطناعي 🎯",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFFF9800)
                        )

                        Text(
                            text = activeOpportunitySelected!!.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider()

                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .maxHeightIn(250.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                item {
                                    Text(
                                        text = generatedProposal,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.clearProposal(); activeOpportunitySelected = null }) {
                                Text("إغلاق", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(generatedProposal))
                                    Toast.makeText(context, "تم نسخ عرض المشروع بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                            ) {
                                Text("نسخ للتقديم فوراً", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Dialog for Client outreach pitch
        if (generatedLeadPitch.isNotBlank() && activeLeadSelected != null) {
            Dialog(onDismissRequest = { viewModel.clearLeadPitch(); activeLeadSelected = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "رسالة التواصل المباشرة واستقطاب صاحب العمل ✉️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFFF5722)
                        )

                        Text(
                            text = activeLeadSelected!!.businessName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider()

                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .maxHeightIn(250.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                item {
                                    Text(
                                        text = generatedLeadPitch,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.clearLeadPitch(); activeLeadSelected = null }) {
                                Text("إغلاق", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(generatedLeadPitch))
                                    Toast.makeText(context, "تم نسخ رسالة العميل واستقطابه بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                            ) {
                                Text("نسخ الرسالة", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 5: PROJECT BUILDER SCREEN ---

@Composable
fun ProjectBuilderScreen(viewModel: AgentViewModel) {
    var libraryApiName by remember { mutableStateOf("") }
    var userConceptName by remember { mutableStateOf("") }

    val projectLoading by viewModel.projectLoading.collectAsStateWithLifecycle()
    val generatedProjectCode by viewModel.generatedProjectCode.collectAsStateWithLifecycle()

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "باني وهيكلية مشاريع العمل الحر (Freelance Workspace) 💻",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "اكتب اسم الـ API وفكرة التطبيق، وسيقوم وكيل التطوير والذكاء بتهيئة وبناء أكواد أندرويد حقيقية كاملة لكوتلن وCompose لتسلم المشروع في دقائق!",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = libraryApiName,
                onValueChange = { libraryApiName = it },
                label = { Text("اسم الـ API المستخدم (مثال: OpenWeather, Custom-Shop)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = userConceptName,
                onValueChange = { userConceptName = it },
                label = { Text("فكرة وخدمات التطبيق المستهدفة بالكامل للكتابة بالتفصيل", fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Button(
                onClick = {
                    if (libraryApiName.isNotBlank() && userConceptName.isNotBlank()) {
                        viewModel.buildFreelancingAppTemplate(libraryApiName, userConceptName)
                    }
                },
                enabled = !projectLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (projectLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("وكيل التطوير يكتب الأكواد...", color = Color.White)
                } else {
                    Text("تصدير وبناء الأكواد المتكاملة للعمل الحر", color = Color.White)
                }
            }
        }

        if (generatedProjectCode.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الكود البرمجي المولد جاهز للاستخدام:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(generatedProjectCode))
                                    Toast.makeText(context, "تم نسخ الكود الكلي للمحافظة", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "نسخ الكود", modifier = Modifier.size(16.dp))
                            }
                        }

                        Divider()

                        Text(
                            text = generatedProjectCode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// --- SCREEN 6: NEWS BROWSER SCREEN ---

@Composable
fun NewsBrowserScreen(viewModel: AgentViewModel, newsList: List<TechNewsItem>) {
    val chatLoading by viewModel.chatLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
            Text(
                text = "موجز الأخبار والتحديثات للـ APIs الصاعدة:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Button(
                onClick = { viewModel.fetchLatestTechNews() },
                enabled = !chatLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
            ) {
                if (chatLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.dp, color = Color.White)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جلب بـ AI", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (newsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "الجريدة فارغة حالياً. قم بالضغط على 'جلب بـ AI' ليستعرض الوكيل أحدث الأخبار لك!",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(newsList) { news ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = news.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF9C27B0),
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { viewModel.deleteNews(news.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "مسح الخبر",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "المصدر: ${news.source}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = news.summary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER METADATA FOR THE 4 AGENTS ---

data class AgentMetaData(
    val id: String,
    val name: String,
    val chatTabName: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color,
    val suggestions: List<String>
)

fun getAgentsList(): List<AgentMetaData> {
    return listOf(
        AgentMetaData(
            id = "api_agent",
            name = "وكيل كشف وتكامل الـ APIs",
            chatTabName = "جامع الـ APIs",
            description = "متخصص في كشف وربط واجهات برمجة التطبيقات المجانية لشركات التقنية وكتابة كود Kotlin المكامل لها.",
            icon = Icons.Default.List,
            accentColor = Color(0xFF2196F3),
            suggestions = listOf(
                "ابحث عن APIs مجانية للحصول على أسعار الذهب والعملات.",
                "كيف أربط OpenWeather API مع تطبيق أندرويد؟",
                "أقترح علي شركات توفر APIs مجانية ممتازة لبناء مشاريع تداول."
            )
        ),
        AgentMetaData(
            id = "dev_agent",
            name = "وكيل التطوير وتجربة الأكواد",
            chatTabName = "مطور ومختبر الأكواد",
            description = "مساعدك البرمجي المحترف لكتابة، تصحيح الأخطاء، ومطابقة الأكواد البرمجية للعمل المستقل.",
            icon = Icons.Default.Build,
            accentColor = Color(0xFF4CAF50),
            suggestions = listOf(
                "اكتب لي واجهة Room Database كاملة لحفظ المقالات.",
                "لماذا يظهر خطأ NullPointerException عند الاتصال بـ Retrofit؟",
                "كيف يمكنني تحسين كود Jetpack Compose مخصص لقائمة سريعة التحديث؟"
            )
        ),
        AgentMetaData(
            id = "design_agent",
            name = "وكيل تفوق التصميم وواجهات Material 3",
            chatTabName = "مهندس الـ UI/UX",
            description = "خبير في هندسة تجربة المستخدم وتصميم واجهات أندرويد متوافقة مع أحدث إرشادات Material Design 3.",
            icon = Icons.Default.Face,
            accentColor = Color(0xFFE91E63),
            suggestions = listOf(
                "اقترح علي باليتة ألوان دافئة وحديثة لتطبيق رعاية صحية وطاقة.",
                "كيف أطبق تأثيرات البطاقات المشطوفة والظلال الناعمة بـ Compose؟",
                "ما هي الطريقة القياسية لتوزيع المساحات Touch Targets للأشكال اللمسية؟"
            )
        ),
        AgentMetaData(
            id = "pricing_agent",
            name = "وكيل التفاوض وتثمين العقود والأسعار",
            chatTabName = "أخصائي التسعير والمفاوضات",
            description = "مستشارك المالي والقانوني لتقدير قيم المشاريع وتجزئتها باحترافية وتوثيق عقود العمل الحر.",
            icon = Icons.Default.ShoppingCart,
            accentColor = Color(0xFF009688),
            suggestions = listOf(
                "كيف أحسب سعر الساعة مقابل السعر الثابت لمشروع أندرويد متكامل؟",
                "كيف أقسم العمل لمشروع كبير إلى دفعات Milestones لتفادي المخاطرة؟",
                "اكتب لي مسودة تعاقد باللغة العربية لشرح شروط الملكية الفكرية وصيانة التطبيق."
            )
        ),
        AgentMetaData(
            id = "opp_agent",
            name = "وكيل التعلم واقتناص فرص العمل الحر",
            chatTabName = "مقتنص فرص مستقل",
            description = "يدربك على مهارات السوق ويعلمك كتابة العروض وتقديم خدمات برمجية ناجحة لأصحاب الأعمال.",
            icon = Icons.Default.Star,
            accentColor = Color(0xFFFF9800),
            suggestions = listOf(
                "كيف أحصل على أول مشروع لي كمطور أندرويد مستقل؟",
                "اكتب لي قالباً جذاباً للتنافس على مشروع تطبيق توصيل طلبات.",
                "ما هي أكثر المهارات المطلوبة حالياً في منصة مستقل بمجال الهواتف؟"
            )
        ),
        AgentMetaData(
            id = "client_agent",
            name = "وكيل صيد واستكشاف العملاء المحليين",
            chatTabName = "صائد العملاء المستهدفين",
            description = "متخصص في تنقيب واستكشاف الشركات المحلية والمتاجر المعرضة للخروج من السوق دون تطبيقات الهواتف، وصياغة أفضل رسائل جذب مخصصة (Cold Outreach).",
            icon = Icons.Default.Search,
            accentColor = Color(0xFFFF5722),
            suggestions = listOf(
                "ابحث عن عملاء محتملين في قطاع المراكز والمجمعات الرياضية.",
                "اكتب رسالة ترويجية أولى لعيادة بيطرية لإقناعهم بامتلاك تطبيق أندرويد.",
                "كيف أقنع صاحب مطعم تقليدي بالانتقال إلى نظام الطلبات الرقمي الخاص به؟"
            )
        ),
        AgentMetaData(
            id = "news_agent",
            name = "وكيل الأخبار والاتجاهات الحديثة",
            chatTabName = "متصفح أخبار التقنية",
            description = "يجمع المستجدات التقنية، أخبار الـ APIs الصاعدة، وتحديثات أندرويد لمنحك السبق والتفوق البرمجي.",
            icon = Icons.Default.Notifications,
            accentColor = Color(0xFF9C27B0),
            suggestions = listOf(
                "ما هي مخرجات مؤتمر Google I/O الأخيرة بخصوص أندرويد؟",
                "لخص لي آخر الاتجاهات في إطار عمل Jetpack Compose لعام 2026.",
                "هل توجد تحديثات أمنية هامة تجب مراعاتها عند برمجة APIs هواتف؟"
            )
        )
    )
}

fun circleShapeForAgent(agentId: String) = RoundedCornerShape(12.dp)

// Extension functions for max height constraint
fun Modifier.maxHeightIn(max: androidx.compose.ui.unit.Dp) = this.heightIn(max = max)
