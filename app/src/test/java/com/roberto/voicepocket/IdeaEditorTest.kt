package com.roberto.voicepocket

import com.roberto.voicepocket.data.local.IdeaDraftEntity
import com.roberto.voicepocket.data.local.IdeaDraftSaveResult
import com.roberto.voicepocket.data.local.IdeaDraftStore
import com.roberto.voicepocket.data.local.IdeaEntity
import com.roberto.voicepocket.presentation.home.DraftPersistence
import com.roberto.voicepocket.presentation.home.DraftRecovery
import com.roberto.voicepocket.presentation.home.EditIdeaStatus
import com.roberto.voicepocket.presentation.home.IdeaEditor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdeaEditorTest {
    private val note = IdeaEntity(id = 7, text = "Original")

    @Test
    fun openingIsCleanAndRapidChangesWriteOnlyLatestText() = runBlocking {
        val store = MemoryStore()
        val editor = IdeaEditor(this, store) { note }
        awaitReady(editor)
        editor.begin(note)
        editor.cancel()
        assertNull(editor.state.value)
        assertNull(store.record)

        editor.begin(note)
        editor.changeDraft("U")
        editor.changeDraft("Un")
        editor.changeDraft("Un borrador")
        assertEquals(DraftPersistence.Pending, editor.state.value?.persistence)
        assertNull(store.record)
        awaitPersistence(editor, DraftPersistence.Persisted)
        assertEquals(listOf("Un borrador"), store.writes)
    }

    @Test
    fun discardWaitsForInflightWriteAndDoesNotRecreateOrAffectNextEdit() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val store = MemoryStore {
            started.complete(Unit)
            release.await()
        }
        val editor = IdeaEditor(this, store) { note }
        awaitReady(editor)
        editor.begin(note)
        editor.previewVoice(note.id, "Pendiente")
        withTimeout(3_000) { started.await() }
        editor.changeDraft("Cambio en cola")
        editor.cancel()
        assertEquals(true, editor.state.value?.confirmDiscard)
        editor.confirmDiscard()
        assertEquals(EditIdeaStatus.Discarding, editor.state.value?.status)
        release.complete(Unit)
        withTimeout(3_000) { editor.state.first { it == null } }
        assertNull(store.record)

        editor.begin(note)
        editor.changeDraft("Segunda edición")
        awaitPersistence(editor, DraftPersistence.Persisted)
        assertEquals("Segunda edición", store.record?.draft)
        assertNull(store.record?.voicePreview)
        assertEquals(listOf(note.text, "Segunda edición"), store.writes)
    }

    @Test
    fun failedAutosaveRetainsDraftAndRetryPersistsCurrentVersion() = runBlocking {
        var attempts = 0
        val store = MemoryStore {
            if (++attempts == 1) error("Write failed")
        }
        val editor = IdeaEditor(this, store) { note }
        awaitReady(editor)
        editor.begin(note)
        editor.changeDraft("Edición manual")
        editor.previewVoice(note.id, "Vista previa")
        awaitPersistence(editor, DraftPersistence.Failed)
        assertEquals("Edición manual", editor.state.value?.draft)
        assertEquals("Vista previa", editor.state.value?.voicePreview)
        assertNull(store.record)

        editor.flushDraft()
        awaitPersistence(editor, DraftPersistence.Persisted)
        assertEquals(2, attempts)
        assertEquals("Vista previa", store.record?.voicePreview)
        editor.discardVoice()
        awaitPersistence(editor, DraftPersistence.Persisted)
        assertEquals("Edición manual", store.record?.draft)
        assertNull(store.record?.voicePreview)
    }

    private suspend fun awaitReady(editor: IdeaEditor) {
        withTimeout(3_000) { editor.recovery.first { it == DraftRecovery.None } }
    }

    private suspend fun awaitPersistence(editor: IdeaEditor, status: DraftPersistence) {
        withTimeout(3_000) { editor.state.first { it?.persistence == status } }
    }

    private class MemoryStore(private val beforeCreate: suspend () -> Unit = {}) : IdeaDraftStore {
        var record: IdeaDraftEntity? = null
            private set
        val writes = mutableListOf<String>()

        override suspend fun read() = record

        override suspend fun create(
            ideaId: Long,
            originalText: String,
            draft: String,
            voicePreview: String?
        ): IdeaDraftEntity? {
            beforeCreate()
            if (record != null) return null
            return IdeaDraftEntity(
                "session-${writes.size}", ideaId, originalText, draft, voicePreview
            ).also {
                record = it
                writes += draft
            }
        }

        override suspend fun update(
            expected: IdeaDraftEntity,
            draft: String,
            voicePreview: String?
        ): IdeaDraftEntity? {
            if (record != expected) return null
            return expected.copy(draft = draft, voicePreview = voicePreview, revision = expected.revision + 1)
                .also {
                    record = it
                    writes += draft
                }
        }

        override suspend fun discard(sessionId: String): Boolean {
            if (record?.sessionId != sessionId) return false
            record = null
            return true
        }

        override suspend fun save(expected: IdeaDraftEntity) = error("Unexpected save")
    }
}
