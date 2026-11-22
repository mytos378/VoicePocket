package com.roberto.voicepocket.presentation.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.roberto.voicepocket.presentation.home.EditIdeaState
import com.roberto.voicepocket.presentation.home.EditIdeaStatus

@Composable
fun EditIdeaDialog(
    edit: EditIdeaState,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val saving = edit.status == EditIdeaStatus.Saving
    val conflict = edit.status == EditIdeaStatus.Conflict

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text("Editar idea")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = edit.draft,
                    onValueChange = onDraftChange,
                    label = { Text("Contenido") },
                    readOnly = saving || conflict
                )
                when (edit.status) {
                    EditIdeaStatus.Failed -> Text("No se pudo guardar. Puedes reintentar.")
                    EditIdeaStatus.Conflict -> Text(
                        "La nota cambió o fue eliminada. Copia tu borrador antes de cerrar."
                    )
                    EditIdeaStatus.Editing, EditIdeaStatus.Saving -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !saving && !conflict && edit.draft.isNotBlank()
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
