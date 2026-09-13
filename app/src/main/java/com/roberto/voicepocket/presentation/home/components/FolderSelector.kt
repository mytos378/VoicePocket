package com.roberto.voicepocket.presentation.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.ui.theme.VoicePocketDimens

@Composable
fun FolderSelector(
    folders: List<FolderEntity>,
    selectedFolderId: Long?,
    isUnfiledSelected: Boolean,
    onAllClick: () -> Unit,
    onUnfiledClick: () -> Unit,
    onFolderClick: (FolderEntity) -> Unit,
    onCreateFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(
            rememberScrollState()
        ),
        horizontalArrangement = Arrangement.spacedBy(
            VoicePocketDimens.SpaceS
        )
    ) {
        FilterChip(
            selected = selectedFolderId == null && !isUnfiledSelected,
            onClick = onAllClick,
            label = {
                Text("Todas")
            }
        )

        FilterChip(
            selected = isUnfiledSelected,
            onClick = onUnfiledClick,
            label = {
                Text("Sin carpeta")
            }
        )

        folders.forEach { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = {
                    onFolderClick(folder)
                },
                label = {
                    Text(folder.name)
                }
            )
        }

        Spacer(
            modifier = Modifier.width(
                VoicePocketDimens.SpaceXS
            )
        )

        TextButton(
            onClick = onCreateFolderClick
        ) {
            Text("+")
        }
    }
}