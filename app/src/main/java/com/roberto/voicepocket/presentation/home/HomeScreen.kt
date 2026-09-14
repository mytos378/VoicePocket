package com.roberto.voicepocket.presentation.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.IconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.data.local.IdeaEntity
import com.roberto.voicepocket.presentation.home.components.CreateFolderDialog
import com.roberto.voicepocket.presentation.home.components.FolderActionsDialog
import com.roberto.voicepocket.presentation.home.components.DeleteFolderDialog
import com.roberto.voicepocket.presentation.home.components.DeleteIdeaDialog
import com.roberto.voicepocket.presentation.home.components.EditIdeaDialog
import com.roberto.voicepocket.presentation.home.components.EmptyIdeasState
import com.roberto.voicepocket.presentation.home.components.FolderSelector
import com.roberto.voicepocket.presentation.home.components.IdeaCard
import com.roberto.voicepocket.presentation.home.components.MoveIdeaToFolderDialog
import com.roberto.voicepocket.ui.theme.VoicePocketDimens
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.launch
import java.util.Locale


@Composable
fun HomeScreen(
    ideas: List<IdeaEntity>,
    onIdeaRecognized: (String, Long?) -> Unit,
    onEditIdea: (IdeaEntity, String) -> Unit,
    onShareIdea: (IdeaEntity) -> Unit,
    onCopyIdea: (IdeaEntity) -> Unit,
    onDeleteIdea: (IdeaEntity) -> Unit,
    folders: List<FolderEntity>,
    onCreateFolder: (String) -> Unit,
    onMoveIdeaToFolder: (IdeaEntity, Long?) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit
){
    val context = LocalContext.current

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Pulsa para hablar") }

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var ideaPendingDeletion by remember {
        mutableStateOf<IdeaEntity?>(null)
    }

    var ideaPendingEdition by remember {
        mutableStateOf<IdeaEntity?>(null)
    }

    var ideaPendingMove by remember {
        mutableStateOf<IdeaEntity?>(null)
    }

    var showCreateFolderDialog by remember {
        mutableStateOf(false)
    }

    var selectedFolderId by remember {
        mutableStateOf<Long?>(null)
    }

    var isUnfiledSelected by remember {
        mutableStateOf(false)
    }

    var folderPendingAction by remember {
        mutableStateOf<FolderEntity?>(null)
    }

    var folderPendingDeletion by remember {
        mutableStateOf<FolderEntity?>(null)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val folderFilteredIdeas = when {
        isUnfiledSelected -> {
            ideas.filter { idea ->
                idea.folderId == null
            }
        }

        selectedFolderId != null -> {
            ideas.filter { idea ->
                idea.folderId == selectedFolderId
            }
        }

        else -> {
            ideas
        }
    }

    val filteredIdeas = folderFilteredIdeas.filter { idea ->
        idea.text.contains(
            searchQuery,
            ignoreCase = true
        )
    }

    val folderContentKey = when {
        isUnfiledSelected -> "unfiled"
        selectedFolderId != null -> "folder_$selectedFolderId"
        else -> "all"
    }

    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault().toLanguageTag()
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    statusMessage = "Escuchando..."
                }

                override fun onBeginningOfSpeech() {
                    statusMessage = "Te escucho..."
                }

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    statusMessage = "Procesando..."
                }

                override fun onError(error: Int) {
                    isListening = false
                    recognizedText = ""
                    statusMessage = speechErrorMessage(error)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false

                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    recognizedText = matches
                        ?.firstOrNull()
                        .orEmpty()
                        .trim()

                    statusMessage = if (recognizedText.isBlank()) {
                        "No se reconoció ninguna frase"
                    } else {
                        val targetFolderId = when {
                            isUnfiledSelected -> null
                            selectedFolderId != null -> selectedFolderId
                            else -> null
                        }

                        onIdeaRecognized(
                            recognizedText,
                            targetFolderId
                        )

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Idea guardada"
                            )
                        }

                        recognizedText = ""
                        "Pulsa para hablar"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partialMatches = partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    partialMatches
                        ?.firstOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { recognizedText = it }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }
        )

        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            statusMessage = "El reconocimiento de voz no está disponible"
            return
        }

        recognizedText = ""
        statusMessage = "Preparando micrófono..."
        speechRecognizer.startListening(recognitionIntent)
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { permissionGranted ->
        if (permissionGranted) {
            startListening()
        } else {
            statusMessage = "Se necesita permiso para usar el micrófono"
        }
    }

    fun handleMicrophoneClick() {
        if (isListening) {
            speechRecognizer?.stopListening()
            statusMessage = "Procesando..."
            return
        }

        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            startListening()
        } else {
            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    val fabSize by animateDpAsState(
        targetValue = if (isListening) {
            VoicePocketDimens.FabSize + 6.dp
        } else {
            VoicePocketDimens.FabSize
        },
        animationSpec = spring(),
        label = "FabSize"
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        start = VoicePocketDimens.SpaceXL,
                        end = VoicePocketDimens.SpaceXL,
                        top = VoicePocketDimens.SpaceXL,
                        bottom = VoicePocketDimens.SpaceM
                    )
            ) {
                Text(
                    text = "VoicePocket",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(
                        VoicePocketDimens.SpaceXS
                    )
                )

                Text(
                    text = "Nunca pierdas una idea",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = ::handleMicrophoneClick,
                modifier = Modifier.size(
                    fabSize
                ),
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(
                    imageVector = if (isListening) {
                        Icons.Default.Stop
                    } else {
                        Icons.Default.Mic
                    },
                    contentDescription = if (isListening) {
                        "Detener reconocimiento"
                    } else {
                        "Capturar idea"
                    },
                    modifier = Modifier.size(
                        VoicePocketDimens.FabIcon
                    )
                )
            }
        }
    )
    { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = VoicePocketDimens.ScreenPadding
                )
        ) {
            Text(
                text = "Mis ideas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            VoicePocketDimens.SpaceXS

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isListening) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            VoicePocketDimens.SpaceL

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newQuery ->
                    searchQuery = newQuery
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text("Buscar ideas")
                },
                placeholder = {
                    Text("Escribe una palabra")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar búsqueda"
                            )
                        }
                    }
                }
            )
            Spacer(
                modifier = Modifier.height(
                    VoicePocketDimens.SpaceS
                )
            )

            FolderSelector(
                folders = folders,
                selectedFolderId = selectedFolderId,
                isUnfiledSelected = isUnfiledSelected,
                onAllClick = {
                    selectedFolderId = null
                    isUnfiledSelected = false
                },
                onUnfiledClick = {
                    selectedFolderId = null
                    isUnfiledSelected = true
                },
                onFolderClick = { folder ->
                    selectedFolderId = folder.id
                    isUnfiledSelected = false
                },
                onCreateFolderClick = {
                    showCreateFolderDialog = true
                },
                onFolderLongClick = { folder ->
                    folderPendingAction = folder
                }
            )

            Spacer(
                modifier = Modifier.height(
                    VoicePocketDimens.SpaceS
                )
            )

            VoicePocketDimens.SpaceL

            if (recognizedText.isNotBlank()) {
                VoicePocketDimens.SpaceS

                Text(
                    text = recognizedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            VoicePocketDimens.SpaceL

        AnimatedContent(
            targetState = folderContentKey,
            transitionSpec = {
                (fadeIn() + slideInHorizontally { fullWidth ->
                    fullWidth / 10
                }) togetherWith
                        (fadeOut() + slideOutHorizontally { fullWidth ->
                            -fullWidth / 10
                        })
            },
            label = "FolderContentTransition"
        )   {


            when {
                ideas.isEmpty() -> {
                    EmptyIdeasState()
                }

                folderFilteredIdeas.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No hay ideas en esta carpeta",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(
                                modifier = Modifier.height(
                                    VoicePocketDimens.SpaceS
                                )
                            )

                            Text(
                                text = "Puedes mover ideas aquí desde el menú de cada idea.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                filteredIdeas.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No se encontraron ideas",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(
                                modifier = Modifier.height(
                                    VoicePocketDimens.SpaceS
                                )
                            )

                            Text(
                                text = "Prueba con otra palabra.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            VoicePocketDimens.ListSpacing
                        )
                    ) {
                        items(
                            items = filteredIdeas,
                            key = { idea -> idea.id }
                        ) { idea ->
                            IdeaCard(
                                idea = idea,
                                modifier = Modifier.animateItem(),
                                onEditClick = { selectedIdea ->
                                    ideaPendingEdition = selectedIdea
                                },
                                onMoveClick = { selectedIdea ->
                                    ideaPendingMove = selectedIdea
                                },
                                onShareClick = onShareIdea,
                                onCopyClick = onCopyIdea,
                                onDeleteClick = { selectedIdea ->
                                    ideaPendingDeletion = selectedIdea
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            existingFolderNames = folders.map { it.name },
            onConfirm = { folderName ->
                onCreateFolder(folderName)
                showCreateFolderDialog = false
            },
            onDismiss = {
                showCreateFolderDialog = false
            }
        )
    }

    ideaPendingDeletion?.let { idea ->
        DeleteIdeaDialog(
            onConfirm = {
                onDeleteIdea(idea)
                ideaPendingDeletion = null
            },
            onDismiss = {
                ideaPendingDeletion = null
            }
        )
    }
    ideaPendingEdition?.let { idea ->
        EditIdeaDialog(
            initialText = idea.text,
            onConfirm = { editedText ->
                onEditIdea(idea, editedText)
                ideaPendingEdition = null
            },
            onDismiss = {
                ideaPendingEdition = null
            }
        )
    }
    ideaPendingMove?.let { idea ->
        MoveIdeaToFolderDialog(
            folders = folders,
            currentFolderId = idea.folderId,
            onFolderSelected = { folderId ->
                onMoveIdeaToFolder(
                    idea,
                    folderId
                )
                ideaPendingMove = null
            },
            onDismiss = {
                ideaPendingMove = null
            }
        )
    }
    folderPendingAction?.let { folder ->
        FolderActionsDialog(
            folder = folder,
            onDeleteClick = {
                folderPendingAction = null
                folderPendingDeletion = folder
            },
            onDismiss = {
                folderPendingAction = null
            }
        )
    }
    folderPendingDeletion?.let { folder ->
        DeleteFolderDialog(
            folder = folder,
            onConfirm = {
                onDeleteFolder(folder)

                if (selectedFolderId == folder.id) {
                    selectedFolderId = null
                    isUnfiledSelected = true
                }

                folderPendingDeletion = null
            },
            onDismiss = {
                folderPendingDeletion = null
            }
        )
    }
}

private fun speechErrorMessage(error: Int): String {
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO ->
            "Ocurrió un problema con el micrófono"

        SpeechRecognizer.ERROR_CLIENT ->
            "No fue posible iniciar el reconocimiento"

        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "No existe permiso para usar el micrófono"

        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Revisa tu conexión e inténtalo nuevamente"

        SpeechRecognizer.ERROR_NO_MATCH ->
            "No entendí la frase. Inténtalo otra vez"

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "El reconocedor está ocupado. Inténtalo nuevamente"

        SpeechRecognizer.ERROR_SERVER ->
            "El servicio de voz no está disponible"

        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "No escuché ninguna voz"

        else ->
            "No fue posible reconocer la voz"
    }
}