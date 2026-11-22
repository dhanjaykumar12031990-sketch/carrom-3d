package com.example.data

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.example.R
import com.example.game.GameMode
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(
  exception: Throwable,
  operationType: OperationType,
  path: String?,
): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError =
    causes.firstOrNull { it is FirebaseFirestoreException }
      ?: causes.firstOrNull { it !is CancellationException }
      ?: exception

  val auth = Firebase.auth
  val currentUser = auth.currentUser

  val providerInfoList =
    currentUser?.providerData?.map { provider ->
      JSONObject().apply {
        put("providerId", provider.providerId)
        put("email", provider.email)
      }
    } ?: emptyList()

  val authInfoJson =
    JSONObject().apply {
      put("userId", currentUser?.uid)
      put("email", currentUser?.email)
      put("emailVerified", currentUser?.isEmailVerified)
      put("tenantId", currentUser?.tenantId)
      put("providerInfo", JSONArray(providerInfoList))
    }

  val errorInfoJson =
    JSONObject().apply {
      put("error", rootError.message ?: rootError.toString())
      put("operationType", operationType.value)
      put("path", path)
      put("authInfo", authInfoJson)
    }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "Royal Striker",
  val email: String = "",
  val avatarId: Long = 0L,
  val coins: Long = 200L,
  val highScore: Long = 0L,
  val careerPoints: Long = 0L,
  val matchesWon: Long = 0L,
  val matchesPlayed: Long = 0L,
  val selectedStrikerId: String = "classic",
  val unlockedStrikerIds: List<String> = listOf("classic"),
  val selectedBoardId: String = "birch_royal",
  val unlockedBoardIds: List<String> = listOf("birch_royal"),
  val highestChallengeUnlocked: Long = 1L,
  val completedChallenges: List<Long> = emptyList(),
  val lastSpinEpochDay: Long = -1L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CloudMatchRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val mode: String = GameMode.CLASSIC_1V1.name,
  val playerScore: Long = 0L,
  val won: Boolean = false,
  val challengeLevel: Long = 0L,
  val coinsEarned: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

class CloudProfileRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeUserProfile(userId: String): Flow<CloudUserProfile?> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch; refusing cross-user listener." }
    val docRef = db.collection("users").document(userId)
    val path = docRef.path
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              CloudUserProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeRecentMatches(userId: String): Flow<List<CloudMatchRecord>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch; refusing cross-user query." }
    val path = "users/$userId/matches"
    val query =
      db.collection("users")
        .document(userId)
        .collection("matches")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(20)
    emitAll(
      query
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            CloudMatchRecord::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun syncOrInitializeProfile(
    userId: String,
    localStats: OfflinePlayerStats,
    googleEmail: String,
    googleDisplayName: String,
  ): Result<CloudUserProfile> {
    val docPath = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      val docRef = db.collection("users").document(userId)
      val snap = docRef.get().await()
      val existing =
        if (snap.exists()) {
          snap.toObject(
            CloudUserProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }

      if (existing == null) {
        val cleanName =
          googleDisplayName.trim().ifBlank { localStats.playerName }.take(30).ifBlank { "Royal Striker" }
        val cleanEmail = googleEmail.trim().take(120)
        val createPayload = buildMutableFields(localStats, cleanName, cleanEmail).toMutableMap()
        createPayload["userId"] = userId
        createPayload["createdAt"] = FieldValue.serverTimestamp()
        docRef.set(createPayload).await()
        val createdProfile =
          CloudUserProfile(
            id = userId,
            userId = userId,
            displayName = cleanName,
            email = cleanEmail,
            avatarId = localStats.avatarId.coerceIn(0, 5).toLong(),
            coins = localStats.coins.coerceIn(0, 10_000_000).toLong(),
            highScore = localStats.highScore.coerceIn(0, 1_000_000).toLong(),
            careerPoints = localStats.careerPoints.coerceIn(0, 100_000_000).toLong(),
            matchesWon = localStats.matchesWon.coerceIn(0, 1_000_000).toLong(),
            matchesPlayed = localStats.matchesPlayed.coerceIn(0, 1_000_000).toLong(),
            selectedStrikerId = localStats.selectedStrikerId.ifBlank { "classic" }.take(40),
            unlockedStrikerIds = (localStats.unlockedStrikerIds + "classic").take(20),
            selectedBoardId = localStats.selectedBoardId.ifBlank { "birch_royal" }.take(40),
            unlockedBoardIds = (localStats.unlockedBoardIds + "birch_royal").take(20),
            highestChallengeUnlocked = localStats.highestChallengeUnlocked.coerceIn(1, 50).toLong(),
            completedChallenges = localStats.completedChallenges.sorted().take(50).map { it.toLong() },
            lastSpinEpochDay = localStats.lastSpinEpochDay.coerceIn(-1L, 1_000_000L),
          )
        Result.success(createdProfile)
      } else {
        // Merge cloud and local progress non-destructively (preserving max achievements & union of unlocks)
        val mergedStrikers =
          (existing.unlockedStrikerIds + localStats.unlockedStrikerIds + "classic").distinct().take(20)
        val mergedBoards =
          (existing.unlockedBoardIds + localStats.unlockedBoardIds + "birch_royal").distinct().take(20)
        val mergedChallenges =
          (existing.completedChallenges.map { it.toInt() } + localStats.completedChallenges)
            .distinct()
            .sorted()
            .take(50)
        val cleanName =
          existing.displayName.ifBlank { googleDisplayName }.ifBlank { localStats.playerName }.take(30)
        val cleanEmail = existing.email.ifBlank { googleEmail }.take(120)
        val mergedStats =
          localStats.copy(
            playerName = cleanName,
            avatarId = existing.avatarId.toInt().coerceIn(0, 5),
            coins = maxOf(existing.coins.toInt(), localStats.coins).coerceIn(0, 10_000_000),
            highScore = maxOf(existing.highScore.toInt(), localStats.highScore).coerceIn(0, 1_000_000),
            careerPoints = maxOf(existing.careerPoints.toInt(), localStats.careerPoints).coerceIn(0, 100_000_000),
            matchesWon = maxOf(existing.matchesWon.toInt(), localStats.matchesWon).coerceIn(0, 1_000_000),
            matchesPlayed = maxOf(existing.matchesPlayed.toInt(), localStats.matchesPlayed).coerceIn(0, 1_000_000),
            selectedStrikerId =
              if (existing.selectedStrikerId in mergedStrikers) existing.selectedStrikerId
              else localStats.selectedStrikerId,
            unlockedStrikerIds = mergedStrikers.toSet(),
            selectedBoardId =
              if (existing.selectedBoardId in mergedBoards) existing.selectedBoardId
              else localStats.selectedBoardId,
            unlockedBoardIds = mergedBoards.toSet(),
            highestChallengeUnlocked =
              maxOf(existing.highestChallengeUnlocked.toInt(), localStats.highestChallengeUnlocked)
                .coerceIn(1, 50),
            completedChallenges = mergedChallenges.toSet(),
            lastSpinEpochDay = maxOf(existing.lastSpinEpochDay, localStats.lastSpinEpochDay).coerceIn(-1L, 1_000_000L),
          )
        docRef.update(buildMutableFields(mergedStats, cleanName, cleanEmail)).await()
        Result.success(
          existing.copy(
            displayName = cleanName,
            email = cleanEmail,
            avatarId = mergedStats.avatarId.toLong(),
            coins = mergedStats.coins.toLong(),
            highScore = mergedStats.highScore.toLong(),
            careerPoints = mergedStats.careerPoints.toLong(),
            matchesWon = mergedStats.matchesWon.toLong(),
            matchesPlayed = mergedStats.matchesPlayed.toLong(),
            selectedStrikerId = mergedStats.selectedStrikerId,
            unlockedStrikerIds = mergedStrikers,
            selectedBoardId = mergedStats.selectedBoardId,
            unlockedBoardIds = mergedBoards,
            highestChallengeUnlocked = mergedStats.highestChallengeUnlocked.toLong(),
            completedChallenges = mergedChallenges.map { it.toLong() },
            lastSpinEpochDay = mergedStats.lastSpinEpochDay,
          )
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun pushProfileUpdate(
    userId: String,
    stats: OfflinePlayerStats,
    emailOverride: String? = null,
  ): Result<Unit> {
    val docPath = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      val cleanName = stats.playerName.trim().take(30).ifBlank { "Royal Striker" }
      val cleanEmail = (emailOverride ?: stats.accountEmail).trim().take(120)
      val docRef = db.collection("users").document(userId)
      docRef.update(buildMutableFields(stats, cleanName, cleanEmail)).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun recordMatch(
    userId: String,
    mode: GameMode,
    playerScore: Int,
    won: Boolean,
    challengeLevel: Int,
    coinsEarned: Int,
  ): Result<Unit> {
    var docPath = "users/$userId/matches"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      val docRef = db.collection("users").document(userId).collection("matches").document()
      docPath = docRef.path
      val payload =
        mapOf(
          "userId" to userId,
          "mode" to mode.name,
          "playerScore" to playerScore.coerceIn(0, 10_000),
          "won" to won,
          "challengeLevel" to challengeLevel.coerceIn(0, 50),
          "coinsEarned" to coinsEarned.coerceIn(0, 10_000),
          "createdAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  private fun buildMutableFields(
    stats: OfflinePlayerStats,
    displayName: String,
    email: String,
  ): Map<String, Any> {
    val strikers = (stats.unlockedStrikerIds + "classic").distinct().take(20)
    val boards = (stats.unlockedBoardIds + "birch_royal").distinct().take(20)
    val completed = stats.completedChallenges.distinct().sorted().take(50)
    return mapOf(
      "displayName" to displayName.trim().take(30).ifBlank { "Royal Striker" },
      "email" to email.trim().take(120),
      "avatarId" to stats.avatarId.coerceIn(0, 5),
      "coins" to stats.coins.coerceIn(0, 10_000_000),
      "highScore" to stats.highScore.coerceIn(0, 1_000_000),
      "careerPoints" to stats.careerPoints.coerceIn(0, 100_000_000),
      "matchesWon" to stats.matchesWon.coerceIn(0, 1_000_000),
      "matchesPlayed" to stats.matchesPlayed.coerceIn(0, 1_000_000),
      "selectedStrikerId" to stats.selectedStrikerId.ifBlank { "classic" }.take(40),
      "unlockedStrikerIds" to strikers,
      "selectedBoardId" to stats.selectedBoardId.ifBlank { "birch_royal" }.take(40),
      "unlockedBoardIds" to boards,
      "highestChallengeUnlocked" to stats.highestChallengeUnlocked.coerceIn(1, 50),
      "completedChallenges" to completed,
      "lastSpinEpochDay" to stats.lastSpinEpochDay.coerceIn(-1L, 1_000_000L),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
  }
}
