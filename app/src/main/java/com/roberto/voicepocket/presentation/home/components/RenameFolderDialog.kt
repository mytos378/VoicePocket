package com.roberto.voicepocket.presentation.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.roberto.voicepocket.data.local.FolderEntity

@Composable
fun RenameFolderDialog(
    folder: FolderEntity,
    existingFolderNames: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(folder.id) {
        mutableStateOf(folder.name)
    }

    val cleanName = name.trim()

    val isDuplicate = existingFolderNames.any { existingName ->
        existingName.equals(
            cleanName,
            ignoreCase = true
        )
    }

    val canSave =
        cleanName.isNotBlank() &&
                !isDuplicate

    val confirmRename = {
        if (canSave) {
            onConfirm(cleanName)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Renombrar carpeta")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("Nombre de la carpeta")
                    },
                    singleLine = true,
                    isError = isDuplicate,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            confirmRename()
                        }
                    )
                )

                if (isDuplicate) {
                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Ya existe una carpeta con ese nombre."
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    confirmRename()
                }
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