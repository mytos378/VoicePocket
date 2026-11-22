package com.roberto.voicepocket.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface IdeaDraftDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(draft: IdeaDraftEntity): Long

    @Query("SELECT * FROM idea_drafts WHERE slot = 1")
    suspend fun read(): IdeaDraftEntity?

    @Query(
        """
        UPDATE idea_drafts
        SET draft = :text, voicePreview = :voicePreview, revision = revision + 1
        WHERE slot = 1 AND sessionId = :sessionId AND revision = :revision
        """
    )
    suspend fun update(sessionId: String, revision: Long, text: String, voicePreview: String?): Int

    @Query("DELETE FROM idea_drafts WHERE slot = 1 AND sessionId = :sessionId")
    suspend fun delete(sessionId: String): Int
}
