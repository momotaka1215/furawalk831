package com.momo.furawalk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.momo.furawalk.core.engine.logic.*
import com.momo.furawalk.core.engine.PetEngine
import com.momo.furawalk.data.local.room.AppDatabase
import com.momo.furawalk.data.local.room.entity.*
import com.momo.furawalk.core.domain.model.map.CheckpointType
import com.momo.furawalk.core.domain.model.event.Event
import com.momo.furawalk.data.remote.model.ShopResponseDto
import com.momo.furawalk.data.remote.model.RewardConfigDto
import com.momo.furawalk.data.remote.api.RetrofitWorldApi
import com.momo.furawalk.data.repository.WorldRepositoryImpl
import com.momo.furawalk.platform.android.hardware.AndroidVibrationProvider
import com.momo.furawalk.platform.android.location.AndroidHeadingProvider
import com.momo.furawalk.platform.android.hardware.AndroidStepProvider
import com.momo.furawalk.platform.android.location.AndroidLocationProvider
import com.momo.furawalk.platform.android.nearby.AndroidNearbyProvider
import com.momo.furawalk.ui.navigation.AppNavGraph
import com.momo.furawalk.ui.navigation.Screen
import com.momo.furawalk.ui.navigation.bottomNavItems
import com.momo.furawalk.ui.screens.SplashScreen
import com.momo.furawalk.ui.theme.FurawalkTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class MainActivity : ComponentActivity() {

    private lateinit var locationProvider: AndroidLocationProvider
    private lateinit var headingProvider: AndroidHeadingProvider
    private lateinit var nearbyProvider: AndroidNearbyProvider
    private lateinit var stepProvider: AndroidStepProvider
    private lateinit var vibrationProvider: AndroidVibrationProvider
    private lateinit var playerDao: com.momo.furawalk.data.local.room.dao.PlayerDao
    
    // スピード警告用変数は現状未使用のため整理可能だが、ロジック維持のため一旦保持

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationProvider.startTracking()
            headingProvider.startListening()
        }
        if (permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true) {
            stepProvider.startListening()
        }
        if (permissions.filter { it.key.contains("BLUETOOTH") || it.key.contains("NEARBY") }.all { it.value }) {
            startNearbyServices()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.i("FurawalkInit", "=== onCreate Started ===")
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "furawalk-db")
            .fallbackToDestructiveMigration().build()
        
        playerDao = db.playerDao()
        
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://fulavono.neocities.org/")
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            
        val worldApi = retrofit.create(RetrofitWorldApi::class.java)
        val worldRepository = WorldRepositoryImpl(db.checkpointDao())
        val avatarDir = File(filesDir, "avatars")
        val syncManager = SyncManager(worldApi, worldRepository, db.avatarDao(), avatarDir)
        val itemImageDir = File(filesDir, "items")
        val shopSyncManager = ShopSyncManager(worldApi, db.shopDao(), itemImageDir, assets)
        val petImageDir = File(filesDir, "pets")
        val petSyncManager = PetSyncManager(worldApi, db.petDao(), petImageDir)
        
        val bokkaManager = BokkaManager(db.bokkaDao(), db.checkpointDao(), playerDao)
        val petNurturingManager = PetNurturingManager(db.petDao(), playerDao, db.shopDao(), db.growthDao())
        val activityManager = ActivityManager(playerDao, lifecycleScope, petNurturingManager)
        val dailyQuestManager = DailyQuestManager(playerDao)
        val eventSyncManager = EventSyncManager(worldApi, db.eventDao())
        val tortoiseManager = TortoiseEventManager(db.tortoiseDao(), db.checkpointDao(), playerDao, this)
        
        val petEngine = PetEngine(
            nurturing = petNurturingManager,
            evolution = PetEvolutionManager(db.petDao(), playerDao),
            interaction = PetInteractionManager(db.petDao(), db.growthDao())
        )
        
        locationProvider = AndroidLocationProvider(this)
        headingProvider = AndroidHeadingProvider(this, locationProvider)
        nearbyProvider = AndroidNearbyProvider(this)
        stepProvider = AndroidStepProvider(this)
        vibrationProvider = AndroidVibrationProvider(this)
        
        activityManager.startTracking(stepProvider.currentSteps, locationProvider.currentDistance)
        
        val serverUrls = listOf(
            "https://fulavono.neocities.org/furawalk/master_data.json",
            "https://fulavono.neocities.org/furawalk/furawalk_map1.json",
            "https://fulavono.neocities.org/furawalk/furawalk_map2.json",
            "https://fulavono.neocities.org/furawalk/furawalk_map3.json",
            "https://fulavono.neocities.org/furawalk/furawalk_map4.json",
            "https://fulavono.neocities.org/furawalk/furawalk_map5.json"
        )
        val shopUrl = "https://fulavono.neocities.org/furawalk/shop_items.json" // items.json から修正
        val petUrl = "https://fulavono.neocities.org/furawalk/pet_data.json"
        val eventUrl = "https://fulavono.neocities.org/furawalk/events.json"

        enableEdgeToEdge()
        setContent {
            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as android.app.Activity).window
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }

            FurawalkTheme {
                val navController = rememberNavController()
                var isLoading by remember { mutableStateOf(true) }
                var loadingMessage by remember { mutableStateOf("準備中...") } // 追加
                var showSafetyWarning by remember { mutableStateOf(false) }
                var startDestination by remember { mutableStateOf(Screen.Home.route) }
                var isDebugMode by remember { mutableStateOf(false) } // デバッグモード状態

                LaunchedEffect(Unit) {
                    val tag = "FurawalkInit"
                    android.util.Log.i(tag, ">>> Launcher Effect Started <<<")
                    println(">>> FurawalkInit: Launcher Effect Started <<<")
                    try {
                        // 1. バックグラウンドループの開始
                        lifecycleScope.launch { 
                            android.util.Log.d("FurawalkInit", "Starting PetLoop")
                            while (true) { 
                                try { petEngine.update() } catch (e: Exception) { android.util.Log.e("PetLoop", "Error: ${e.message}") }
                                kotlinx.coroutines.delay(60000) 
                            } 
                        }
                        lifecycleScope.launch {
                            android.util.Log.d("FurawalkInit", "Starting WorldLoop")
                            while (true) {
                                try {
                                    val currentLoc = locationProvider.location.value
                                    bokkaManager.updateBokkaStatus(currentLoc)
                                    tortoiseManager.update(currentLoc)
                                } catch (e: Exception) {
                                    android.util.Log.e("WorldLoop", "Error: ${e.message}")
                                }
                                kotlinx.coroutines.delay(300000)
                            }
                        }

                        // 2. 同期処理の実行 (完了を待機)
                        android.util.Log.i(tag, "Starting SyncWorldData...")
                        println("FurawalkInit: Syncing world data...")
                        loadingMessage = "マップデータを同期中..."
                        syncManager.syncWorldData(serverUrls) { fileName ->
                            android.util.Log.d("FurawalkInit", "Syncing map: $fileName")
                            loadingMessage = "マップを同期中: $fileName"
                        }

                        android.util.Log.i(tag, "Starting ShopSync...")
                        println("FurawalkInit: Starting ShopSync...")
                        loadingMessage = "ショップデータを同期中..."
                        shopSyncManager.loadItemsFromAssets { fileName ->
                            android.util.Log.d("FurawalkInit", "Loading asset: $fileName")
                            loadingMessage = "カタログ初期化中: $fileName"
                        }
                        shopSyncManager.syncShopData(shopUrl) { fileName ->
                            android.util.Log.d("FurawalkInit", "Syncing shop: $fileName")
                            loadingMessage = "ショップを同期中: $fileName"
                        }
                        shopSyncManager.syncShopData("https://fulavono.neocities.org/furawalk/items.json") { fileName ->
                            android.util.Log.d("FurawalkInit", "Syncing items: $fileName")
                            loadingMessage = "アイテムを同期中: $fileName"
                        }

                        android.util.Log.i(tag, "Starting EventSync...")
                        println("FurawalkInit: Starting EventSync...")
                        loadingMessage = "イベントデータを同期中..."
                        eventSyncManager.syncEventData(eventUrl) { fileName ->
                            android.util.Log.d("FurawalkInit", "Syncing event: $fileName")
                            loadingMessage = "イベントを同期中: $fileName"
                        }

                        android.util.Log.i(tag, "Starting PetSync...")
                        println("FurawalkInit: Starting PetSync...")
                        loadingMessage = "ペットデータを同期中..."
                        petSyncManager.syncPetData(petUrl) { fileName ->
                            android.util.Log.d("FurawalkInit", "Syncing pet: $fileName")
                            loadingMessage = "ペットデータを同期中: $fileName"
                        }

                        // 3. 初期モックデータの挿入 (必要に応じて)
                        val existingSpecies = db.petDao().getAllSpecies().first()
                        if (existingSpecies.isEmpty()) {
                            android.util.Log.i("FurawalkInit", "Inserting initial pet species mocks")
                            val mocks = listOf(
                                PetSpeciesEntity(id = "shiba_01", name = "シバ丸", species = "dog", rarity = 1, description = "柴犬", type1Description = "イヌ科", type2Description = "忠実", iconEmoji = "🐕", baseHp = 100, baseStamina = 90, baseSpeed = 80, basePower = 70, baseIntelligence = 60),
                                PetSpeciesEntity(id = "calico_01", name = "ミケ", species = "cat", rarity = 1, description = "三毛猫", type1Description = "ネコ科", type2Description = "マイペース", iconEmoji = "🐈", baseHp = 85, baseStamina = 70, baseSpeed = 95, basePower = 50, baseIntelligence = 80)
                            )
                            mocks.forEach { db.petDao().insertOrUpdateSpecies(it) }
                        }

                        // 4. プロフィールの確認と終了
                        val profile = playerDao.getPlayerProfile().first()
                        if (profile == null) {
                            android.util.Log.i("FurawalkInit", "No profile found, navigating to Registration")
                            startDestination = Screen.Registration.route
                        } else {
                            android.util.Log.i("FurawalkInit", "Profile found: ${profile.name}")
                            showSafetyWarning = true
                        }
                        
                        checkPermissions()
                        petEngine.nurturing.processTimePassage()
                        
                        android.util.Log.i(tag, "Init Complete!")
                        println("FurawalkInit: Init Complete!")
                        loadingMessage = "完了"
                        isLoading = false
                        
                    } catch (e: Exception) {
                        android.util.Log.e(tag, "FATAL_INIT_ERROR: ${e.message}")
                        println("FurawalkInit: FATAL ERROR - ${e.message}")
                        e.printStackTrace()
                        loadingMessage = "初期化エラー: ${e.message}"
                        kotlinx.coroutines.delay(3000)
                        isLoading = false
                    }
                }

                if (isLoading) {
                    SplashScreen(message = loadingMessage)
                } else {
                    val checkpoints by worldRepository.getAllCheckpoints().collectAsState(initial = emptyList())
                    val currentLocation by locationProvider.location.collectAsState()
                    val currentHeading by headingProvider.heading.collectAsState()
                    val currentDistance by locationProvider.currentDistance.collectAsState()
                    val currentSteps by stepProvider.currentSteps.collectAsState()
                    val currencies by playerDao.getAllCurrencies().collectAsState(initial = emptyList())
                    val playerProfile by playerDao.getPlayerProfile().collectAsState(initial = null)
                    val inventory by playerDao.getInventory().collectAsState(initial = emptyList())
                    val petStatus by playerDao.getPetStatus().collectAsState(initial = null)
                    val encounterHistory by playerDao.getEncounterHistory().collectAsState(initial = emptyList())
                    val avatars by db.avatarDao().getAllAvatars().collectAsState(initial = emptyList())
                    val shopItems by db.shopDao().getAllItems().collectAsState(initial = emptyList())
                    val petSpecies by db.petDao().getAllSpecies().collectAsState(initial = emptyList())
                    val recentActivity by playerDao.getRecentActivity().collectAsState(initial = emptyList())
                    val visitStats by playerDao.getVisitStatsByType().collectAsState(initial = emptyList())
                    val unshownGrowthRecords by db.growthDao().getUnshownGrowthRecords().collectAsState(initial = emptyList())
                    
                    val currentMoney = currencies.find { it.type == "MONEY" }?.currentAmount ?: 0L
                    val currentExp = currencies.find { it.type == "EXP" }?.currentAmount ?: 0L

                    val now = remember { mutableStateOf(System.currentTimeMillis()) }
                    val activeBokkaEvent by db.bokkaDao().getActiveBokkaEvent(now.value).collectAsState(initial = null)
                    val bokkaInventory by remember(activeBokkaEvent) { if (activeBokkaEvent != null) db.bokkaDao().getBokkaInventory(activeBokkaEvent!!.eventId) else kotlinx.coroutines.flow.flowOf(emptyList()) }.collectAsState(initial = emptyList())
                    val tortoiseEventState by db.tortoiseDao().getEventState().collectAsState(initial = null)
                    val totalDistance by remember(playerProfile) { derivedStateOf { playerProfile?.totalDistance ?: 0.0 } }
                    val discoveredCheckpoints by playerDao.getDiscoveredCheckpoints().collectAsState(initial = emptyList())
                    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                    val dailyQuest by playerDao.getDailyQuest(today).collectAsState(initial = null)
                    val masterEvents by db.eventDao().getAllEvents().collectAsState(initial = emptyList())
                    val questProgress by playerDao.getQuestProgress().collectAsState(initial = emptyList())
                    
                    var showAddCheckpointDialog by remember { mutableStateOf(false) }
                    var newCheckpointName by remember { mutableStateOf("") }
                    var selectedCheckpointType by remember { mutableStateOf(CheckpointType.SIGHTSEEING) }
                    var isHighPriority by remember { mutableStateOf(false) }
                    var typeDropdownExpanded by remember { mutableStateOf(false) }
                    
                    if (showAddCheckpointDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddCheckpointDialog = false },
                            title = { Text("新規目的地の作成") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TextField(value = newCheckpointName, onValueChange = { newCheckpointName = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(onClick = { typeDropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedCheckpointType.name) }
                                        DropdownMenu(expanded = typeDropdownExpanded, onDismissRequest = { typeDropdownExpanded = false }) {
                                            CheckpointType.entries.forEach { type -> DropdownMenuItem(text = { Text(type.name) }, onClick = { selectedCheckpointType = type; typeDropdownExpanded = false }) }
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().clickable { isHighPriority = !isHighPriority }, verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isHighPriority, onCheckedChange = { isHighPriority = it })
                                        Text("✨ 重要地点")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val loc = currentLocation
                                    if (loc != null && newCheckpointName.isNotBlank()) {
                                        lifecycleScope.launch {
                                            db.checkpointDao().insertAll(listOf(CheckpointEntity(id = "manual_${System.currentTimeMillis()}", name = newCheckpointName, latitude = loc.latitude, longitude = loc.longitude, radiusMeter = 30f, type = selectedCheckpointType.name, priority = if (isHighPriority) 5 else 3, expReward = 150, moneyReward = 100, itemIdReward = null)))
                                            vibrationProvider.vibrateSuccess(); showAddCheckpointDialog = false; newCheckpointName = ""
                                        }
                                    }
                                }) { Text("作成") }
                            },
                            dismissButton = { TextButton(onClick = { showAddCheckpointDialog = false }) { Text("キャンセル") } }
                        )
                    }

                    val rewardConfig = remember {
                        try { val jsonString = assets.open("shop_items.json").bufferedReader().use { it.readText() }; Json { ignoreUnknownKeys = true }.decodeFromString<ShopResponseDto>(jsonString) } catch (e: Exception) { null }
                    }
                    
                    val events by remember(masterEvents, questProgress, dailyQuest, checkpoints) {
                        derivedStateOf {
                            val list = mutableListOf<Event>()
                            masterEvents.filter { it.id != "daily_template" }.forEach { master ->
                                val progress = questProgress.find { it.questId == master.id }
                                list.add(Event(id = master.id, title = master.title, description = master.description, bonusHeso = master.bonusHeso, bonusExp = master.bonusExp, rewardItemId = master.rewardItemId, startDate = master.startDate ?: "", endDate = master.endDate ?: "", iconEmoji = master.iconEmoji, targetCheckpointId = master.targetCheckpointId, conditionType = master.conditionType, conditionValue = master.conditionValue, isCompleted = progress?.status == "COMPLETED" || progress?.status == "REWARDED", isRewarded = progress?.status == "REWARDED"))
                            }
                            dailyQuest?.let { dq ->
                                val template = masterEvents.find { it.id == "daily_template" }
                                val cp = checkpoints.find { it.id == dq.checkpointId }
                                if (cp != null && template != null) list.add(0, Event(id = "daily_${dq.date}", title = template.title.replace("%s", cp.name), description = template.description, bonusHeso = template.bonusHeso, bonusExp = template.bonusExp, iconEmoji = template.iconEmoji, startDate = dq.date, endDate = dq.date, targetCheckpointId = dq.checkpointId, conditionType = "LOCATION", isCompleted = dq.isCompleted))
                            }
                            list
                        }
                    }

                    var showSpeedWarning by remember { mutableStateOf(false) }

                    val recentlyVisitedIds by remember(discoveredCheckpoints) {
                        derivedStateOf {
                            val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                            discoveredCheckpoints.filter { it.lastVisitedAt > todayStart }.map { it.checkpointId }.toSet()
                        }
                    }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val showBottomBar = currentDestination?.route != Screen.Registration.route && currentDestination?.route != null

                    Scaffold(
                        contentWindowInsets = WindowInsets(0.dp),
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar {
                                    bottomNavItems.forEach { screen ->
                                        NavigationBarItem(
                                            icon = { Icon(screen.icon!!, contentDescription = null) },
                                            label = { Text(screen.title) },
                                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                            onClick = { navController.navigate(screen.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        val metPlayers by nearbyProvider.metPlayers.collectAsState()
                        val isScanning by nearbyProvider.isScanning.collectAsState()

                        AppNavGraph(
                            navController = navController,
                            startDestination = startDestination,
                            checkpoints = checkpoints,
                            currentLocation = currentLocation,
                            currentHeading = currentHeading,
                            currentDistance = currentDistance,
                            currentSteps = currentSteps,
                            currentMoney = currentMoney,
                            currentExp = currentExp,
                            totalSteps = playerProfile?.totalSteps ?: 0L,
                            totalDistance = playerProfile?.totalDistance ?: 0.0,
                            inventory = inventory,
                            petStatus = petStatus,
                            avatars = avatars,
                            shopItems = shopItems,
                            petSpecies = petSpecies,
                            encounterHistory = encounterHistory,
                            playerProfile = playerProfile,
                            checkedInIds = recentlyVisitedIds,
                            events = events,
                            visitStats = visitStats,
                            metPlayers = metPlayers,
                            isScanning = isScanning,
                            recentActivity = recentActivity,
                            dailyQuest = dailyQuest,
                            unshownGrowthRecords = unshownGrowthRecords,
                            activeBokkaEvent = activeBokkaEvent,
                            bokkaInventory = bokkaInventory,
                            tortoiseEventState = tortoiseEventState,
                            isDebugMode = isDebugMode, // 追加
                            onToggleDebugMode = { isDebugMode = !isDebugMode }, // 追加
                            onRegistrationComplete = { name, birthDate, avatarPath ->
                                lifecycleScope.launch {
                                    playerDao.insertOrUpdatePlayer(PlayerEntity(name = name, birthDate = birthDate, avatarPath = avatarPath))
                                    playerDao.updateCurrency(PlayerCurrencyEntity("MONEY", 1000))
                                    playerDao.updateCurrency(PlayerCurrencyEntity("EXP", 0))
                                    playerDao.updatePetStatus(PetEntity(name = "新しいペット", speciesId = ""))
                                    showSafetyWarning = true; navController.navigate(Screen.Home.route) { popUpTo(Screen.Registration.route) { inclusive = true } }
                                }
                            },
                            vibrationProvider = vibrationProvider,
                            onUseItem = { itemId -> lifecycleScope.launch { if (petEngine.nurturing.useItemOnPet(itemId)) vibrationProvider.vibrateOnce() } },
                            onPurchaseItem = { itemId -> lifecycleScope.launch { if (db.shopDao().purchaseItem(itemId)) { vibrationProvider.vibrateOnce(); android.widget.Toast.makeText(this@MainActivity, "購入しました！", android.widget.Toast.LENGTH_SHORT).show() } else android.widget.Toast.makeText(this@MainActivity, "購入失敗", android.widget.Toast.LENGTH_SHORT).show() } },
                            onPurchaseBokkaItem = { itemId -> lifecycleScope.launch { val eventId = activeBokkaEvent?.eventId ?: return@launch; if (bokkaManager.purchaseItem(eventId, itemId)) { vibrationProvider.vibrateSuccess(); android.widget.Toast.makeText(this@MainActivity, "購入しました！", android.widget.Toast.LENGTH_SHORT).show() } } },
                            onSelectPetSpecies = { speciesId -> lifecycleScope.launch { val profile = playerDao.getPlayerProfile().first() ?: return@launch; val species = db.petDao().getSpeciesById(speciesId); if (species != null) { db.petDao().updatePetStatus(GeneticsEngine.generateInitialPet(profile, species)); vibrationProvider.vibrateOnce() } } },
                            onSaveGreeting = { newGreeting -> lifecycleScope.launch { playerProfile?.let { playerDao.insertOrUpdatePlayer(it.copy(greetingMessage = newGreeting)); startNearbyServices() } } },
                            onSelectCheckpoint = { checkpoint -> lifecycleScope.launch { playerProfile?.let { playerDao.insertOrUpdatePlayer(it.copy(activeCheckpointId = checkpoint.id, startLatitude = currentLocation?.latitude, startLongitude = currentLocation?.longitude, lastSelectionTime = System.currentTimeMillis(), isDistanceBonusInvalidated = false)) } } },
                            onCheckIn = { checkpoint ->
                                if (checkpoint.id == "tortoise_target") {
                                    lifecycleScope.launch { tortoiseManager.update(locationProvider.location.value) }
                                } else {
                                    lifecycleScope.launch {
                                        val isAlreadyReachedToday = recentlyVisitedIds.contains(checkpoint.id)
                                        val canGetBonus = !isAlreadyReachedToday && playerProfile?.isDistanceBonusInvalidated == false
                                        val bonus = if (canGetBonus && playerProfile?.startLatitude != null) {
                                            val res = FloatArray(1); android.location.Location.distanceBetween(playerProfile!!.startLatitude!!, playerProfile!!.startLongitude!!, checkpoint.latitude, checkpoint.longitude, res)
                                            val d = res[0]; if (d < 300) 0 else ((d / 500).toInt() * 20).coerceAtMost(200)
                                        } else 0
                                        val earnedMoney = (if (isAlreadyReachedToday) 0 else checkpoint.rewards.money) + bonus
                                        val earnedExp = if (isAlreadyReachedToday) 0 else checkpoint.rewards.exp
                                        val curExp = currencies.find { it.type == "EXP" }; playerDao.updateCurrency(PlayerCurrencyEntity("EXP", (curExp?.currentAmount ?: 0L) + earnedExp, totalEarned = (curExp?.totalEarned ?: 0L) + earnedExp))
                                        val curMon = currencies.find { it.type == "MONEY" }; playerDao.updateCurrency(PlayerCurrencyEntity("MONEY", (curMon?.currentAmount ?: 0L) + earnedMoney, totalEarned = (curMon?.totalEarned ?: 0L) + earnedMoney))
                                        val discovery = playerDao.getDiscoveredCheckpointById(checkpoint.id)
                                        if (discovery == null) playerDao.recordCheckpointVisit(DiscoveredCheckpointEntity(checkpointId = checkpoint.id, visitCount = 1))
                                        else playerDao.recordCheckpointVisit(discovery.copy(lastVisitedAt = System.currentTimeMillis(), visitCount = discovery.visitCount + 1))
                                        dailyQuestManager.completeDailyQuest(checkpoint.id)
                                        events.filter { it.targetCheckpointId == checkpoint.id && !it.isCompleted }.forEach { playerDao.updateQuest(QuestProgressEntity(it.id, "COMPLETED")) }
                                        val roll = Random().nextDouble() * 100.0; var rewardItemId: String? = null
                                        rewardConfig?.checkInRewardConfig?.probabilityGroups?.let { groups ->
                                            var cumulative = 0.0; for (group in groups) { cumulative += group.chance; if (roll < cumulative) { rewardItemId = group.itemIds.random(); break } }
                                        }
                                        if (rewardItemId != null) {
                                            val currentInv = playerDao.getInventory().first(); val item = currentInv.find { it.itemId == rewardItemId }; val qty = item?.quantity ?: 0
                                            if (qty < 30) { playerDao.updateInventoryItem(InventoryEntity(rewardItemId!!, qty + 1)); val master = db.shopDao().getItemById(rewardItemId!!); android.widget.Toast.makeText(this@MainActivity, "🎁 ${master?.name ?: rewardItemId!!}", android.widget.Toast.LENGTH_LONG).show() }
                                        }
                                        playerProfile?.let { playerDao.insertOrUpdatePlayer(it.copy(activeCheckpointId = null, startLatitude = null, startLongitude = null, isDistanceBonusInvalidated = false)) }
                                        vibrationProvider.vibrateOnce(); android.widget.Toast.makeText(this@MainActivity, if (isAlreadyReachedToday) "獲得済み" else "チェックイン完了！", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onCalibrate = { checkpoint, loc ->
                                lifecycleScope.launch {
                                    db.checkpointDao().getCheckpointById(checkpoint.id)?.let { entity ->
                                        val updated = entity.copy(
                                            latitude = loc.latitude, 
                                            longitude = loc.longitude,
                                            lastCalibratedAt = System.currentTimeMillis() // 補正日時を記録
                                        )
                                        db.checkpointDao().updateCheckpoint(updated)
                                        
                                        // 補正した1件分を即座にJSON形式でログ出力
                                        val singleJson = """  {
    "id": "${updated.id}",
    "name": "${updated.name}",
    "latitude": ${updated.latitude},
    "longitude": ${updated.longitude},
    "radiusMeter": ${updated.radiusMeter},
    "type": "${updated.type}",
    "priority": ${updated.priority}
  }"""
                                        android.util.Log.i("CalibrationExport", "補正結果:\n$singleJson")
                                        
                                        vibrationProvider.vibrateOnce()
                                        android.widget.Toast.makeText(this@MainActivity, "補正完了 (ログに出力しました)", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onAvatarChange = { path -> lifecycleScope.launch { playerProfile?.let { playerDao.insertOrUpdatePlayer(it.copy(avatarPath = path)); startNearbyServices() } } },
                            onRetryGPS = { locationProvider.stopTracking(); headingProvider.stopListening(); locationProvider.startTracking(); headingProvider.startListening() },
                            onReincarnate = { lifecycleScope.launch { petStatus?.let { if (petEngine.evolution.reincarnatePet(it)) vibrationProvider.vibrateOnce() } } },
                            onMarkGrowthAsShown = { lifecycleScope.launch { db.growthDao().markAllAsShown() } },
                            onInsertTestData = { lifecycleScope.launch { TestDataInitializer.insertMockData(db); android.widget.Toast.makeText(this@MainActivity, "再起動で反映", android.widget.Toast.LENGTH_LONG).show() } },
                            onExportData = {
                                lifecycleScope.launch {
                                    val list = db.checkpointDao().getAllCheckpointsList()
                                    val jsonStr = list.joinToString(",\n", prefix = "[\n", postfix = "\n]") { 
                                        """  {
    "id": "${it.id}",
    "name": "${it.name}",
    "latitude": ${it.latitude},
    "longitude": ${it.longitude},
    "radiusMeter": ${it.radiusMeter},
    "type": "${it.type}",
    "priority": ${it.priority}
  }"""
                                    }
                                    
                                    // Logcat出力
                                    android.util.Log.i("DataExport", "--- ALL CHECKPOINTS JSON ---\n$jsonStr")
                                    
                                    // クリップボードにコピー (保険)
                                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Checkpoints", jsonStr))
                                    android.widget.Toast.makeText(this@MainActivity, "クリップボードにコピーしました", android.widget.Toast.LENGTH_SHORT).show()

                                    // ファイルとして共有
                                    shareJsonAsFile(jsonStr, "all_checkpoints.json", "ふらわーく 全地点データ")
                                }
                            },
                            onExportModifiedData = {
                                lifecycleScope.launch {
                                    val list = db.checkpointDao().getModifiedCheckpoints()
                                    if (list.isEmpty()) {
                                        android.widget.Toast.makeText(this@MainActivity, "補正された地点はありません", android.widget.Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val jsonStr = list.joinToString(",\n", prefix = "[\n", postfix = "\n]") { 
                                        """  {
    "id": "${it.id}",
    "name": "${it.name}",
    "latitude": ${it.latitude},
    "longitude": ${it.longitude},
    "radiusMeter": ${it.radiusMeter},
    "type": "${it.type}",
    "priority": ${it.priority}
  }"""
                                    }
                                    
                                    // クリップボードにコピー
                                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ModifiedCheckpoints", jsonStr))
                                    android.widget.Toast.makeText(this@MainActivity, "クリップボードにコピーしました", android.widget.Toast.LENGTH_SHORT).show()

                                    // ファイルとして共有 (メールアドレスを指定)
                                    shareJsonAsFile(
                                        jsonStr, 
                                        "modified_checkpoints.json", 
                                        "ふらわーく 補正地点データエクスポート",
                                        arrayOf("hoketu444@gmail.com")
                                    )
                                }
                            },
                            onAddNewCheckpoint = { showAddCheckpointDialog = true },
                            onStartTortoiseEvent = { loc -> lifecycleScope.launch { tortoiseManager.startEvent(loc) } },
                            onPauseTortoiseEvent = { lifecycleScope.launch { tortoiseManager.pauseEvent() } },
                            onResumeTortoiseEvent = { lifecycleScope.launch { tortoiseManager.resumeEvent() } },
                            onCancelTortoiseEvent = { lifecycleScope.launch { tortoiseManager.cancelEvent() } },
                            onNavigateToBokkaShop = { navController.navigate(Screen.BokkaShop.route) },
                            onNavigateToToraShop = { navController.navigate(Screen.ToraShop.route) },
                            onSellItem = { itemId ->
                                lifecycleScope.launch {
                                    val currentInv = playerDao.getInventory().first()
                                    val invItem = currentInv.find { it.itemId == itemId }
                                    val itemMaster = db.shopDao().getItemById(itemId)
                                    if (invItem != null && invItem.quantity > 0 && itemMaster != null && itemMaster.sellPrice > 0) {
                                        playerDao.updateInventoryItem(invItem.copy(quantity = invItem.quantity - 1))
                                        val curMon = playerDao.getAllCurrencies().first().find { it.type == "MONEY" }
                                        playerDao.updateCurrency(PlayerCurrencyEntity(
                                            "MONEY", 
                                            (curMon?.currentAmount ?: 0L) + itemMaster.sellPrice,
                                            totalEarned = (curMon?.totalEarned ?: 0L) + itemMaster.sellPrice
                                        ))
                                        vibrationProvider.vibrateOnce()
                                        android.widget.Toast.makeText(this@MainActivity, "${itemMaster.name}を売却しました (+${itemMaster.sellPrice}ヘソ)", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onCareAction = { actionType ->
                                lifecycleScope.launch {
                                    val result = petEngine.nurturing.performCareAction(actionType)
                                    when (result) {
                                        "SUCCESS" -> {
                                            vibrationProvider.vibrateOnce()
                                            if (actionType == "REST") {
                                                android.widget.Toast.makeText(this@MainActivity, "今日はゆっくり休みます", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        "LIMIT_STROKE" -> {
                                            android.app.AlertDialog.Builder(this@MainActivity)
                                                .setMessage("なでるは一日3回までです")
                                                .setPositiveButton("OK", null)
                                                .show()
                                        }
                                        "LIMIT_PLAY" -> {
                                            android.app.AlertDialog.Builder(this@MainActivity)
                                                .setMessage("遊ぶは一日3回までです")
                                                .setPositiveButton("OK", null)
                                                .show()
                                        }
                                        "IS_RESTING" -> {
                                            android.widget.Toast.makeText(this@MainActivity, "ペットは休み中です...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onRegisterPet = { name, species, weight, height, color, food, uri ->
                                lifecycleScope.launch {
                                    var savedPath: String? = null
                                    uri?.let { sourceUri ->
                                        try {
                                            val petDir = File(filesDir, "pets")
                                            if (!petDir.exists()) petDir.mkdirs()
                                            val destFile = File(petDir, "custom_pet_${System.currentTimeMillis()}.jpg")
                                            contentResolver.openInputStream(sourceUri)?.use { input ->
                                                destFile.outputStream().use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            savedPath = destFile.absolutePath
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    
                                    val newPet = PetInitialStatFactory.createPet(
                                        name = name,
                                        speciesId = species,
                                        weight = weight,
                                        height = height,
                                        imagePath = savedPath,
                                        color = color,
                                        food = food
                                    )
                                    db.petDao().updatePetStatus(newPet)
                                    vibrationProvider.vibrateOnce()
                                    android.widget.Toast.makeText(this@MainActivity, "${name}を登録しました！", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        
                        if (showSafetyWarning) {
                            AlertDialog(onDismissRequest = { showSafetyWarning = false }, title = { Text("注意") }, text = { Text("周囲に注意して遊んでください。") }, confirmButton = { Button(onClick = { showSafetyWarning = false }, modifier = Modifier.fillMaxWidth()) { Text("OK") } })
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun startNearbyServices() {
        lifecycleScope.launch {
            playerDao.getPlayerProfile().first()?.let { profile ->
                val exp = playerDao.getAllCurrencies().first().find { it.type == "EXP" }?.currentAmount ?: 0L
                nearbyProvider.startAdvertising(profile.name, (exp / 100).toInt() + 1, profile.totalDistance, profile.greetingMessage, profile.avatarPath)
                nearbyProvider.startDiscovery()
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { permissions.add(Manifest.permission.BLUETOOTH_SCAN); permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE); permissions.add(Manifest.permission.BLUETOOTH_CONNECT) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES) }
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) { locationProvider.startTracking(); headingProvider.startListening(); stepProvider.startListening(); startNearbyServices() }
        else requestPermissionLauncher.launch(missing.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        locationProvider.stopTracking(); headingProvider.stopListening(); stepProvider.stopListening(); nearbyProvider.stopAll()
    }

    private fun shareJsonAsFile(jsonStr: String, fileName: String, subject: String, email: Array<String>? = null) {
        try {
            val cacheFile = File(cacheDir, fileName)
            cacheFile.writeText(jsonStr)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", cacheFile)
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain" // Gmailでの互換性のためtext/plainに変更
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                putExtra(android.content.Intent.EXTRA_TEXT, "JSONデータを添付しました。")
                if (email != null) {
                    putExtra(android.content.Intent.EXTRA_EMAIL, email)
                }
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "データを共有"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "共有に失敗しました: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
