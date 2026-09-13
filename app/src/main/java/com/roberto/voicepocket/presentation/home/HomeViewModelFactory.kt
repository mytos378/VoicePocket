package com.roberto.voicepocket.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.roberto.voicepocket.data.local.FolderDao
import com.roberto.voicepocket.data.local.IdeaDao

class HomeViewModelFactory(
    private val ideaDao: IdeaDao,
    private val folderDao: FolderDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                ideaDao = ideaDao,
                folderDao = folderDao
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}