package com.roberto.voicepocket.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM folders
            WHERE name = :name COLLATE NOCASE
        )
        """
    )
    suspend fun folderNameExists(name: String): Boolean

    @Query(
        """
        UPDATE ideas
        SET folderId = NULL
        WHERE folderId = :folderId
        """
    )
    suspend fun removeFolderFromIdeas(folderId: Long)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Long)

    @Transaction
    suspend fun deleteFolderKeepingIdeas(folderId: Long) {
        removeFolderFromIdeas(folderId)
        deleteFolderById(folderId)
    }
}