package com.roberto.voicepocket.data.local

import androidx.room.withTransaction
import java.util.UUID

internal enum class IdeaDraftSaveResult { Saved, Conflict, Stale, NotReady }

internal class IdeaDraftRepository(private val database: VoicePocketDatabase) {
    private val drafts = database.ideaDraftDao()
    private val ideas = database.ideaDao()

    suspend fun read(): IdeaDraftEntity? = drafts.read()

    // Called only when explicitly starting to persist an edit, never by a delayed update.
    suspend fun create(
        ideaId: Long,
        originalText: String,
        draft: String,
        voicePreview: String? = null
    ): IdeaDraftEntity? {
        val entry = IdeaDraftEntity(UUID.randomUUID().toString(), ideaId, originalText, draft, voicePreview)
        return entry.takeIf { drafts.insert(it) != -1L }
    }

    suspend fun update(
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

    suspend fun discard(sessionId: String): Boolean = drafts.delete(sessionId) == 1

    suspend fun save(expected: IdeaDraftEntity): IdeaDraftSaveResult = database.withTransaction {
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
