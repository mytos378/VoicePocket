package com.roberto.voicepocket

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
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
        val factory = HomeViewModelFactory(database.ideaDao())

        setContent {
            VoicePocketTheme {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = factory
                )

                val ideas by homeViewModel.ideas.collectAsState()

                HomeScreen(
                    ideas = ideas,
                    onIdeaRecognized = homeViewModel::saveIdea,
                    onEditIdea = homeViewModel::updateIdea,
                    onShareIdea = ::shareIdea,
                    onDeleteIdea = homeViewModel::deleteIdea
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
}