package com.roberto.voicepocket.presentation.home

import com.roberto.voicepocket.data.local.IdeaEntity
import java.text.CollationKey
import java.text.Collator
import java.util.Locale

internal enum class IdeaOrder(val label: String) {
    Newest("Más recientes"),
    Oldest("Más antiguas"),
    Alphabetical("Alfabético (A-Z)")
}

private val newestFirst = compareByDescending<IdeaEntity> { it.createdAt }.thenByDescending { it.id }

internal fun orderIdeas(ideas: List<IdeaEntity>, order: IdeaOrder): List<IdeaEntity> =
    when (order) {
        IdeaOrder.Newest -> ideas.sortedWith(newestFirst)
        IdeaOrder.Oldest -> ideas.sortedWith(newestFirst.reversed())
        IdeaOrder.Alphabetical -> {
            val spanish = Collator.getInstance(Locale.forLanguageTag("es")).apply {
                strength = Collator.SECONDARY
                decomposition = Collator.CANONICAL_DECOMPOSITION
            }
            val byText = compareBy<Pair<IdeaEntity, CollationKey>> { it.second }
                .thenComparator { first, second -> newestFirst.compare(first.first, second.first) }
            ideas
                .map { it to spanish.getCollationKey(it.text.trimStart()) }
                .sortedWith(byText)
                .map { it.first }
        }
    }
