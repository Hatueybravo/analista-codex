package com.analista.fileorganizer.data.local

import androidx.room.*
import com.analista.fileorganizer.data.model.FileCategory
import com.analista.fileorganizer.data.model.FileItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Query("SELECT * FROM files ORDER BY lastModified DESC")
    fun getAllFiles(): Flow<List<FileItem>>

    @Query("SELECT * FROM files WHERE category = :category ORDER BY lastModified DESC")
    fun getFilesByCategory(category: FileCategory): Flow<List<FileItem>>

    @Query("SELECT * FROM files WHERE isOrganized = 0 ORDER BY lastModified DESC")
    fun getUnorganizedFiles(): Flow<List<FileItem>>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' OR path LIKE '%' || :query || '%'")
    fun searchFiles(query: String): Flow<List<FileItem>>

    @Query("SELECT * FROM files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): FileItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileItem>)

    @Update
    suspend fun updateFile(file: FileItem)

    @Delete
    suspend fun deleteFile(file: FileItem)

    @Query("DELETE FROM files")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM files WHERE category = :category")
    suspend fun countByCategory(category: FileCategory): Int

    @Query("SELECT COUNT(*) FROM files")
    suspend fun totalCount(): Int
}
