package com.familyos.feature.notes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.Note
import com.familyos.core.domain.model.NoteChecklistItem
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.notes.DeleteNoteUseCase
import com.familyos.core.domain.usecase.notes.GetNoteUseCase
import com.familyos.core.domain.usecase.notes.ObserveNotesUseCase
import com.familyos.core.domain.usecase.notes.SearchNotesUseCase
import com.familyos.core.domain.usecase.notes.SetNoteArchivedUseCase
import com.familyos.core.domain.usecase.notes.SetNotePinnedUseCase
import com.familyos.core.domain.usecase.notes.UpsertNoteUseCase
import com.familyos.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** UI state for notes list and editor. */
data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val selected: Note? = null,
    val query: String = "",
    val showArchived: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val familyId: String? = null,
    val userId: String? = null,
)

/**
 * ViewModel for notes list, search, archive, and editor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val observeNotes: ObserveNotesUseCase,
    private val searchNotes: SearchNotesUseCase,
    private val getNote: GetNoteUseCase,
    private val upsertNote: UpsertNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val setPinned: SetNotePinnedUseCase,
    private val setArchived: SetNoteArchivedUseCase,
    private val preferencesRepository: UserPreferencesRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state.asStateFlow()

    private val query = MutableStateFlow("")
    private val showArchived = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            _state.update {
                it.copy(
                    familyId = prefs.activeFamilyId ?: user?.familyId,
                    userId = user?.id,
                )
            }
        }

        viewModelScope.launch {
            combine(query, showArchived, preferencesRepository.observe()) { q, archived, prefs ->
                Triple(q, archived, prefs.activeFamilyId)
            }.flatMapLatest { (q, archived, familyId) ->
                if (familyId.isNullOrBlank()) flowOf(emptyList())
                else if (q.isBlank()) observeNotes(familyId, archived)
                else searchNotes(familyId, q, archived)
            }.collect { notes ->
                _state.update {
                    it.copy(
                        notes = notes.sortedWith(
                            compareByDescending<Note> { n -> n.isPinned }.thenByDescending { n -> n.updatedAt },
                        ),
                        isLoading = false,
                        query = query.value,
                        showArchived = showArchived.value,
                    )
                }
            }
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setShowArchived(value: Boolean) {
        showArchived.value = value
    }

    fun loadNote(id: String) {
        viewModelScope.launch {
            when (val result = getNote(id)) {
                is Result.Success -> _state.update { it.copy(selected = result.data, errorMessage = null) }
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun startNewNote() {
        val familyId = _state.value.familyId ?: return
        val userId = _state.value.userId ?: return
        val now = System.currentTimeMillis()
        _state.update {
            it.copy(
                selected = Note(
                    id = "",
                    familyId = familyId,
                    title = "",
                    body = "",
                    createdBy = userId,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun updateDraft(
        title: String? = null,
        body: String? = null,
        tags: List<String>? = null,
        photoUrls: List<String>? = null,
        checklist: List<NoteChecklistItem>? = null,
        colorHex: String? = null,
    ) {
        _state.update { s ->
            val draft = s.selected ?: return@update s
            s.copy(
                selected = draft.copy(
                    title = title ?: draft.title,
                    body = body ?: draft.body,
                    tags = tags ?: draft.tags,
                    photoUrls = photoUrls ?: draft.photoUrls,
                    checklist = checklist ?: draft.checklist,
                    colorHex = colorHex ?: draft.colorHex,
                ),
            )
        }
    }

    fun addChecklistItem(text: String) {
        val draft = _state.value.selected ?: return
        val item = NoteChecklistItem(
            id = UUID.randomUUID().toString(),
            text = text,
            order = draft.checklist.size,
        )
        updateDraft(checklist = draft.checklist + item)
    }

    fun toggleChecklistItem(id: String) {
        val draft = _state.value.selected ?: return
        updateDraft(
            checklist = draft.checklist.map {
                if (it.id == id) it.copy(isChecked = !it.isChecked) else it
            },
        )
    }

    fun save() {
        val draft = _state.value.selected ?: return
        viewModelScope.launch {
            when (val result = upsertNote(draft.copy(updatedBy = _state.value.userId))) {
                is Result.Success -> _state.update { it.copy(selected = result.data, errorMessage = null) }
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val result = deleteNote(id)) {
                is Result.Success -> _state.update { it.copy(selected = null) }
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun togglePin(id: String, pinned: Boolean) {
        viewModelScope.launch { setPinned(id, pinned) }
    }

    fun toggleArchive(id: String, archived: Boolean) {
        viewModelScope.launch { setArchived(id, archived) }
    }
}
