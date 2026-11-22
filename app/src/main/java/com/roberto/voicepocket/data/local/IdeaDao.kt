package com.roberto.voicepocket.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: IdeaEntity): Long

    @Query("UPDATE ideas SET text = :text WHERE id = :ideaId AND text = :originalText")
    suspend fun updateIdeaText(ideaId: Long, originalText: String, text: String): Int

    @Query("SELECT * FROM ideas WHERE id = :ideaId")
    suspend fun findIdea(ideaId: Long): IdeaEntity?

    @Query("SELECT * FROM ideas ORDER BY createdAt DESC")
    fun getAllIdeas(): Flow<List<IdeaEntity>>

    @Query("DELETE FROM ideas WHERE id = :ideaId")
    suspend fun deleteIdeaById(ideaId: Long)

    @Query("UPDATE ideas SET folderId = :folderId WHERE id = :ideaId" )
    suspend fun moveIdeaToFolder(
        ideaId: Long,
        folderId: Long?
    )
}
