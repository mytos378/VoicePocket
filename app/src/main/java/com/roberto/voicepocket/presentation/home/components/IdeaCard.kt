package com.roberto.voicepocket.presentation.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roberto.voicepocket.data.local.IdeaEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IdeaCard(
    idea: IdeaEntity,
    onEditClick: (IdeaEntity) -> Unit,
    onDeleteClick: (IdeaEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                start = 18.dp,
                top = 12.dp,
                end = 8.dp,
                bottom = 18.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = idea.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        menuExpanded = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones de la idea"
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("Editar")
                        },
                        onClick = {
                            menuExpanded = false
                            onEditClick(idea)
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Eliminar")
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick(idea)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatIdeaDate(idea.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatIdeaDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val difference = (now - timestamp).coerceAtLeast(0L)

    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour

    return when {
        difference < minute -> "Ahora"

        difference < hour -> {
            val minutes = difference / minute
            "Hace $minutes min"
        }

        difference < day -> {
            val hours = difference / hour
            "Hace $hours h"
        }

        difference < 2 * day -> "Ayer"

        else -> {
            val formatter = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            formatter.format(Date(timestamp))
        }
    }
}