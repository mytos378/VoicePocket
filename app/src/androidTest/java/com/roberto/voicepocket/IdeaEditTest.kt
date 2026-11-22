package com.roberto.voicepocket

import android.database.sqlite.SQLiteException
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.data.local.IdeaDraftEntity
import com.roberto.voicepocket.data.local.IdeaDraftRepository
import com.roberto.voicepocket.data.local.IdeaDraftSaveResult
import com.roberto.voicepocket.data.local.IdeaDraftStore
import com.roberto.voicepocket.presentation.home.DraftPersistence
import com.roberto.voicepocket.presentation.home.DraftRecovery
import org.junit.Assert.assertNull
import com.roberto.voicepocket.data.local.IdeaEntity
import com.roberto.voicepocket.data.local.VoicePocketDatabase
import com.roberto.voicepocket.presentation.home.EditIdeaStatus
import com.roberto.voicepocket.presentation.home.HomeViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdeaEditTest {
    private lateinit var database: VoicePocketDatabase
    private lateinit var viewModel: HomeViewModel

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            VoicePocketDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() {
        if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
        database.close()
    }

    @Test
    fun closesOnlyAfterRoomConfirmsAndPreservesMetadata() = runBlocking {
        val dao = database.ideaDao()
        val folders = database.folderDao()
        folders.insertFolder(FolderEntity(name = "Trabajo"))
        val folderId = folders.getAllFolders().first().single().id
        val original = insertOriginal(folderId)
        val allowUpdate = CompletableDeferred<Unit>()
        var attempts = 0
        val store = IdeaDraftRepository(database)
        val delayedStore = object : IdeaDraftStore by store {
            override suspend fun save(expected: IdeaDraftEntity): IdeaDraftSaveResult {
                attempts++
                allowUpdate.await()
                return store.save(expected)
            }
        }
        startEditor(delayedStore)

        withContext(Dispatchers.Main) {
            viewModel.editor.begin(original)
            viewModel.editor.changeDraft("Texto editado")
            viewModel.editor.save()
            viewModel.editor.save()
            viewModel.editor.cancel()
            assertEquals(EditIdeaStatus.Saving, viewModel.editor.state.value?.status)
        }
        assertEquals(original, dao.getAllIdeas().first().single())

        dao.moveIdeaToFolder(original.id, null)
        allowUpdate.complete(Unit)
        awaitStatus(null)
        assertEquals(1, attempts)
        assertEquals(
            original.copy(text = "Texto editado", folderId = null),
            dao.getAllIdeas().first().single()
        )
    }

    @Test
    fun retainsDraftAfterErrorAndRetriesOnce() = runBlocking {
        val dao = database.ideaDao()
        val original = insertOriginal()
        val allowRetry = CompletableDeferred<Unit>()
        var attempts = 0
        val store = IdeaDraftRepository(database)
        val failingStore = object : IdeaDraftStore by store {
            override suspend fun save(expected: IdeaDraftEntity): IdeaDraftSaveResult {
                attempts++
                if (attempts == 1) throw SQLiteException("Fallo simulado")
                allowRetry.await()
                return store.save(expected)
            }
        }
        startEditor(failingStore)

        saveDraft(original)
        awaitStatus(EditIdeaStatus.Failed)
        assertEquals("Mi borrador", viewModel.editor.state.value?.draft)
        assertEquals(original, dao.getAllIdeas().first().single())

        withContext(Dispatchers.Main) {
            viewModel.editor.save()
            viewModel.editor.save()
            assertEquals(EditIdeaStatus.Saving, viewModel.editor.state.value?.status)
        }
        allowRetry.complete(Unit)
        awaitStatus(null)
        assertEquals(2, attempts)
        assertEquals(original.copy(text = "Mi borrador"), dao.getAllIdeas().first().single())
    }

    @Test
    fun preservesDraftAndConcurrentTextOnConflict() = runBlocking {
        val dao = database.ideaDao()
        val original = insertOriginal()
        startEditor()
        assertEquals(1, dao.updateIdeaText(original.id, original.text, "Cambio concurrente"))

        saveDraft(original)
        awaitStatus(EditIdeaStatus.Conflict)
        withContext(Dispatchers.Main) { viewModel.editor.save() }
        assertEquals(EditIdeaStatus.Conflict, viewModel.editor.state.value?.status)
        assertEquals("Mi borrador", viewModel.editor.state.value?.draft)
        assertEquals(original.copy(text = "Cambio concurrente"), dao.getAllIdeas().first().single())
    }

    @Test
    fun preservesDraftIfNoteWasDeleted() = runBlocking {
        val dao = database.ideaDao()
        val original = insertOriginal()
        startEditor()
        dao.deleteIdeaById(original.id)

        saveDraft(original)
        awaitStatus(EditIdeaStatus.Conflict)
        assertEquals("Mi borrador", viewModel.editor.state.value?.draft)
        assertEquals(emptyList<IdeaEntity>(), dao.getAllIdeas().first())
    }

    @Test
    fun reviewsAndAppendsOnceToLatestDraftBeforeSaving() = runBlocking {
        val dao = database.ideaDao()
        val original = insertOriginal()
        startEditor()
        withContext(Dispatchers.Main) {
            viewModel.editor.begin(original)
            viewModel.editor.previewVoice(original.id + 1, "Otro destino")
            assertEquals(null, viewModel.editor.state.value?.voicePreview)
            viewModel.editor.previewVoice(original.id, "  Fragmento  ")
            viewModel.editor.previewVoice(original.id, "Duplicado")
            viewModel.editor.save()
            assertEquals("Fragmento", viewModel.editor.state.value?.voicePreview)
            assertEquals(original.text, viewModel.editor.state.value?.draft)
        }
        assertEquals(original, dao.getAllIdeas().first().single())

        withContext(Dispatchers.Main) {
            viewModel.editor.changeDraft("Edición manual\n")
            viewModel.editor.addVoice()
            viewModel.editor.addVoice()
            assertEquals("Edición manual\nFragmento", viewModel.editor.state.value?.draft)
            viewModel.editor.save()
        }
        awaitStatus(null)
        assertEquals(
            original.copy(text = "Edición manual\nFragmento"),
            dao.getAllIdeas().first().single()
        )
    }

    @Test
    fun discardAndCancelDoNotPersistDictatedText() = runBlocking {
        val dao = database.ideaDao()
        val original = insertOriginal()
        startEditor()
        withContext(Dispatchers.Main) {
            viewModel.editor.begin(original)
            viewModel.editor.previewVoice(original.id, "Descartado")
            viewModel.editor.discardVoice()
            assertEquals(null, viewModel.editor.state.value?.voicePreview)
            assertEquals(original.text, viewModel.editor.state.value?.draft)

            viewModel.editor.previewVoice(original.id, "Aceptado")
            viewModel.editor.addVoice()
            assertEquals("${original.text}\nAceptado", viewModel.editor.state.value?.draft)
            viewModel.editor.cancel()
            assertEquals(true, viewModel.editor.state.value?.confirmDiscard)
            viewModel.editor.confirmDiscard()
            viewModel.editor.previewVoice(original.id, "Resultado tardío")
        }
        awaitStatus(null)
        assertNull(IdeaDraftRepository(database).read())
        assertEquals(original, dao.getAllIdeas().first().single())
    }

    @Test
    fun restoresLatestTextAcceptedFragmentAndPendingPreviewInNewViewModel() = runBlocking {
        val original = insertOriginal()
        val store = IdeaDraftRepository(database)
        startEditor(store)
        withContext(Dispatchers.Main) {
            viewModel.editor.begin(original)
            viewModel.editor.changeDraft("T")
            viewModel.editor.changeDraft("Texto manual")
            viewModel.editor.previewVoice(original.id, "Aceptado")
            viewModel.editor.addVoice()
            viewModel.editor.addVoice()
            viewModel.editor.previewVoice(original.id, "Pendiente")
        }
        withTimeout(10_000) {
            viewModel.editor.state.first { it?.persistence == DraftPersistence.Persisted }
        }
        assertEquals(original, database.ideaDao().findIdea(original.id))

        viewModel.viewModelScope.cancel()
        viewModel = HomeViewModel(database.ideaDao(), database.folderDao(), store)
        awaitRecovery(DraftRecovery.Available)
        withContext(Dispatchers.Main) { viewModel.editor.recover() }
        awaitRecovery(DraftRecovery.None)
        assertEquals("Texto manual\nAceptado", viewModel.editor.state.value?.draft)
        assertEquals("Pendiente", viewModel.editor.state.value?.voicePreview)
        assertEquals(true, viewModel.editor.state.value?.recovered)

        withContext(Dispatchers.Main) {
            viewModel.editor.discardVoice()
            viewModel.editor.save()
        }
        awaitStatus(null)
        assertNull(store.read())
        assertEquals(original.copy(text = "Texto manual\nAceptado"), database.ideaDao().findIdea(original.id))
    }

    @Test
    fun offersRecoveryForDeletedNoteAndRetainsItsContents() = runBlocking {
        val original = insertOriginal()
        val store = IdeaDraftRepository(database)
        store.create(original.id, original.text, "Recuperable", "Pendiente")
        database.ideaDao().deleteIdeaById(original.id)
        viewModel = HomeViewModel(database.ideaDao(), database.folderDao(), store)
        awaitRecovery(DraftRecovery.Available)

        withContext(Dispatchers.Main) { viewModel.editor.recover() }
        awaitStatus(EditIdeaStatus.Conflict)
        assertEquals("Recuperable", viewModel.editor.state.value?.draft)
        assertEquals("Pendiente", viewModel.editor.state.value?.voicePreview)
        withContext(Dispatchers.Main) {
            viewModel.editor.save()
            viewModel.editor.cancel()
            viewModel.editor.confirmDiscard()
        }
        awaitStatus(null)
        assertNull(store.read())
        assertNull(database.ideaDao().findIdea(original.id))
    }

    @Test
    fun discardsRecoveryOnlyAfterDiskConfirmsAndCanRetryFailure() = runBlocking {
        val original = insertOriginal()
        val store = IdeaDraftRepository(database)
        store.create(original.id, original.text, "Recuperable")
        val allowDiscard = CompletableDeferred<Unit>()
        var attempts = 0
        val failingStore = object : IdeaDraftStore by store {
            override suspend fun discard(sessionId: String): Boolean {
                if (++attempts == 1) throw SQLiteException("Fallo simulado")
                allowDiscard.await()
                return store.discard(sessionId)
            }
        }
        viewModel = HomeViewModel(database.ideaDao(), database.folderDao(), failingStore)
        awaitRecovery(DraftRecovery.Available)
        withContext(Dispatchers.Main) { viewModel.editor.discardRecovery() }
        awaitRecovery(DraftRecovery.ActionFailed)
        assertEquals("Recuperable", store.read()?.draft)

        withContext(Dispatchers.Main) { viewModel.editor.discardRecovery() }
        awaitRecovery(DraftRecovery.Working)
        allowDiscard.complete(Unit)
        awaitRecovery(DraftRecovery.None)
        assertNull(store.read())
        assertEquals(original, database.ideaDao().findIdea(original.id))
    }

    private suspend fun startEditor(store: IdeaDraftStore = IdeaDraftRepository(database)) {
        viewModel = HomeViewModel(database.ideaDao(), database.folderDao(), store)
        awaitRecovery(DraftRecovery.None)
    }

    private suspend fun awaitRecovery(state: DraftRecovery) {
        withTimeout(10_000) { viewModel.editor.recovery.first { it == state } }
    }

    private suspend fun insertOriginal(folderId: Long? = null): IdeaEntity {
        val idea = IdeaEntity(text = "Texto original", createdAt = 123L, folderId = folderId)
        return idea.copy(id = database.ideaDao().insertIdea(idea))
    }

    private suspend fun saveDraft(original: IdeaEntity) = withContext(Dispatchers.Main) {
        viewModel.editor.begin(original)
        viewModel.editor.changeDraft("Mi borrador")
        viewModel.editor.save()
    }

    private suspend fun awaitStatus(status: EditIdeaStatus?) {
        withTimeout(10_000) {
            viewModel.editor.state.first { it?.status == status }
        }
    }
}
