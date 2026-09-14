package com.roberto.voicepocket.presentation.home.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.roberto.voicepocket.data.local.FolderEntity

@Composable
fun FolderActionsDialog(
    folder: FolderEntity,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(folder.name)
        },
        text = {
            Text(
                "Selecciona una acción para esta carpeta."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDeleteClick
            ) {
                Text("Eliminar carpeta")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    )
}