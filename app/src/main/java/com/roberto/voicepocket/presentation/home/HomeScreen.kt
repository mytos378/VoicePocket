package com.roberto.voicepocket.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.roberto.voicepocket.data.local.FolderEntity
import com.roberto.voicepocket.data.local.IdeaEntity
import com.roberto.voicepocket.presentation.home.components.CreateFolderDialog
import com.roberto.voicepocket.presentation.home.components.FolderActionsDialog
import com.roberto.voicepocket.presentation.home.components.DeleteFolderDialog
import com.roberto.voicepocket.presentation.home.components.DeleteIdeaDialog
import com.roberto.voicepocket.presentation.home.components.EditIdeaDialog
import com.roberto.voicepocket.presentation.home.components.DraftRecoveryDialog
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
import com.roberto.voicepocket.presentation.home.components.RenameFolderDialog
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect


@Composable
internal fun HomeScreen(
    lifecycle: Lifecycle,
    ideas: List<IdeaEntity>,
    ideaSaveState: IdeaSaveState,
    onIdeaRecognized: (String, Long?) -> Unit,
    onRetryIdeaSave: () -> Unit,
    onDiscardFailedIdea: () -> Unit,
    onIdeaSavedShown: (Long) -> Unit,
    editor: IdeaEditor,
    onCopyDraft: (String) -> Unit,
    onShareIdea: (IdeaEntity) -> Unit,
    onCopyIdea: (IdeaEntity) -> Unit,
    onDeleteIdea: (IdeaEntity) -> Unit,
    folders: List<FolderEntity>,
    onCreateFolder: (String) -> Unit,
    onMoveIdeaToFolder: (IdeaEntity, Long?) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onRenameFolder: (FolderEntity, String) -> Unit,
){
    val context = LocalContext.current
    val ideaEditState by editor.state.collectAsState()
    val recovery by editor.recovery.collectAsState()

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var ideaPendingDeletion by remember {
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

    var folderPendingRename by remember {
        mutableStateOf<FolderEntity?>(null)
    }

    var savedIdeaToReveal by remember { mutableStateOf<Long?>(null) }

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

    val routeVoiceResult by rememberUpdatedState<(VoiceTarget, String) -> Unit>(
        { target, text ->
            when (target) {
                VoiceTarget.NewIdea -> onIdeaRecognized(text, selectedFolderId)
                is VoiceTarget.Edit -> editor.previewVoice(target.ideaId, text)
            }
        }
    )
    val speech = remember(context) {
        SpeechRecognitionSession(
            createRecognizer = { androidVoiceRecognizer(context) },
            onResult = { target, text -> routeVoiceResult(target, text) }
        )
    }

    DisposableEffect(lifecycle, speech, editor) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                speech.cancel()
                editor.flushDraft()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            speech.cancel()
        }
    }

    fun canStartVoice(target: VoiceTarget): Boolean {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return false
        return when (target) {
            VoiceTarget.NewIdea -> ideaSaveState == IdeaSaveState.Idle && ideaEditState == null
            is VoiceTarget.Edit -> ideaEditState?.let {
                it.ideaId == target.ideaId && it.voicePreview == null && it.canChange
            } == true
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { permissionGranted ->
        speech.permissionResult(permissionGranted, ::canStartVoice)
    }

    fun handleMicrophoneClick(target: VoiceTarget) {
        val current = speech.state
        if (current?.busy == true) {
            speech.stop(target)
            return
        }
        if (!canStartVoice(target)) return

        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        speech.request(target, granted) {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val mainVoice = speech.state?.takeIf { it.target == VoiceTarget.NewIdea }
    val isListening = mainVoice?.phase == VoicePhase.Listening
    val recognizedText = mainVoice?.partial.orEmpty()


    val savedIdea = ideaSaveState as? IdeaSaveState.Saved
    LaunchedEffect(savedIdea) {
        if (savedIdea != null) {
            savedIdeaToReveal = savedIdea.ideaId
            onIdeaSavedShown(savedIdea.ideaId)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Idea guardada")
            }
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
                onClick = { handleMicrophoneClick(VoiceTarget.NewIdea) },
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
                text = when (ideaSaveState) {
                    is IdeaSaveState.Saving -> "Guardando idea..."
                    is IdeaSaveState.Failed -> "No se pudo guardar la idea"
                    else -> mainVoice?.message ?: "Pulsa para hablar"
                },
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

            if (ideaSaveState is IdeaSaveState.Failed) {
                Text(ideaSaveState.idea.text)
                Row {
                    TextButton(onClick = onRetryIdeaSave) { Text("Reintentar") }
                    TextButton(onClick = onDiscardFailedIdea) { Text("Descartar") }
                }
            }

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
            ) {
                val listState = rememberLazyListState()

                LaunchedEffect(savedIdeaToReveal, ideas) {
                    val savedId = savedIdeaToReveal ?: return@LaunchedEffect
                    if (ideas.none { it.id == savedId }) return@LaunchedEffect

                    val index = filteredIdeas.indexOfFirst { it.id == savedId }
                    if (index >= 0) listState.animateScrollToItem(index)
                    savedIdeaToReveal = null
                }

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
                        state = listState,
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
                                onEditClick = editor::begin,
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
    DraftRecoveryDialog(recovery, editor::recover, editor::discardRecovery, editor::retryLoad)
    ideaEditState?.let { edit ->
        val target = VoiceTarget.Edit(edit.ideaId)
        val editorVoice = speech.state?.takeIf { it.target == target }
        EditIdeaDialog(
            edit = edit,
            onDraftChange = editor::changeDraft,
            voice = editorVoice,
            microphoneAvailable = speech.state?.busy != true || editorVoice != null,
            onMicrophoneClick = { handleMicrophoneClick(target) },
            onAddVoice = editor::addVoice,
            onDiscardVoice = editor::discardVoice,
            onRetryDraft = editor::flushDraft,
            onCopyDraft = { onCopyDraft(listOfNotNull(edit.draft, edit.voicePreview).joinToString("\n")) },
            onKeepEditing = editor::keepEditing,
            onConfirmDiscard = editor::confirmDiscard,
            onConfirm = {
                if (speech.state?.let { it.target == target && it.busy } != true) editor.save()
            },
            onDismiss = {
                if (speech.state?.target == target) speech.cancel()
                editor.cancel()
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
            onRenameClick = {
                folderPendingAction = null
                folderPendingRename = folder
            },
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

    folderPendingRename?.let { folder ->
        RenameFolderDialog(
            folder = folder,
            existingFolderNames = folders
                .filterNot { it.id == folder.id }
                .map { it.name },
            onConfirm = { newName ->
                onRenameFolder(
                    folder,
                    newName
                )

                folderPendingRename = null
            },
            onDismiss = {
                folderPendingRename = null
            }
        )
    }
}
