package com.roberto.voicepocket

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.data.local.IdeaDraftRepository
import com.roberto.voicepocket.data.local.IdeaDraftSaveResult
import com.roberto.voicepocket.data.local.IdeaEntity
import com.roberto.voicepocket.data.local.VoicePocketDatabase
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdeaDraftPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "draft-test-${UUID.randomUUID()}.db"
    private var database: VoicePocketDatabase? = null

    @After
    fun closeDatabase() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migratesVersionOneAndPreservesNotes() = runBlocking {
        createLegacyDatabase(version = 1)

        val db = openDatabase()
        assertEquals(listOf(IdeaEntity(7, "Nota antigua", 100L)), db.ideaDao().getAllIdeas().first())
        assertTrue(db.folderDao().getAllFolders().first().isEmpty())
        val repository = IdeaDraftRepository(db)
        assertNull(repository.read())
        assertNotNull(repository.create(7, "Nota antigua", "Borrador"))
    }

    @Test
    fun migratesVersionTwoAndPreservesNotesAndFolders() = runBlocking {
        createLegacyDatabase(version = 2)

        val db = openDatabase()
        assertEquals(listOf(IdeaEntity(7, "Nota antigua", 100L, 9)), db.ideaDao().getAllIdeas().first())
        assertEquals(listOf(FolderEntity(9, "Trabajo", 200L)), db.folderDao().getAllFolders().first())
        val repository = IdeaDraftRepository(db)
        assertNull(repository.read())
        assertNotNull(repository.create(7, "Nota antigua", "Borrador"))
    }

    @Test
    fun persistsOneDraftAndSeparatePreviewAfterCloseAndReopen() = runBlocking {
        val db = openDatabase()
        val original = insertNote(db)
        val repository = IdeaDraftRepository(db)
        val draft = requireNotNull(repository.create(original.id, original.text, "Edición manual"))
        val updated = requireNotNull(
            repository.update(draft, "Edición manual\nFragmento aceptado", "Fragmento pendiente")
        )
        assertNull(repository.create(123, "Otra nota", "Otro borrador"))
        assertNull(repository.update(draft, "Escritura atrasada", null))

        db.close()
        val reopened = openDatabase()
        assertEquals(updated, IdeaDraftRepository(reopened).read())
        assertEquals(original, reopened.ideaDao().getAllIdeas().first().single())
    }

    @Test
    fun savesTextAndClearsDraftWhilePreservingMetadata() = runBlocking {
        val db = openDatabase()
        val original = insertNote(db)
        val repository = IdeaDraftRepository(db)
        val draft = requireNotNull(
            repository.create(original.id, original.text, "Texto editado", "Fragmento pendiente")
        )
        assertEquals(IdeaDraftSaveResult.NotReady, repository.save(draft))
        assertEquals(draft, repository.read())
        val reviewed = requireNotNull(repository.update(draft, "Texto editado", null))
        db.ideaDao().moveIdeaToFolder(original.id, null)

        assertEquals(IdeaDraftSaveResult.Stale, repository.save(draft))
        assertEquals(IdeaDraftSaveResult.Saved, repository.save(reviewed))
        assertEquals(
            original.copy(text = "Texto editado", folderId = null),
            db.ideaDao().getAllIdeas().first().single()
        )
        assertNull(repository.update(reviewed, "Escritura atrasada", null))
        assertNull(repository.read())
    }

    @Test
    fun discardPreventsLateOperationsFromAffectingReopenedEdit() = runBlocking {
        val db = openDatabase()
        val original = insertNote(db)
        val repository = IdeaDraftRepository(db)
        val first = requireNotNull(repository.create(original.id, original.text, "Primer borrador"))
        assertTrue(repository.discard(first.sessionId))
        assertNull(repository.update(first, "Resultado tardío", null))
        assertNull(repository.read())

        val second = requireNotNull(repository.create(original.id, original.text, "Segundo borrador"))
        assertNull(repository.update(first, "Resultado tardío", null))
        assertFalse(repository.discard(first.sessionId))
        assertEquals(IdeaDraftSaveResult.Stale, repository.save(first))
        assertEquals(second, repository.read())
        assertEquals(original, db.ideaDao().getAllIdeas().first().single())
    }

    @Test
    fun preservesDraftForChangedAndDeletedNote() = runBlocking {
        val db = openDatabase()
        val original = insertNote(db)
        val repository = IdeaDraftRepository(db)
        val draft = requireNotNull(repository.create(original.id, original.text, "Mi borrador"))

        db.ideaDao().updateIdeaText(original.id, original.text, "Cambio concurrente")
        assertEquals(IdeaDraftSaveResult.Conflict, repository.save(draft))
        assertEquals(draft, repository.read())
        assertEquals("Cambio concurrente", db.ideaDao().getAllIdeas().first().single().text)

        db.ideaDao().deleteIdeaById(original.id)
        assertEquals(IdeaDraftSaveResult.Conflict, repository.save(draft))
        assertEquals(draft, repository.read())
        assertTrue(db.ideaDao().getAllIdeas().first().isEmpty())
        assertTrue(repository.discard(draft.sessionId))
        assertNull(repository.read())
    }

    @Test
    fun rollsBackNoteUpdateIfDraftDeletionFailsAndAllowsRetry() = runBlocking {
        val db = openDatabase()
        val original = insertNote(db)
        val repository = IdeaDraftRepository(db)
        val draft = requireNotNull(repository.create(original.id, original.text, "Mi borrador"))
        val sqlite = db.openHelper.writableDatabase
        sqlite.execSQL(
            """
            CREATE TRIGGER reject_draft_delete BEFORE DELETE ON idea_drafts
            BEGIN SELECT RAISE(ABORT, 'Fallo simulado'); END
            """.trimIndent()
        )
        try {
            repository.save(draft)
            fail("La transacción debía propagar el error de SQLite")
        } catch (expected: SQLiteException) {
            assertTrue(expected.message.orEmpty().contains("Fallo simulado"))
        }
        assertEquals(original, db.ideaDao().getAllIdeas().first().single())
        assertEquals(draft, repository.read())

        sqlite.execSQL("DROP TRIGGER reject_draft_delete")
        assertEquals(IdeaDraftSaveResult.Saved, repository.save(draft))
        assertEquals(original.copy(text = "Mi borrador"), db.ideaDao().getAllIdeas().first().single())
        assertNull(repository.read())
    }

    private fun openDatabase(): VoicePocketDatabase =
        VoicePocketDatabase.open(context, databaseName).also { database = it }

    private suspend fun insertNote(db: VoicePocketDatabase): IdeaEntity {
        db.folderDao().insertFolder(FolderEntity(9, "Trabajo", 200L))
        val note = IdeaEntity(text = "Original", createdAt = 100L, folderId = 9)
        return note.copy(id = db.ideaDao().insertIdea(note))
    }

    // The legacy versions did not export Room schemas, so build fixtures with their original layout.
    private fun createLegacyDatabase(version: Int) {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { legacy ->
            val folderColumn = if (version == 2) ", folderId INTEGER DEFAULT NULL" else ""
            legacy.execSQL(
                """
                CREATE TABLE ideas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    text TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                    $folderColumn
                )
                """.trimIndent()
            )
            if (version == 2) {
                legacy.execSQL(
                    """
                    CREATE TABLE folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                legacy.execSQL("INSERT INTO folders (id, name, createdAt) VALUES (9, 'Trabajo', 200)")
                legacy.execSQL(
                    "INSERT INTO ideas (id, text, createdAt, folderId) VALUES (7, 'Nota antigua', 100, 9)"
                )
            } else {
                legacy.execSQL("INSERT INTO ideas (id, text, createdAt) VALUES (7, 'Nota antigua', 100)")
            }
            legacy.version = version
        }
    }
}
