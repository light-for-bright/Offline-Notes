package com.offlinenotes.data.repository

import com.offlinenotes.data.local.LocalNotesDataSource
import com.offlinenotes.data.remote.WebPageDataSource
import com.offlinenotes.domain.model.Note
import com.offlinenotes.utils.HtmlToMarkdownConverter
import com.offlinenotes.utils.Logger
import kotlinx.coroutines.flow.Flow

class NotesRepository(
    private val localDataSource: LocalNotesDataSource,
    private val webPageDataSource: WebPageDataSource
) {
    
    private val htmlToMarkdownConverter = HtmlToMarkdownConverter()

    // Local operations
    fun getAllNotes(): Flow<List<Note>> = localDataSource.getAllNotes()
    
    suspend fun getNoteById(id: String): Note? = localDataSource.getNoteById(id)
    
    suspend fun deleteNote(note: Note) = localDataSource.deleteNote(note)
    
    suspend fun getNotesCount(): Int = localDataSource.getNotesCount()
    
    fun readMarkdownContent(fileName: String): String? = 
        localDataSource.readMarkdownContent(fileName)

    // Web page operations
    suspend fun addNoteFromUrl(url: String): AddNoteResult {
        return try {
            Logger.d("Starting to add note from URL: $url")
            
            // Валидация URL
            if (!webPageDataSource.isValidUrl(url)) {
                return AddNoteResult.Error("Invalid URL format")
            }
            
            // Загрузка веб-страницы
            val webPageResult = webPageDataSource.fetchWebPage(url)
            if (!webPageResult.success) {
                return AddNoteResult.Error(webPageResult.error ?: "Failed to load page")
            }
            
            // Конвертация HTML в Markdown
            val markdownContent = htmlToMarkdownConverter.convert(webPageResult.html)
            if (markdownContent.isBlank()) {
                return AddNoteResult.Error("Failed to convert page content")
            }
            
            // Создание заметки
            val noteId = localDataSource.generateNoteId()
            val fileName = localDataSource.generateFileName()
            val currentTime = System.currentTimeMillis()
            
            val note = Note(
                id = noteId,
                title = webPageResult.title,
                url = url,
                fileName = fileName,
                dateCreated = currentTime,
                dateModified = currentTime,
                size = markdownContent.length.toLong()
            )
            
            // Сохранение markdown файла
            val fileSaved = localDataSource.saveMarkdownContent(fileName, markdownContent)
            if (!fileSaved) {
                return AddNoteResult.Error("Failed to save markdown file")
            }
            
            // Сохранение в БД
            localDataSource.insertNote(note)
            
            Logger.d("Successfully added note: ${note.title}")
            AddNoteResult.Success(note)
            
        } catch (e: Exception) {
            Logger.e("Error adding note from URL: $url", e)
            AddNoteResult.Error(e.message ?: "Unknown error occurred")
        }
    }
}

sealed class AddNoteResult {
    data class Success(val note: Note) : AddNoteResult()
    data class Error(val message: String) : AddNoteResult()
}
