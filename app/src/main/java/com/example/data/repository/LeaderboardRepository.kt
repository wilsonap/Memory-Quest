package com.example.data.repository

import android.util.Log
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.model.UsernameStatus
import com.example.config.FirebaseBootstrap
import com.example.util.UsernameNormalizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LeaderboardPlayer(
    val uid: String = "",
    val name: String = "",
    val avatar: String = "avatar_01",
    val avatarType: String = "PRESET",
    val avatarValue: String = "avatar_01",
    val totalScore: Long = 0L,
    val highestLevel: Long = 1L,
    val totalPairs: Long = 0L,
    val bestStreak: Long = 0L,
    val gamesCompleted: Long = 0L,
    val rank: Int = 0,
    val isCurrentUser: Boolean = false
)

class LeaderboardRepository(
    authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {

    private val auth: FirebaseAuth by lazy {
        FirebaseBootstrap.requireReady()
        authProvider()
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseBootstrap.requireReady()
        firestoreProvider()
    }

    companion object {
        private const val LOG_TAG = "MemoryQuestUsername"
        private const val ENSURE_LOG_TAG = "MemoryQuestLeaderboardEnsure"
    }

    private val ensureMutex = Mutex()

    /**
     * Central function to verify and recreate leaderboard/{uid} if missing.
     * Idempotent, thread-safe, and enforces reservation UID match & CONFIRMED status.
     */
    suspend fun ensureLeaderboardExists(
        player: PlayerEntity,
        stats: StatisticsEntity?
    ): Result<Boolean> = ensureMutex.withLock {
        Log.d(ENSURE_LOG_TAG, "Início da verificação de leaderboard")

        val usernameStatus = player.usernameStatus
        Log.d(ENSURE_LOG_TAG, "UsernameStatus: $usernameStatus")

        if (usernameStatus != UsernameStatus.CONFIRMED.name) {
            Log.d(ENSURE_LOG_TAG, "Username não confirmado (status='$usernameStatus'). Não criando leaderboard.")
            return Result.success(false)
        }

        var currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(ENSURE_LOG_TAG, "FirebaseAuth.currentUser é null. Solicitando autenticação...")
            val newUid = ensureAuthenticated()
            currentUser = auth.currentUser
            if (currentUser == null || newUid.isNullOrEmpty()) {
                Log.e(ENSURE_LOG_TAG, "Falha na autenticação. currentUser continua null.")
                return Result.failure(IllegalStateException("Sem autenticação disponível"))
            }
        }

        val uid = currentUser.uid
        Log.d(ENSURE_LOG_TAG, "UID atual: $uid")

        val confirmedDisplayName = player.confirmedDisplayName.ifEmpty { player.name }.trim().ifEmpty { "Explorador" }
        val confirmedNormalizedName = player.confirmedNormalizedName.ifEmpty { UsernameNormalizer.normalizeUsername(confirmedDisplayName) }
        Log.d(ENSURE_LOG_TAG, "NormalizedName: $confirmedNormalizedName")

        if (confirmedNormalizedName.isEmpty()) {
            Log.w(ENSURE_LOG_TAG, "NormalizedName está vazio. Não recriando leaderboard.")
            return Result.success(false)
        }

        try {
            val leaderboardRef = firestore.collection("leaderboard").document(uid)
            val leaderboardSnap = leaderboardRef.get().await()

            if (leaderboardSnap.exists()) {
                Log.d(ENSURE_LOG_TAG, "Documento leaderboard/$uid encontrado no Firestore")
                return Result.success(true)
            }

            Log.d(ENSURE_LOG_TAG, "Documento leaderboard/$uid ausente")

            val usernameRef = firestore.collection("usernames").document(confirmedNormalizedName)
            val usernameSnap = usernameRef.get().await()

            if (!usernameSnap.exists()) {
                Log.w(ENSURE_LOG_TAG, "Documento usernames/$confirmedNormalizedName não encontrado. Reserva ausente. Não recriando leaderboard.")
                return Result.success(false)
            }

            val reservedUid = usernameSnap.getString("uid")
            if (reservedUid != uid) {
                Log.e(ENSURE_LOG_TAG, "Conflito de identidade: usernames/$confirmedNormalizedName pertence ao UID '$reservedUid', mas o UID atual é '$uid'. Não recriando leaderboard.")
                return Result.success(false)
            }

            Log.d(ENSURE_LOG_TAG, "Iniciando recriação do documento leaderboard/$uid")

            val totalPairs = stats?.totalPairsFound?.toLong() ?: 0L
            val bestStreak = stats?.highestStreak?.toLong() ?: 0L
            val gamesCompleted = stats?.totalGames?.toLong() ?: 0L
            val highestLevel = maxOf(1L, player.highestLevel.toLong())
            val totalScore = (highestLevel * 1000L) + (totalPairs * 10L) + (bestStreak * 50L) + (gamesCompleted * 20L)
            val avatarPresetVal = player.avatarPresetId.ifEmpty { "avatar_01" }

            val safeName = confirmedDisplayName.trim().take(20).let {
                if (it.length < 3) "Explorador" else it
            }

            val leaderboardData = hashMapOf(
                "uid" to uid,
                "name" to safeName,
                "normalizedName" to confirmedNormalizedName,
                "avatar" to avatarPresetVal,
                "totalScore" to totalScore,
                "highestLevel" to highestLevel,
                "totalPairs" to totalPairs,
                "bestStreak" to bestStreak,
                "gamesCompleted" to gamesCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            Log.d(ENSURE_LOG_TAG, "Campos enviados para leaderboard/$uid: $leaderboardData")

            leaderboardRef.set(leaderboardData, SetOptions.merge()).await()
            Log.d(ENSURE_LOG_TAG, "Recriação do documento leaderboard/$uid concluída com sucesso")

            Result.success(true)
        } catch (e: FirebaseFirestoreException) {
            Log.e(ENSURE_LOG_TAG, "Erro no Firestore ao verificar/recriar leaderboard: código=[${e.code}] mensagem=${e.message}", e)
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.e(ENSURE_LOG_TAG, "PERMISSION_DENIED ao tentar acessar ou recriar leaderboard/$uid")
            }
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(ENSURE_LOG_TAG, "Erro no Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun restoreUserDataFromFirestoreIfAvailable(
        memoryQuestDao: com.example.data.local.dao.MemoryQuestDao
    ): Boolean {
        return try {
            val uid = ensureAuthenticated() ?: return false
            val leaderboardRef = firestore.collection("leaderboard").document(uid)
            val snap = leaderboardRef.get().await()

            if (!snap.exists()) {
                Log.d(LOG_TAG, "Nenhum documento de leaderboard encontrado no Firestore para UID $uid para restaurar dados")
                return false
            }

            val remoteName = snap.getString("name") ?: ""
            val remoteNormalized = snap.getString("normalizedName") ?: UsernameNormalizer.normalizeUsername(remoteName)
            val remoteAvatar = snap.getString("avatarValue") ?: snap.getString("avatar") ?: "avatar_01"
            val remoteAvatarType = snap.getString("avatarType") ?: "PRESET"
            val remoteHighestLevel = (snap.getLong("highestLevel") ?: 1L).toInt()
            val remoteTotalPairs = (snap.getLong("totalPairs") ?: 0L).toInt()
            val remoteBestStreak = (snap.getLong("bestStreak") ?: 0L).toInt()
            val remoteGamesCompleted = (snap.getLong("gamesCompleted") ?: 0L).toInt()

            val currentPlayer = memoryQuestDao.getPlayer() ?: PlayerEntity()
            val currentStats = memoryQuestDao.getStatistics() ?: StatisticsEntity()

            val needsRestore = currentPlayer.name.isEmpty() ||
                    currentPlayer.confirmedDisplayName.isEmpty() ||
                    currentPlayer.usernameStatus != UsernameStatus.CONFIRMED.name ||
                    remoteHighestLevel > currentPlayer.highestLevel ||
                    remoteTotalPairs > currentStats.totalPairsFound

            if (needsRestore && remoteName.isNotEmpty()) {
                Log.d(LOG_TAG, "Restaurando progresso do usuário do Firestore: nome='$remoteName', avatar='$remoteAvatar', nível=$remoteHighestLevel")

                val restoredPlayer = currentPlayer.copy(
                    name = remoteName,
                    confirmedDisplayName = remoteName,
                    confirmedNormalizedName = remoteNormalized,
                    usernameStatus = UsernameStatus.CONFIRMED.name,
                    highestLevel = maxOf(currentPlayer.highestLevel, remoteHighestLevel),
                    currentLevel = maxOf(currentPlayer.currentLevel, remoteHighestLevel),
                    avatarPresetId = remoteAvatar,
                    avatarType = remoteAvatarType,
                    avatarLocalPath = if (remoteAvatarType == "PRESET") "" else currentPlayer.avatarLocalPath
                )
                memoryQuestDao.insertOrUpdatePlayer(restoredPlayer)

                val restoredStats = currentStats.copy(
                    totalPairsFound = maxOf(currentStats.totalPairsFound, remoteTotalPairs),
                    highestStreak = maxOf(currentStats.highestStreak, remoteBestStreak),
                    totalGames = maxOf(currentStats.totalGames, remoteGamesCompleted)
                )
                memoryQuestDao.insertOrUpdateStatistics(restoredStats)

                Log.d(LOG_TAG, "Restauração do Firestore concluída com sucesso!")
                return true
            }
            false
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao tentar restaurar dados do Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Ensures an anonymous Firebase Auth user exists.
     * Reuses existing user UID if already authenticated.
     */
    suspend fun ensureAuthenticated(): String? {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d(LOG_TAG, "UID autenticado já existente: ${currentUser.uid}")
                currentUser.uid
            } else {
                Log.d(LOG_TAG, "Iniciando signInAnonymously no Firebase Auth...")
                val result = auth.signInAnonymously().await()
                val newUid = result.user?.uid
                Log.d(LOG_TAG, "Autenticado anonimamente com sucesso. UID: $newUid")
                newUid
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Falha na autenticação Firebase Auth: ${e.message}", e)
            auth.currentUser?.uid
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Syncs user's leaderboard public data to Firestore under leaderboard/{uid}.
     * Only runs if usernameStatus is CONFIRMED.
     */
    suspend fun syncLeaderboard(player: PlayerEntity, stats: StatisticsEntity?): Result<Unit> {
        if (player.usernameStatus != UsernameStatus.CONFIRMED.name) {
            Log.d(LOG_TAG, "Ordem preservada: Não criando leaderboard/{uid} pois usernameStatus é '${player.usernameStatus}' (não é CONFIRMED)")
            return Result.failure(IllegalStateException("Username not confirmed"))
        }

        return try {
            val uid = ensureAuthenticated() ?: return Result.failure(Exception("Sem autenticação disponível"))
            
            val totalPairs = stats?.totalPairsFound?.toLong() ?: 0L
            val bestStreak = stats?.highestStreak?.toLong() ?: 0L
            val gamesCompleted = stats?.totalGames?.toLong() ?: 0L
            val highestLevel = player.highestLevel.toLong()
            
            // Total score formula: Level weight + pairs + streak + games
            val totalScore = (highestLevel * 1000L) + (totalPairs * 10L) + (bestStreak * 50L) + (gamesCompleted * 20L)
            val displayName = player.confirmedDisplayName.ifEmpty { player.name }.trim().ifEmpty { "Explorador" }
            val normalizedName = UsernameNormalizer.normalizeUsername(displayName)

            val avatarPresetVal = player.avatarPresetId.ifEmpty { "avatar_01" }
            val onlineAvatarType = if (player.avatarType == "CUSTOM") "PRESET" else player.avatarType

            val leaderboardData = hashMapOf(
                "uid" to uid,
                "name" to displayName,
                "normalizedName" to normalizedName,
                "avatar" to avatarPresetVal,
                "avatarType" to onlineAvatarType,
                "avatarValue" to avatarPresetVal,
                "totalScore" to totalScore,
                "highestLevel" to highestLevel,
                "totalPairs" to totalPairs,
                "bestStreak" to bestStreak,
                "gamesCompleted" to gamesCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            Log.d(LOG_TAG, "Criando/Atualizando leaderboard/$uid com normalizedName '$normalizedName'")

            firestore.collection("leaderboard")
                .document(uid)
                .set(leaderboardData, SetOptions.merge())
                .await()

            Log.d(LOG_TAG, "Leaderboard/$uid atualizado com sucesso no Firestore!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao sincronizar leaderboard: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches top 100 players ordered by totalScore descending from Firestore.
     * Applies deduplication by UID and strict tie-breaker sorting:
     * 1. totalScore (descending)
     * 2. highestLevel (descending)
     * 3. totalPairs (descending)
     * 4. uid (ascending)
     */
    suspend fun fetchTop100Leaderboard(
        source: Source = Source.DEFAULT,
        limit: Long = 50L
    ): Result<List<LeaderboardPlayer>> {
        return try {
            val currentUid = getCurrentUserId() ?: ensureAuthenticated()
            val querySnapshot = firestore.collection("leaderboard")
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(limit)
                .get(source)
                .await()

            val rawList = querySnapshot.documents.map { doc ->
                val uid = doc.getString("uid") ?: doc.id
                val name = doc.getString("name") ?: "Explorador"
                val avatarType = doc.getString("avatarType") ?: "PRESET"
                val avatarValue = doc.getString("avatarValue") ?: doc.getString("avatar") ?: "avatar_01"
                val totalScore = doc.getLong("totalScore") ?: 0L
                val highestLevel = doc.getLong("highestLevel") ?: 1L
                val totalPairs = doc.getLong("totalPairs") ?: 0L
                val bestStreak = doc.getLong("bestStreak") ?: 0L
                val gamesCompleted = doc.getLong("gamesCompleted") ?: 0L

                LeaderboardPlayer(
                    uid = uid,
                    name = name,
                    avatar = avatarValue,
                    avatarType = avatarType,
                    avatarValue = avatarValue,
                    totalScore = totalScore,
                    highestLevel = highestLevel,
                    totalPairs = totalPairs,
                    bestStreak = bestStreak,
                    gamesCompleted = gamesCompleted,
                    rank = 0,
                    isCurrentUser = (currentUid != null && uid == currentUid)
                )
            }

            // Remove duplicates by UID
            val distinctList = rawList.distinctBy { it.uid }

            // Sort with tie-breakers: totalScore DESC, highestLevel DESC, totalPairs DESC, uid ASC
            val sortedList = distinctList.sortedWith(
                compareByDescending<LeaderboardPlayer> { it.totalScore }
                    .thenByDescending { it.highestLevel }
                    .thenByDescending { it.totalPairs }
                    .thenBy { it.uid }
            )

            // Assign sequential rank numbers
            val rankedList = sortedList.mapIndexed { index, player ->
                player.copy(rank = index + 1)
            }

            Result.success(rankedList)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
