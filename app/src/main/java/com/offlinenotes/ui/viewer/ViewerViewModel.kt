package com.offlinenotes.ui.viewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.offlinenotes.data.local.LocalNotesDataSource
import com.offlinenotes.domain.model.Note
import com.offlinenotes.domain.model.UiState
import com.offlinenotes.utils.Logger
import kotlinx.coroutines.launch

class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val localDataSource = LocalNotesDataSource(application)
    
    init {
        Logger.d("ViewerViewModel initialized")
    }

    private val _noteState = MutableLiveData<UiState<Note>>()
    val noteState: LiveData<UiState<Note>> = _noteState

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            try {
                Logger.d("Loading note with ID: $noteId")
                _noteState.value = UiState(isLoading = true)

                val note = localDataSource.getNoteById(noteId)
                if (note != null) {
                    _noteState.value = UiState(data = note)
                    Logger.d("Successfully loaded note: ${note.title}")
                } else {
                    _noteState.value = UiState(error = "Note not found")
                    Logger.e("Note not found with ID: $noteId")
                }
            } catch (e: Exception) {
                Logger.e("Error loading note with ID: $noteId", e)
                _noteState.value = UiState(error = "Failed to load note: ${e.message}")
            }
        }
    }

    private val _markdownContent = MutableLiveData<String?>()
    val markdownContent: LiveData<String?> = _markdownContent

    fun getMarkdownContent(fileName: String) {
        viewModelScope.launch {
            try {
                Logger.d("Reading markdown content from file: $fileName")
                val content = localDataSource.readMarkdownContent(fileName)
                if (content != null) {
                    Logger.d("Successfully read markdown content, length: ${content.length}")
                    _markdownContent.value = content
                } else {
                    Logger.e("Markdown content is null for file: $fileName")
                    _markdownContent.value = null
                }
            } catch (e: Exception) {
                Logger.e("Error reading markdown content from file: $fileName", e)
                _markdownContent.value = null
            }
        }
    }
}
