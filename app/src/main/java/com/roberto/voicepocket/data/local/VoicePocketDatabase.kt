package com.roberto.voicepocket.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [IdeaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VoicePocketDatabase : RoomDatabase() {

    abstract fun ideaDao(): IdeaDao

    companion object {

        @Volatile
        private var INSTANCE: VoicePocketDatabase? = null

        fun getInstance(context: Context): VoicePocketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoicePocketDatabase::class.java,
                    "voicepocket_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}