package com.familyos.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

/** Data access for documents. */
@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE familyId = :familyId AND isDeleted = 0 AND (:type IS NULL OR type = :type) ORDER BY updatedAt DESC")
    fun observe(familyId: String, type: String?): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE familyId = :familyId AND isDeleted = 0 AND (:type IS NULL OR type = :type) ORDER BY updatedAt DESC")
    fun paging(familyId: String, type: String?): PagingSource<Int, DocumentEntity>

    @Query("""
        SELECT * FROM documents
        WHERE familyId = :familyId AND isDeleted = 0
          AND (title LIKE '%' || :query || '%' OR tagsCsv LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun search(familyId: String, query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DocumentEntity?

    @Upsert
    suspend fun upsert(entity: DocumentEntity)

    @Query("UPDATE documents SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)
}
