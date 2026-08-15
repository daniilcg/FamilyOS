package com.familyos.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.familyos.core.data.local.entity.ChecklistItemEntity
import com.familyos.core.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/** Data access for notes and checklist items. */
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE familyId = :familyId AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observe(familyId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE familyId = :familyId AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun paging(familyId: String): PagingSource<Int, NoteEntity>

    @Query("""
        SELECT * FROM notes
        WHERE familyId = :familyId AND isDeleted = 0
          AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' OR tagsCsv LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun search(familyId: String, query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Upsert
    suspend fun upsert(entity: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)

    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY orderIndex ASC")
    suspend fun getChecklist(noteId: String): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY orderIndex ASC")
    fun observeChecklist(noteId: String): Flow<List<ChecklistItemEntity>>

    @Upsert
    suspend fun upsertChecklistItems(items: List<ChecklistItemEntity>)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun clearChecklist(noteId: String)

    @Transaction
    suspend fun replaceChecklist(noteId: String, items: List<ChecklistItemEntity>) {
        clearChecklist(noteId)
        if (items.isNotEmpty()) upsertChecklistItems(items)
    }
}
