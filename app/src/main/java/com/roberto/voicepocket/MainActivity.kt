package com.roberto.voicepocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roberto.voicepocket.data.local.VoicePocketDatabase
import com.roberto.voicepocket.presentation.home.HomeScreen
import com.roberto.voicepocket.presentation.home.HomeViewModel
import com.roberto.voicepocket.presentation.home.HomeViewModelFactory
import com.roberto.voicepocket.ui.theme.VoicePocketTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

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
                    onEditIdea = {
                        // Se implementará después.
                    },
                    onDeleteIdea = {
                        // Se implementará después.
                    }
                )
            }
        }
    }
}