package com.roberto.voicepocket.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.roberto.voicepocket.data.local.FolderDao
import com.roberto.voicepocket.data.local.IdeaDao
import com.roberto.voicepocket.data.local.IdeaDraftStore

internal class HomeViewModelFactory(
    private val ideaDao: IdeaDao,
    private val folderDao: FolderDao,
    private val draftStore: IdeaDraftStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                ideaDao = ideaDao,
                folderDao = folderDao,
                draftStore = draftStore
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
