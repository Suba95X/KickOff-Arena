package com.example.game

import androidx.compose.ui.graphics.Color

object ArenaConstants {
    // Exact 16:9 landscape coordinate space (1120 / 630 = 16 / 9)
    const val W = 1120f
    const val H = 630f
    const val FX0 = 76f
    const val FX1 = 1044f
    const val FY0 = 48f
    const val FY1 = 582f
    const val GY0 = 248f
    const val GY1 = 382f
    const val GD = 36f
    const val CX = 560f
    const val CY = 315f
    const val BR = 12f
    const val PR = 20f
    const val MATCH_TIME = 120f
    const val SPEED = 290f
    const val SPRINT = 470f
    const val STEP = 1f / 60f

    val HOME = listOf(
        485f to CY,
        (FX0 + 48f) to CY,
        635f to CY,
        (FX1 - 48f) to CY
    )

    val CONFETTI_COLORS = listOf(
        Color(0xFFFFD23F),
        Color(0xFFFF5364),
        Color(0xFF3AA7FF),
        Color(0xFF2FD28A),
        Color(0xFFFFFFFF),
        Color(0xFFB28BFF)
    )
}

enum class ArenaScreenState {
    MENU,
    DIFFICULTY_SELECT,
    LOBBY,
    COUNTDOWN,
    PLAY,
    GOAL,
    END,
    OVER;

    fun isInMatch(): Boolean =
        this == COUNTDOWN || this == PLAY || this == GOAL || this == END
}

enum class CpuDifficulty(
    val title: String,
    val shortCode: String,
    val subtitle: String,
    val icon: String,
    val description: String,
    val statSummary: String,
    val levelBars: Int,
    val accentColor: Color,
    val speedScale: Float,
    val gkSpeedScale: Float,
    val reactionInterval: Float,
    val aimJitter: Float,
    val sprintDist: Float,
    val sprintMinStamina: Float,
    val kickPowerScale: Float,
    val kickCooldownBonus: Float,
    val powerupRadius: Float,
    val leadBallPrediction: Float,
    val smartCornerAim: Boolean,
    val pointsMultiplier: Float
) {
    EASY(
        title = "EASY",
        shortCode = "EASY",
        subtitle = "Warm-Up",
        icon = "🌱",
        description = "Relaxed pace, slower CPU reactions & forgiving keeper.",
        statSummary = "68% SPD · 58% GK",
        levelBars = 1,
        accentColor = Color(0xFF2FD28A),
        speedScale = 0.68f,
        gkSpeedScale = 0.58f,
        reactionInterval = 0.34f,
        aimJitter = 28f,
        sprintDist = 440f,
        sprintMinStamina = 0.85f,
        kickPowerScale = 0.84f,
        kickCooldownBonus = 0.35f,
        powerupRadius = 0f,
        leadBallPrediction = 0f,
        smartCornerAim = false,
        pointsMultiplier = 1.0f
    ),
    MEDIUM(
        title = "MEDIUM",
        shortCode = "MED",
        subtitle = "Pro Match",
        icon = "⚡",
        description = "Balanced arcade challenge with smart positioning.",
        statSummary = "90% SPD · 85% GK",
        levelBars = 2,
        accentColor = Color(0xFF3AA7FF),
        speedScale = 0.90f,
        gkSpeedScale = 0.85f,
        reactionInterval = 0.16f,
        aimJitter = 10f,
        sprintDist = 260f,
        sprintMinStamina = 0.58f,
        kickPowerScale = 1.0f,
        kickCooldownBonus = 0.10f,
        powerupRadius = 220f,
        leadBallPrediction = 0.12f,
        smartCornerAim = false,
        pointsMultiplier = 1.25f
    ),
    HARD(
        title = "HARD",
        shortCode = "HARD",
        subtitle = "All-Star",
        icon = "🔥",
        description = "Fast sprints, sharp shots & active power-up hunting.",
        statSummary = "104% SPD · 105% GK",
        levelBars = 3,
        accentColor = Color(0xFFFF7A00),
        speedScale = 1.04f,
        gkSpeedScale = 1.05f,
        reactionInterval = 0.08f,
        aimJitter = 3.5f,
        sprintDist = 175f,
        sprintMinStamina = 0.35f,
        kickPowerScale = 1.10f,
        kickCooldownBonus = 0f,
        powerupRadius = 340f,
        leadBallPrediction = 0.25f,
        smartCornerAim = false,
        pointsMultiplier = 1.6f
    ),
    ULTIMATE(
        title = "ULTIMATE",
        shortCode = "ULT",
        subtitle = "Legend",
        icon = "👑",
        description = "Predictive AI, laser corner shots & wall-like keeper.",
        statSummary = "115% SPD · 122% GK",
        levelBars = 4,
        accentColor = Color(0xFFFF5364),
        speedScale = 1.15f,
        gkSpeedScale = 1.22f,
        reactionInterval = 0.03f,
        aimJitter = 0f,
        sprintDist = 110f,
        sprintMinStamina = 0.18f,
        kickPowerScale = 1.20f,
        kickCooldownBonus = 0f,
        powerupRadius = 420f,
        leadBallPrediction = 0.36f,
        smartCornerAim = true,
        pointsMultiplier = 2.2f
    );

    companion object {
        fun fromName(raw: String?): CpuDifficulty {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class MatchRole {
    NONE,
    CPU,
    HOST,
    GUEST
}

data class PlayerActor(
    val index: Int,
    val team: Int = index shr 1,
    val isGk: Boolean = (index and 1) == 1,
    var x: Float = ArenaConstants.HOME[index].first,
    var y: Float = ArenaConstants.HOME[index].second,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var human: Boolean = false,
    var stam: Float = 1f,
    var cd: Float = 0f,
    var tk: Float = 0f,
    var sq: Float = 0f,
    var sprint: Boolean = false,
    var aiT: Float = 0f,
    var aiX: Float = 0f,
    var aiY: Float = 0f,
    var buffSpeed: Float = 0f,
    var buffKick: Float = 0f,
    val maxSpeedBuff: Float = 7f,
    val maxKickBuff: Float = 8f
)

data class TrailPoint(val x: Float, val y: Float)

data class BallActor(
    var x: Float = ArenaConstants.CX,
    var y: Float = ArenaConstants.CY,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var px: Float = ArenaConstants.CX,
    var py: Float = ArenaConstants.CY,
    var rot: Float = 0f,
    val trail: MutableList<TrailPoint> = mutableListOf(),
    var fireT: Float = 0f
)

data class PowerupActor(
    val id: Int,
    val type: String, // "speed" or "kick"
    val x: Float,
    val y: Float,
    val r: Float = 16f
)

data class ParticleFx(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    var size: Float,
    val color: Color,
    val gravity: Float = 0f,
    val growth: Float = 0f,
    val isRect: Boolean = false,
    var angle: Float = 0f,
    val angularVel: Float = 0f
)

data class RingFx(
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    val color: Color,
    var life: Float,
    val maxLife: Float
)

data class FloatingTextFx(
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val fontSize: Float,
    var life: Float,
    val maxLife: Float
)
