package com.roberto.voicepocket.presentation.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.roberto.voicepocket.presentation.home.EditIdeaState
import com.roberto.voicepocket.presentation.home.EditIdeaStatus
import com.roberto.voicepocket.presentation.home.VoicePhase
import com.roberto.voicepocket.presentation.home.VoiceState

@Composable
internal fun EditIdeaDialog(
    edit: EditIdeaState,
    onDraftChange: (String) -> Unit,
    voice: VoiceState?,
    microphoneAvailable: Boolean,
    onMicrophoneClick: () -> Unit,
    onAddVoice: () -> Unit,
    onDiscardVoice: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val saving = edit.status == EditIdeaStatus.Saving
    val conflict = edit.status == EditIdeaStatus.Conflict
    val listening = voice?.phase == VoicePhase.Listening
    val voiceBusy = voice?.busy == true

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text("Editar idea")
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = edit.draft,
                    onValueChange = onDraftChange,
                    label = { Text("Contenido") },
                    readOnly = saving || conflict,
                    maxLines = 6
                )
                when (edit.status) {
                    EditIdeaStatus.Failed -> Text("No se pudo guardar. Puedes reintentar.")
                    EditIdeaStatus.Conflict -> Text(
                        "La nota cambió o fue eliminada. Copia tu borrador antes de cerrar."
                    )
                    EditIdeaStatus.Editing, EditIdeaStatus.Saving -> Unit
                }
                if (!saving && !conflict) {
                    val preview = edit.voicePreview
                    if (preview != null) {
                        Text("Transcripción")
                        Text(preview)
                        Row {
                            TextButton(onClick = onAddVoice) { Text("Agregar") }
                            TextButton(onClick = onDiscardVoice) { Text("Descartar") }
                        }
                    } else {
                        TextButton(
                            onClick = onMicrophoneClick,
                            enabled = microphoneAvailable && (!voiceBusy || listening)
                        ) {
                            Text(if (listening) "Detener" else "Agregar por voz")
                        }
                        voice?.let {
                            Text(it.message)
                            if (it.partial.isNotBlank()) Text(it.partial)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !saving && !conflict && !voiceBusy &&
                    edit.voicePreview == null && edit.draft.isNotBlank()
            ) {
                Text(
                    when (edit.status) {
                        EditIdeaStatus.Saving -> "Guardando…"
                        EditIdeaStatus.Failed -> "Reintentar"
                        else -> "Guardar"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !saving
            ) {
                Text("Cancelar")
            }
        }
    )
}
