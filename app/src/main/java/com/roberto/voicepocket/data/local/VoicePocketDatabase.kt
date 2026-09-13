package com.roberto.voicepocket.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        IdeaEntity::class,
        FolderEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VoicePocketDatabase : RoomDatabase() {

    abstract fun ideaDao(): IdeaDao
    abstract fun folderDao(): FolderDao

    companion object {

        @Volatile
        private var INSTANCE: VoicePocketDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `folders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            ALTER TABLE `ideas`
            ADD COLUMN `folderId` INTEGER DEFAULT NULL
            """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): VoicePocketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoicePocketDatabase::class.java,
                    "voicepocket_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}