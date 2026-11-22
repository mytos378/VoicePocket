package com.roberto.voicepocket

import android.database.sqlite.SQLiteException
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.data.local.IdeaDao
import com.roberto.voicepocket.data.local.IdeaDraftRepository
import com.roberto.voicepocket.data.local.IdeaEntity
import com.roberto.voicepocket.data.local.VoicePocketDatabase
import com.roberto.voicepocket.presentation.home.HomeViewModel
import com.roberto.voicepocket.presentation.home.IdeaSaveState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdeaSaveTest {
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
    fun reportsSuccessOnlyAfterInsertAndIgnoresRepeatedRequests() = runBlocking {
        val allowInsert = CompletableDeferred<Unit>()
        val dao = database.ideaDao()
        val delayedDao = object : IdeaDao by dao {
            override suspend fun insertIdea(idea: IdeaEntity): Long {
                allowInsert.await()
                return dao.insertIdea(idea)
            }
        }
        viewModel = HomeViewModel(delayedDao, database.folderDao(), IdeaDraftRepository(database))

        withContext(Dispatchers.Main) {
            viewModel.saveIdea("Primera transcripción")
            viewModel.saveIdea("Resultado duplicado")
            assertTrue(viewModel.ideaSaveState.value is IdeaSaveState.Saving)
        }
        assertTrue(dao.getAllIdeas().first().isEmpty())

        allowInsert.complete(Unit)
        val saved = awaitSaved()
        val persisted = dao.getAllIdeas().first().single()
        assertEquals(persisted.id, saved.ideaId)
        assertEquals("Primera transcripción", persisted.text)

        withContext(Dispatchers.Main) {
            viewModel.acknowledgeIdeaSaved(saved.ideaId)
            assertEquals(IdeaSaveState.Idle, viewModel.ideaSaveState.value)
        }
    }

    @Test
    fun preservesTranscriptionAndFolderAfterFailureAndRetriesOnce() = runBlocking {
        val dao = database.ideaDao()
        val folders = database.folderDao()
        folders.insertFolder(FolderEntity(name = "Trabajo"))
        val folderId = folders.getAllFolders().first().single().id
        var attempts = 0
        val allowRetry = CompletableDeferred<Unit>()
        val failingDao = object : IdeaDao by dao {
            override suspend fun insertIdea(idea: IdeaEntity): Long {
                attempts++
                if (attempts == 1) throw SQLiteException("Fallo de escritura simulado")
                allowRetry.await()
                return dao.insertIdea(idea)
            }
        }
        viewModel = HomeViewModel(failingDao, folders, IdeaDraftRepository(database))

        withContext(Dispatchers.Main) { viewModel.saveIdea("Texto dictado", folderId) }
        val failed = withTimeout(5_000) {
            viewModel.ideaSaveState.first { it is IdeaSaveState.Failed } as IdeaSaveState.Failed
        }
        assertEquals("Texto dictado", failed.idea.text)
        assertEquals(folderId, failed.idea.folderId)
        assertTrue(dao.getAllIdeas().first().isEmpty())

        withContext(Dispatchers.Main) {
            viewModel.saveIdea("Otra transcripción")
            viewModel.retryIdeaSave()
            viewModel.retryIdeaSave()
            assertTrue(viewModel.ideaSaveState.value is IdeaSaveState.Saving)
        }
        allowRetry.complete(Unit)
        val saved = awaitSaved()
        val persisted = dao.getAllIdeas().first().single()
        assertEquals(2, attempts)
        assertEquals(failed.idea.copy(id = saved.ideaId), persisted)
    }

    private suspend fun awaitSaved(): IdeaSaveState.Saved = withTimeout(5_000) {
        viewModel.ideaSaveState.first { it is IdeaSaveState.Saved } as IdeaSaveState.Saved
    }
}
