package com.roberto.voicepocket

import android.database.sqlite.SQLiteException
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.data.local.IdeaDao
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
        val delayedDao = object : IdeaDao by dao {
            override suspend fun updateIdeaText(ideaId: Long, originalText: String, text: String): Int {
                attempts++
                allowUpdate.await()
                return dao.updateIdeaText(ideaId, originalText, text)
            }
        }
        viewModel = HomeViewModel(delayedDao, folders)

        withContext(Dispatchers.Main) {
            viewModel.beginIdeaEdit(original)
            viewModel.changeIdeaDraft("Texto editado")
            viewModel.saveIdeaEdit()
            viewModel.saveIdeaEdit()
            viewModel.cancelIdeaEdit()
            assertEquals(EditIdeaStatus.Saving, viewModel.ideaEditState.value?.status)
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
        val failingDao = object : IdeaDao by dao {
            override suspend fun updateIdeaText(ideaId: Long, originalText: String, text: String): Int {
                attempts++
                if (attempts == 1) throw SQLiteException("Fallo simulado")
                allowRetry.await()
                return dao.updateIdeaText(ideaId, originalText, text)
            }
        }
        viewModel = HomeViewModel(failingDao, database.folderDao())

        saveDraft(original)
        awaitStatus(EditIdeaStatus.Failed)
        assertEquals("Mi borrador", viewModel.ideaEditState.value?.draft)
        assertEquals(original, dao.getAllIdeas().first().single())

        withContext(Dispatchers.Main) {
            viewModel.saveIdeaEdit()
            viewModel.saveIdeaEdit()
            assertEquals(EditIdeaStatus.Saving, viewModel.ideaEditState.value?.status)
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
        viewModel = HomeViewModel(dao, database.folderDao())
        assertEquals(1, dao.updateIdeaText(original.id, original.text, "Cambio concurrente"))

        saveDraft(original)
        awaitStatus(EditIdeaStatus.Conflict)
        withContext(Dispatchers.Main) { viewModel.saveIdeaEdit() }
        assertEquals(EditIdeaStatus.Conflict, viewModel.ideaEditState.value?.status)
        assertEquals("Mi borrador", viewModel.ideaEditState.value?.draft)
        assertEquals(original.copy(text = "Cambio concurrente"), dao.getAllIdeas().first().single())
    }

    @Test
    fun preservesDraftIfNoteWasDeleted() = runBlocking {
        val dao = database.ideaDao()
        val original = insertOriginal()
        viewModel = HomeViewModel(dao, database.folderDao())
        dao.deleteIdeaById(original.id)

        saveDraft(original)
        awaitStatus(EditIdeaStatus.Conflict)
        assertEquals("Mi borrador", viewModel.ideaEditState.value?.draft)
        assertEquals(emptyList<IdeaEntity>(), dao.getAllIdeas().first())
    }

    private suspend fun insertOriginal(folderId: Long? = null): IdeaEntity {
        val idea = IdeaEntity(text = "Texto original", createdAt = 123L, folderId = folderId)
        return idea.copy(id = database.ideaDao().insertIdea(idea))
    }

    private suspend fun saveDraft(original: IdeaEntity) = withContext(Dispatchers.Main) {
        viewModel.beginIdeaEdit(original)
        viewModel.changeIdeaDraft("Mi borrador")
        viewModel.saveIdeaEdit()
    }

    private suspend fun awaitStatus(status: EditIdeaStatus?) {
        withTimeout(10_000) {
            viewModel.ideaEditState.first { it?.status == status }
        }
    }
}
