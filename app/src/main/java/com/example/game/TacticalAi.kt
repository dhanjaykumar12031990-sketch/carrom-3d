package com.example.game

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class AiShotPlan(
  val sx: Float,
  val dirX: Float,
  val dirZ: Float,
  val speed: Float,
  val targetPieceId: Int,
  val isBankShot: Boolean,
)

object TacticalAi {
  fun computeShot(
    pieces: List<Piece>,
    striker: Piece?,
    arenaElements: List<ArenaElement>,
    queenState: QueenState,
    queenHolder: Int,
    aiPlayer: Int,
    difficulty: AiDifficulty,
    lastTargetPieceId: Int,
  ): AiShotPlan? {
    val aiZ = -CarromConfig.BASELINE_Z
    val activeCoins = pieces.filter { it.active && !it.pocketing && it !== striker }
    if (activeCoins.isEmpty()) return null

    val blockers =
      arenaElements.filter {
        it.type == ArenaElementType.BUMPER || it.type == ArenaElementType.DEFLECTOR
      }

    fun isStrikerSpotFree(tx: Float): Boolean {
      for (p in activeCoins) {
        val dx = p.x - tx
        val dz = p.z - aiZ
        val minD = p.radius + CarromConfig.STRIKER_R + 0.02f
        if (dx * dx + dz * dz < minD * minD) return false
      }
      for (obs in blockers) {
        val dx = obs.x - tx
        val dz = obs.z - aiZ
        val minD = obs.radius + CarromConfig.STRIKER_R + 0.02f
        if (dx * dx + dz * dz < minD * minD) return false
      }
      return true
    }

    fun isSegmentClear(
      x1: Float,
      z1: Float,
      x2: Float,
      z2: Float,
      clearanceR: Float,
      ignoreA: Piece?,
    ): Boolean {
      val dx = x2 - x1
      val dz = z2 - z1
      val lenSq = dx * dx + dz * dz
      if (lenSq < 1e-4f) return true
      val len = sqrt(lenSq)
      val ux = dx / len
      val uz = dz / len
      for (p in activeCoins) {
        if (p === ignoreA) continue
        val px = p.x - x1
        val pz = p.z - z1
        val proj = px * ux + pz * uz
        if (proj <= 0.02f || proj >= len - 0.02f) continue
        val perpSq = px * px + pz * pz - proj * proj
        val minD = p.radius + clearanceR
        if (perpSq < minD * minD) return false
      }
      for (obs in blockers) {
        val px = obs.x - x1
        val pz = obs.z - z1
        val proj = px * ux + pz * uz
        if (proj <= 0.02f || proj >= len - 0.02f) continue
        val perpSq = px * px + pz * pz - proj * proj
        val minD = obs.radius + clearanceR
        if (perpSq < minD * minD) return false
      }
      return true
    }

    data class ScoredCandidate(val score: Float, val plan: AiShotPlan)

    val topCandidates = ArrayList<ScoredCandidate>()
    val baselineSamples = ArrayList<Float>()
    var x = -CarromConfig.BASELINE_LIMIT
    while (x <= CarromConfig.BASELINE_LIMIT + 0.01f) {
      baselineSamples.add(x.coerceIn(-CarromConfig.BASELINE_LIMIT, CarromConfig.BASELINE_LIMIT))
      x += 0.24f
    }
    if (!baselineSamples.contains(0f)) baselineSamples.add(0f)

    val railLimit = CarromConfig.BOARD_HALF - CarromConfig.STRIKER_R

    for (sx in baselineSamples) {
      if (!isStrikerSpotFree(sx)) continue

      for (coin in activeCoins) {
        var coinBaseValue =
          when (coin.type) {
            PieceType.QUEEN -> 56f
            PieceType.WHITE -> 25f
            else -> 16f
          }
        if (queenState == QueenState.PENDING_COVER && queenHolder == aiPlayer && coin.type != PieceType.QUEEN) {
          coinBaseValue += 52f
        }
        // Subtle anti-repetition penalty if targeting the exact same coin as previous turn
        if (coin.id == lastTargetPieceId && activeCoins.size > 2) {
          coinBaseValue -= 5f
        }

        for (pocket in CarromConfig.POCKETS) {
          val toPx = pocket.x - coin.x
          val toPz = pocket.z - coin.z
          val distCP = hypot(toPx, toPz)
          if (distCP < 0.01f) continue
          val pDirX = toPx / distCP
          val pDirZ = toPz / distCP
          val gx = coin.x - pDirX * (CarromConfig.COIN_R + CarromConfig.STRIKER_R)
          val gz = coin.z - pDirZ * (CarromConfig.COIN_R + CarromConfig.STRIKER_R)
          if (abs(gx) > railLimit || abs(gz) > railLimit) continue

          if (!isSegmentClear(coin.x, coin.z, pocket.x, pocket.z, CarromConfig.COIN_R * 0.90f, coin)) {
            continue
          }

          // 1) Evaluate Direct Shot
          val sToGx = gx - sx
          val sToGz = gz - aiZ
          val distSG = hypot(sToGx, sToGz)
          if (distSG > 0.16f) {
            val sDirX = sToGx / distSG
            val sDirZ = sToGz / distSG
            val cutDot = sDirX * pDirX + sDirZ * pDirZ
            if (cutDot >= 0.36f && isSegmentClear(sx, aiZ, gx, gz, CarromConfig.STRIKER_R * 0.90f, coin)) {
              var scratchRisk = 0f
              for (pk in CarromConfig.POCKETS) {
                if (hypot(gx - pk.x, gz - pk.z) < 1.05f && cutDot > 0.85f) scratchRisk += 22f
              }
              val score = coinBaseValue + cutDot * 24f - distCP * 1.7f - distSG * 1.05f - scratchRisk
              val speed =
                min(
                  CarromConfig.MAX_SHOT_SPEED * 0.88f,
                  max(9.2f, 8.4f + (distSG + distCP) * 0.92f + (1f - cutDot) * 4.2f),
                )
              topCandidates.add(
                ScoredCandidate(score, AiShotPlan(sx, sDirX, sDirZ, speed, coin.id, isBankShot = false))
              )
            }
          }

          // 2) Evaluate 1-Cushion Bank Shots off all 4 wooden rails
          val mirrors =
            arrayOf(
              // Left rail x = -railLimit, Right rail x = +railLimit, Top rail z = -railLimit, Bottom rail z = +railLimit
              Triple(-2f * railLimit - gx, gz, 0),
              Triple(2f * railLimit - gx, gz, 1),
              Triple(gx, -2f * railLimit - gz, 2),
              Triple(gx, 2f * railLimit - gz, 3),
            )
          for ((mx, mz, wallId) in mirrors) {
            val toMx = mx - sx
            val toMz = mz - aiZ
            val distM = hypot(toMx, toMz)
            if (distM < 0.25f) continue
            val bDirX = toMx / distM
            val bDirZ = toMz / distM

            // Intersect ray (sx, aiZ) + t * (bDirX, bDirZ) with the chosen rail
            val tHit =
              when (wallId) {
                0 -> if (abs(bDirX) > 1e-4f) (-railLimit - sx) / bDirX else -1f
                1 -> if (abs(bDirX) > 1e-4f) (railLimit - sx) / bDirX else -1f
                2 -> if (abs(bDirZ) > 1e-4f) (-railLimit - aiZ) / bDirZ else -1f
                else -> if (abs(bDirZ) > 1e-4f) (railLimit - aiZ) / bDirZ else -1f
              }
            if (tHit <= 0.15f || tHit >= distM - 0.15f) continue
            val bx = sx + bDirX * tHit
            val bz = aiZ + bDirZ * tHit
            // Avoid bouncing right into a corner pocket opening
            var nearPocketCorner = false
            for (pk in CarromConfig.POCKETS) {
              if (hypot(bx - pk.x, bz - pk.z) < CarromConfig.POCKET_R + 0.32f) {
                nearPocketCorner = true
                break
              }
            }
            if (nearPocketCorner) continue

            val leg2X = gx - bx
            val leg2Z = gz - bz
            val distLeg2 = hypot(leg2X, leg2Z)
            if (distLeg2 < 0.2f) continue
            val l2DirX = leg2X / distLeg2
            val l2DirZ = leg2Z / distLeg2
            val bankCutDot = l2DirX * pDirX + l2DirZ * pDirZ
            if (bankCutDot < 0.42f) continue

            if (!isSegmentClear(sx, aiZ, bx, bz, CarromConfig.STRIKER_R * 0.88f, null)) continue
            if (!isSegmentClear(bx, bz, gx, gz, CarromConfig.STRIKER_R * 0.88f, coin)) continue

            val bankScore = (coinBaseValue - 9f) + bankCutDot * 20f - (tHit + distLeg2) * 1.15f - distCP * 1.5f
            val bankSpeed =
              min(
                CarromConfig.MAX_SHOT_SPEED * 0.92f,
                max(11.5f, 10.5f + (tHit + distLeg2 + distCP) * 1.05f),
              )
            topCandidates.add(
              ScoredCandidate(bankScore, AiShotPlan(sx, bDirX, bDirZ, bankSpeed, coin.id, isBankShot = true))
            )
          }
        }
      }
    }

    var chosen: AiShotPlan? = null
    if (topCandidates.isNotEmpty()) {
      topCandidates.sortByDescending { it.score }
      val bestScore = topCandidates.first().score
      // Pick among top near-equivalent shots on Easy/Medium for natural human variety; tightest on Pro
      val tolerance =
        when (difficulty) {
          AiDifficulty.EASY -> 8.5f
          AiDifficulty.MEDIUM -> 4.5f
          AiDifficulty.PRO -> 1.5f
        }
      val pool = topCandidates.filter { it.score >= bestScore - tolerance }.take(4)
      chosen = pool[Random.nextInt(pool.size)].plan
    }

    // 3) Cluster-Break fallback when no clean direct or bank shot exists
    if (chosen == null) {
      var bestClusterCoin = activeCoins.first()
      var bestNeighbors = -1
      for (c in activeCoins) {
        val neighbors = activeCoins.count { other -> other !== c && hypot(other.x - c.x, other.z - c.z) < 1.15f }
        if (neighbors > bestNeighbors) {
          bestNeighbors = neighbors
          bestClusterCoin = c
        }
      }
      val preferredX = ((bestClusterCoin.x * 0.45f) + (Random.nextFloat() - 0.5f) * 1.1f)
        .coerceIn(-CarromConfig.BASELINE_LIMIT, CarromConfig.BASELINE_LIMIT)
      val validXs = baselineSamples.filter { isStrikerSpotFree(it) }
      val sx = validXs.minByOrNull { abs(it - preferredX) } ?: 0f
      val offsetTargetX = bestClusterCoin.x + (Random.nextFloat() - 0.5f) * 0.24f
      val offsetTargetZ = bestClusterCoin.z + (Random.nextFloat() - 0.5f) * 0.16f
      val dx = offsetTargetX - sx
      val dz = offsetTargetZ - aiZ
      val d = hypot(dx, dz).coerceAtLeast(0.5f)
      val breakSpeed = 18.5f + Random.nextFloat() * 3.8f
      chosen = AiShotPlan(sx, dx / d, dz / d, breakSpeed, bestClusterCoin.id, isBankShot = false)
    }

    // 4) Apply Human Accuracy Variance (90% Easy, 93% Medium, 98% Pro)
    val maxJitterRad =
      when (difficulty) {
        AiDifficulty.EASY -> 0.056f
        AiDifficulty.MEDIUM -> 0.032f
        AiDifficulty.PRO -> 0.010f
      }
    val speedVariance =
      when (difficulty) {
        AiDifficulty.EASY -> 0.075f
        AiDifficulty.MEDIUM -> 0.042f
        AiDifficulty.PRO -> 0.015f
      }
    val jitter = (Random.nextFloat() - 0.5f) * 2f * maxJitterRad
    val c = cos(jitter)
    val s = sin(jitter)
    val variedSpeed =
      (chosen.speed * (1f + (Random.nextFloat() - 0.5f) * 2f * speedVariance))
        .coerceIn(CarromConfig.MIN_SHOT_SPEED, CarromConfig.MAX_SHOT_SPEED)

    return AiShotPlan(
      sx = chosen.sx,
      dirX = chosen.dirX * c - chosen.dirZ * s,
      dirZ = chosen.dirX * s + chosen.dirZ * c,
      speed = variedSpeed,
      targetPieceId = chosen.targetPieceId,
      isBankShot = chosen.isBankShot,
    )
  }
}
