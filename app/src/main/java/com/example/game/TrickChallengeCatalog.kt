package com.example.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ChallengeCoinSpec(
  val type: PieceType,
  val x: Float,
  val z: Float,
)

data class TrickChallenge(
  val level: Int,
  val title: String,
  val subtitle: String,
  val shotLimit: Int,
  val rewardCoins: Int,
  val coins: List<ChallengeCoinSpec>,
  val hasChaosObstacles: Boolean,
)

object TrickChallengeCatalog {
  private val titles =
    listOf(
      "Corner Warmup",
      "Twin Gate",
      "Royal Break",
      "Crimson Solo",
      "Crossfire Cut",
      "Backspin Split",
      "Pocket Alley",
      "Diagonal Mirror",
      "Queen's Guard",
      "Double Bank",
      "Golden Triangle",
      "Pinball Alley",
      "Center Siege",
      "Edge Thread",
      "Ruby Corridor",
      "Starlight Cluster",
      "Ricochet Twist",
      "Bumper Bounce",
      "Turbo Pad Run",
      "Emerald Gauntlet",
      "Twin Bumpers",
      "Angle Master",
      "Crown Gambit",
      "Shadow Pocket",
      "Velocity Boost",
      "Imperial Rack",
      "Narrow Window",
      "Diagonal Trap",
      "Bank & Cover",
      "Thunder Cut",
      "Maharaja Trial",
      "Deflector Maze",
      "Speedway Carrom",
      "Queen's Labyrinth",
      "Triple Split",
      "Diamond Fortress",
      "Rapid Fire",
      "Golden Orbit",
      "Precision Arc",
      "Chaos Crucible",
      "Royal Bastion",
      "Phantom Cut",
      "Grandmaster Gate",
      "Apex Carom",
      "Sovereign Shot",
      "Celestial Bank",
      "Imperial Finale",
      "Sultan's Puzzle",
      "Crown Jewel",
      "Royale Legend",
    )

  val all: List<TrickChallenge> = (1..50).map { level -> buildLevel(level) }

  fun findByLevel(level: Int): TrickChallenge =
    all.getOrElse(level - 1) { all.first() }

  private fun buildLevel(level: Int): TrickChallenge {
    val title = titles.getOrElse(level - 1) { "Challenge #$level" }
    val hasObstacles = level >= 12 && (level % 2 == 0 || level >= 26)
    val coins =
      when (level) {
        1 ->
          listOf(
            ChallengeCoinSpec(PieceType.WHITE, -2.9f, -2.9f),
            ChallengeCoinSpec(PieceType.WHITE, 2.9f, -2.9f),
          )
        2 ->
          listOf(
            ChallengeCoinSpec(PieceType.WHITE, -2.6f, -2.5f),
            ChallengeCoinSpec(PieceType.BLACK, 2.6f, -2.5f),
            ChallengeCoinSpec(PieceType.WHITE, 0f, -1.6f),
          )
        3 ->
          listOf(
            ChallengeCoinSpec(PieceType.QUEEN, 0f, -1.4f),
            ChallengeCoinSpec(PieceType.WHITE, -2.7f, -2.7f),
            ChallengeCoinSpec(PieceType.WHITE, 2.8f, -2.4f),
          )
        4 ->
          listOf(
            ChallengeCoinSpec(PieceType.BLACK, -3.1f, 2.7f),
            ChallengeCoinSpec(PieceType.WHITE, 3.1f, -2.9f),
            ChallengeCoinSpec(PieceType.QUEEN, 0.2f, 0.2f),
          )
        5 ->
          listOf(
            ChallengeCoinSpec(PieceType.WHITE, -0.45f, -0.2f),
            ChallengeCoinSpec(PieceType.BLACK, 0.45f, -0.2f),
            ChallengeCoinSpec(PieceType.WHITE, 0f, -0.8f),
            ChallengeCoinSpec(PieceType.QUEEN, 0f, 0.35f),
          )
        else -> generateDeterministicLayout(level, hasObstacles)
      }

    val coinCount = coins.size
    val shotLimit =
      when {
        level <= 4 -> (coinCount + 1).coerceIn(2, 4)
        level <= 15 -> (coinCount + 1).coerceIn(3, 5)
        level <= 35 -> (coinCount + 1).coerceIn(3, 6)
        else -> (coinCount + 1).coerceIn(4, 6)
      }
    val reward = 45 + level * 6
    val modeTag = if (hasObstacles) "Bumpers & Speed Pads active" else "Precision trick layout"
    return TrickChallenge(
      level = level,
      title = title,
      subtitle = "$coinCount coins • $shotLimit shots max • $modeTag",
      shotLimit = shotLimit,
      rewardCoins = reward,
      coins = coins,
      hasChaosObstacles = hasObstacles,
    )
  }

  private fun generateDeterministicLayout(level: Int, hasObstacles: Boolean): List<ChallengeCoinSpec> {
    val rng = Random(level * 7919 + 137)
    val result = ArrayList<ChallengeCoinSpec>()
    val count = (3 + (level / 9)).coerceAtMost(7)
    val includeQueen = level % 3 == 0 || level >= 40
    val forbidden =
      if (hasObstacles) {
        listOf(
          Triple(-1.95f, 0f, 0.72f),
          Triple(1.95f, 0f, 0.72f),
          Triple(-2.25f, -1.65f, 0.62f),
          Triple(2.25f, 1.65f, 0.62f),
        )
      } else {
        emptyList()
      }

    fun isClear(tx: Float, tz: Float): Boolean {
      if (tz > 2.4f) return false
      for (pk in CarromConfig.POCKETS) {
        val dx = tx - pk.x
        val dz = tz - pk.z
        if (dx * dx + dz * dz < 1.15f * 1.15f) return false
      }
      for ((ox, oz, rad) in forbidden) {
        val dx = tx - ox
        val dz = tz - oz
        if (dx * dx + dz * dz < rad * rad) return false
      }
      for (c in result) {
        val dx = tx - c.x
        val dz = tz - c.z
        val minD = CarromConfig.COIN_R * 2.18f
        if (dx * dx + dz * dz < minD * minD) return false
      }
      return true
    }

    val pattern = level % 4
    for (i in 0 until count) {
      val type =
        when {
          includeQueen && i == 0 -> PieceType.QUEEN
          i % 2 == 0 -> PieceType.WHITE
          else -> PieceType.BLACK
        }
      var placed = false
      for (attempt in 0 until 32) {
        val (tx, tz) =
          when (pattern) {
            0 -> {
              // Symmetric diamond / arc near top pockets
              val angle = (i.toFloat() / count) * PI.toFloat() * 2f + level * 0.2f
              val r = 0.75f + (attempt * 0.09f)
              (cos(angle) * r).coerceIn(-3.6f, 3.6f) to (sin(angle) * r - 0.55f).coerceIn(-3.5f, 2.0f)
            }
            1 -> {
              // Pocket-line runners
              val side = if (i % 2 == 0) -1f else 1f
              val d = 1.6f + rng.nextFloat() * 1.9f
              side * d to (-2.8f + rng.nextFloat() * 4.4f)
            }
            2 -> {
              // Tight center cluster
              val a = (i * 2.0f * PI.toFloat() / count) + 0.35f
              val r = if (i == 0) 0f else CarromConfig.COIN_R * 2.35f + attempt * 0.06f
              cos(a) * r to sin(a) * r - 0.35f
            }
            else -> {
              // Cross-board trick positions
              ((rng.nextFloat() * 2f - 1f) * 3.4f) to (-3.3f + rng.nextFloat() * 5.2f)
            }
          }
        if (isClear(tx, tz)) {
          result.add(ChallengeCoinSpec(type, tx, tz))
          placed = true
          break
        }
      }
      if (!placed) {
        val fallbackX = (-2.4f + i * 0.85f).coerceIn(-3.4f, 3.4f)
        val fallbackZ = -1.8f + (i % 2) * 0.8f
        if (isClear(fallbackX, fallbackZ)) {
          result.add(ChallengeCoinSpec(type, fallbackX, fallbackZ))
        }
      }
    }
    return result
  }
}
