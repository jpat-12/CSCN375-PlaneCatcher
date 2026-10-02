package com.planecatcher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.planecatcher.core.model.Tier

@Database(
    entities = [CaughtPlaneEntity::class, NearbyPlaneCacheEntity::class, QuizLockEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caughtPlaneDao(): CaughtPlaneDao
    abstract fun nearbyCacheDao(): NearbyCacheDao
    abstract fun quizLockDao(): QuizLockDao
}

class Converters {
    @TypeConverter fun tierToString(tier: Tier): String = tier.name
    @TypeConverter fun stringToTier(name: String): Tier = Tier.fromName(name) ?: Tier.COMMON
}
