package com.example

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.data.GameDao
import com.example.data.GameDatabase
import com.example.data.GameObject
import com.example.data.LeaderboardEntry
import com.example.data.Level
import com.example.data.LevelsDataSource
import com.example.data.Platform
import com.example.data.TrapType
import com.example.data.UserProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Random

enum class ScreenState {
    SPLASH,
    MAIN_MENU,
    GAME_LEVEL,
    DEATH_SCREEN,
    LEVEL_COMPLETE,
    LEADERBOARD,
    SETTINGS
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "GameViewModel"
    private val database = GameDatabase.getDatabase(application, viewModelScope)
    private val dao: GameDao = database.gameDao()

    // --- State variables ---
    var screenState by mutableStateOf(ScreenState.SPLASH)
    var currentLevelIndex by mutableStateOf(1) // 1-indexed (1 to 50)
    var deathCount by mutableStateOf(0)
    var levelDeaths by mutableStateOf(0)
    var isMuted by mutableStateOf(false)
    var playerName by mutableStateOf("Player 1")

    // UI overlays
    var showPauseDialog by mutableStateOf(false)
    var showFakeWinCrack by mutableStateOf(false)
    var isGeneratingTaunt by mutableStateOf(false)
    var activeTaunt by mutableStateOf("")
    var aiHintText by mutableStateOf("")
    var isGeneratingHint by mutableStateOf(false)
    var levelAnnouncementText by mutableStateOf("")
    var showIntroductionToast by mutableStateOf(false)

    // Room DB Observation
    val userProgress: StateFlow<UserProgress?> = dao.getUserProgressFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = dao.getLeaderboardFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Game Loop Physics state ---
    var playerX by mutableStateOf(100f)
    var playerY by mutableStateOf(300f)
    var vx by mutableStateOf(0f)
    var vy by mutableStateOf(0f)
    var isGrounded by mutableStateOf(false)
    var doubleJumpAvailable by mutableStateOf(true)
    var gravityFlipped by mutableStateOf(false)
    var gravityFlipTimer by mutableStateOf(0f) // gravity flip runs for 3 seconds if triggered
    
    // Active Level structures (copied per level start so we can mutate freely)
    var activePlatforms by mutableStateOf<List<Platform>>(emptyList())
    var activeObjects by mutableStateOf<List<GameObject>>(emptyList())
    var currentLevelData by mutableStateOf<Level?>(null)

    // Player dimensions
    val playerWidth = 24f
    val playerHeight = 32f

    // Control inputs (can be changed by touch buttons or keyboard)
    var keyMoveLeft = false
    var keyMoveRight = false

    init {
        // Load initial progress
        viewModelScope.launch {
            val progress = dao.getUserProgress() ?: UserProgress()
            currentLevelIndex = progress.currentLevel
            deathCount = progress.totalDeaths
            levelDeaths = progress.currentLevelDeaths
            isMuted = progress.isMuted
            playerName = progress.playerName
            SoundManager.setMuted(isMuted)
        }
    }

    fun startGame() {
        screenState = ScreenState.GAME_LEVEL
        activeTaunt = ""
        aiHintText = ""
        loadCurrentLevel()
    }

    fun restartCurrentLevel() {
        activeTaunt = ""
        aiHintText = ""
        loadCurrentLevel()
    }

    fun goToMenu() {
        showPauseDialog = false
        screenState = ScreenState.MAIN_MENU
        SoundManager.startBackgroundMusic()
    }

    fun toggleMute() {
        val newMuted = !isMuted
        isMuted = newMuted
        SoundManager.setMuted(newMuted)
        viewModelScope.launch {
            val progress = dao.getUserProgress() ?: UserProgress()
            dao.saveUserProgress(progress.copy(isMuted = newMuted))
        }
    }

    fun savePlayerName(name: String) {
        playerName = name.take(15) // Limit name lengths
        viewModelScope.launch {
            val progress = dao.getUserProgress() ?: UserProgress()
            dao.saveUserProgress(progress.copy(playerName = playerName))
        }
    }

    fun submitLeaderboardScore() {
        viewModelScope.launch {
            dao.insertLeaderboardEntry(
                LeaderboardEntry(
                    name = playerName,
                    levelsCompleted = currentLevelIndex - 1,
                    totalDeaths = deathCount,
                    isRealPlayer = true
                )
            )
        }
    }

    private fun loadCurrentLevel() {
        showFakeWinCrack = false
        val levelIndex = currentLevelIndex.coerceIn(1, 50)
        val level = LevelsDataSource.levels[levelIndex - 1]
        currentLevelData = level
        
        // Deep copy platforms and objects to avoid mutating master list
        activePlatforms = level.platforms.map { it.copy() }
        activeObjects = level.objects.map { it.copy() }

        // Level Devil surprising dynamic properties assigned procedurally
        activeObjects.forEach { obj ->
            if (obj.type == TrapType.SPIKES) {
                if (levelIndex == 2 || levelIndex == 15 || levelIndex == 30 || levelIndex == 50) {
                    obj.extraData = "POPUP"
                    obj.y += 44f // starts hidden underground!
                } else if (levelIndex == 4 || levelIndex == 12 || levelIndex == 25 || levelIndex == 48) {
                    obj.extraData = "FALLING"
                    obj.y -= 150f // starts high up on ceiling!
                }
            }
        }
        
        // Position player at startup points
        playerX = level.startX
        playerY = level.startY
        vx = 0f
        vy = 0f
        isGrounded = false
        doubleJumpAvailable = true
        gravityFlipped = false
        gravityFlipTimer = 0f

        // Play ambient launch music
        SoundManager.startBackgroundMusic()

        // Fetch evil level introduction
        triggerLevelIntroduction(levelIndex)
    }

    private fun triggerLevelIntroduction(levelIndex: Int) {
        viewModelScope.launch {
            showIntroductionToast = true
            levelAnnouncementText = "PREPARING LEVEL... 😏"
            levelAnnouncementText = GeminiService.fetchLevelIntroduction(levelIndex)
            delay(2000)
            showIntroductionToast = false
        }
    }

    // --- GAME PHYSICS LOOP ---
    fun updatePhysics(dt: Float) {
        if (screenState != ScreenState.GAME_LEVEL || showPauseDialog || showFakeWinCrack || showIntroductionToast) {
            return
        }

        val level = currentLevelData ?: return

        // Dynamic difficulty triggers: Check if player has done well for 3 straight stages
        // In level 50, gravity flips randomly every few seconds
        var systemGravity = if (gravityFlipped) -900f else 900f
        if (level.id == 45 || level.id == 50) {
            // Level 45/50 random quick flips
            gravityFlipTimer += dt
            if (gravityFlipTimer >= 3.2f) {
                gravityFlipTimer = 0f
                gravityFlipped = !gravityFlipped
                SoundManager.playGravityFlip()
            }
        }

        // Apply input velocities
        var targetVx = 0f
        if (keyMoveLeft) {
            targetVx = -240f
        } else if (keyMoveRight) {
            targetVx = 240f
        }

        // Apply external speed floor boosts (if player stands on a platform with speed boosts)
        var hasSpeedBoost = false
        activePlatforms.forEach { platform ->
            if (platform.standsOn && platform.type == TrapType.SPEED_FLOOR) {
                hasSpeedBoost = true
            }
        }
        if (hasSpeedBoost) {
            targetVx = if (keyMoveRight || vx >= 0) 520f else -520f
        }

        // Horizontal damping (accel / decel)
        vx = vx + (targetVx - vx) * 15f * dt

        // Verticals: gravity accumulator
        vy += systemGravity * dt
        val terminalVel = 750f
        vy = vy.coerceIn(-terminalVel, terminalVel)

        // Apply movement increments separately for perfect collision sliding
        val proposedX = playerX + vx * dt
        val proposedY = playerY + vy * dt

        // We check X collision first
        var collidedX = false
        var currentPlatformOn: Platform? = null

        // Safe boundaries check
        if (proposedX < 0) {
            playerX = 0f
            vx = 0f
            collidedX = true
        } else if (proposedX + playerWidth > 1000f) {
            playerX = 1000f - playerWidth
            vx = 0f
            collidedX = true
        } else {
            // Check platform vertical/horizontal intersections
            val oldX = playerX
            playerX = proposedX
            activePlatforms.forEach { p ->
                if (!p.isVanished && p.type != TrapType.FAKE_WALL && checkAABBCollision(playerX, playerY, playerWidth, playerHeight, p.x, p.y, p.width, p.height)) {
                    // Back out X
                    playerX = oldX
                    vx = 0f
                    collidedX = true
                }
            }
        }

        // Now move Y
        val oldY = playerY
        playerY = proposedY
        var hitCeilingOrFloor = false
        isGrounded = false

        activePlatforms.forEach { p ->
            p.standsOn = false
            if (!p.isVanished && p.type != TrapType.FAKE_WALL && checkAABBCollision(playerX, playerY, playerWidth, playerHeight, p.x, p.y, p.width, p.height)) {
                playerY = oldY
                vy = 0f
                hitCeilingOrFloor = true
                
                // Grounded check (Normal vs flipped gravity)
                if (!gravityFlipped) {
                    if (oldY + playerHeight <= p.y + 4f) {
                        isGrounded = true
                        p.standsOn = true
                        doubleJumpAvailable = true
                        currentPlatformOn = p
                    }
                } else {
                    if (oldY >= p.y + p.height - 4f) {
                        isGrounded = true
                        p.standsOn = true
                        doubleJumpAvailable = true
                        currentPlatformOn = p
                    }
                }
            }
        }

        // Falling out of world pit bounds triggers sudden tragedy
        if (playerY > 570f || playerY < -30f) {
            triggerPlayerDeath()
            return
        }

        // Handle Active Platform Status Mechanics (disappearing, shrinking, tilting)
        activePlatforms.forEach { p ->
            if (p.standsOn) {
                when (p.type) {
                    TrapType.DISAPPEARING_FLOOR -> {
                        p.progress += dt
                        if (p.progress >= 0.5f) { // 0.5 seconds stand trigger
                            p.isVanished = true
                            SoundManager.playFloorCrumble()
                        }
                    }
                    TrapType.SHRINKING_PLATFORM -> {
                        p.progress += dt
                        // shrink width over 1.2s
                    }
                    TrapType.TILTING_PLATFORM -> {
                        p.progress += dt
                        if (p.progress >= 0.4f) {
                            p.angle += 110f * dt // rapid tilt to throw player off
                            // accelerate player horizontal slide
                            vx += if (playerX < p.x + p.width/2) -300f * dt else 300f * dt
                        }
                    }
                    else -> {}
                }
            }
        }

        // Active Object Actions (door slides, checkpoints, button collisions)
        activeObjects.forEach { obj ->
            // Level Devil Trap triggers
            if (obj.type == TrapType.SPIKES) {
                if (obj.extraData == "POPUP" && !obj.isActivated) {
                    val dx = Math.abs((playerX + playerWidth / 2) - (obj.x + obj.width / 2))
                    if (dx < 130f) {
                        obj.isActivated = true
                        obj.y -= 44f  // Suddenly pops up from underground!
                        SoundManager.playFloorCrumble()
                    }
                } else if (obj.extraData == "FALLING") {
                    if (!obj.isActivated) {
                        val dx = Math.abs((playerX + playerWidth / 2) - (obj.x + obj.width / 2))
                        if (dx < 120f && playerY > obj.y) {
                            obj.isActivated = true
                            SoundManager.playGravityFlip() // Alert trigger sound
                        }
                    } else {
                        // Apply falling downward motion
                        if (obj.y < 510f) {
                            obj.y += 440f * dt
                        }
                    }
                }
            }

            // Running Door sliding away from player within 150px range
            if (obj.type == TrapType.MOVING_DOOR) {
                val dx = Math.abs((playerX + playerWidth/2) - (obj.x + obj.width/2))
                val dy = Math.abs((playerY + playerHeight/2) - (obj.y + obj.height/2))
                if (dx < 160f && dy < 100f) {
                    val slideDir = if (playerX < obj.x) 180f else -180f
                    obj.x += slideDir * dt
                    // Make sure the exit port follows the door movement
                    SoundManager.playDoorEscape()
                }
            }

            // Hit collision tests with active Hazards
            if (checkAABBCollision(playerX, playerY, playerWidth, playerHeight, obj.x, obj.y, obj.width, obj.height)) {
                when (obj.type) {
                    TrapType.SPIKES -> {
                        triggerPlayerDeath()
                    }
                    TrapType.LYING_BUTTON -> {
                        obj.isActivated = true
                        SoundManager.playButtonTrap()
                        triggerPlayerDeath()
                    }
                    TrapType.FAKE_CHECKPOINT -> {
                        if (!obj.isActivated) {
                            obj.isActivated = true
                            SoundManager.playCheckpointWrong()
                            // Do not actually save player! Sarcastic toast alert
                        }
                    }
                    TrapType.GRAVITY_FLIP -> {
                        if (!obj.isActivated) {
                            obj.isActivated = true
                            gravityFlipped = !gravityFlipped
                            SoundManager.playGravityFlip()
                        }
                    }
                    else -> {}
                }
            }
        }

        // Exit port collision ends stage
        val distanceToExitX = Math.abs((playerX + playerWidth/2) - level.exitX)
        val distanceToExitY = Math.abs((playerY + playerHeight/2) - level.exitY)
        if (distanceToExitX < 36f && distanceToExitY < 48f) {
            if (level.isFakeWinLevel) {
                triggerFakeWinExposed()
            } else {
                triggerLevelVictory()
            }
        }
    }

    private fun checkAABBCollision(
        x1: Float, y1: Float, w1: Float, h1: Float,
        x2: Float, y2: Float, w2: Float, h2: Float
    ): Boolean {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2
    }

    // --- BUTTON TRIGGER INPUTS ---

    fun pressJump() {
        if (screenState != ScreenState.GAME_LEVEL) return
        if (isGrounded) {
            vy = if (gravityFlipped) 440f else -440f
            isGrounded = false
            SoundManager.playJump()
        } else if (doubleJumpAvailable) {
            vy = if (gravityFlipped) 380f else -380f
            doubleJumpAvailable = false
            SoundManager.playJump()
        }
    }

    // --- GAME EVENT ACTIONS ---

    private fun triggerPlayerDeath() {
        SoundManager.stopBackgroundMusic()
        SoundManager.playEvilLaugh()

        levelDeaths++
        deathCount++

        // Save death progress to SQLite
        viewModelScope.launch {
            val progress = dao.getUserProgress() ?: UserProgress()
            dao.saveUserProgress(
                progress.copy(
                    totalDeaths = deathCount,
                    currentLevelDeaths = levelDeaths
                )
            )
        }

        screenState = ScreenState.DEATH_SCREEN
        isGeneratingTaunt = true
        activeTaunt = "MOCKING YOU... 💀"

        viewModelScope.launch {
            activeTaunt = GeminiService.fetchDeathTaunt(deathCount, currentLevelIndex)
            isGeneratingTaunt = false
        }
    }

    private fun triggerFakeWinExposed() {
        showFakeWinCrack = true
        SoundManager.playFakeWin()
        viewModelScope.launch {
            delay(2200)
            triggerPlayerDeath() // Trick win kills player instantly!
        }
    }

    private fun triggerLevelVictory() {
        SoundManager.stopBackgroundMusic()
        SoundManager.playRealWin()

        screenState = ScreenState.LEVEL_COMPLETE
    }

    fun proceedToNextLevel() {
        viewModelScope.launch {
            val progress = dao.getUserProgress() ?: UserProgress()
            currentLevelIndex++
            levelDeaths = 0
            
            val updatedMaxLevel = maxOf(progress.currentLevel, currentLevelIndex)
            
            // Save level progression to DB
            dao.saveUserProgress(
                progress.copy(
                    currentLevel = updatedMaxLevel,
                    currentLevelDeaths = 0
                )
            )

            if (currentLevelIndex > 50) {
                // Game completed loop back or congrats
                currentLevelIndex = 1
            }

            startGame()
        }
    }

    // ADS SUBMISSION SIMULATIONS & PROMPTED TAUNTS
    fun requestRealAILobbyHint(isFake: Boolean) {
        isGeneratingHint = true
        aiHintText = "FETCHING SYSTEM INTEL... 💡"
        
        viewModelScope.launch {
            val level = currentLevelData ?: return@launch
            if (isFake) {
                val trapString = level.trapTypes.joinToString { it.displayName }
                aiHintText = GeminiService.fetchFakeHint(level.id, trapString)
            } else {
                aiHintText = GeminiService.fetchRealHint(level.id, level.safePathDescription)
            }
            isGeneratingHint = false
        }
    }

    fun skipToLevel(index: Int) {
        currentLevelIndex = index
        levelDeaths = 0
        viewModelScope.launch {
            val progress = dao.getUserProgress() ?: UserProgress()
            dao.saveUserProgress(progress.copy(currentLevel = index, currentLevelDeaths = 0))
            startGame()
        }
    }
}
