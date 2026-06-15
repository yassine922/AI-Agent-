package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AgentViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AgentRepository(database)

    // Current navigation tab
    private val _currentTab = MutableStateFlow("dashboard") // "dashboard", "chat", "apis", "gigs", "news", "project_builder"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Current Active Agent for Chat
    private val _activeAgentId = MutableStateFlow("api_agent") // "api_agent", "dev_agent", "design_agent", "pricing_agent", "opp_agent", "client_agent", "news_agent"
    val activeAgentId: StateFlow<String> = _activeAgentId.asStateFlow()

    // Chat states
    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    // Screen states derived from Room Flows
    val apisList: StateFlow<List<FreeApi>> = repository.allApis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val opportunitiesList: StateFlow<List<FreelanceOpportunity>> = repository.allOpportunities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientLeadsList: StateFlow<List<ClientLead>> = repository.allLeads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val newsList: StateFlow<List<TechNewsItem>> = repository.allNews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat history for selected Agent (re-evaluates automatically when activeAgentId changes)
    val chatHistory: StateFlow<List<ChatMessage>> = _activeAgentId
        .flatMapLatest { agentId -> repository.getChatHistory(agentId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Interactive custom state for generator outputs
    private val _generatedProposal = MutableStateFlow("")
    val generatedProposal: StateFlow<String> = _generatedProposal.asStateFlow()

    private val _proposalLoading = MutableStateFlow(false)
    val proposalLoading: StateFlow<Boolean> = _proposalLoading.asStateFlow()

    // Client Lead outreach states
    private val _generatedLeadPitch = MutableStateFlow("")
    val generatedLeadPitch: StateFlow<String> = _generatedLeadPitch.asStateFlow()

    private val _leadLoading = MutableStateFlow(false)
    val leadLoading: StateFlow<Boolean> = _leadLoading.asStateFlow()

    private val _generatedProjectCode = MutableStateFlow("")
    val generatedProjectCode: StateFlow<String> = _generatedProjectCode.asStateFlow()

    private val _projectLoading = MutableStateFlow(false)
    val projectLoading: StateFlow<Boolean> = _projectLoading.asStateFlow()

    // API connection check states. ID maps to status: "idle", "checking", "online", "fallback"
    private val _apiStatuses = MutableStateFlow<Map<Int, String>>(emptyMap())
    val apiStatuses: StateFlow<Map<Int, String>> = _apiStatuses.asStateFlow()

    fun testAndAutoSwitchApi(apiId: Int, url: String) {
        viewModelScope.launch {
            _apiStatuses.value = _apiStatuses.value + (apiId to "checking")
            
            // Run network operations in IO Dispatcher
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                try {
                    val cleanedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        "https://$url"
                    } else {
                        url
                    }
                    val request = okhttp3.Request.Builder()
                        .url(cleanedUrl)
                        .header("User-Agent", "Android Agent Hub App")
                        .build()
                    
                    client.newCall(request).execute().use { _ ->
                        // Any socket reply indicates host is alive!
                        _apiStatuses.value = _apiStatuses.value + (apiId to "online")
                    }
                } catch (e: Exception) {
                    // Down, DNS-failing or timed out => auto-switch to fallback
                    _apiStatuses.value = _apiStatuses.value + (apiId to "fallback")
                }
            }
        }
    }

    fun simulateFallback(apiId: Int) {
        _apiStatuses.value = _apiStatuses.value + (apiId to "fallback")
    }

    fun resetApiStatus(apiId: Int) {
        _apiStatuses.value = _apiStatuses.value + (apiId to "idle")
    }

    init {
        // Seed database on launch with initial info
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setActiveAgent(agentId: String) {
        _activeAgentId.value = agentId
    }

    // --- Actions ---

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val agentId = _activeAgentId.value

        viewModelScope.launch {
            // 1. Save user message to database
            repository.insertMessage(
                ChatMessage(
                    agentId = agentId,
                    content = text,
                    isUser = true
                )
            )

            _chatLoading.value = true

            // 2. Select system instruction based on active agent
            val systemInstruction = getSystemInstructionForAgent(agentId)

            // 3. Request Gemini AI response
            val responseText = GeminiApiClient.getGeminiResponse(
                prompt = text,
                systemInstruction = systemInstruction
            )

            // 4. Save agent response to database
            repository.insertMessage(
                ChatMessage(
                    agentId = agentId,
                    content = responseText,
                    isUser = false
                )
            )

            _chatLoading.value = false
        }
    }

    fun clearChat(agentId: String) {
        viewModelScope.launch {
            repository.clearChatHistory(agentId)
        }
    }

    // --- API Finder Actions ---

    fun addManualApi(
        name: String,
        provider: String,
        desc: String,
        cat: String,
        url: String,
        code: String,
        fallbackName: String = "",
        fallbackUrl: String = "",
        fallbackDesc: String = "",
        fallbackCode: String = ""
    ) {
        viewModelScope.launch {
            repository.insertApi(
                FreeApi(
                    name = name,
                    provider = provider,
                    description = desc,
                    category = cat,
                    url = url,
                    integrationCode = code,
                    fallbackName = fallbackName,
                    fallbackUrl = fallbackUrl,
                    fallbackDescription = fallbackDesc,
                    fallbackIntegrationCode = fallbackCode
                )
            )
        }
    }

    fun deleteApi(id: Int) {
        viewModelScope.launch {
            repository.deleteApi(id)
        }
    }

    // --- Freelance Gig Actions ---

    fun searchAndAddOpportunity(serviceName: String) {
        viewModelScope.launch {
            _proposalLoading.value = true
            val prompt = """
                أنا مطور أندرويد. أريد العثور على فرصة عمل حر جديدة بخصوص "$serviceName".
                قم بإنشاء فرصة عمل حر حقيقية المظهر وبجودة عالية في ملف JSON يحتوي الخصائص التالية باللغة العربية:
                - title: عنوان احترافي للمشروع البرمجي
                - budget: الميزانية المتوقعة مثلا (150$ - 300$)
                - platform: المنصة (مستقل / خمسات / فايفر )
                - description: تفاصيل المشروع بالكامل والمخرجات المطلوبة
                - requiredSkills: المهارات المطلوبة مفصولة بفاصلة
                - matchPercent: النسبة المئوية لمطابقة مهارات مطور أندرويد (رقم عشوائي بين 80 و 97)
                - sampleProposalText: أفضل نموذج عرض تقديم مكتوب خصيصاً للتنافس على هذا المشروع البرمجي.
                
                ارجع فقط كائن JSON صالح، لا تضع شيئاً آخر غير كائن الـ JSON. مثال:
                {
                  "title": "...",
                  "budget": "...",
                  "platform": "...",
                  "description": "...",
                  "requiredSkills": "...",
                  "matchPercent": 90,
                  "sampleProposalText": "..."
                }
            """.trimIndent()

            val jsonString = GeminiApiClient.getGeminiResponse(
                prompt = prompt,
                systemInstruction = "You are a professional freelance matching bot. You ONLY output valid JSON. No markdown backticks, just raw JSON.",
                isJsonResult = true
            )

            try {
                // Parse manually or clean backticks
                val cleanJson = jsonString
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                // Parse with Moshi
                val adapter = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                    .adapter(FreelanceOpportunityJson::class.java)

                val parsed = adapter.fromJson(cleanJson)
                if (parsed != null) {
                    repository.insertOpportunity(
                        FreelanceOpportunity(
                            title = parsed.title,
                            budget = parsed.budget,
                            platform = parsed.platform,
                            description = parsed.description,
                            requiredSkills = parsed.requiredSkills,
                            matchPercent = parsed.matchPercent,
                            sampleProposalText = parsed.sampleProposalText
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallback to simple generation if parsing fails
                repository.insertOpportunity(
                    FreelanceOpportunity(
                        title = "تطوير تطبيق متكامل لخدمة $serviceName",
                        budget = "200$ - 400$",
                        platform = "مستقل",
                        description = "مطلوب مطور لبناء تطبيق متكامل مع واجهات API لخدمات $serviceName بكفاءة وسرعة.",
                        requiredSkills = "Kotlin, Jetpack Compose, Retrofit",
                        matchPercent = 92,
                        sampleProposalText = "أهلاً بك، قرأت طلبك لتطوير تطبيق $serviceName ويسعدني تلبية رغباتك بأفضل كود برمجي مع توفير تجربة مستخدم سلسلة."
                    )
                )
            }
            _proposalLoading.value = false
        }
    }

    fun deleteOpportunity(id: Int) {
        viewModelScope.launch {
            repository.deleteOpportunity(id)
        }
    }

    fun generateCustomBidProposal(opp: FreelanceOpportunity) {
        viewModelScope.launch {
            _proposalLoading.value = true
            val prompt = """
                اكتب لي عرضاً برمجياً غاية في الاحترافية والذكاء للمشروع التالي:
                العنوان: ${opp.title}
                المنصة: ${opp.platform}
                الميزانية: ${opp.budget}
                الوصف والمخرجات المطلوبة: ${opp.description}
                المهارات المحورية: ${opp.requiredSkills}
                
                اجعل العرض مؤثراً، يبدأ بمقدمة سريعة وصادقة، ويشرح مراحل التنفيذ بدقة، ويبرز خبرتنا في تسهيل دمج الـ API وقاعدة بيانات Room، مع إنهاء العرض بشكل احترافي ومحفز للرد السريع. لا تستخدم العبارات الجاهزة والمكررة.
            """.trimIndent()

            val proposal = GeminiApiClient.getGeminiResponse(
                prompt = prompt,
                systemInstruction = "أنت كاتب عروض برمجية محترف وخبير في الفوز بالمشاريع البرمجية في منصات العمل الحر مثل مستقل وخمسات وأبويرك. اكتب باللغة العربية الفصحى بطريقة أنيقة وجذابة."
            )
            _generatedProposal.value = proposal
            _proposalLoading.value = false
        }
    }

    fun clearProposal() {
        _generatedProposal.value = ""
    }

    // --- Client Prospecting Actions ---

    fun searchAndAddClientLead(sectorOrKeyword: String) {
        viewModelScope.launch {
            _leadLoading.value = true
            val prompt = """
                أريد استكشاف عملاء محتملين (شركات محليّة أو مشاريع ناشئة أو محلات خدمية) ينقصهم تطبيق أندرويد لنمو مبيعاتهم في قطاع: "$sectorOrKeyword".
                قم بإنشاء عميل محتمل واقعي ومدروس بالكامل في ملف JSON يحتوي الخصائص التالية باللغة العربية:
                - businessName: اسم الشركة أو المحل التجاري (مثال: مخبز التنور الطازج، صالون الحلاقة العصري)
                - industry: اسم المجال التقني أو الخدمي (مثال: الأطعمة والمطاعم، الرياضة واللياقة)
                - painPoint: المشكلة الحالية الكبرى التي يعانون منها وتسبب خسارة مبيعاتهم أو تراجع خدمتهم (شرح مفصل وذكي)
                - mobileAppConcept: فكرة تطبيق أندرويد مخصصة وحل تكنولوجي ذكي يقترحه مطور أندرويد لحل تلك المشكلة بالذات
                - coldOutreachMessage: رسالة ترويجية أولى (Outreach / Cold Email / WhatsApp Pitch) شديدة الإقناع والذكاء ومختصرة للاتصال بصاحب العمل، بهدف الترحيب والاجتماع معه وعرض عليه كود أندرويد أولي
                - conversionPotential: نسبة وفرصة نجاح إقناعه بالخدمة (يجب أن تكون قيمة نصية من الثلاثة: "High" أو "Medium" أو "Low")
                
                ارجع فقط كائن JSON صالح وبدون ترميز علامات Markdown. مثال:
                {
                  "businessName": "...",
                  "industry": "...",
                  "painPoint": "...",
                  "mobileAppConcept": "...",
                  "coldOutreachMessage": "...",
                  "conversionPotential": "High"
                }
            """.trimIndent()

            val jsonString = GeminiApiClient.getGeminiResponse(
                prompt = prompt,
                systemInstruction = "You are a professional CRM Lead and outreach prospecting bot. You ONLY output valid JSON. No markdown backticks, just raw JSON.",
                isJsonResult = true
            )

            try {
                val cleanJson = jsonString
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val adapter = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                    .adapter(ClientLeadJson::class.java)

                val parsed = adapter.fromJson(cleanJson)
                if (parsed != null) {
                    repository.insertLead(
                        ClientLead(
                            businessName = parsed.businessName,
                            industry = parsed.industry,
                            painPoint = parsed.painPoint,
                            mobileAppConcept = parsed.mobileAppConcept,
                            coldOutreachMessage = parsed.coldOutreachMessage,
                            conversionPotential = parsed.conversionPotential
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallback to simple generation if parsing fails
                repository.insertLead(
                    ClientLead(
                        businessName = "محل $sectorOrKeyword المتميز",
                        industry = "البيع بالتجزئة / الخدمات",
                        painPoint = "صعوبة استقبال وإدارة وتتبع عمليات التسليم والطلبات مما يسبب ضياع البيانات وتشتيت العملاء.",
                        mobileAppConcept = "برمجة تطبيق هاتف خفيف لعرض المنتجات واستقبال طلبات الحجز الفوري مع بوابة دفع لتسريع العمليات لقطاع $sectorOrKeyword.",
                        coldOutreachMessage = "مرحباً إدارة المحل الموقر، يسعدنا كمطورين تقديم نموذج تطبيق مبرمج كلياً لتفادي تشتت عملاءكم وزيادة قنوات بيعكم بـ 30% مع حجز مباشر وسلس لقسم $sectorOrKeyword.",
                        conversionPotential = "High"
                    )
                )
            }
            _leadLoading.value = false
        }
    }

    fun deleteClientLead(id: Int) {
        viewModelScope.launch {
            repository.deleteLead(id)
        }
    }

    fun generateOutreachPitch(lead: ClientLead) {
        viewModelScope.launch {
            _leadLoading.value = true
            val prompt = """
                اكتب لي رسالة بريد إلكتروني ترويجية أو رسالة واتساب (Commercial Outreach Cold Pitch) احترافية ومقنعة للغاية ومخصصة للتواصل مع '${lead.businessName}'.
                المجال: ${lead.industry}
                المشكلة الحالية: ${lead.painPoint}
                الحل المقترح (فكرة تطبيق الأندرويد): ${lead.mobileAppConcept}
                
                اجعل الرسالة تبدأ بتهنئة، تبرز فهمنا لعملهم بالتحديد، ثم تستعرض فكرة تطبيق أندرويد مقترح لإنهاء هذه المعاناة، وتدعوهم بشكل واعد جداً إلى مكالمة أو اجتماع قصير لعرض نموذج بروتوتايب تفاعلي سريع قمنا بتجهيزه خصيصاً لهم.
            """.trimIndent()

            val pitch = GeminiApiClient.getGeminiResponse(
                prompt = prompt,
                systemInstruction = "أنت كاتب رسائل وصائد عملاء محترف ومستشار أعمال مستقل فائق الخبرة في جذب اهتمام مزارعي وملاك الأنشطة التجارية والمتاجر والعيادات. اكتب بلغة عربية رسمية رصينة ومؤثرة تضمن أعلى نسبة فتح ورد."
            )
            _generatedLeadPitch.value = pitch
            _leadLoading.value = false
        }
    }

    fun clearLeadPitch() {
        _generatedLeadPitch.value = ""
    }

    // --- Developers Code Builder Actions ---

    fun buildFreelancingAppTemplate(apiName: String, appConcept: String) {
        viewModelScope.launch {
            _projectLoading.value = true
            val prompt = """
                أنشئ هيكل وتصميم تطبيق برمجيا متكامل ومناسب للعمل الحر كنموذج أولي قابل للبيع.
                اسم واجهة التطبيق المستهدفة: $apiName
                فكرة وفائدة التطبيق: $appConcept
                
                المطلوب إنشاء ملف متكامل يشمل:
                1. واجهات الاتصال البرمجي بالـ API باستخدام كود Retrofit متكامل مع Kotlin.
                2. كود نموذج الـ (Data Layer / Entities) لتخزين البيانات محلياً في قاعدة بيانات Room.
                3. كود واجهة مستخدم (UI Screens Layout) بتفاصيل مبهرة تخدم تقنية Jetpack Compose الحديثة، بمؤثرات وألوان متناهية الجمال.
                
                يرجى تفصيل الأكواد والشرح بطريقة عملية تمكنني من تصديرها للعمل الحر فوراً.
            """.trimIndent()

            val result = GeminiApiClient.getGeminiResponse(
                prompt = prompt,
                systemInstruction = "أنت مطور أندرويد أول ومعماري برمجيات خبير. مهمتك هي كتابة أكواد كوتلن وCompose متكاملة وبنائية ونظيفة خالية من الأخطاء لمساعدة المطورين المستقلين على تسييل وبناء منتجاتهم وبيعها للشركات والأفراد."
            )
            _generatedProjectCode.value = result
            _projectLoading.value = false
        }
    }

    fun clearProjectCode() {
        _generatedProjectCode.value = ""
    }

    // --- News Agent Actions ---

    fun fetchLatestTechNews() {
        viewModelScope.launch {
            _chatLoading.value = true
            val prompt = """
                ما هي آخر التحديثات والأخبار التقنية لعالم برمجة تطبيقات الأندرويد، دمج الذكاء الاصطناعي، والـ APIs المجانية الجديدة؟
                قم بإنشاء 3 أخبار جديدة متميزة باللغة العربية بتنسيق JSON يحتوي العناصر التالية:
                - title: عنوان الخبر التقني جذاب
                - summary: ملخص مفيد ومكثف للخبر
                - source: مصدر الخبر (مثلا: Android Developers, TechCrunch, Google AI Blog)
                
                ارجع مصفوفة JSON فقط، لا تضع علامات ترميز أخرى ولا تتحدث. مثال:
                [
                  {
                    "title": "...",
                    "summary": "...",
                    "source": "..."
                  }
                ]
            """.trimIndent()

            val jsonString = GeminiApiClient.getGeminiResponse(
                prompt = prompt,
                systemInstruction = "You are a professional tech editor. You ONLY reply with valid JSON array of articles. No markdown format or explanation.",
                isJsonResult = true
            )

            try {
                val cleanJson = jsonString
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val moshi = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, TechNewsItemJson::class.java)
                val adapter = moshi.adapter<List<TechNewsItemJson>>(type)

                val list = adapter.fromJson(cleanJson)
                if (list != null) {
                    for (item in list) {
                        repository.insertNews(
                            TechNewsItem(
                                title = item.title,
                                summary = item.summary,
                                source = item.source
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback news on error
                val nowStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                repository.insertNews(
                    TechNewsItem(
                        title = "مطورون متميزون يدمجون نموذج جيميناي الجديد في أندرويد $nowStr",
                        summary = "رصدت دراسة برمجية تزايد استخدام نماذج Gemini Flash في تطبيقات الهواتف الذكية بفضل الاستجابة الفائقة وتقليل استهلاك الطاقة لـ 35% مقارنة بنماذج العام السابق.",
                        source = "أخبار المطورين المستقلين"
                    )
                )
            }
            _chatLoading.value = false
        }
    }

    fun deleteNews(id: Int) {
        viewModelScope.launch {
            repository.deleteNews(id)
        }
    }

    // --- System Prompts Helper ---

    private fun getSystemInstructionForAgent(agentId: String): String {
        return when (agentId) {
            "api_agent" -> """
                أنت "وكيل كشف وتكامل الـ APIs المجانية".
                دائماً تبحث عن شركات ومواقع جديدة توفر واجهات برمجة تطبيقات (API) مجانية أو تجريبية قيّمة للمطورين.
                عملك الرئيسي هو:
                - مساعدة المطور في العثور على APIs مجانية للمشاريع وتوفير روابطها.
                - شرح كيفية التسجيل وتوليد المفتاح (API Key).
                - كتابة كود كامل باللغة العربية وبتقنية Retrofit / Kotlin لدمج هذا الـ API في تطبيقات أندرويد.
                تحدَّث مع المطور بلغة عربية احترافية، ودودة، ومحفزة وموجهة لتسهيل مسار العمل الحر.
            """.trimIndent()

            "dev_agent" -> """
                أنت "وكيل التطوير وتجربة الأكواد".
                تساعد المستخدمين والمطورين على اختبار، كتابة، وتنظيف الكود البرمجي (Kotlin, Java, NodeJS, Flutter... إلخ).
                مهمتك مراجعة الأكواد، محاكاة اختبارات الكومبايلر، تصحيح الأخطاء المنطقية أو النحوية، واقتراح التحسينات الهندسية.
                شجع المطورين دائما على اتباع نمط MVVM و Jetpack Compose، وأعطهم ثقة تامة بمقترحاتك البرمجية العبقرية.
                أجب دائماً باللغة العربية الواضحة وبأسطر كود برمجية مفهومة بدقة.
            """.trimIndent()

            "design_agent" -> """
                أنت "وكيل تفوق التصميم وواجهات Material 3" المحترف.
                مهمتك هي إرشاد مطوري أندرويد المستقلين لتصميم واجهات مستخدم (UI) خلابة وتجربة مستخدم (UX) فائقة السلاسة بالتوافق مع إرشادات جوجل لتصميم الهواتف.
                تقدم نصائح عملية حول:
                - اختيار وتنسيق لوحات الألوان (Color Palettes) المناسبة ونسب التباين العالية.
                - توظيف مكونات Material Design 3 بشكل قياسي (مثل أزرار الحركة FABs، بطاقات Elevators، والـ Navigation componentry).
                - توجيهات توزيع المساحات والهوامش (Spacing & Density Guidelines) بناءً على الرمز القياسي 8dp لأندرويد لمنع تداخل العناصر.
                - حلول مبتكرة لزيادة معدل الكفاءة والوصول الشامل (Accessibility targets) بحيث لا تقل الأهداف اللمسية عن 48dp ومراعاة ذوي الاحتياجات.
                تحدَّث بلغة مصمم تقني ملهمة وبسيطة، وأعطِ مطور أندرويد أكواد جيت باك كومبوز (Jetpack Compose Layouts & Theming Code) خلابة تعكس روعة التصميم ومبادئ هندسة الواجهات الاحترافية بلغة عربية واضحة ومكثفة.
            """.trimIndent()

            "pricing_agent" -> """
                أنت "وكيل التفاوض وتثمين العقود والأسعار" الخبير المالي والقانوني لأعمال الفريلانس.
                مهمتك مساعدة المهندسين البرمجيين لتخطي عقبات تسعير مشاريعهم باحترافية، وتجزئة المسؤوليات والمهام، وكسب مفاوضات التعاقد وحماية أنفسهم من الاستغلال أو زحف المتطلبات (Scope Creep).
                عملك يتضمن تقديم إستراتيجيات ممتازة حول:
                - كيفية تسعير مشاريع أندرويد بدقة (تحديد معدل الأجر بالساعة مقابل التسعير الإجمالي للمشروع بناءً على المهام والتعقيد الهندسي).
                - تقسيم المشروع الطويل إلى دفعات مالية مرحلية مجزأة (Milestone structure) لتقليل المخاطر على المستقل والعميل معاً.
                - تقديم سيناريوهات تفاوضية ذكية، صياغة ردود احترافية لبقة وقوية تقنع أصحاب الأعمال، وحل نزاعات العمل بلين حكيم.
                - كتابة مسودات تعاقد واتفاقيات تفاهم (Contract drafting / SLAs) مهنية لحفظ الحقوق الفكرية للكود وتقديم الصيانة وتوثيق بنود التعديلات.
                تحدَّث بصفة خبير رائد أعمال رصين، واثق ومتزن باللغة العربية الفصحى.
            """.trimIndent()

            "opp_agent" -> """
                أنت "وكيل التعلم واقتناص فرص العمل الحر".
                تقوم بتدريب المستخدمين على كيفية اختيار مهنة العمل الحر كبرمجة تطبيقات الأندرويد، وكيف يحصلون على مشاريع حقيقية على منصات متعددة (مثل مستقل، خمسات، Upwork).
                تساعد المستخدم في كتابة وبناء "عروض تقديمية (Bids/Proposals)" جذابة وخاطفة لعين العميل تفوز بالمشاريع.
                كما تقدم خارطة طريق لتعلم المهارات الشحيحة في السوق والتي تدر مكاسباً عالية للغاية.
                تحدث بحرارة، حماسة، وعقل بروفيسور ريادة الأعمال باللغة العربية.
            """.trimIndent()

            "client_agent" -> """
                أنت "وكيل صيد واستكشاف العملاء المحليين وعبر الويب".
                مهمتك الحصرية هي مساعدة المطورين المستقلين على العثور على أصحاب أعمال ومحلات تجارية وعيادات وشركات صغيرة ينقصها أو يفيدها بقوة وجود تطبيق أندرويد لزيادة فاعليتها وتسييل مبيعاتها.
                توجه المطور بكفاءة لتقفي أثرهم بالذكاء الاصطناعي وكتابة مقترحات ورسائل (Cold Outreach Message) تضرب في صميم مشاكلهم (Pain Points) وتغري بالتعاقد.
                تقدم نصائح عملية حول:
                - استراتيجيات الـ Cold Calling والـ Cold Emailing للمشاريع الصغيرة.
                - اكتشاف الفجوات التقنية (مثلا عدم وجود حجز مواعيد، دفع إلكتروني، تتبع طلبات لايف) وعرض حلول عبر تطبيقات الموبايل.
                - تزويد المطور برسائل مخصصة جذابة على منصات التواصل مثل لينكدإن، الواتساب أو بريد الأعمال.
                تحدث مع المطور بحماس شديد، وامنحه إستراتيجيات ريادية عملية وأرسل له نماذج تواصل مبهرة باللغة العربية.
            """.trimIndent()

            "news_agent" -> """
                أنت "وكيل تصفح الأخبار وعالم التقنية".
                تستعرض آخر صيحات تطوير التطبيقات، مستجدات الهواتف، التحديثات المهمة في أندرويد والقرارات الجديدة للـ APIs وعام الذكاء الاصطناعي.
                تساعد المطورين على معرفة التحديثات الأمنية والأدوات المستجدة في بيئة المطورين.
                اكتب بأسلوب صحفي تقني ملخّص وممتع، وركّز على القيمة العملية للمستقلين.
                كل ردودك باللغة العربية الفصحى.
            """.trimIndent()

            else -> "أنت مساعد برمجي كفء بلغة عربية احترافية ومكثفة."
        }
    }
}

// --- Auxiliary JSON Classes for Moshi ---

@JsonClass(generateAdapter = true)
data class FreelanceOpportunityJson(
    val title: String,
    val budget: String,
    val platform: String,
    val description: String,
    val requiredSkills: String,
    val matchPercent: Int,
    val sampleProposalText: String
)

@JsonClass(generateAdapter = true)
data class ClientLeadJson(
    val businessName: String,
    val industry: String,
    val painPoint: String,
    val mobileAppConcept: String,
    val coldOutreachMessage: String,
    val conversionPotential: String
)

@JsonClass(generateAdapter = true)
data class TechNewsItemJson(
    val title: String,
    val summary: String,
    val source: String
)
