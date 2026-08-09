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
import com.example.data.local.entity.DailyQuestEntity
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
        PendingSyncEntity::class,
        DailyQuestEntity::class
    ],
    version = 8,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `player` ADD COLUMN `avatarType` TEXT NOT NULL DEFAULT 'PRESET'")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `avatarPresetId` TEXT NOT NULL DEFAULT 'avatar_01'")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `avatarLocalPath` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `avatarUpdatedAt` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Previne perdas na transição 4 -> 5
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Previne perdas na transição 5 -> 6
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `player` ADD COLUMN `lastDailyRewardDate` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `dailyRewardStreak` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `rewardedAdsToday` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `rewardedAdsDate` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_quests` (
                        `id` TEXT NOT NULL,
                        `questType` TEXT NOT NULL,
                        `targetProgress` INTEGER NOT NULL,
                        `currentProgress` INTEGER NOT NULL DEFAULT 0,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `dateString` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `player` ADD COLUMN `dailyChestClaimed` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `dailyChestDoubled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `dailyChestDate` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `dailyChestRewardType` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `dailyChestRewardAmount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `player` ADD COLUMN `dailyChestRewardBoosterId` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Previne perdas na transição 4 -> 6
            }
        }

        val MIGRATION_3_6 = object : Migration(3, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_4.migrate(db)
            }
        }

        val MIGRATION_2_6 = object : Migration(2, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
            }
        }

        val MIGRATION_1_6 = object : Migration(1, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "memory_quest_db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_4_6,
                        MIGRATION_3_6,
                        MIGRATION_2_6,
                        MIGRATION_1_6
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
