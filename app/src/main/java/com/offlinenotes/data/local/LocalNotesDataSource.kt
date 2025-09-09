package com.offlinenotes.data.local

import android.content.Context
import com.offlinenotes.domain.model.Note
import com.offlinenotes.utils.AppConfig
import com.offlinenotes.utils.Logger
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.*

class LocalNotesDataSource(private val context: Context) {
    
    private val database = NotesDatabase.getDatabase(context)
    private val noteDao = database.noteDao()
    
    init {
        Logger.d("LocalNotesDataSource initialized")
        Logger.d("Database: $database")
        Logger.d("NoteDao: $noteDao")
    }
    
    // Папка для хранения markdown файлов
    private val pagesDir: File by lazy {
        File(context.filesDir, AppConfig.PAGES_FOLDER).apply {
            if (!exists()) {
                mkdirs()
                Logger.d("Created pages directory: $absolutePath")
            }
        }
    }
    
    // Database operations
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()
    
    suspend fun getNoteById(id: String): Note? {
        return try {
            Logger.d("Getting note by ID: $id")
            val note = noteDao.getNoteById(id)
            if (note != null) {
                Logger.d("Found note: ${note.title}")
            } else {
                Logger.e("Note not found with ID: $id")
            }
            note
        } catch (e: Exception) {
            Logger.e("Error getting note by ID: $id", e)
            null
        }
    }
    
    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
        Logger.d("Inserted note: ${note.title}")
    }
    
    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
        Logger.d("Updated note: ${note.title}")
    }
    
    suspend fun deleteNote(note: Note) {
        // Удаляем файл
        deleteMarkdownFile(note.fileName)
        // Удаляем запись из БД
        noteDao.deleteNote(note)
        Logger.d("Deleted note: ${note.title}")
    }
    
    suspend fun getNotesCount(): Int = noteDao.getNotesCount()
    
    // File operations
    fun saveMarkdownContent(fileName: String, content: String): Boolean {
        return try {
            val file = File(pagesDir, fileName)
            file.writeText(content)
            Logger.d("Saved markdown file: $fileName")
            true
        } catch (e: Exception) {
            Logger.e("Failed to save markdown file: $fileName", e)
            false
        }
    }
    
    fun readMarkdownContent(fileName: String): String? {
        return try {
            val file = File(pagesDir, fileName)
            Logger.d("Attempting to read file: ${file.absolutePath}")
            Logger.d("File exists: ${file.exists()}")
            Logger.d("File size: ${if (file.exists()) file.length() else 0} bytes")
            
            if (file.exists()) {
                val content = file.readText()
                Logger.d("Successfully read file content, length: ${content.length}")
                content
            } else {
                Logger.e("Markdown file not found: $fileName")
                Logger.e("Pages directory: ${pagesDir.absolutePath}")
                Logger.e("Looking for file: ${file.absolutePath}")
                null
            }
        } catch (e: Exception) {
            Logger.e("Failed to read markdown file: $fileName", e)
            null
        }
    }
    
    fun deleteMarkdownFile(fileName: String): Boolean {
        return try {
            val file = File(pagesDir, fileName)
            if (file.exists()) {
                file.delete()
            } else {
                true // Файл уже не существует
            }
        } catch (e: Exception) {
            Logger.e("Failed to delete markdown file: $fileName", e)
            false
        }
    }
    
    fun getFileSize(fileName: String): Long {
        return try {
            val file = File(pagesDir, fileName)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            Logger.e("Failed to get file size: $fileName", e)
            0L
        }
    }
    
    // Helper methods
    fun generateFileName(): String {
        return "${UUID.randomUUID()}.md"
    }
    
    fun generateNoteId(): String {
        return UUID.randomUUID().toString()
    }
}
