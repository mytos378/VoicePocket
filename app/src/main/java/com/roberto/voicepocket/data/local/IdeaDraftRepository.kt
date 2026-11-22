package com.roberto.voicepocket.data.local

import androidx.room.withTransaction
import java.util.UUID

internal enum class IdeaDraftSaveResult { Saved, Conflict, Stale, NotReady }

internal interface IdeaDraftStore {
    suspend fun read(): IdeaDraftEntity?
    suspend fun create(
        ideaId: Long,
        originalText: String,
        draft: String,
        voicePreview: String? = null
    ): IdeaDraftEntity?
    suspend fun update(expected: IdeaDraftEntity, draft: String, voicePreview: String?): IdeaDraftEntity?
    suspend fun discard(sessionId: String): Boolean
    suspend fun save(expected: IdeaDraftEntity): IdeaDraftSaveResult
}

internal class IdeaDraftRepository(private val database: VoicePocketDatabase) : IdeaDraftStore {
    private val drafts = database.ideaDraftDao()
    private val ideas = database.ideaDao()

    override suspend fun read(): IdeaDraftEntity? = drafts.read()

    // Called only when explicitly starting to persist an edit, never by a delayed update.
    override suspend fun create(
        ideaId: Long,
        originalText: String,
        draft: String,
        voicePreview: String?
    ): IdeaDraftEntity? {
        val entry = IdeaDraftEntity(UUID.randomUUID().toString(), ideaId, originalText, draft, voicePreview)
        return entry.takeIf { drafts.insert(it) != -1L }
    }

    override suspend fun update(
        expected: IdeaDraftEntity,
        draft: String,
        voicePreview: String?
    ): IdeaDraftEntity? {
        val updated = drafts.update(expected.sessionId, expected.revision, draft, voicePreview)
        return if (updated == 1) {
            expected.copy(draft = draft, voicePreview = voicePreview, revision = expected.revision + 1)
        } else {
            null
        }
    }

    override suspend fun discard(sessionId: String): Boolean = drafts.delete(sessionId) == 1

    override suspend fun save(expected: IdeaDraftEntity): IdeaDraftSaveResult = database.withTransaction {
        val entry = drafts.read()
        if (entry == null || entry.sessionId != expected.sessionId || entry.revision != expected.revision) {
            return@withTransaction IdeaDraftSaveResult.Stale
        }
        val text = entry.draft.trim()
        if (entry.voicePreview != null || text.isBlank()) return@withTransaction IdeaDraftSaveResult.NotReady

        val updated = ideas.updateIdeaText(entry.ideaId, entry.originalText, text)
        if (updated == 0) return@withTransaction IdeaDraftSaveResult.Conflict

        check(drafts.delete(entry.sessionId) == 1)
        IdeaDraftSaveResult.Saved
    }
}
