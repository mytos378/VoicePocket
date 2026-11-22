package com.roberto.voicepocket.presentation.home

import com.roberto.voicepocket.data.local.IdeaDraftEntity
import com.roberto.voicepocket.data.local.IdeaDraftSaveResult
import com.roberto.voicepocket.data.local.IdeaDraftStore
import com.roberto.voicepocket.data.local.IdeaEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class EditIdeaStatus { Editing, Saving, Failed, Conflict, Discarding }

internal enum class DraftPersistence { None, Pending, Persisted, Failed }

internal enum class DraftRecovery { Loading, None, Available, Working, ReadFailed, ActionFailed }

internal data class EditIdeaState(
    val ideaId: Long,
    val originalText: String,
    val draft: String = originalText,
    val status: EditIdeaStatus = EditIdeaStatus.Editing,
    val voicePreview: String? = null,
    val persistence: DraftPersistence = DraftPersistence.None,
    val confirmDiscard: Boolean = false,
    val discardFailed: Boolean = false,
    val recovered: Boolean = false
) {
    val busy: Boolean get() = status == EditIdeaStatus.Saving || status == EditIdeaStatus.Discarding
    val canChange: Boolean get() = !busy && !confirmDiscard && status != EditIdeaStatus.Conflict
    val hasChanges: Boolean get() = draft != originalText || voicePreview != null
}

// UI actions and state updates run on Main. The mutex orders all disk operations for an open editor.
internal class IdeaEditor(
    private val scope: CoroutineScope,
    private val store: IdeaDraftStore,
    private val findIdea: suspend (Long) -> IdeaEntity?
) {
    private val _state = MutableStateFlow<EditIdeaState?>(null)
    val state = _state.asStateFlow()

    private val _recovery = MutableStateFlow(DraftRecovery.Loading)
    val recovery = _recovery.asStateFlow()

    private val disk = Mutex()
    private var delayJob: Job? = null
    private var requestVersion = 0L
    private var persisted: IdeaDraftEntity? = null

    init {
        load()
    }

    fun retryLoad() {
        if (_recovery.value == DraftRecovery.ReadFailed) load()
    }

    private fun load() {
        _recovery.value = DraftRecovery.Loading
        scope.launch {
            try {
                persisted = store.read()
                _recovery.value = if (persisted == null) DraftRecovery.None else DraftRecovery.Available
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _recovery.value = DraftRecovery.ReadFailed
            }
        }
    }

    fun recover() = resolveRecovery { saved ->
        val note = findIdea(saved.ideaId)
        _state.value = EditIdeaState(
            ideaId = saved.ideaId,
            originalText = saved.originalText,
            draft = saved.draft,
            status = if (note?.text == saved.originalText) EditIdeaStatus.Editing else EditIdeaStatus.Conflict,
            voicePreview = saved.voicePreview,
            persistence = DraftPersistence.Persisted,
            recovered = true
        )
    }

    fun discardRecovery() = resolveRecovery { saved ->
        check(store.discard(saved.sessionId))
        persisted = null
    }

    private fun resolveRecovery(action: suspend (IdeaDraftEntity) -> Unit) {
        if (_recovery.value != DraftRecovery.Available && _recovery.value != DraftRecovery.ActionFailed) return
        val saved = persisted ?: return
        _recovery.value = DraftRecovery.Working
        scope.launch {
            try {
                action(saved)
                _recovery.value = DraftRecovery.None
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _recovery.value = DraftRecovery.ActionFailed
            }
        }
    }

    fun begin(idea: IdeaEntity) {
        if (_state.value == null && _recovery.value == DraftRecovery.None) {
            _state.value = EditIdeaState(idea.id, idea.text)
        }
    }

    fun changeDraft(text: String) {
        val edit = _state.value?.takeIf { it.canChange && it.draft != text } ?: return
        change(edit.copy(draft = text), delayMillis = 300)
    }

    fun previewVoice(ideaId: Long, text: String) {
        val edit = _state.value?.takeIf { it.canChange && it.ideaId == ideaId && it.voicePreview == null }
            ?: return
        val fragment = text.trim()
        if (fragment.isNotEmpty()) change(edit.copy(voicePreview = fragment))
    }

    fun addVoice() {
        val edit = _state.value?.takeIf { it.canChange } ?: return
        val fragment = edit.voicePreview ?: return
        val separator = if (edit.draft.isEmpty() || edit.draft.endsWith('\n')) "" else "\n"
        change(edit.copy(draft = edit.draft + separator + fragment, voicePreview = null))
    }

    fun discardVoice() {
        val edit = _state.value?.takeIf { it.canChange && it.voicePreview != null } ?: return
        change(edit.copy(voicePreview = null))
    }

    private fun change(edit: EditIdeaState, delayMillis: Long = 0) {
        _state.value = edit.copy(
            status = EditIdeaStatus.Editing,
            persistence = DraftPersistence.Pending,
            discardFailed = false
        )
        scheduleWrite(delayMillis)
    }

    fun flushDraft() {
        val edit = _state.value ?: return
        if (!edit.busy && (edit.persistence == DraftPersistence.Pending ||
                edit.persistence == DraftPersistence.Failed)
        ) {
            _state.value = edit.copy(persistence = DraftPersistence.Pending)
            scheduleWrite(0)
        }
    }

    private fun cancelScheduledWrite() {
        requestVersion++
        delayJob?.cancel()
        delayJob = null
    }

    private fun scheduleWrite(delayMillis: Long) {
        cancelScheduledWrite()
        val version = requestVersion
        delayJob = scope.launch(start = CoroutineStart.LAZY) {
            // Only the debounce is cancellable by later UI events. Once the write starts,
            // its returned session and revision must be observed before the next operation.
            delay(delayMillis)
            delayJob = null
            disk.withLock {
                val edit = _state.value ?: return@withLock
                if (version != requestVersion || edit.busy) return@withLock
                val result = try {
                    persist(edit)
                    persistenceStatus()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    DraftPersistence.Failed
                }
                if (version == requestVersion) {
                    _state.value = _state.value?.copy(persistence = result)
                }
            }
        }
        delayJob?.start()
    }

    private fun persistenceStatus(): DraftPersistence =
        if (persisted == null) DraftPersistence.None else DraftPersistence.Persisted

    private suspend fun persist(edit: EditIdeaState) {
        val current = persisted
        if (!edit.hasChanges) {
            if (current != null) check(store.discard(current.sessionId))
            persisted = null
        } else if (current == null) {
            persisted = checkNotNull(store.create(edit.ideaId, edit.originalText, edit.draft, edit.voicePreview))
        } else if (current.draft != edit.draft || current.voicePreview != edit.voicePreview) {
            persisted = checkNotNull(store.update(current, edit.draft, edit.voicePreview))
        }
    }

    fun cancel() {
        val edit = _state.value?.takeUnless { it.busy } ?: return
        if (edit.hasChanges || edit.persistence != DraftPersistence.None) {
            _state.value = edit.copy(confirmDiscard = true)
        } else {
            closeEditor()
        }
    }

    fun keepEditing() {
        _state.value = _state.value?.copy(confirmDiscard = false)
    }

    fun confirmDiscard() {
        val edit = _state.value?.takeIf { it.confirmDiscard && !it.busy } ?: return
        cancelScheduledWrite()
        _state.value = edit.copy(status = EditIdeaStatus.Discarding, confirmDiscard = false)
        scope.launch {
            disk.withLock {
                try {
                    persisted?.let { check(store.discard(it.sessionId)) }
                    closeEditor()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _state.value = edit.copy(
                        confirmDiscard = false,
                        discardFailed = true,
                        persistence = if (persisted?.draft == edit.draft &&
                            persisted?.voicePreview == edit.voicePreview
                        ) DraftPersistence.Persisted else DraftPersistence.Failed
                    )
                }
            }
        }
    }

    fun save() {
        val edit = _state.value?.takeIf { it.canChange && it.voicePreview == null && it.draft.isNotBlank() }
            ?: return
        cancelScheduledWrite()
        _state.value = edit.copy(status = EditIdeaStatus.Saving)
        scope.launch {
            disk.withLock {
                var lastVersionStored = false
                try {
                    persist(edit)
                    lastVersionStored = true
                    val saved = persisted
                    when (if (saved == null) IdeaDraftSaveResult.Saved else store.save(saved)) {
                        IdeaDraftSaveResult.Saved -> closeEditor()
                        IdeaDraftSaveResult.Conflict -> _state.value = edit.copy(
                            status = EditIdeaStatus.Conflict,
                            persistence = DraftPersistence.Persisted
                        )
                        IdeaDraftSaveResult.Stale, IdeaDraftSaveResult.NotReady ->
                            error("The persisted draft could not be saved")
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _state.value = edit.copy(
                        status = EditIdeaStatus.Failed,
                        persistence = if (lastVersionStored) persistenceStatus() else DraftPersistence.Failed
                    )
                }
            }
        }
    }

    private fun closeEditor() {
        cancelScheduledWrite()
        persisted = null
        _state.value = null
    }
}
