package com.momo.furawalk.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.momo.furawalk.core.domain.model.map.Checkpoint
import com.momo.furawalk.core.domain.provider.LocationData
import com.momo.furawalk.core.domain.provider.MetPlayer
import com.momo.furawalk.core.domain.provider.VibrationProvider
import com.momo.furawalk.data.local.room.entity.AvatarEntity
import com.momo.furawalk.data.local.room.entity.InventoryEntity
import com.momo.furawalk.data.local.room.entity.PetEntity
import com.momo.furawalk.data.local.room.entity.EncounterHistoryEntity
import com.momo.furawalk.data.local.room.entity.PlayerEntity
import com.momo.furawalk.data.local.room.entity.ShopItemEntity
import com.momo.furawalk.data.local.room.entity.PetSpeciesEntity
import com.momo.furawalk.data.local.room.entity.DailyActivityEntity
import com.momo.furawalk.data.local.room.entity.DailyQuestEntity
import com.momo.furawalk.data.local.room.entity.PetGrowthRecordEntity
import com.momo.furawalk.data.local.room.entity.BokkaEventEntity
import com.momo.furawalk.data.local.room.entity.BokkaItemEntity
import com.momo.furawalk.data.local.room.entity.TortoiseEventStateEntity
import com.momo.furawalk.data.local.room.dao.TypeVisitStat
import com.momo.furawalk.core.domain.model.event.Event
import com.momo.furawalk.ui.screens.*

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    // ...
    checkpoints: List<Checkpoint>,
    currentLocation: LocationData?,
    currentHeading: Float,
    currentDistance: Double,
    currentSteps: Int,
    currentMoney: Long,
    currentExp: Long,
    totalSteps: Long,
    totalDistance: Double,
    inventory: List<InventoryEntity>,
    petStatus: PetEntity?,
    avatars: List<AvatarEntity>,
    shopItems: List<ShopItemEntity>,
    petSpecies: List<PetSpeciesEntity>,
    encounterHistory: List<EncounterHistoryEntity>,
    playerProfile: PlayerEntity?,
    checkedInIds: Set<String>,
    events: List<Event>,
    visitStats: List<TypeVisitStat>,
    metPlayers: List<MetPlayer>,
    isScanning: Boolean,
    recentActivity: List<DailyActivityEntity>,
    dailyQuest: DailyQuestEntity?,
    unshownGrowthRecords: List<PetGrowthRecordEntity>,
    activeBokkaEvent: BokkaEventEntity?, // 追加
    bokkaInventory: List<BokkaItemEntity>, // 追加
    tortoiseEventState: TortoiseEventStateEntity?, // 追加
    isDebugMode: Boolean, // 追加
    onToggleDebugMode: () -> Unit, // 追加
    onRegistrationComplete: (String, Long, String?) -> Unit,
    vibrationProvider: VibrationProvider,
    onUseItem: (String) -> Unit,
    onPurchaseItem: (String) -> Unit,
    onPurchaseBokkaItem: (String) -> Unit, // 追加
    onSelectPetSpecies: (String) -> Unit,
    onSaveGreeting: (String) -> Unit,
    onSelectCheckpoint: (Checkpoint) -> Unit,
    onCheckIn: (Checkpoint) -> Unit,
    onCalibrate: (Checkpoint, LocationData) -> Unit, // 追加
    onAvatarChange: (String) -> Unit,
    onRetryGPS: () -> Unit,
    onReincarnate: () -> Unit,
    onMarkGrowthAsShown: () -> Unit,
    onInsertTestData: () -> Unit,
    onExportData: () -> Unit,
    onExportModifiedData: () -> Unit,
    onAddNewCheckpoint: () -> Unit,
    onStartTortoiseEvent: (LocationData) -> Unit,
    onPauseTortoiseEvent: () -> Unit,
    onResumeTortoiseEvent: () -> Unit,
    onCancelTortoiseEvent: () -> Unit,
    onNavigateToBokkaShop: () -> Unit,
    onNavigateToToraShop: () -> Unit,
    onSellItem: (String) -> Unit,
    onCareAction: (String) -> Unit, // 追加
    onRegisterPet: (name: String, species: String, weight: Float, height: Float, color: String, food: String, imageUri: android.net.Uri?) -> Unit,
    currentDialogue: com.momo.furawalk.core.engine.conversation.Dialogue?, // 追加
    onTalkWithPet: () -> Unit, // 追加
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Registration.route) {
            RegistrationScreen(
                avatars = avatars,
                onRegistrationComplete = onRegistrationComplete,
                onInsertTestData = onInsertTestData // 追加
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                playerProfile = playerProfile,
                petStatus = petStatus,
                recentActivity = recentActivity,
                metPlayers = metPlayers, 
                isScanning = isScanning,
                dailyQuest = dailyQuest,
                checkpoints = checkpoints,
                unshownGrowthRecords = unshownGrowthRecords,
                shopCatalog = shopItems,
                activeBokkaEvent = activeBokkaEvent,
                tortoiseEventState = tortoiseEventState,
                isDebugMode = isDebugMode,
                onToggleDebugMode = onToggleDebugMode,
                onMarkGrowthAsShown = onMarkGrowthAsShown,
                onInsertTestData = onInsertTestData,
                onExportData = onExportData,
                onExportModifiedData = onExportModifiedData,
                onAddNewCheckpoint = onAddNewCheckpoint,
                onStartTortoiseEvent = { currentLocation?.let { onStartTortoiseEvent(it) } },
                onNavigateToShop = { navController.navigate(Screen.Shop.route) },
                onNavigateToBokkaShop = onNavigateToBokkaShop,
                onNavigateToToraShop = onNavigateToToraShop,
                onNavigateToPlay = { navController.navigate(Screen.Play.route) }
            )
        }
        composable(Screen.Play.route) {
            PlayScreen(
                checkpoints = checkpoints,
                currentLocation = currentLocation,
                currentHeading = currentHeading,
                currentDistance = currentDistance,
                currentSteps = currentSteps,
                currentMoney = currentMoney,
                currentExp = currentExp,
                events = events,
                checkedInIds = checkedInIds,
                playerProfile = playerProfile,
                dailyQuest = dailyQuest,
                activeBokkaEvent = activeBokkaEvent,
                tortoiseEventState = tortoiseEventState,
                isDebugMode = isDebugMode, // 追加
                vibrationProvider = vibrationProvider,
                onCheckIn = onCheckIn,
                onSelectCheckpoint = onSelectCheckpoint,
                onCalibrate = onCalibrate,
                onRetryGPS = onRetryGPS,
                onNavigateToTasks = { navController.navigate(Screen.Task.route) },
                onOpenBokkaShop = onNavigateToBokkaShop,
                onPauseTortoise = onPauseTortoiseEvent,
                onResumeTortoise = onResumeTortoiseEvent,
                onCancelTortoise = onCancelTortoiseEvent
            )
        }
        composable(Screen.Pet.route) {
            PetScreen(
                inventory = inventory,
                petStatus = petStatus,
                petSpecies = petSpecies,
                shopCatalog = shopItems,
                isDebugMode = isDebugMode,
                onUseItem = onUseItem,
                onCareAction = onCareAction, // 追加
                onReincarnate = onReincarnate,
                onNavigateToDetail = { speciesId ->
                    navController.navigate(Screen.PetDetail.createRoute(speciesId))
                },
                onNavigateToCatalog = {
                    navController.navigate(Screen.PetCatalog.route)
                },
                onNavigateToRegistration = {
                    navController.navigate(Screen.PetRegistration.route)
                }
            )
        }
        composable(Screen.PetCatalog.route) {
            PetCatalogScreen(
                petSpecies = petSpecies,
                onSpeciesClick = { speciesId ->
                    navController.navigate(Screen.PetDetail.createRoute(speciesId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PetDetail.route) { backStackEntry ->
            val speciesId = backStackEntry.arguments?.getString("speciesId") ?: ""
            val species = petSpecies.find { it.id == speciesId }
            PetDetailScreen(
                species = species,
                onSelect = {
                    onSelectPetSpecies(speciesId)
                    navController.popBackStack(Screen.Pet.route, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PlayerInfo.route) {
            PlayerInfoScreen(
                playerProfile = playerProfile,
                currentExp = currentExp,
                currentMoney = currentMoney,
                totalSteps = totalSteps,
                totalDistance = totalDistance,
                inventory = inventory,
                shopCatalog = shopItems,
                petStatus = petStatus,
                petSpecies = petSpecies,
                visitStats = visitStats,
                onNavigateToItems = { navController.navigate(Screen.OwnedItems.route) },
                onNavigateToTrophies = { navController.navigate(Screen.OwnedTrophies.route) },
                onNavigateToAvatarCatalog = { navController.navigate(Screen.AvatarCatalog.route) },
                onNavigateToShop = { navController.navigate(Screen.Shop.route) },
                onNavigateToItemShop = { navController.navigate(Screen.ItemShop.route) },
                onNavigateToHistory = { navController.navigate(Screen.ActivityHistory.route) }
            )
        }
        composable(Screen.ItemShop.route) {
            ItemShopScreen(
                shopItems = shopItems,
                currentMoney = currentMoney,
                onPurchase = onPurchaseItem,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ActivityHistory.route) {
            ActivityHistoryScreen(
                history = recentActivity,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AvatarCatalog.route) {
            AvatarCatalogScreen(
                avatars = avatars,
                currentAvatarPath = playerProfile?.avatarPath,
                onAvatarSelected = { path ->
                    onAvatarChange(path)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Encounter.route) {
            EncounterScreen(
                metPlayers = metPlayers,
                encounterHistory = encounterHistory,
                isScanning = isScanning,
                onNavigateToSetGreeting = { navController.navigate(Screen.SetGreeting.route) }
            )
        }
        composable(Screen.SetGreeting.route) {
            SetGreetingScreen(
                currentGreeting = playerProfile?.greetingMessage ?: "こんにちは！",
                onSave = onSaveGreeting,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.OwnedItems.route) {
            OwnedItemsScreen(
                inventory = inventory,
                shopCatalog = shopItems,
                onSellItem = onSellItem,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.OwnedTrophies.route) {
            OwnedTrophiesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Shop.route) {
            ShopScreen(
                shopItems = shopItems,
                currentMoney = currentMoney,
                onPurchase = onPurchaseItem,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.BokkaShop.route) {
            BokkaShopScreen(
                event = activeBokkaEvent,
                inventory = bokkaInventory,
                catalogItems = shopItems, // 追加
                currentMoney = currentMoney,
                onPurchase = onPurchaseBokkaItem,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ToraShop.route) {
            ToraShopScreen(
                shopItems = shopItems,
                currentMoney = currentMoney,
                onPurchase = onPurchaseItem,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Task.route) {
            TaskScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PetRegistration.route) {
            PetRegistrationScreen(
                onRegistrationComplete = { name, species, weight, height, color, food, uri ->
                    onRegisterPet(name, species, weight, height, color, food, uri)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
