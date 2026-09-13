package com.roberto.voicepocket.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roberto.voicepocket.data.local.FolderDao
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.data.local.IdeaDao
import com.roberto.voicepocket.data.local.IdeaEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val ideaDao: IdeaDao,
    private val folderDao: FolderDao
) : ViewModel() {

    val ideas = ideaDao
        .getAllIdeas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val folders = folderDao
        .getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun createFolder(name: String) {
        val cleanName = name.trim()

        if (cleanName.isBlank()) return

        viewModelScope.launch {
            val alreadyExists = folderDao.folderNameExists(cleanName)

            if (!alreadyExists) {
                folderDao.insertFolder(
                    FolderEntity(name = cleanName)
                )
            }
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            folderDao.deleteFolderKeepingIdeas(folder.id)
        }
    }

    fun saveIdea(
        text: String,
        folderId: Long? = null
    ) {
        val cleanText = text.trim()

        if (cleanText.isBlank()) return

        viewModelScope.launch {
            ideaDao.insertIdea(
                IdeaEntity(
                    text = cleanText,
                    folderId = folderId
                )
            )
        }
    }

    fun deleteIdea(idea: IdeaEntity) {
        viewModelScope.launch {
            ideaDao.deleteIdeaById(idea.id)
        }
    }

    fun updateIdea(
        idea: IdeaEntity,
        newText: String
    ) {
        val cleanText = newText.trim()

        if (cleanText.isBlank()) return

        viewModelScope.launch {
            ideaDao.updateIdea(
                idea.copy(text = cleanText)
            )
        }
    }

    fun moveIdeaToFolder(
        idea: IdeaEntity,
        folderId: Long?
    ) {
        viewModelScope.launch {
            ideaDao.moveIdeaToFolder(
                ideaId = idea.id,
                folderId = folderId
            )
        }
    }
}