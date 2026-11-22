package com.roberto.voicepocket.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roberto.voicepocket.data.local.FolderDao
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.data.local.IdeaDao
import com.roberto.voicepocket.data.local.IdeaEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface IdeaSaveState {
    data object Idle : IdeaSaveState
    data class Saving(val idea: IdeaEntity) : IdeaSaveState
    data class Failed(val idea: IdeaEntity) : IdeaSaveState
    data class Saved(val ideaId: Long) : IdeaSaveState
}

enum class EditIdeaStatus { Editing, Saving, Failed, Conflict }

data class EditIdeaState(
    val ideaId: Long,
    val originalText: String,
    val draft: String = originalText,
    val status: EditIdeaStatus = EditIdeaStatus.Editing
)

class HomeViewModel(
    private val ideaDao: IdeaDao,
    private val folderDao: FolderDao
) : ViewModel() {

    private val _ideaSaveState = MutableStateFlow<IdeaSaveState>(IdeaSaveState.Idle)
    val ideaSaveState = _ideaSaveState.asStateFlow()

    private val _ideaEditState = MutableStateFlow<EditIdeaState?>(null)
    val ideaEditState = _ideaEditState.asStateFlow()

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

        if (cleanText.isBlank() || _ideaSaveState.value != IdeaSaveState.Idle) return

        insertIdea(IdeaEntity(text = cleanText, folderId = folderId))
    }

    fun retryIdeaSave() {
        val failed = _ideaSaveState.value as? IdeaSaveState.Failed ?: return
        insertIdea(failed.idea)
    }

    fun discardFailedIdea() {
        if (_ideaSaveState.value is IdeaSaveState.Failed) {
            _ideaSaveState.value = IdeaSaveState.Idle
        }
    }

    fun acknowledgeIdeaSaved(ideaId: Long) {
        if (_ideaSaveState.value == IdeaSaveState.Saved(ideaId)) {
            _ideaSaveState.value = IdeaSaveState.Idle
        }
    }

    private fun insertIdea(idea: IdeaEntity) {
        _ideaSaveState.value = IdeaSaveState.Saving(idea)
        viewModelScope.launch {
            _ideaSaveState.value = try {
                IdeaSaveState.Saved(ideaDao.insertIdea(idea))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                IdeaSaveState.Failed(idea)
            }
        }
    }

    fun deleteIdea(idea: IdeaEntity) {
        viewModelScope.launch {
            ideaDao.deleteIdeaById(idea.id)
        }
    }

    fun beginIdeaEdit(idea: IdeaEntity) {
        if (_ideaEditState.value == null) {
            _ideaEditState.value = EditIdeaState(idea.id, idea.text)
        }
    }

    fun changeIdeaDraft(text: String) {
        val edit = _ideaEditState.value ?: return
        if (edit.status == EditIdeaStatus.Saving || edit.status == EditIdeaStatus.Conflict) return
        _ideaEditState.value = edit.copy(draft = text, status = EditIdeaStatus.Editing)
    }

    fun cancelIdeaEdit() {
        if (_ideaEditState.value?.status != EditIdeaStatus.Saving) {
            _ideaEditState.value = null
        }
    }

    fun saveIdeaEdit() {
        val edit = _ideaEditState.value ?: return
        if (edit.status == EditIdeaStatus.Saving || edit.status == EditIdeaStatus.Conflict) return
        val text = edit.draft.trim()
        if (text.isBlank()) return

        _ideaEditState.value = edit.copy(status = EditIdeaStatus.Saving)
        viewModelScope.launch {
            _ideaEditState.value = try {
                val updated = ideaDao.updateIdeaText(edit.ideaId, edit.originalText, text)
                if (updated == 1) null else edit.copy(status = EditIdeaStatus.Conflict)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                edit.copy(status = EditIdeaStatus.Failed)
            }
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

    fun renameFolder(
        folder: FolderEntity,
        newName: String
    ) {
        val cleanName = newName.trim()

        if (cleanName.isBlank()) return

        viewModelScope.launch {
            val alreadyExists = folderDao.folderNameExistsExcept(
                name = cleanName,
                folderId = folder.id
            )

            if (!alreadyExists) {
                folderDao.renameFolder(
                    folderId = folder.id,
                    name = cleanName
                )
            }
        }
    }
}
