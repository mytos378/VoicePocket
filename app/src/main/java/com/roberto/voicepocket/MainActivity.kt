package com.roberto.voicepocket

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roberto.voicepocket.data.local.IdeaDraftRepository
import com.roberto.voicepocket.data.local.IdeaEntity
import com.roberto.voicepocket.data.local.VoicePocketDatabase
import com.roberto.voicepocket.presentation.home.HomeScreen
import com.roberto.voicepocket.presentation.home.HomeViewModel
import com.roberto.voicepocket.presentation.home.HomeViewModelFactory
import com.roberto.voicepocket.ui.theme.VoicePocketTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val database = VoicePocketDatabase.getInstance(applicationContext)
        val factory = HomeViewModelFactory(
            ideaDao = database.ideaDao(),
            folderDao = database.folderDao(),
            draftStore = IdeaDraftRepository(database)
        )

        setContent {
            VoicePocketTheme {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = factory
                )

                val ideas by homeViewModel.ideas.collectAsState()

                val folders by homeViewModel.folders.collectAsState()
                val ideaSaveState by homeViewModel.ideaSaveState.collectAsState()

                HomeScreen(
                    lifecycle = this@MainActivity.lifecycle,
                    ideas = ideas,
                    folders = folders,
                    ideaSaveState = ideaSaveState,
                    onIdeaRecognized = homeViewModel::saveIdea,
                    onRetryIdeaSave = homeViewModel::retryIdeaSave,
                    onDiscardFailedIdea = homeViewModel::discardFailedIdea,
                    onIdeaSavedShown = homeViewModel::acknowledgeIdeaSaved,
                    editor = homeViewModel.editor,
                    onCopyDraft = { copyText(it, "Borrador copiado") },
                    onShareIdea = ::shareIdea,
                    onCopyIdea = ::copyIdea,
                    onDeleteIdea = homeViewModel::deleteIdea,
                    onCreateFolder = homeViewModel::createFolder,
                    onMoveIdeaToFolder = homeViewModel::moveIdeaToFolder,
                    onDeleteFolder = homeViewModel::deleteFolder,
                    onRenameFolder = homeViewModel::renameFolder
                )
            }
        }
    }

    private fun shareIdea(idea: IdeaEntity) {
        val sharedText = """
        💡 Idea capturada con VoicePocket

        ${idea.text}
    """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sharedText)
        }

        startActivity(
            Intent.createChooser(
                shareIntent,
                "Compartir idea"
            )
        )
    }
    private fun copyIdea(idea: IdeaEntity) = copyText(idea.text, "Idea copiada")

    private fun copyText(text: String, confirmation: String) {
        val clipboardManager = getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

        val clipData = ClipData.newPlainText(
            "Idea de VoicePocket",
            text
        )

        clipboardManager.setPrimaryClip(clipData)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(
                this,
                confirmation,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
