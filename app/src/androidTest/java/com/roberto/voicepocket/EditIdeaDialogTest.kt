package com.roberto.voicepocket

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.roberto.voicepocket.presentation.home.EditIdeaState
import com.roberto.voicepocket.presentation.home.EditIdeaStatus
import com.roberto.voicepocket.presentation.home.components.EditIdeaDialog
import org.junit.Rule
import org.junit.Test

class EditIdeaDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun keepsDialogOpenDuringSaveAndShowsRetryAfterFailure() {
        val editor = mutableStateOf<EditIdeaState?>(EditIdeaState(1, "Original"))
        compose.setContent {
            MaterialTheme {
                editor.value?.let { edit ->
                    EditIdeaDialog(
                        edit = edit,
                        onDraftChange = { editor.value = edit.copy(draft = it) },
                        onConfirm = { editor.value = edit.copy(status = EditIdeaStatus.Saving) },
                        onDismiss = { editor.value = null }
                    )
                }
            }
        }

        compose.onNodeWithText("Original").performTextReplacement("Borrador")
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithText("Guardando…").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").assertIsNotEnabled()
        compose.onNodeWithText("Editar idea").assertIsDisplayed()

        compose.runOnIdle {
            editor.value = editor.value?.copy(status = EditIdeaStatus.Failed)
        }
        compose.onNodeWithText("Borrador").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        compose.onNodeWithText("Guardando…").assertIsNotEnabled()

        compose.runOnIdle { editor.value = null }
        compose.onNodeWithText("Editar idea").assertDoesNotExist()
    }
}
