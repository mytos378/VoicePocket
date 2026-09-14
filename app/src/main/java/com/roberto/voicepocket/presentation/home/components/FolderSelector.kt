package com.roberto.voicepocket.presentation.home.components

import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun FolderSelector(
    folders: List<FolderEntity>,
    selectedFolderId: Long?,
    isUnfiledSelected: Boolean,
    onAllClick: () -> Unit,
    onUnfiledClick: () -> Unit,
    onFolderClick: (FolderEntity) -> Unit,
    onCreateFolderClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFolderLongClick: (FolderEntity) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
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
            FolderFilterChip(
                text = folder.name,
                selected = selectedFolderId == folder.id,
                onClick = {
                    onFolderClick(folder)
                },
                onLongClick = {
                    onFolderLongClick(folder)
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
@Composable
private fun FolderFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
            style = MaterialTheme.typography.labelLarge
        )
    }
}