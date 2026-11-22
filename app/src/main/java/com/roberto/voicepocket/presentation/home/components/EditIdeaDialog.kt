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
import com.roberto.voicepocket.presentation.home.DraftPersistence
import com.roberto.voicepocket.presentation.home.DraftRecovery
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
    onRetryDraft: () -> Unit,
    onCopyDraft: () -> Unit,
    onKeepEditing: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val busy = edit.busy
    val conflict = edit.status == EditIdeaStatus.Conflict
    val listening = voice?.phase == VoicePhase.Listening
    val voiceBusy = voice?.busy == true

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Text("Editar idea")
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = edit.draft,
                    onValueChange = onDraftChange,
                    label = { Text("Contenido") },
                    readOnly = !edit.canChange,
                    maxLines = 6
                )
                when (edit.status) {
                    EditIdeaStatus.Failed -> Text("No se pudo guardar. Puedes reintentar.")
                    EditIdeaStatus.Conflict -> Text(
                        "La nota cambió o fue eliminada. Copia tu borrador antes de cerrar."
                    )
                    EditIdeaStatus.Editing, EditIdeaStatus.Saving, EditIdeaStatus.Discarding -> Unit
                }
                if (edit.recovered) Text("Borrador recuperado")
                if (edit.discardFailed) Text("No se pudo descartar el borrador. Puedes reintentar.")
                if (!busy) when (edit.persistence) {
                    DraftPersistence.None -> Unit
                    DraftPersistence.Pending -> Text("Guardando borrador…")
                    DraftPersistence.Persisted -> Text("Borrador guardado")
                    DraftPersistence.Failed -> {
                        Text("No se guardó la última versión del borrador.")
                        TextButton(onClick = onRetryDraft) { Text("Reintentar borrador") }
                    }
                }
                if (conflict) {
                    edit.voicePreview?.let {
                        Text("Transcripción pendiente")
                        Text(it)
                    }
                    TextButton(onClick = onCopyDraft) { Text("Copiar borrador") }
                }
                if (edit.canChange) {
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
                enabled = edit.canChange && !voiceBusy &&
                    edit.voicePreview == null && edit.draft.isNotBlank()
            ) {
                Text(
                    when (edit.status) {
                        EditIdeaStatus.Saving -> "Guardando…"
                        EditIdeaStatus.Discarding -> "Descartando…"
                        EditIdeaStatus.Failed -> "Reintentar"
                        else -> "Guardar"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy
            ) {
                Text("Cancelar")
            }
        }
    )

    if (edit.confirmDiscard) {
        AlertDialog(
            onDismissRequest = onKeepEditing,
            title = { Text("¿Descartar borrador?") },
            text = { Text("Se perderán los cambios del borrador. La nota conservará su contenido.") },
            confirmButton = { TextButton(onClick = onConfirmDiscard) { Text("Descartar borrador") } },
            dismissButton = { TextButton(onClick = onKeepEditing) { Text("Conservar borrador") } }
        )
    }
}

@Composable
internal fun DraftRecoveryDialog(
    recovery: DraftRecovery,
    onRecover: () -> Unit,
    onDiscard: () -> Unit,
    onRetry: () -> Unit
) {
    if (recovery == DraftRecovery.Loading || recovery == DraftRecovery.None) return

    val working = recovery == DraftRecovery.Working
    val readFailed = recovery == DraftRecovery.ReadFailed
    AlertDialog(
        onDismissRequest = {},
        title = { Text(if (readFailed) "No se pudo buscar un borrador" else "Borrador disponible") },
        text = {
            Text(
                when (recovery) {
                    DraftRecovery.ReadFailed -> "Reintenta la lectura para continuar."
                    DraftRecovery.ActionFailed -> "No se pudo completar la operación. Puedes reintentar."
                    DraftRecovery.Working -> "Procesando borrador…"
                    else -> "Puedes retomar la edición pendiente o descartar sus cambios."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = if (readFailed) onRetry else onRecover, enabled = !working) {
                Text(if (readFailed) "Reintentar" else "Recuperar")
            }
        },
        dismissButton = {
            if (!readFailed) TextButton(onClick = onDiscard, enabled = !working) { Text("Descartar") }
        }
    )
}
