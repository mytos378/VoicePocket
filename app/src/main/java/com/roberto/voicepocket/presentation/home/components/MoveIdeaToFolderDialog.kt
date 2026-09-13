package com.roberto.voicepocket.presentation.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.ui.theme.VoicePocketDimens

@Composable
fun MoveIdeaToFolderDialog(
    folders: List<FolderEntity>,
    currentFolderId: Long?,
    onFolderSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Mover a carpeta")
        },
        text = {
            Column {
                FolderOption(
                    name = "Sin carpeta",
                    selected = currentFolderId == null,
                    onClick = {
                        onFolderSelected(null)
                    }
                )

                folders.forEach { folder ->
                    FolderOption(
                        name = folder.name,
                        selected = currentFolderId == folder.id,
                        onClick = {
                            onFolderSelected(folder.id)
                        }
                    )
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

@Composable
private fun FolderOption(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                vertical = VoicePocketDimens.SpaceS
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Text(
            text = name,
            modifier = Modifier.padding(
                start = VoicePocketDimens.SpaceS
            )
        )
    }
}