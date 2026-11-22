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
        FolderEntity::class,
        IdeaDraftEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VoicePocketDatabase : RoomDatabase() {

    abstract fun ideaDao(): IdeaDao
    abstract fun folderDao(): FolderDao
    internal abstract fun ideaDraftDao(): IdeaDraftDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE `idea_drafts` (
                        `slot` INTEGER NOT NULL PRIMARY KEY,
                        `sessionId` TEXT NOT NULL,
                        `revision` INTEGER NOT NULL,
                        `ideaId` INTEGER NOT NULL,
                        `originalText` TEXT NOT NULL,
                        `draft` TEXT NOT NULL,
                        `voicePreview` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        internal fun open(context: Context, name: String): VoicePocketDatabase =
            Room.databaseBuilder(context.applicationContext, VoicePocketDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        fun getInstance(context: Context): VoicePocketDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: open(context, "voicepocket_database").also { INSTANCE = it }
            }
        }
    }
}
