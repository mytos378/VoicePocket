package com.roberto.voicepocket.presentation.home.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun EditIdeaDialog(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editedText by remember(initialText) {
        mutableStateOf(initialText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Editar idea")
        },
        text = {
            OutlinedTextField(
                value = editedText,
                onValueChange = {
                    editedText = it
                },
                label = {
                    Text("Contenido")
                },
                singleLine = false
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(editedText)
                },
                enabled = editedText.isNotBlank()
            ) {
                Text("Guardar")
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