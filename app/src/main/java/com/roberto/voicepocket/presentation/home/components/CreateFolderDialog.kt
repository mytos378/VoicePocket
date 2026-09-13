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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun CreateFolderDialog(
    existingFolderNames: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    val cleanName = folderName.trim()

    val alreadyExists = existingFolderNames.any {
        it.equals(cleanName, ignoreCase = true)
    }

    val canSave = cleanName.isNotBlank() && !alreadyExists

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Nueva carpeta")
        },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = {
                    Text("Nombre de la carpeta")
                },
                singleLine = true,
                isError = alreadyExists,
                supportingText = {
                    if (alreadyExists) {
                        Text("Ya existe una carpeta con ese nombre.")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canSave) {
                            onConfirm(cleanName)
                        }
                    }
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(cleanName)
                },
                enabled = canSave
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}