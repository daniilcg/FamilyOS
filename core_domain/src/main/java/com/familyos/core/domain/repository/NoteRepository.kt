package com.familyos.core.domain.repository

import androidx.paging.PagingData
import com.familyos.core.domain.model.Note
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Notes persistence with search, pin, and archive support.
 */
interface NoteRepository {
    fun observeNotes(familyId: String, includeArchived: Boolean = false): Flow<List<Note>>
    fun pagingNotes(familyId: String, includeArchived: Boolean = false): Flow<PagingData<Note>>
    fun search(familyId: String, query: String, includeArchived: Boolean = false): Flow<List<Note>>
    suspend fun getById(id: String): Result<Note>
    suspend fun upsert(note: Note): Result<Note>
    suspend fun delete(id: String): Result<Unit>
    suspend fun setPinned(id: String, pinned: Boolean): Result<Note>
    suspend fun setArchived(id: String, archived: Boolean): Result<Note>
}
