package com.planecatcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CaughtPlaneDao {
    @Query("SELECT * FROM caught_planes ORDER BY caughtAt DESC")
    fun observeAll(): Flow<List<CaughtPlaneEntity>>

    @Query("SELECT * FROM caught_planes WHERE hex = :hex")
    fun observe(hex: String): Flow<CaughtPlaneEntity?>

    @Query("SELECT hex FROM caught_planes")
    fun observeHexes(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM caught_planes WHERE hex = :hex)")
    suspend fun isCaught(hex: String): Boolean

    /** Returns -1 if the plane was already caught. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(plane: CaughtPlaneEntity): Long
}

@Dao
interface NearbyCacheDao {
    @Upsert
    suspend fun upsertAll(planes: List<NearbyPlaneCacheEntity>)

    @Query("SELECT * FROM nearby_plane_cache WHERE hex = :hex")
    suspend fun get(hex: String): NearbyPlaneCacheEntity?

    @Query("DELETE FROM nearby_plane_cache WHERE lastSeen < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface QuizLockDao {
    @Query("SELECT * FROM quiz_locks")
    fun observeAll(): Flow<List<QuizLockEntity>>

    @Query("SELECT * FROM quiz_locks WHERE planeHex = :hex")
    suspend fun get(hex: String): QuizLockEntity?

    @Upsert
    suspend fun upsert(lock: QuizLockEntity)

    @Query("DELETE FROM quiz_locks WHERE planeHex = :hex")
    suspend fun delete(hex: String)

    /** Keeps expired locks for a day so a retry can still avoid the failed question kind. */
    @Query("DELETE FROM quiz_locks WHERE retryAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
