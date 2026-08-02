package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.MemoryQuestDao
import com.example.data.local.dao.PendingSyncDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.InventoryEntity
import com.example.data.local.entity.PendingSyncEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.local.entity.UnlockedThemeEntity

@Database(
    entities = [
        PlayerEntity::class,
        StatisticsEntity::class,
        InventoryEntity::class,
        UnlockedThemeEntity::class,
        AchievementEntity::class,
        PendingSyncEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun memoryQuestDao(): MemoryQuestDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_sync` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `uid` TEXT NOT NULL,
                        `totalScore` INTEGER NOT NULL,
                        `highestLevel` INTEGER NOT NULL,
                        `bestStreak` INTEGER NOT NULL,
                        `totalPairs` INTEGER NOT NULL,
                        `gamesCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `player` ADD COLUMN `usernameStatus` TEXT NOT NULL DEFAULT 'NOT_SELECTED'")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `pendingDisplayName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `pendingNormalizedName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `confirmedDisplayName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `confirmedNormalizedName` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "memory_quest_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
