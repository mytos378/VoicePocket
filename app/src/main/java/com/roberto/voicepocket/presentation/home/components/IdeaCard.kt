package com.roberto.voicepocket.presentation.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roberto.voicepocket.data.local.IdeaEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IdeaCard(
    idea: IdeaEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = idea.text,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(10.dp))

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