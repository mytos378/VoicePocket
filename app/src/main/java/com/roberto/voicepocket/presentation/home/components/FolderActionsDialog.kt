package com.roberto.voicepocket.presentation.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.roberto.voicepocket.data.local.FolderEntity

@Composable
fun FolderActionsDialog(
    folder: FolderEntity,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(folder.name)
        },
        text = {
            Column {
                TextButton(
                    onClick = onRenameClick
                ) {
                    Text("Renombrar")
                }

                TextButton(
                    onClick = onDeleteClick
                ) {
                    Text("Eliminar carpeta")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    )
}