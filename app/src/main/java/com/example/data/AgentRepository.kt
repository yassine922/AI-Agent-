package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgentRepository(private val database: AppDatabase) {
    private val chatDao = database.chatDao()
    private val freeApiDao = database.freeApiDao()
    private val freelanceOpportunityDao = database.freelanceOpportunityDao()
    private val techNewsDao = database.techNewsDao()
    private val clientLeadDao = database.clientLeadDao()

    // Chats
    fun getChatHistory(agentId: String): Flow<List<ChatMessage>> = chatDao.getChatHistory(agentId)
    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }
    suspend fun clearChatHistory(agentId: String) = withContext(Dispatchers.IO) {
        chatDao.clearChatHistory(agentId)
    }

    // Free APIs
    val allApis: Flow<List<FreeApi>> = freeApiDao.getAllApisFlow()
    suspend fun insertApi(api: FreeApi) = withContext(Dispatchers.IO) {
        freeApiDao.insertApi(api)
    }
    suspend fun updateApi(api: FreeApi) = withContext(Dispatchers.IO) {
        freeApiDao.updateApi(api)
    }
    suspend fun deleteApi(id: Int) = withContext(Dispatchers.IO) {
        freeApiDao.deleteApi(id)
    }

    // Freelance Gigs
    val allOpportunities: Flow<List<FreelanceOpportunity>> = freelanceOpportunityDao.getAllOpportunitiesFlow()
    suspend fun insertOpportunity(opp: FreelanceOpportunity) = withContext(Dispatchers.IO) {
        freelanceOpportunityDao.insertOpportunity(opp)
    }
    suspend fun deleteOpportunity(id: Int) = withContext(Dispatchers.IO) {
        freelanceOpportunityDao.deleteOpportunity(id)
    }

    // Client Leads
    val allLeads: Flow<List<ClientLead>> = clientLeadDao.getAllLeadsFlow()
    suspend fun insertLead(lead: ClientLead) = withContext(Dispatchers.IO) {
        clientLeadDao.insertLead(lead)
    }
    suspend fun deleteLead(id: Int) = withContext(Dispatchers.IO) {
        clientLeadDao.deleteLead(id)
    }

    // Tech News
    val allNews: Flow<List<TechNewsItem>> = techNewsDao.getAllNewsFlow()
    suspend fun insertNews(news: TechNewsItem) = withContext(Dispatchers.IO) {
        techNewsDao.insertNews(news)
    }
    suspend fun deleteNews(id: Int) = withContext(Dispatchers.IO) {
        techNewsDao.deleteNews(id)
    }

    // Seeding method to make the app ready on first launch with real, relatable info
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        if (freeApiDao.getApisCount() == 0) {
            // 1. Seed Free APIs
            val defaultApis = listOf(
                FreeApi(
                    name = "JSONPlaceholder",
                    provider = "Typicode",
                    description = "واجهة برمجة تطبيقات مجانية شهيرة للغاية لمحاكاة المنشورات والمستخدمين والتعليقات.",
                    category = "بيانات تجريبية / Mock Data",
                    url = "https://jsonplaceholder.typicode.com/posts",
                    fallbackName = "ReqRes Users API",
                    fallbackUrl = "https://reqres.in/api/users",
                    fallbackDescription = "الـ API الاحتياطي الأفضل الذي يحاكي استلام قوائم المستخدمين في شاشات الإدارة.",
                    integrationCode = """
                        interface TypicodeService {
                            @GET("posts")
                            suspend fun getPosts(): List<Post>
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface ReqResService {
                            @GET("api/users?page=1")
                            suspend fun getUsers(): ReqResResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "CoinGecko API",
                    provider = "CoinGecko",
                    description = "بيانات أسعار العملات الرقمية والأسواق اللحظية بدقة محدثة دون تسجيل ومجاناً.",
                    category = "العملات والمالية / Finance",
                    url = "https://api.coingecko.com/api/v3/coins/list",
                    fallbackName = "CoinDesk BPI",
                    fallbackUrl = "https://api.coindesk.com/v1/bpi/currentprice.json",
                    fallbackDescription = "المصدر البديل والآمن لاستلام وتتبع أسعار البيتكوين عالمياً ومحدث بشكل دوري.",
                    integrationCode = """
                        interface CoinGeckoService {
                            @GET("simple/price")
                            suspend fun getPrice(
                                @Query("ids") ids: String,
                                @Query("vs_currencies") vs: String
                            ): Map<String, Map<String, Double>>
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface CoinDeskService {
                            @GET("bpi/currentprice.json")
                            suspend fun getCurrentPrice(): CoinDeskResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "Open Weather Map",
                    provider = "OpenWeather",
                    description = "بيانات الطقس الحالية والحرارة والتوقعات في كافة دول وعواصم وقرى العالم.",
                    category = "الطقس والمناخ / Weather",
                    url = "https://api.openweathermap.org/data/2.5/weather",
                    fallbackName = "Wttr.in Weather API",
                    fallbackUrl = "https://wttr.in/?format=j1",
                    fallbackDescription = "بوابة طقس احتياطية خفيفة للغاية ومجانية بالكامل ولا تحتاج كود تشغيل.",
                    integrationCode = """
                        interface WeatherService {
                            @GET("weather")
                            suspend fun getCurrentWeather(
                                @Query("q") city: String,
                                @Query("appid") apiKey: String
                            ): WeatherResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface WttrService {
                            @GET("Riyadh?format=j1")
                            suspend fun getWttrData(): WttrJsonResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "PokeAPI",
                    provider = "PokeAPI Co",
                    description = "قاعدة بيانات مفتوحة المصدر وكاملة التفاصيل لجميع وحوش البوكيمون والتطورات الخاصة بها.",
                    category = "الترفيه والألعاب / Entertainment",
                    url = "https://pokeapi.co/api/v2/pokemon/ditto",
                    fallbackName = "Official Joke API",
                    fallbackUrl = "https://official-joke-api.appspot.com/random_joke",
                    fallbackDescription = "الـ API الترفيهي البديل لتوليد نكات عشوائية مسلية للألعاب والتطبيقات التفاعلية.",
                    integrationCode = """
                        interface PokeService {
                            @GET("pokemon/{id}")
                            suspend fun getPokemonDetail(
                                @Path("id") id: Int
                            ): PokemonResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface JokeService {
                            @GET("random_joke")
                            suspend fun getRandomJoke(): JokeResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "RandomUser API",
                    provider = "RandomUser.me",
                    description = "المولد الشهير لبيانات وصور وبريد مستخدمين عشوائيين لغايات فحص الواجهات والمحاكاة.",
                    category = "بيانات تجريبية / Mock Data",
                    url = "https://randomuser.me/api/",
                    fallbackName = "ReqRes Users API",
                    fallbackUrl = "https://reqres.in/api/users",
                    fallbackDescription = "المزود البديل الذي يعيد تفاصيل ومعلومات وهمية مرتبة في شاشات العرض.",
                    integrationCode = """
                        interface RandomUserService {
                            @GET("api/")
                            suspend fun getRandomUsers(
                                @Query("results") count: Int
                            ): RandomUserResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface ReqResUsersService {
                            @GET("api/users?page=1")
                            suspend fun getPageUsers(): ReqResResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "Dog CEO API",
                    provider = "DogCEO",
                    description = "جلب ومعاينة صور عشوائية ممتازة بجودة عالية لكافة سلالات الكلاب حول العالم مجاناً.",
                    category = "ترفيه وحيوانات / Animals",
                    url = "https://dog.ceo/api/breeds/image/random",
                    fallbackName = "TheCatAPI",
                    fallbackUrl = "https://api.thecatapi.com/v1/images/search",
                    fallbackDescription = "بوابة احتياطية ممتازة لعرض وتغذية شاشة مخصصة لصور حيوانات القطط اللطيفة والذكية.",
                    integrationCode = """
                        interface DogService {
                            @GET("breeds/image/random")
                            suspend fun getRandomDogImage(): DogResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface CatService {
                            @GET("images/search")
                            suspend fun getCatImages(): List<CatResponse>
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "The Cat API",
                    provider = "That API Company",
                    description = "صانع صور القطط اللطيفة وبيانات السلالات في واجهة برمجية خفيفة وسريعة الأداء.",
                    category = "ترفيه وحيوانات / Animals",
                    url = "https://api.thecatapi.com/v1/images/search",
                    fallbackName = "Dog CEO API",
                    fallbackUrl = "https://dog.ceo/api/breeds/image/random",
                    fallbackDescription = "البديل الترفيهي اللحظي للحصول على صور حيوانات أليفة فورية ومجانية لتطبيقك.",
                    integrationCode = """
                        interface CatService {
                            @GET("images/search")
                            suspend fun getCatImages(): List<CatResponse>
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface DogService {
                            @GET("breeds/image/random")
                            suspend fun getRandomDog(): DogResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "Frankfurter Rates",
                    provider = "Frankfurter",
                    description = "جرد أسعار صرف العملات والتحويل الفوري من البنك المركزي الأوروبي دون قيود مفاتيح.",
                    category = "العملات والمالية / Finance",
                    url = "https://api.frankfurter.app/latest",
                    fallbackName = "CoinGecko API",
                    fallbackUrl = "https://api.coingecko.com/api/v3/coins/list",
                    fallbackDescription = "التبديل التلقائي لأسواق الأصول الرقمية والأسعار التاريخية للعملات والعملات الرقمية.",
                    integrationCode = """
                        interface FrankfurterService {
                            @GET("latest")
                            suspend fun getLatestRates(
                                @Query("from") base: String
                            ): FrankfurterResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface CoinGeckoRatesService {
                            @GET("simple/price")
                            suspend fun getPrice(
                                @Query("ids") ids: String,
                                @Query("vs_currencies") vs: String
                            ): Map<String, Map<String, Double>>
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "IP-API Geolocation",
                    provider = "IP-API.com",
                    description = "محدد فوري للموقع الجغرافي والإحداثيات والمدينة والدولة الخاصة بأي IP.",
                    category = "أدوات الشبكة / Utilities",
                    url = "http://ip-api.com/json/",
                    fallbackName = "IPWhois API",
                    fallbackUrl = "https://ipwhois.app/json/",
                    fallbackDescription = "مزود بديل مجاني وسريع جداً لقراءة بيانات مزود الإنترنت والتوزيع الإحصائي للبلدان.",
                    integrationCode = """
                        interface IpService {
                            @GET("json")
                            suspend fun getIpLocation(): IpResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface IpWhoisService {
                            @GET("json/")
                            suspend fun getIpWhois(): IpWhoisResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "Bored Activity API",
                    provider = "BoredAPI.com",
                    description = "منظومة اقتراح الأنشطة الترويحية والصحية والترفيهية الفعالة لمحاربة للملل.",
                    category = "نمط الحياة / Lifestyle",
                    url = "https://www.boredapi.com/api/activity",
                    fallbackName = "Advice Slip API",
                    fallbackUrl = "https://api.adviceslip.com/advice",
                    fallbackDescription = "قاعدة نصائح وتوليد الهامات يومية مفيدة يمكن استدعائها لعرضها بواجهة المستخدم كبديل مميز.",
                    integrationCode = """
                        interface BoredService {
                            @GET("activity")
                            suspend fun getActivity(): ActivityResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface AdviceService {
                            @GET("advice")
                            suspend fun getRandomAdvice(): AdviceResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "Advice Slip API",
                    provider = "AdviceSlip.com",
                    description = "توليد وعرض نصائح حياتية ممتازة وحكم ذكية لتطوير الشخصية باللغة الإنجليزية في أندرويد.",
                    category = "نمط الحياة / Lifestyle",
                    url = "https://api.adviceslip.com/advice",
                    fallbackName = "Bored Activity API",
                    fallbackUrl = "https://www.boredapi.com/api/activity",
                    fallbackDescription = "المزود الاحتياطي لتقديم مهام وأفكار رياضية وتأملية عشوائية للمستخدمين.",
                    integrationCode = """
                        interface AdviceService {
                            @GET("advice")
                            suspend fun getRandomAdvice(): AdviceResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface BoredService {
                            @GET("activity")
                            suspend fun getActivity(): ActivityResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "Universities List",
                    provider = "Hipo Labs",
                    description = "دليل ضخم ومجاني بكامل الجامعات المعتمدة ومواقعها وعناوينها في جميع دول العالم.",
                    category = "التعليم والدليل / Education",
                    url = "http://universities.hipolabs.com/search?country=Saudi+Arabia",
                    fallbackName = "Rest Countries API",
                    fallbackUrl = "https://restcountries.com/v3.1/all",
                    fallbackDescription = "بوابة جغرافية مساندة لاستيراد تفاصيل حدود الدول والمجموعات الاقليمية كبديل مميز.",
                    integrationCode = """
                        interface UniversitiesService {
                            @GET("search")
                            suspend fun searchUniversities(
                                @Query("country") country: String
                            ): List<University>
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface CountriesService {
                            @GET("all")
                            suspend fun getAllCountries(): List<CountryResponse>
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "Agify API",
                    provider = "Agify.io",
                    description = "محرك توقع السن المحتمل بناء على تحليل قاعدة أسماء تاريخية عملاقة مجاناً.",
                    category = "الذكاء الاصطناعي / AI & ML",
                    url = "https://api.agify.io/?name=ali",
                    fallbackName = "Genderize API",
                    fallbackUrl = "https://api.genderize.io/?name=ali",
                    fallbackDescription = "محول التخمين البديل لقراءة وتحليل الاسم لتحديد تصنيفه كـ (ذكر / أنثى).",
                    integrationCode = """
                        interface AgifyService {
                            @GET("/")
                            suspend fun getAgeForName(
                                @Query("name") name: String
                            ): AgifyResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface GenderizeService {
                            @GET("/")
                            suspend fun getGenderForName(
                                @Query("name") name: String
                            ): GenderizeResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "Genderize API",
                    provider = "Genderize.io",
                    description = "تنبؤ بدراسة البيانات لجنس الاسم (ذكر أو أنثى) ونسبة مطابقة الثقة الإحصائية لتدوينها.",
                    category = "الذكاء الاصطناعي / AI & ML",
                    url = "https://api.genderize.io/?name=fatima",
                    fallbackName = "Nationalize API",
                    fallbackUrl = "https://api.nationalize.io/?name=fatima",
                    fallbackDescription = "نظام الذكاء الاحتياطي المنوط به التنبؤ والبحث عن جنسية وبلد منشأ وحاملي الاسم.",
                    integrationCode = """
                        interface GenderizeService {
                            @GET("/")
                            suspend fun getGenderForName(
                                @Query("name") name: String
                            ): GenderizeResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface NationalizeService {
                            @GET("/")
                            suspend fun getNationalitiesForName(
                                @Query("name") name: String
                            ): NationalizeResponse
                        }
                    """.trimIndent()
                ),
                FreeApi(
                    name = "Nationalize API",
                    provider = "Nationalize.io",
                    description = "توقع واكتشاف الجنسيات والدول الفضلى لحاملي الأسماء مع نسب الاحتمالات.",
                    category = "الذكاء الاصطناعي / AI & ML",
                    url = "https://api.nationalize.io/?name=samir",
                    fallbackName = "Agify API",
                    fallbackUrl = "https://api.agify.io/?name=samir",
                    fallbackDescription = "أداة توقع وتوليد سن عشوائي للأسماء تلقائياً في حال الحاجة للبديل.",
                    integrationCode = """
                        interface NationalizeService {
                            @GET("/")
                            suspend fun getNationalitiesForName(
                                @Query("name") name: String
                            ): NationalizeResponse
                        }
                    """.trimIndent(),
                    fallbackIntegrationCode = """
                        interface AgifyService {
                            @GET("/")
                            suspend fun getAgeForName(
                                @Query("name") name: String
                            ): AgifyResponse
                        }
                    """.trimIndent()
                )
            )
            for (api in defaultApis) {
                freeApiDao.insertApi(api)
            }
        }

        if (freelanceOpportunityDao.getOpportunitiesCount() == 0) {
            // 2. Seed Freelance Opportunities
            val defaultOpps = listOf(
                FreelanceOpportunity(
                    title = "مطور أندرويد لربط CoinGecko API وعرض أسعار فوري لـ Bitcoin",
                    budget = "250$ - 500$",
                    platform = "مستقل / Mostaql",
                    description = "المطلوب بناء تطبيق أندرويد بسيط وحديث باستخدام Jetpack Compose يقوم بالاتصال بـ CoinGecko API بهدف عرض أسعار البيتكوين والعملات الرقمية بشكل لحظي مع تحديث تلقائي.",
                    requiredSkills = "Kotlin, Jetpack Compose, Retrofit, Coroutines",
                    matchPercent = 95,
                    sampleProposalText = "مرحباً أخي الكريم، أنا مطور تطبيقات أندرويد محترف بخبرة في لغة ن كوتلن وتقنية Jetpack Compose. لقد قمت مسبقاً بدمج CoinGecko API وتطوير واجهات ممتصة للأخطاء ومحدثة بشكل فوري وفعّال. يسعدني العمل معك لإنجاز هذا التطبيق بأعلى كفاءة."
                ),
                FreelanceOpportunity(
                    title = "تطبيق طقس متكامل لعميل محلي باستخدام OpenWeather API",
                    budget = "150$",
                    platform = "خمسات / Khamsat",
                    description = "أحتاج إلى تطبيق لعرض حالة الطقس لـ 10 مدن محددة مسبقاً، وحفظ آخر تحديث محلياً في حال انقطاع الإنترنت.",
                    requiredSkills = "Android, Room Database, Retrofit, LiveData/Flow",
                    matchPercent = 88,
                    sampleProposalText = "أهلاً بك، يمكنني تنفيذ تطبيق الطقس المطلوب بكل احترافية، مع دمج قاعدة بيانات Room لتخزين وحفظ الحالة محلياً للرجوع إليها بدون إنترنت، وتقديم كود نظيف وهيكلية MVVM مميزة."
                )
            )
            for (opp in defaultOpps) {
                freelanceOpportunityDao.insertOpportunity(opp)
            }
        }

        if (clientLeadDao.getLeadsCount() == 0) {
            // Seed defaults for client leads (e.g., local businesses ripe for mobile apps)
            val defaultLeads = listOf(
                ClientLead(
                    businessName = "عيادة ريفال لطب الأسنان",
                    industry = "الرعاية الصحية والعيادات / Healthcare",
                    painPoint = "صعوبة حجز المواعيد وضياع الملفات الورقية وتكدس المرضى في قاعة الانتظار دون وعي بالوقت المتبقي لدورهم.",
                    mobileAppConcept = "برمجة تطبيق أندرويد لحجز المواعيد الفوري وتلقي إشعارات التذكير التلقائية مع تتبع لايف للدور (Live Queue Status).",
                    coldOutreachMessage = """
                        أهلاً دكتور/ة،
                        لقد لاحظت عبر خرائط جوجل تميز عيادتكم والتقييمات الممتازة. بصفتي مهندس تطبيقات أندرويد متخصص، يسعدني التعاون معكم لبناء تطبيق للهواتف يسهل لمرضاكم حجز مواعيدهم بضغطة واحدة، ويقلل مكالمات السكرتارية بـ 50% مع إشعارات ذكية للدور.
                        
                        هل ترحبون بعقد اجتماع سريع مدته 5 دقائق لشرح فائدة هذا النظام وتكلفته المعقولة؟
                    """.trimIndent(),
                    conversionPotential = "High"
                ),
                ClientLead(
                    businessName = "متجر ورد الياسمين للمناسبات",
                    industry = "المتاجر والهدايا / E-Commerce",
                    painPoint = "يعتمدون بشكل كامل على رسائل إنستغرام لطلب باقات الورد والهدايا، مما يؤدي لتأخير الردود البريدية وخسارة 30% من المبيعات المكتملة.",
                    mobileAppConcept = "إنشاء متجر أندرويد سريع (Kotlin/Compose) لاستعراض باقات الورود المتاحة والدفع الإلكتروني المباشر مع تحديد موقع التوصيل على الخريطة.",
                    coldOutreachMessage = """
                        أهلاً فريق 'ورد الياسمين'،
                        تصاميم باقاتكم على إنستقرام رائعة بحق! يسعدني تحويل جودة أعمالكم الفنية لمتجر أندرويد متاح في جيوب عملائكم، لتمكينهم من تنسيق وجدولة طلب الورد والدفع ببضع لمسات خلال دقائق بدون الحاجة لانتظار الرد على الرسائل.
                        
                        يسعدني إرسال نموذج تجريبي مجاني لتقرير واجهة تطبيقكم مبرمجة بأحدث التقنيات. ما رأيكم؟
                    """.trimIndent(),
                    conversionPotential = "High"
                ),
                ClientLead(
                    businessName = "مطعم ومطبخ القرية التراثية",
                    industry = "المطاعم والضيافة / Food & Beverage",
                    painPoint = "دفع عمولات باهظة لشركات التوصيل الكبرى تصل إلى 25% مع رغبتهم في امتلاك أسطول توصيل مستقل والتحكم في قاعدة بيانات عملائهم.",
                    mobileAppConcept = "بناء تطبيق أندرويد لخدمات الطلب والتوصيل الخاص بالمطعم لتفادي العمولات وتفعيل نظام الولاء (برنامج كسب النقاط واستبدالها).",
                    coldOutreachMessage = """
                        مرحباً إدارة 'القرية التراثية'،
                        لماذا تدفعون عمولة 20% لتطبيقات التوصيل بينما يمكنك تسييل الطلبات وبناء أسطولكم الخاص بامتلاك تطبيق أندرويد متفرد لعلامتكم التجارية؟
                        نوفر لكم تطبيقاً ممتازاً يمكن للزبائن الطلب من خلاله مع تجميع نقاط الخصم والولاء، وهو ما يزيد المبيعات المباشرة ويوطد صلتكم بالزبائن.
                        
                        متوفر لمناقشة التفاصيل الكاملة وبرمجة النسخة الأولى لكم خلال أسبوعين فقط.
                    """.trimIndent(),
                    conversionPotential = "Medium"
                )
            )
            for (lead in defaultLeads) {
                clientLeadDao.insertLead(lead)
            }
        }
    }
}
