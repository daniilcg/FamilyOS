package com.familyos.feature.documents.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.DocumentType
import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.security.DocumentLockGate
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.documents.DeleteDocumentUseCase
import com.familyos.core.domain.usecase.documents.GetDocumentUseCase
import com.familyos.core.domain.usecase.documents.ImportDocumentUseCase
import com.familyos.core.domain.usecase.documents.ObserveDocumentsUseCase
import com.familyos.core.domain.usecase.documents.OpenDocumentStreamUseCase
import com.familyos.core.domain.usecase.documents.SearchDocumentsUseCase
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

/**
 * UI state for the documents vault feature.
 */
data class DocumentsUiState(
    val documents: List<FamilyDocument> = emptyList(),
    val selected: FamilyDocument? = null,
    val filterType: DocumentType? = null,
    val query: String = "",
    val isLoading: Boolean = true,
    val isUnlocked: Boolean = false,
    val pinConfigured: Boolean = false,
    val biometricEnabled: Boolean = false,
    val errorMessage: String? = null,
    val importSuccess: Boolean = false,
    val familyId: String? = null,
    val userId: String? = null,
)

/**
 * ViewModel for document list, detail, import, and vault lock screens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val observeDocuments: ObserveDocumentsUseCase,
    private val searchDocuments: SearchDocumentsUseCase,
    private val getDocument: GetDocumentUseCase,
    private val importDocument: ImportDocumentUseCase,
    private val deleteDocument: DeleteDocumentUseCase,
    private val openDocumentStream: OpenDocumentStreamUseCase,
    private val lockGate: DocumentLockGate,
    private val preferencesRepository: UserPreferencesRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentsUiState())
    val state: StateFlow<DocumentsUiState> = _state.asStateFlow()

    private val filterType = MutableStateFlow<DocumentType?>(null)
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            val familyId = prefs.activeFamilyId ?: user?.familyId
            _state.update {
                it.copy(
                    familyId = familyId,
                    userId = user?.id,
                    pinConfigured = lockGate.isPinConfigured(),
                    biometricEnabled = lockGate.isBiometricEnabled(),
                )
            }
        }

        viewModelScope.launch {
            combine(filterType, query, preferencesRepository.observe()) { type, q, prefs ->
                Triple(type, q, prefs.activeFamilyId)
            }.flatMapLatest { (type, q, familyId) ->
                if (familyId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else if (q.isBlank()) {
                    observeDocuments(familyId, type)
                } else {
                    searchDocuments(familyId, q).map { list ->
                        if (type == null) list else list.filter { it.type == type }
                    }
                }
            }.collect { docs ->
                _state.update { it.copy(documents = docs, isLoading = false, query = query.value, filterType = filterType.value) }
            }
        }
    }

    /** Updates type filter chips. */
    fun setFilter(type: DocumentType?) {
        filterType.value = type
    }

    /** Updates search query. */
    fun setQuery(value: String) {
        query.value = value
    }

    /** Loads detail for [documentId]. */
    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            when (val result = getDocument(documentId)) {
                is Result.Success -> _state.update { it.copy(selected = result.data, errorMessage = null) }
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    /** Configures a new vault PIN. */
    fun setupPin(pin: String) {
        viewModelScope.launch {
            runCatching { lockGate.setPin(pin) }
                .onSuccess {
                    _state.update { it.copy(pinConfigured = true, isUnlocked = true, errorMessage = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(errorMessage = e.message ?: "Failed to set PIN") }
                }
        }
    }

    /** Unlocks the vault with PIN. */
    fun unlockWithPin(pin: String) {
        viewModelScope.launch {
            val ok = lockGate.verifyPin(pin)
            _state.update {
                it.copy(
                    isUnlocked = ok,
                    errorMessage = if (ok) null else "Incorrect PIN",
                )
            }
        }
    }

    /** Called after a successful biometric prompt. */
    fun unlockWithBiometric() {
        _state.update { it.copy(isUnlocked = true, errorMessage = null) }
    }

    /** Enables or disables biometric unlock. */
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            lockGate.setBiometricEnabled(enabled)
            _state.update { it.copy(biometricEnabled = enabled) }
        }
    }

    /** Locks the vault again. */
    fun lock() {
        _state.update { it.copy(isUnlocked = false) }
    }

    /**
     * Imports a document from raw bytes (PDF/DOCX/JPG/PNG/WEBP).
     */
    fun import(
        title: String,
        type: DocumentType,
        mimeType: String,
        bytes: ByteArray,
        tags: List<String>,
    ) {
        val familyId = _state.value.familyId
        val userId = _state.value.userId
        if (familyId.isNullOrBlank() || userId.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "No active family") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, importSuccess = false) }
            when (
                val result = importDocument(
                    familyId = familyId,
                    title = title,
                    type = type,
                    mimeType = mimeType,
                    bytes = bytes,
                    uploadedBy = userId,
                    tags = tags,
                    encrypt = true,
                )
            ) {
                is Result.Success -> _state.update {
                    it.copy(isLoading = false, importSuccess = true, selected = result.data, errorMessage = null)
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    /** Deletes the selected or given document. */
    fun delete(documentId: String) {
        viewModelScope.launch {
            when (val result = deleteDocument(documentId)) {
                is Result.Success -> _state.update { it.copy(selected = null, errorMessage = null) }
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    /**
     * Opens a decrypted byte array for preview / export.
     */
    fun openDecrypted(documentId: String, onBytes: (ByteArray) -> Unit) {
        viewModelScope.launch {
            when (val result = openDocumentStream(documentId)) {
                is Result.Success -> {
                    val bytes = result.data.use { it.readBytes() }
                    onBytes(bytes)
                }
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    /** Clears transient error / success flags. */
    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, importSuccess = false) }
    }
}

private fun InputStream.readBytes(): ByteArray {
    val buffer = ByteArrayOutputStream()
    copyTo(buffer)
    return buffer.toByteArray()
}
