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
                        voice = null,
                        microphoneAvailable = true,
                        onMicrophoneClick = {},
                        onAddVoice = {},
                        onDiscardVoice = {},
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

    @Test
    fun presentsFinalTranscriptForReviewBeforeEnablingSave() {
        val preview = mutableStateOf<String?>(null)
        compose.setContent {
            MaterialTheme {
                EditIdeaDialog(
                    edit = EditIdeaState(1, "Original", voicePreview = preview.value),
                    onDraftChange = {},
                    voice = null,
                    microphoneAvailable = true,
                    onMicrophoneClick = { preview.value = "Fragmento dictado" },
                    onAddVoice = { preview.value = null },
                    onDiscardVoice = { preview.value = null },
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        compose.onNodeWithText("Agregar por voz").performClick()
        compose.onNodeWithText("Fragmento dictado").assertIsDisplayed()
        compose.onNodeWithText("Original").assertIsDisplayed()
        compose.onNodeWithText("Guardar").assertIsNotEnabled()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Fragmento dictado").assertDoesNotExist()
        compose.onNodeWithText("Agregar por voz").assertIsDisplayed()
    }
}
