package com.roberto.voicepocket.presentation.home.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.roberto.voicepocket.data.local.FolderEntity

@Composable
fun DeleteFolderDialog(
    folder: FolderEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Eliminar carpeta")
        },
        text = {
            Text(
                "¿Deseas eliminar \"${folder.name}\"?\n\n" +
                        "Las ideas que contiene no se eliminarán. " +
                        "Se moverán a \"Sin carpeta\"."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Eliminar")
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