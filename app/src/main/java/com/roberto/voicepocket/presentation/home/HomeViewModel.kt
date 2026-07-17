package com.roberto.voicepocket.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roberto.voicepocket.data.local.IdeaDao
import com.roberto.voicepocket.data.local.IdeaEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val ideaDao: IdeaDao
) : ViewModel() {

    val ideas = ideaDao
        .getAllIdeas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    fun saveIdea(text: String) {
        val cleanText = text.trim()

        if (cleanText.isBlank()) return

        viewModelScope.launch {
            ideaDao.insertIdea(
                IdeaEntity(text = cleanText)
            )
        }
    }
}