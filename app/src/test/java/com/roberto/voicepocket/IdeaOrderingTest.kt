package com.roberto.voicepocket

import com.roberto.voicepocket.data.local.IdeaEntity
import com.roberto.voicepocket.presentation.home.IdeaOrder
import com.roberto.voicepocket.presentation.home.orderIdeas
import org.junit.Assert.assertEquals
import org.junit.Test

class IdeaOrderingTest {
    @Test
    fun newestUsesDescendingDateAndId() {
        val ideas = listOf(idea(4, 10), idea(3, 20), idea(1, 20), idea(6, 10))
        assertEquals(listOf(3L, 1L, 6L, 4L), orderIdeas(ideas, IdeaOrder.Newest).map { it.id })
    }

    @Test
    fun oldestUsesAscendingDateAndId() {
        val ideas = listOf(idea(3, 20), idea(4, 10), idea(6, 10), idea(1, 20))
        assertEquals(listOf(4L, 6L, 1L, 3L), orderIdeas(ideas, IdeaOrder.Oldest).map { it.id })
    }

    @Test
    fun alphabeticalUsesSpanishAccentsEnyeAndAllLines() {
        val ideas = listOf(
            idea(1, text = "Oso"),
            idea(2, text = "Ñandú"),
            idea(3, text = "Nube"),
            idea(4, text = "Árbol"),
            idea(5, text = "arbol"),
            idea(6, text = "Nota\nZorro"),
            idea(7, text = "Nota\naves")
        )
        assertEquals(listOf(5L, 4L, 7L, 6L, 3L, 2L, 1L), orderIdeas(ideas, IdeaOrder.Alphabetical).map { it.id })
    }

    @Test
    fun alphabeticalIgnoresLeadingSpaceAndCaseThenUsesNewestAndId() {
        val ideas = listOf(
            idea(1, 10, "cafe\u0301"),
            idea(2, 20, "\tCAFÉ"),
            idea(3, 20, "café"),
            idea(4, 20, "café")
        )
        assertEquals(listOf(4L, 3L, 2L, 1L), orderIdeas(ideas, IdeaOrder.Alphabetical).map { it.id })
        assertEquals(listOf(1L, 2L, 3L, 4L), ideas.map { it.id })
    }

    @Test
    fun handlesEmptyListsInEveryMode() {
        IdeaOrder.entries.forEach { assertEquals(emptyList<IdeaEntity>(), orderIdeas(emptyList(), it)) }
    }

    private fun idea(id: Long, date: Long = 10, text: String = "Idea") = IdeaEntity(id, text, date)
}
