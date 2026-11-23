package com.roberto.voicepocket

import androidx.activity.ComponentActivity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.data.local.IdeaDraftEntity
import com.roberto.voicepocket.data.local.IdeaDraftStore
import com.roberto.voicepocket.data.local.IdeaEntity
import com.roberto.voicepocket.presentation.home.HomeScreen
import com.roberto.voicepocket.presentation.home.IdeaEditor
import com.roberto.voicepocket.presentation.home.IdeaSaveState
import com.roberto.voicepocket.ui.theme.VoicePocketTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeIdeaOrderingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val saveState = mutableStateOf<IdeaSaveState>(IdeaSaveState.Idle)
    private val ideas = mutableStateOf(emptyList<IdeaEntity>())
    private val work = FolderEntity(id = 1, name = "Trabajo")
    private val home = FolderEntity(id = 2, name = "Casa")
    private val folders = mutableStateOf<List<FolderEntity>?>(listOf(work, home))
    private val composeViewId = View.generateViewId()

    @Test
    fun changesVisibleOrderWithoutChangingSearchOrFolder() {
        ideas.value = listOf(
            IdeaEntity(4, "Zorro viaje", 40, work.id),
            IdeaEntity(3, "Avión viaje", 30, work.id),
            IdeaEntity(2, "No coincide", 20, work.id),
            IdeaEntity(1, "Casa viaje", 10, home.id)
        )
        compose.setContent { Screen() }

        compose.onNodeWithText("Trabajo").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("viaje")
        assertAbove("Zorro viaje", "Avión viaje")
        selectOrder("Alfabético (A-Z)")
        assertAbove("Avión viaje", "Zorro viaje")
        compose.onNodeWithText("viaje").assertIsDisplayed()
        compose.onNodeWithText("No coincide").assertDoesNotExist()
        compose.onNodeWithText("Casa viaje").assertDoesNotExist()
        compose.onNodeWithText("Casa").performClick()
        compose.onNodeWithText("Casa viaje").assertIsDisplayed()
        assertSelectedOrder("Alfabético (A-Z)")
    }

    @Test
    fun restoresSelectedOrderWithComposeSavedInstanceState() {
        ideas.value = listOf(IdeaEntity(1, "Antigua", 1), IdeaEntity(2, "Reciente", 2))
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Screen() }
        selectOrder("Más antiguas")
        assertAbove("Antigua", "Reciente")

        restoration.emulateSavedInstanceStateRestore()
        assertAbove("Antigua", "Reciente")
        assertSelectedOrder("Más antiguas")
    }

    @Test
    fun recreatesActivityAndRestoresFolderByIdWithQueryAndOrder() {
        ideas.value = listOf(
            IdeaEntity(4, "Zorro viaje", 40, work.id),
            IdeaEntity(3, "Avión viaje", 30, work.id),
            IdeaEntity(2, "Otro texto", 20, work.id),
            IdeaEntity(1, "Casa viaje", 10, home.id)
        )
        installRecreatableScreen()
        compose.onNodeWithText("Trabajo").performClick()
        selectOrder("Alfabético (A-Z)")
        compose.onNode(hasSetTextAction()).performTextReplacement("viaje")

        recreateScreen()
        compose.runOnIdle { folders.value = listOf(home, work.copy(name = "Oficina")) }

        compose.onNodeWithText("Oficina").assertIsSelected()
        compose.onNodeWithText("viaje").assertIsDisplayed()
        compose.onNodeWithText("Otro texto").assertDoesNotExist()
        compose.onNodeWithText("Casa viaje").assertDoesNotExist()
        assertAbove("Avión viaje", "Zorro viaje")
        compose.onNodeWithText("Casa").performClick()
        compose.onNodeWithText("Casa").assertIsSelected()
        compose.onNodeWithText("viaje").assertIsDisplayed()
        compose.onNodeWithText("Casa viaje").assertIsDisplayed()
        assertSelectedOrder("Alfabético (A-Z)")
    }

    @Test
    fun selectsAllIfRestoredFolderIsMissingFromLoadedList() {
        ideas.value = listOf(IdeaEntity(1, "Idea sin carpeta", 10))
        installRecreatableScreen()
        compose.onNodeWithText("Trabajo").performClick()

        recreateScreen()
        compose.runOnIdle { folders.value = emptyList() }
        compose.onNodeWithText("Todas").assertIsSelected()
        compose.onNodeWithText("Idea sin carpeta").assertIsDisplayed()

        compose.runOnIdle { folders.value = listOf(work, home) }
        compose.onNodeWithText("Todas").assertIsSelected()
    }

    @Test
    fun recreatesActivityWithUnfiledSelection() {
        ideas.value = listOf(
            IdeaEntity(2, "Idea de trabajo", 20, work.id),
            IdeaEntity(1, "Idea suelta", 10)
        )
        installRecreatableScreen()
        compose.onNodeWithText("Sin carpeta").performClick()

        recreateScreen()
        compose.runOnIdle { folders.value = listOf(work, home) }
        compose.onNodeWithText("Sin carpeta").assertIsSelected()
        compose.onNodeWithText("Idea suelta").assertIsDisplayed()
        compose.onNodeWithText("Idea de trabajo").assertDoesNotExist()
    }

    @Test
    fun revealsJustSavedIdeaAtItsPositionInTheSortedList() {
        ideas.value = (25 downTo 1).map { IdeaEntity(it.toLong(), "Idea previa $it", it.toLong()) }
        compose.setContent { Screen() }
        selectOrder("Más antiguas")
        compose.onNodeWithText("Idea previa 1").assertIsDisplayed()

        val saved = IdeaEntity(26, "Captura guardada", 26)
        compose.runOnIdle { saveState.value = IdeaSaveState.Saved(saved.id) }
        compose.waitForIdle()
        compose.runOnIdle { ideas.value = listOf(saved) + ideas.value }
        compose.onNodeWithText(saved.text).assertIsDisplayed()
    }

    @Test
    fun savedIdeaThatDoesNotMatchDoesNotClearSearchOrFolder() {
        ideas.value = listOf(IdeaEntity(1, "Viaje visible", 1, work.id))
        compose.setContent { Screen() }
        compose.onNodeWithText("Trabajo").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("viaje")
        selectOrder("Alfabético (A-Z)")

        val saved = IdeaEntity(2, "Fuera de búsqueda", 2, work.id)
        compose.runOnIdle {
            ideas.value = listOf(saved, IdeaEntity(3, "Viaje de otra carpeta", 3, home.id)) + ideas.value
            saveState.value = IdeaSaveState.Saved(saved.id)
        }
        compose.onNodeWithText("viaje").assertIsDisplayed()
        compose.onNodeWithText("Viaje visible").assertIsDisplayed()
        compose.onNodeWithText("Viaje de otra carpeta").assertDoesNotExist()
        compose.onNodeWithText(saved.text).assertDoesNotExist()
    }

    private fun selectOrder(label: String) {
        compose.onNodeWithText("Ordenar").performClick()
        compose.onNodeWithText(label).performClick()
    }

    private fun assertSelectedOrder(label: String) {
        compose.onNodeWithText("Ordenar").performClick()
        compose.onNodeWithText(label).assertIsSelected()
    }

    private fun assertAbove(first: String, second: String) {
        val firstTop = compose.onNodeWithText(first).fetchSemanticsNode().boundsInRoot.top
        val secondTop = compose.onNodeWithText(second).fetchSemanticsNode().boundsInRoot.top
        assertTrue("$first debería aparecer antes que $second", firstTop < secondTop)
    }

    private fun recreateScreen() {
        compose.activityRule.scenario.recreate()
        installRecreatableScreen(loadingFolders = true)
    }

    private fun installRecreatableScreen(loadingFolders: Boolean = false) {
        compose.activityRule.scenario.onActivity { activity ->
            if (loadingFolders) folders.value = null
            activity.setContentView(ComposeView(activity).apply {
                id = composeViewId
                setContent { Screen(activity.lifecycle) }
            })
        }
        compose.waitForIdle()
    }

    @Composable
    private fun Screen(lifecycle: Lifecycle = compose.activity.lifecycle) {
        val scope = rememberCoroutineScope()
        val editor = remember { IdeaEditor(scope, EmptyDraftStore) { null } }
        VoicePocketTheme {
            HomeScreen(
                lifecycle = lifecycle,
                ideas = ideas.value,
                ideaSaveState = saveState.value,
                onIdeaRecognized = { _, _ -> },
                onRetryIdeaSave = {},
                onDiscardFailedIdea = {},
                onIdeaSavedShown = { saveState.value = IdeaSaveState.Idle },
                editor = editor,
                onCopyDraft = {},
                onShareIdea = {},
                onCopyIdea = {},
                onDeleteIdea = {},
                loadedFolders = folders.value,
                onCreateFolder = {},
                onMoveIdeaToFolder = { _, _ -> },
                onDeleteFolder = {},
                onRenameFolder = { _, _ -> }
            )
        }
    }

    private object EmptyDraftStore : IdeaDraftStore {
        override suspend fun read(): IdeaDraftEntity? = null
        override suspend fun create(ideaId: Long, originalText: String, draft: String, voicePreview: String?) =
            error("The ordering tests do not edit drafts")
        override suspend fun update(expected: IdeaDraftEntity, draft: String, voicePreview: String?) =
            error("The ordering tests do not edit drafts")
        override suspend fun discard(sessionId: String) = error("The ordering tests do not discard drafts")
        override suspend fun save(expected: IdeaDraftEntity) = error("The ordering tests do not save drafts")
    }
}
