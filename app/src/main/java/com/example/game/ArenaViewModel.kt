package com.example.game

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ArenaSoundEngine
import com.example.data.HighScoreEntity
import com.example.data.KickoffDatabase
import com.example.data.KickoffRepository
import com.example.data.UserArenaSettings
import com.example.game.ArenaConstants.BR
import com.example.game.ArenaConstants.CONFETTI_COLORS
import com.example.game.ArenaConstants.CX
import com.example.game.ArenaConstants.CY
import com.example.game.ArenaConstants.FX0
import com.example.game.ArenaConstants.FX1
import com.example.game.ArenaConstants.FY0
import com.example.game.ArenaConstants.FY1
import com.example.game.ArenaConstants.GD
import com.example.game.ArenaConstants.GY0
import com.example.game.ArenaConstants.GY1
import com.example.game.ArenaConstants.HOME
import com.example.game.ArenaConstants.MATCH_TIME
import com.example.game.ArenaConstants.PR
import com.example.game.ArenaConstants.SPEED
import com.example.game.ArenaConstants.SPRINT
import com.example.game.ArenaConstants.STEP
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class ArenaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KickoffDatabase.getInstance(application)
    private val prefs = application.getSharedPreferences("kickoff_arena_prefs", Context.MODE_PRIVATE)
    private val repository = KickoffRepository(database.highScoreDao(), prefs)
    val soundEngine = ArenaSoundEngine()

    val highScores: StateFlow<List<HighScoreEntity>> = repository.topScores.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var settings by mutableStateOf(repository.loadSettings())
        private set

    // Screen and UI state
    var screenState by mutableStateOf(ArenaScreenState.MENU)
        private set
    var role by mutableStateOf(MatchRole.NONE)
        private set
    var selectedDifficulty by mutableStateOf(CpuDifficulty.fromName(settings.cpuDifficulty))
        private set
    var isPaused by mutableStateOf(false)
        private set
    var showSettingsModal by mutableStateOf(false)
        private set
    var showRotateNotice by mutableStateOf(false)
        private set
    var forceVirtualLandscape by mutableStateOf(false)
        private set
    var showMultiplayerPanel by mutableStateOf(false)
        private set

    // Match text / lobby state
    var nicknameInput by mutableStateOf(settings.nickname)
        private set
    var pinInput by mutableStateOf("")
        private set
    var menuMsg by mutableStateOf("")
        private set
    var lobbyMsg by mutableStateOf("")
        private set
    var hostPin by mutableStateOf("----")
        private set
    var joinPin by mutableStateOf("----")
        private set
    var opponentConnectedInLobby by mutableStateOf(false)
        private set
    var toastMessage by mutableStateOf("")
        private set
    private var toastJob: Job? = null

    // Match scoreboard & timers
    var score0 by mutableStateOf(0)
        private set
    var score1 by mutableStateOf(0)
        private set
    var teamName0 by mutableStateOf("Blue")
        private set
    var teamName1 by mutableStateOf("CPU")
        private set
    var matchTime by mutableStateOf(MATCH_TIME)
        private set
    var countdownTime by mutableStateOf(0f)
        private set
    var suddenDeath by mutableStateOf(false)
        private set
    var showBottomHint by mutableStateOf(true)
        private set
    var lastEarnedPoints by mutableStateOf(0)
        private set
    var lastSavedScoreId by mutableStateOf(-1L)
        private set
    var lastMatchResult by mutableStateOf("W")
        private set

    // Live actors for 60fps Canvas rendering
    val players = List(4) { i -> PlayerActor(index = i) }
    val ball = BallActor()
    val powerups = mutableListOf<PowerupActor>()
    val particles = mutableListOf<ParticleFx>()
    val rings = mutableListOf<RingFx>()
    val floatingTexts = mutableListOf<FloatingTextFx>()

    var shake by mutableStateOf(0f)
        private set
    var flash by mutableStateOf(0f)
        private set
    var localId by mutableStateOf(0)
        private set

    // Frame tick counter so Compose Canvas invalidates at 60fps
    var frameTick by mutableLongStateOf(0L)
        private set

    // Input state
    var joyX = 0f
    var joyY = 0f
    var joyActive = false
    var sprintPressed = false
    private var pendingKick = false

    // Internal simulation timers
    private var goalTimer = 0f
    private var endTimer = 0f
    private var slowTimer = 0f
    private var hintTimer = 0f
    private var attractTime = 0f
    private var accumulator = 0f
    private var lastTickSec = -1
    private var powerupSpawnTimer = 5f
    private var nextPowerupId = 1
    private var resultSaved = false

    init {
        soundEngine.settings = settings
        placePlayers()
    }

    // Settings & Input mutations
    fun onNicknameChange(value: String) {
        val clipped = value.take(10)
        nicknameInput = clipped
        updateSettings { it.copy(nickname = clipped.ifBlank { "Striker" }) }
    }

    fun onPinInputChange(value: String) {
        pinInput = value.filter { it.isDigit() }.take(4)
        if (menuMsg.isNotEmpty()) menuMsg = ""
    }

    fun toggleMultiplayerPanel() {
        showMultiplayerPanel = !showMultiplayerPanel
    }

    fun openSettingsModal() {
        showSettingsModal = true
    }

    fun closeSettingsModal() {
        showSettingsModal = false
    }

    fun openRotateNotice() {
        showRotateNotice = true
    }

    fun dismissRotateNotice() {
        showRotateNotice = false
    }

    fun enableForceVirtualLandscape() {
        forceVirtualLandscape = true
        showRotateNotice = false
        showToast("Virtual Landscape: ON")
    }

    fun toggleVirtualLandscape() {
        forceVirtualLandscape = !forceVirtualLandscape
        if (!forceVirtualLandscape) {
            showRotateNotice = false
        }
        showToast(if (forceVirtualLandscape) "Virtual Landscape: ON" else "Landscape Mode: AUTO")
    }

    fun toggleAutoRotate() {
        setAutoRotate(!settings.autoRotateEnabled)
    }

    fun setAutoRotate(enabled: Boolean) {
        updateSettings { it.copy(autoRotateEnabled = enabled) }
        showToast(if (enabled) "Auto-Rotate: ON (16:9 Landscape)" else "Auto-Rotate: LOCKED")
    }

    fun toggleJoystickMode() {
        val nextFloating = !settings.joystickFloating
        updateSettings { it.copy(joystickFloating = nextFloating) }
        resetJoystickInput()
        showToast(if (nextFloating) "Stick: Floating Mode" else "Stick: Fixed Mode")
    }

    fun toggleMute() {
        val nextMuted = !settings.muted
        updateSettings { it.copy(muted = nextMuted) }
        showToast(if (nextMuted) "Sound: Muted" else "Sound: Enabled")
    }

    fun setVolume(type: String, value: Int) {
        val v = value.coerceIn(0, 100)
        updateSettings { cur ->
            when (type) {
                "master" -> cur.copy(masterVolume = v, muted = if (v > 0) false else cur.muted)
                "sfx" -> cur.copy(sfxVolume = v)
                "crowdLoop" -> cur.copy(crowdLoopVolume = v)
                "crowd" -> cur.copy(whistleVolume = v)
                else -> cur
            }
        }
    }

    fun setSensitivity(sens: Float) {
        updateSettings { it.copy(sensitivity = sens) }
        showToast("Sensitivity: ${sens}x")
    }

    fun setJerseyNumber(number: String) {
        updateSettings { it.copy(jerseyNumber = number) }
        showToast("Jersey: #$number")
    }

    fun setKitColor(hex: String) {
        updateSettings { it.copy(kitColorHex = hex) }
        showToast("Kit Color Updated")
    }

    fun testSound(name: String) {
        soundEngine.sfx(name)
    }

    private fun updateSettings(transform: (UserArenaSettings) -> UserArenaSettings) {
        val next = transform(settings)
        settings = next
        soundEngine.settings = next
        repository.saveSettings(next)
    }

    fun showToast(msg: String) {
        toastMessage = msg
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(2000L)
            toastMessage = ""
        }
    }

    // Match flow commands
    private fun resolvedNickname(): String =
        nicknameInput.trim().ifBlank { "Striker" }.take(10)

    fun openCpuDifficultySelect() {
        val myName = resolvedNickname()
        updateSettings { it.copy(nickname = myName) }
        isPaused = false
        menuMsg = ""
        screenState = ArenaScreenState.DIFFICULTY_SELECT
    }

    fun selectDifficulty(difficulty: CpuDifficulty) {
        selectedDifficulty = difficulty
        updateSettings { it.copy(cpuDifficulty = difficulty.name) }
        soundEngine.sfx("tick")
    }

    fun startCpuMatch(difficulty: CpuDifficulty = selectedDifficulty) {
        selectedDifficulty = difficulty
        val myName = resolvedNickname()
        updateSettings { it.copy(nickname = myName, cpuDifficulty = difficulty.name) }
        role = MatchRole.CPU
        localId = 0
        players.forEachIndexed { idx, p -> p.human = (idx == 0) }
        teamName0 = myName
        teamName1 = "CPU"
        beginMatch()
    }

    fun startHostLobby() {
        val myName = resolvedNickname()
        updateSettings { it.copy(nickname = myName) }
        role = MatchRole.HOST
        screenState = ArenaScreenState.LOBBY
        localId = 0
        hostPin = (1000 + Random.nextInt(9000)).toString()
        opponentConnectedInLobby = false
        lobbyMsg = "Waiting for an opponent on your network..."
        menuMsg = ""
        // Simulate a network challenger joining after 2.5 seconds if still waiting
        viewModelScope.launch {
            delay(2500L)
            if (screenState == ArenaScreenState.LOBBY && role == MatchRole.HOST) {
                opponentConnectedInLobby = true
                lobbyMsg = "Rival_10 joined PIN $hostPin!"
            }
        }
    }

    fun joinMatchWithPin() {
        val pin = pinInput.trim()
        if (!pin.matches(Regex("^\\d{4}$"))) {
            menuMsg = "Enter 4-digit PIN."
            return
        }
        val myName = resolvedNickname()
        updateSettings { it.copy(nickname = myName) }
        role = MatchRole.GUEST
        joinPin = pin
        screenState = ArenaScreenState.LOBBY
        localId = 2
        opponentConnectedInLobby = true
        lobbyMsg = "Connected to PIN $pin! Ready to kick off."
        menuMsg = ""
    }

    fun startFromLobby() {
        val myName = resolvedNickname()
        if (role == MatchRole.GUEST) {
            localId = 2
            players.forEachIndexed { idx, p -> p.human = (idx == 2) }
            teamName0 = "Host_$joinPin"
            teamName1 = myName
        } else {
            localId = 0
            players.forEachIndexed { idx, p -> p.human = (idx == 0) }
            teamName0 = myName
            teamName1 = if (opponentConnectedInLobby) "Rival_10" else "CPU"
        }
        beginMatch()
    }

    fun beginMatch() {
        score0 = 0
        score1 = 0
        matchTime = MATCH_TIME
        suddenDeath = false
        resultSaved = false
        lastSavedScoreId = -1L
        placePlayers()
        clearPowerups()
        players.forEach { p ->
            p.stam = 1f
            p.cd = 0f
            p.tk = 0f
            p.buffSpeed = 0f
            p.buffKick = 0f
        }
        particles.clear()
        floatingTexts.clear()
        rings.clear()
        shake = 0f
        flash = 0f
        slowTimer = 0f
        pendingKick = false
        screenState = ArenaScreenState.COUNTDOWN
        countdownTime = 3f
        lastTickSec = -1
        accumulator = 0f
        hintTimer = 7f
        showBottomHint = true
        isPaused = false
        resetJoystickInput()
    }

    fun rematch() {
        beginMatch()
    }

    fun leaveToMenu(msg: String = "") {
        role = MatchRole.NONE
        screenState = ArenaScreenState.MENU
        isPaused = false
        clearPowerups()
        menuMsg = msg
        resetJoystickInput()
    }

    fun togglePause() {
        if (!screenState.isInMatch()) return
        isPaused = !isPaused
    }

    fun setPausedState(paused: Boolean) {
        isPaused = paused
    }

    fun triggerShoot() {
        pendingKick = true
    }

    fun resetJoystickInput() {
        joyX = 0f
        joyY = 0f
        joyActive = false
        sprintPressed = false
    }

    // Physics & Simulation Loop (called on every animation frame)
    fun stepFrame(dtRaw: Float, nowMillis: Long) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        val inMatch = screenState.isInMatch()

        if (screenState == ArenaScreenState.MENU ||
            screenState == ArenaScreenState.DIFFICULTY_SELECT ||
            screenState == ArenaScreenState.LOBBY
        ) {
            attract(dt)
        }

        if (inMatch && !isPaused && !showSettingsModal) {
            accumulator += dt * (if (slowTimer > 0f) 0.35f else 1f)
            var steps = 0
            while (accumulator >= STEP && steps < 4) {
                simStep(STEP)
                accumulator -= STEP
                steps++
            }
            if (steps == 4) accumulator = 0f
        } else {
            accumulator = 0f
        }

        if (slowTimer > 0f) slowTimer = max(0f, slowTimer - dt)

        val bd = hypot(ball.x - ball.px, ball.y - ball.py)
        ball.rot += bd / BR
        ball.px = ball.x
        ball.py = ball.y
        if (hypot(ball.vx, ball.vy) > 60f || bd > 1f) {
            ball.trail.add(TrailPoint(ball.x, ball.y))
            if (ball.trail.size > 9) ball.trail.removeAt(0)
        } else if (ball.trail.isNotEmpty()) {
            ball.trail.removeAt(0)
        }

        updateFX(dt)

        if (hintTimer > 0f) {
            hintTimer -= dt
            if (hintTimer <= 0f) showBottomHint = false
        }

        frameTick = nowMillis
    }

    private fun attract(dt: Float) {
        attractTime += dt
        ball.px = ball.x
        ball.py = ball.y
        ball.x = CX + cos(attractTime * 0.8f) * 280f
        ball.y = CY + sin(attractTime * 1.3f) * 160f
        players.forEachIndexed { i, p ->
            p.x = HOME[i].first + sin(attractTime * 1.2f + i) * 16f
            p.y = HOME[i].second + cos(attractTime * 0.9f + i * 2) * 35f
            p.vx = 0f
            p.vy = 0f
        }
    }

    private fun placePlayers() {
        players.forEachIndexed { i, p ->
            p.x = HOME[i].first
            p.y = HOME[i].second
            p.vx = 0f
            p.vy = 0f
            p.cd = 0f
            p.sq = 0f
            p.tk = 0f
        }
        ball.x = CX
        ball.y = CY
        ball.vx = 0f
        ball.vy = 0f
        ball.px = CX
        ball.py = CY
        ball.trail.clear()
        ball.fireT = 0f
    }

    private fun clearPowerups() {
        powerups.clear()
        powerupSpawnTimer = 4.5f
        players.forEach { p ->
            p.buffSpeed = 0f
            p.buffKick = 0f
        }
        ball.fireT = 0f
    }

    private fun spawnPowerup() {
        if (powerups.size >= 2) return
        val type = if (Random.nextFloat() < 0.5f) "speed" else "kick"
        val pu = PowerupActor(
            id = nextPowerupId++,
            type = type,
            x = rnd(FX0 + 140f, FX1 - 140f),
            y = rnd(FY0 + 60f, FY1 - 60f),
            r = 16f
        )
        powerups.add(pu)
        rings.add(
            RingFx(
                x = pu.x,
                y = pu.y,
                maxRadius = 26f,
                color = if (type == "speed") Color(0xFF00F0FF) else Color(0xFFFF7A00),
                life = 0.35f,
                maxLife = 0.35f
            )
        )
    }

    private fun simStep(dt: Float) {
        val live = screenState == ArenaScreenState.PLAY || screenState == ArenaScreenState.GOAL
        for (p in players) {
            if (p.buffSpeed > 0f) p.buffSpeed = max(0f, p.buffSpeed - dt)
            if (p.buffKick > 0f) p.buffKick = max(0f, p.buffKick - dt)
            if (p.human && p.index == localId) {
                if (live) {
                    val sens = settings.sensitivity
                    movePlayer(
                        p,
                        if (joyActive) joyX * sens else 0f,
                        if (joyActive) joyY * sens else 0f,
                        sprintPressed,
                        dt
                    )
                } else {
                    movePlayer(p, 0f, 0f, false, dt)
                }
            } else if (live) {
                aiStep(p, dt)
            } else {
                movePlayer(p, 0f, 0f, false, dt)
            }
        }

        if (pendingKick) {
            pendingKick = false
            val me = players[localId]
            if (live) kickBall(me, null)
        }

        for (p in players) ballTouch(p)
        for (a in 0 until 4) {
            for (b in a + 1 until 4) {
                playerPush(players[a], players[b])
            }
        }
        checkPickups()
        ballStep(dt)
        for (p in players) {
            p.cd = max(0f, p.cd - dt)
            p.tk = max(0f, p.tk - dt)
        }
        stateStep(dt)
    }

    private fun movePlayer(
        p: PlayerActor,
        rawIx: Float,
        rawIy: Float,
        sp: Boolean,
        dt: Float,
        speedMultiplier: Float = 1.0f
    ) {
        var ix = rawIx
        var iy = rawIy
        var m = hypot(ix, iy)
        if (m > 1f) {
            ix /= m
            iy /= m
            m = 1f
        }
        val sm = (if (p.buffSpeed > 0f) 1.6f else 1.0f) * speedMultiplier
        var top = (if (p.isGk) SPEED * 0.95f else SPEED) * sm
        val spr = sp && m > 0.25f && p.stam > 0.02f
        if (spr) top = SPRINT * sm
        if (p.buffSpeed > 0f) {
            p.stam = 1.0f
        } else {
            if (spr) p.stam = max(0f, p.stam - 0.55f * dt)
            else p.stam = min(1f, p.stam + 0.3f * dt)
        }
        p.sprint = spr
        val k = 1f - exp(-(if (m > 0.05f) 16f else 10f) * dt)
        p.vx += (ix * top * m - p.vx) * k
        p.vy += (iy * top * m - p.vy) * k
        p.x = (p.x + p.vx * dt).coerceIn(FX0 + 12f, FX1 - 12f)
        p.y = (p.y + p.vy * dt).coerceIn(FY0 + 12f, FY1 - 12f)
    }

    private fun kickBall(
        p: PlayerActor,
        aim: Pair<Float, Float>?,
        powerScale: Float = 1.0f,
        cooldownBonus: Float = 0f
    ) {
        if (p.cd > 0f) return
        p.cd = 0.28f + cooldownBonus
        p.sq = 1f
        val dx = ball.x - p.x
        val dy = ball.y - p.y
        val d = hypot(dx, dy)
        if (d > PR + BR + 20f) {
            val wx = p.x + (if (p.team == 1) -1f else 1f) * PR * 0.9f
            burst(wx, p.y, 4, listOf(Color(0xFFCFE3FF)), 120f, 0.25f, 2f)
            soundEngine.sfx("whiff")
            return
        }
        var ux = if (d < 1f) (if (p.team == 1) -1f else 1f) else dx / d
        var uy = if (d < 1f) 0f else dy / d
        if (aim != null) {
            ux = aim.first
            uy = aim.second
        }
        val sp = hypot(p.vx, p.vy)
        val isSuper = p.buffKick > 0f
        val base = ((if (p.sprint) 560f else 460f) + min(sp, 420f) * 0.45f + (if (aim != null) 60f else 0f)) * powerScale
        val pw = if (isSuper) base * 1.75f + 200f else base
        ball.vx = ux * pw + p.vx * 0.3f
        ball.vy = uy * pw + p.vy * 0.3f
        if (isSuper) {
            ball.fireT = 1.8f
            burst(
                ball.x,
                ball.y,
                25,
                listOf(Color(0xFFFF7A00), Color(0xFFFFD23F), Color(0xFFFF5364)),
                500f,
                0.5f,
                5f
            )
            rings.add(RingFx(ball.x, ball.y, 50f, Color(0xFFFF7A00), 0.4f, 0.4f))
            shake = max(shake, 12f)
            addText("SUPER KICK!", ball.x, ball.y - 25f, Color(0xFFFF9900), 24f, 1.0f)
            soundEngine.sfx("superkick")
        } else {
            val big = pw > 650f
            burst(ball.x, ball.y, if (big) 12 else 6, listOf(Color.White), if (big) 360f else 200f, 0.35f, 3.2f)
            rings.add(RingFx(ball.x, ball.y, if (big) 34f else 20f, Color.White.copy(alpha = 0.8f), 0.25f, 0.25f))
            shake = max(shake, if (big) 5f else 2.5f)
            soundEngine.sfx("kick")
        }
    }

    private fun ballTouch(p: PlayerActor) {
        var dx = ball.x - p.x
        var dy = ball.y - p.y
        var d = hypot(dx, dy)
        val minD = PR + BR
        if (d >= minD) return
        if (d < 0.01f) {
            dx = if (p.team == 1) 1f else -1f
            dy = 0f
            d = 1f
        }
        val nx = dx / d
        val ny = dy / d
        ball.x = p.x + nx * minD
        ball.y = p.y + ny * minD
        val rel = (ball.vx - p.vx) * nx + (ball.vy - p.vy) * ny
        if (rel < 0f) {
            ball.vx -= 1.6f * rel * nx
            ball.vy -= 1.6f * rel * ny
        }
        val sp = hypot(ball.vx, ball.vy)
        val pv = hypot(p.vx, p.vy)
        if (pv > 30f && sp < 300f) {
            ball.vx += (p.vx - ball.vx) * 0.1f
            ball.vy += (p.vy - ball.vy) * 0.1f
        }
    }

    private fun playerPush(a: PlayerActor, b: PlayerActor) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val d = hypot(dx, dy)
        val m = PR * 2f
        if (d >= m || d < 0.01f) return
        val nx = dx / d
        val ny = dy / d
        val o = (m - d) / 2f
        a.x -= nx * o
        a.y -= ny * o
        b.x += nx * o
        b.y += ny * o
        val rel = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny
        if (rel > 180f && a.team != b.team && a.tk <= 0f && b.tk <= 0f) {
            a.tk = 0.4f
            b.tk = 0.4f
            burst(
                (a.x + b.x) / 2f,
                (a.y + b.y) / 2f,
                14,
                listOf(Color(0xFFFFD23F), Color(0xFFFF5364)),
                300f,
                0.35f,
                3.5f
            )
            soundEngine.sfx("tackle")
        }
    }

    private fun checkPickups() {
        if (screenState != ArenaScreenState.PLAY) return
        val it = powerups.iterator()
        while (it.hasNext()) {
            val pu = it.next()
            var picked = false
            for (p in players) {
                if (hypot(p.x - pu.x, p.y - pu.y) < PR + pu.r) {
                    if (pu.type == "speed") {
                        p.buffSpeed = p.maxSpeedBuff
                        p.stam = 1f
                        burst(pu.x, pu.y, 22, listOf(Color(0xFF00F0FF), Color.White), 320f, 0.45f, 4f)
                        rings.add(RingFx(pu.x, pu.y, 40f, Color(0xFF00F0FF), 0.3f, 0.3f))
                        addText("+SPEED BOOST!", pu.x, pu.y - 25f, Color(0xFF00F0FF), 22f, 1.1f)
                        soundEngine.sfx("pu_speed")
                    } else {
                        p.buffKick = p.maxKickBuff
                        burst(pu.x, pu.y, 22, listOf(Color(0xFFFF7A00), Color(0xFFFFD23F)), 320f, 0.45f, 4.5f)
                        rings.add(RingFx(pu.x, pu.y, 40f, Color(0xFFFF7A00), 0.3f, 0.3f))
                        addText("+SUPER KICK!", pu.x, pu.y - 25f, Color(0xFFFF9900), 22f, 1.1f)
                        soundEngine.sfx("pu_kick")
                    }
                    picked = true
                    break
                }
            }
            if (picked) it.remove()
        }
    }

    private fun scoreGoal(team: Int) {
        if (team == 0) score0++ else score1++
        screenState = ArenaScreenState.GOAL
        goalTimer = 2.2f
        clearPowerups()
        slowTimer = 0.8f
        shake = 20f
        flash = 0.6f
        burst(ball.x, ball.y, 120, CONFETTI_COLORS, 500f, 2.0f, 7f, gravity = 300f, isRect = true)
        rings.add(RingFx(ball.x, ball.y, 80f, Color(0xFFFFD23F), 0.7f, 0.7f))
        addText("GOAL!", CX, CY - 25f, Color(0xFFFFD23F), 110f, 1.8f)
        val scorer = (if (team == 0) teamName0 else teamName1).uppercase()
        addText("$scorer SCORES", CX, CY + 50f, Color.White, 30f, 1.8f)
        soundEngine.sfx("goal")
    }

    private fun ballStep(dt: Float) {
        ball.x += ball.vx * dt
        ball.y += ball.vy * dt
        val f = 0.6f.pow(dt)
        ball.vx *= f
        ball.vy *= f
        val sp = hypot(ball.vx, ball.vy)
        val maxV = if (ball.fireT > 0f) 1500f else 1150f
        if (sp > maxV) {
            val k = maxV / sp
            ball.vx *= k
            ball.vy *= k
        }
        if (ball.fireT > 0f) {
            ball.fireT -= dt
            if (Random.nextFloat() < 0.8f) {
                burst(
                    ball.x + rnd(-4f, 4f),
                    ball.y + rnd(-4f, 4f),
                    3,
                    listOf(Color(0xFFFF7A00), Color(0xFFFFD23F)),
                    80f,
                    0.35f,
                    4f
                )
            }
        }
        if (ball.y < FY0 + BR) {
            ball.y = FY0 + BR
            if (ball.vy < -120f) wallFx(ball.x, FY0)
            ball.vy = abs(ball.vy) * 0.72f
        } else if (ball.y > FY1 - BR) {
            ball.y = FY1 - BR
            if (ball.vy > 120f) wallFx(ball.x, FY1)
            ball.vy = -abs(ball.vy) * 0.72f
        }
        val mouth = ball.y > GY0 + 3f && ball.y < GY1 - 3f
        // Left goal (Team 1 scores on left goal!)
        if (ball.x < FX0 && mouth) {
            if (screenState == ArenaScreenState.PLAY) scoreGoal(1)
            if (ball.x < FX0 - GD + BR) {
                ball.x = FX0 - GD + BR
                ball.vx = abs(ball.vx) * 0.35f
            }
        } else if (ball.x < FX0 + BR) {
            ball.x = FX0 + BR
            if (ball.vx < 0f) {
                if (-ball.vx > 120f) wallFx(FX0, ball.y)
                ball.vx = -ball.vx * 0.72f
            }
        }
        // Right goal (Team 0 scores on right goal!)
        if (ball.x > FX1 && mouth) {
            if (screenState == ArenaScreenState.PLAY) scoreGoal(0)
            if (ball.x > FX1 + GD - BR) {
                ball.x = FX1 + GD - BR
                ball.vx = -abs(ball.vx) * 0.35f
            }
        } else if (ball.x > FX1 - BR) {
            ball.x = FX1 - BR
            if (ball.vx > 0f) {
                if (ball.vx > 120f) wallFx(FX1, ball.y)
                ball.vx = -ball.vx * 0.72f
            }
        }
    }

    private fun wallFx(x: Float, y: Float) {
        burst(x, y, 5, listOf(Color(0xFFE7FFF0)), 200f, 0.3f, 2f)
        soundEngine.sfx("wall")
    }

    private fun stateStep(dt: Float) {
        when (screenState) {
            ArenaScreenState.COUNTDOWN -> {
                countdownTime -= dt
                val n = ceil(countdownTime).toInt()
                if (n != lastTickSec && countdownTime > 0f) {
                    lastTickSec = n
                    soundEngine.sfx("tick")
                }
                if (countdownTime <= 0f) {
                    screenState = ArenaScreenState.PLAY
                    lastTickSec = -1
                    addText("KICK OFF!", CX, CY, Color(0xFFFFD23F), 64f, 1.0f)
                    soundEngine.sfx("whistle")
                }
            }
            ArenaScreenState.PLAY -> {
                matchTime -= dt
                powerupSpawnTimer -= dt
                if (powerupSpawnTimer <= 0f) {
                    powerupSpawnTimer = rnd(8f, 14f)
                    spawnPowerup()
                }
                if (matchTime <= 0f) {
                    matchTime = 0f
                    if (score0 == score1) {
                        if (!suddenDeath) {
                            suddenDeath = true
                            addText("SUDDEN DEATH", CX, CY, Color(0xFFFF5364), 60f, 1.8f)
                            soundEngine.sfx("sudden")
                        }
                    } else {
                        endMatch()
                    }
                }
            }
            ArenaScreenState.GOAL -> {
                goalTimer -= dt
                if (goalTimer <= 0f) {
                    if (suddenDeath) {
                        endMatch()
                    } else {
                        placePlayers()
                        screenState = ArenaScreenState.COUNTDOWN
                        countdownTime = 2.4f
                        lastTickSec = -1
                    }
                }
            }
            ArenaScreenState.END -> {
                endTimer -= dt
                if (endTimer <= 0f) {
                    finishMatchAndShowOver()
                }
            }
            else -> {}
        }
    }

    private fun endMatch() {
        screenState = ArenaScreenState.END
        endTimer = 1.8f
        clearPowerups()
        addText("FULL TIME", CX, CY, Color.White, 76f, 1.6f)
        soundEngine.sfx("long")
    }

    private fun finishMatchAndShowOver() {
        screenState = ArenaScreenState.OVER
        val mySide = if (role == MatchRole.GUEST) 1 else 0
        val myGoals = if (mySide == 0) score0 else score1
        val opGoals = if (mySide == 0) score1 else score0
        val res = when {
            myGoals > opGoals -> "W"
            myGoals < opGoals -> "L"
            else -> "D"
        }
        lastMatchResult = res
        val mult = if (role == MatchRole.CPU) selectedDifficulty.pointsMultiplier else 1.0f
        lastEarnedPoints = KickoffRepository.calculatePoints(myGoals, opGoals, res, mult)
        if (!resultSaved && role != MatchRole.NONE) {
            resultSaved = true
            val myName = resolvedNickname()
            val modeStr = if (role == MatchRole.CPU) selectedDifficulty.shortCode else "P2P"
            viewModelScope.launch {
                val (_, rowId) = repository.recordMatchResult(
                    playerName = myName,
                    myGoals = myGoals,
                    opGoals = opGoals,
                    result = res,
                    mode = modeStr,
                    pointsMultiplier = mult
                )
                lastSavedScoreId = rowId
            }
        }
    }

    private fun aiStep(p: PlayerActor, dt: Float) {
        // Apply selected CPU difficulty to the opponent team; keep the player's allied GK balanced at MEDIUM
        val localTeam = players[localId].team
        val diff = if (role == MatchRole.CPU && p.team != localTeam) {
            selectedDifficulty
        } else {
            CpuDifficulty.MEDIUM
        }

        if (p.isGk) {
            val hx = if (p.team == 1) FX1 - 48f else FX0 + 48f
            val predictedBallY = (ball.y + ball.vy * (diff.leadBallPrediction * 0.7f))
                .coerceIn(GY0 + 14f, GY1 - 14f)
            val reactDiv = if (diff == CpuDifficulty.EASY) 42f else 22f
            movePlayer(
                p = p,
                rawIx = ((hx - p.x) / 38f).coerceIn(-1f, 1f),
                rawIy = ((predictedBallY - p.y) / reactDiv).coerceIn(-1f, 1f),
                sp = diff == CpuDifficulty.ULTIMATE && abs(predictedBallY - p.y) > 48f,
                dt = dt,
                speedMultiplier = diff.gkSpeedScale
            )
            val d = hypot(ball.x - p.x, ball.y - p.y)
            if (d < PR + BR + 18f && p.cd <= 0f) {
                val ax = if (p.team == 1) -0.82f else 0.82f
                val ay = ((ball.y - CY) / 240f).coerceIn(-0.55f, 0.55f)
                val l = hypot(ax, ay)
                kickBall(
                    p = p,
                    aim = (ax / l) to (ay / l),
                    powerScale = diff.kickPowerScale,
                    cooldownBonus = diff.kickCooldownBonus
                )
            }
            return
        }

        // Predictive ball target based on difficulty
        val predBx = (ball.x + ball.vx * diff.leadBallPrediction).coerceIn(FX0 + BR, FX1 - BR)
        val predBy = (ball.y + ball.vy * diff.leadBallPrediction).coerceIn(FY0 + BR, FY1 - BR)

        val gx = if (p.team == 1) FX0 else FX1
        // Ultimate difficulty aims for the open goal corner away from the opponent goalkeeper
        val targetGoalY = if (diff.smartCornerAim) {
            val oppGk = players.firstOrNull { it.team != p.team && it.isGk }
            if (oppGk != null && oppGk.y > CY) GY0 + 22f else GY1 - 22f
        } else {
            CY + if (diff.aimJitter > 0f) rnd(-diff.aimJitter, diff.aimJitter) else 0f
        }

        var ux = gx - predBx
        var uy = targetGoalY - predBy
        val ul = hypot(ux, uy).let { if (it < 0.001f) 1f else it }
        ux /= ul
        uy /= ul

        p.aiT -= dt
        if (p.aiT <= 0f) {
            p.aiT = diff.reactionInterval
            val sx = predBx - ux * (PR + BR + 8f)
            val sy = predBy - uy * (PR + BR + 8f)
            val jitter = diff.aimJitter
            p.aiX = sx + if (jitter > 0f) rnd(-jitter, jitter) else 0f
            p.aiY = sy + if (jitter > 0f) rnd(-jitter, jitter) else 0f
        }

        var tx = predBx
        var ty = predBy
        val dBall = hypot(ball.x - p.x, ball.y - p.y)

        if (diff.powerupRadius > 0f && powerups.isNotEmpty() && dBall > 190f) {
            for (pu in powerups) {
                if (hypot(pu.x - p.x, pu.y - p.y) < diff.powerupRadius) {
                    tx = pu.x
                    ty = pu.y
                    break
                }
            }
        } else {
            val side = (p.x - predBx) * ux + (p.y - predBy) * uy
            if (side > 0f && dBall > PR + BR + 12f) {
                tx = p.aiX
                ty = p.aiY
            }
        }

        val dx = tx - p.x
        val dy = ty - p.y
        val dl = hypot(dx, dy).let { if (it < 0.001f) 1f else it }
        val k = min(1f, dl / 65f) * (if (diff == CpuDifficulty.EASY) 0.85f else 0.96f)
        val shouldSprint = (dl > diff.sprintDist && p.stam > diff.sprintMinStamina) || p.buffSpeed > 0f

        movePlayer(
            p = p,
            rawIx = (dx / dl) * k,
            rawIy = (dy / dl) * k,
            sp = shouldSprint,
            dt = dt,
            speedMultiplier = diff.speedScale
        )

        if (dBall < PR + BR + 16f && p.cd <= 0f) {
            // Easy/Medium get slight shot angle spread; Hard/Ultimate fire laser shots
            val shotUy = (uy + if (diff.aimJitter > 6f) rnd(-0.18f, 0.18f) else 0f)
            val shotL = hypot(ux, shotUy).let { if (it < 0.001f) 1f else it }
            kickBall(
                p = p,
                aim = (ux / shotL) to (shotUy / shotL),
                powerScale = diff.kickPowerScale,
                cooldownBonus = diff.kickCooldownBonus
            )
        }
    }

    private fun burst(
        x: Float,
        y: Float,
        count: Int,
        colors: List<Color>,
        speed: Float,
        life: Float,
        size: Float,
        gravity: Float = 0f,
        growth: Float = 0f,
        isRect: Boolean = false
    ) {
        for (i in 0 until count) {
            val a = Random.nextFloat() * (PI.toFloat() * 2f)
            val s = speed * (0.35f + Random.nextFloat() * 0.65f)
            val l = life * (0.7f + Random.nextFloat() * 0.5f)
            particles.add(
                ParticleFx(
                    x = x,
                    y = y,
                    vx = cos(a) * s,
                    vy = sin(a) * s,
                    life = l,
                    maxLife = l,
                    size = size * (0.6f + Random.nextFloat() * 0.8f),
                    color = colors[Random.nextInt(colors.size)],
                    gravity = gravity,
                    growth = growth,
                    isRect = isRect,
                    angle = Random.nextFloat() * (PI.toFloat() * 2f),
                    angularVel = (Random.nextFloat() - 0.5f) * 12f
                )
            )
        }
        if (particles.size > 400) {
            particles.subList(0, particles.size - 400).clear()
        }
    }

    private fun addText(text: String, x: Float, y: Float, color: Color, size: Float, life: Float) {
        floatingTexts.add(
            FloatingTextFx(
                text = text,
                x = x,
                y = y,
                color = color,
                fontSize = size,
                life = life,
                maxLife = life
            )
        )
    }

    private fun updateFX(dt: Float) {
        shake *= exp(-dt * 9f)
        if (shake < 0.05f) shake = 0f
        flash = max(0f, flash - dt * 2.5f)
        val dd = exp(-dt * 3f)
        val pIt = particles.iterator()
        while (pIt.hasNext()) {
            val p = pIt.next()
            p.life -= dt
            p.vx *= dd
            p.vy *= dd
            p.vy += p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.size += p.growth * dt
            p.angle += p.angularVel * dt
            if (p.life <= 0f || p.size <= 0.2f) pIt.remove()
        }
        val rIt = rings.iterator()
        while (rIt.hasNext()) {
            val r = rIt.next()
            r.life -= dt
            if (r.life <= 0f) rIt.remove()
        }
        val tIt = floatingTexts.iterator()
        while (tIt.hasNext()) {
            val t = tIt.next()
            t.life -= dt
            if (t.life <= 0f) tIt.remove()
        }
        for (p in players) {
            p.sq = max(0f, p.sq - dt * 6f)
        }
    }

    private fun rnd(a: Float, b: Float): Float = a + Random.nextFloat() * (b - a)
}
