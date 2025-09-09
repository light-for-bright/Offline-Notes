package com.offlinenotes.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.offlinenotes.data.local.LocalNotesDataSource
import com.offlinenotes.data.remote.WebPageDataSource
import com.offlinenotes.data.repository.AddNoteResult
import com.offlinenotes.data.repository.NotesRepository
import com.offlinenotes.domain.model.Note
import com.offlinenotes.domain.model.UiState
import com.offlinenotes.utils.Logger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val localDataSource = LocalNotesDataSource(application)
    private val webPageDataSource = WebPageDataSource()
    private val repository = NotesRepository(localDataSource, webPageDataSource)

    private val _notesState = MutableLiveData<UiState<List<Note>>>()
    val notesState: LiveData<UiState<List<Note>>> = _notesState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _addNoteResult = MutableLiveData<AddNoteResult?>()
    val addNoteResult: LiveData<AddNoteResult?> = _addNoteResult

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _isLoading.value = true
            _notesState.value = UiState(isLoading = true)

            try {
                repository.getAllNotes()
                    .catch { exception ->
                        Logger.e("Error loading notes", exception)
                        _notesState.value = UiState(error = "Failed to load notes")
                        _isLoading.value = false
                    }
                    .collect { notes ->
                        _notesState.value = UiState(data = notes)
                        _isLoading.value = false
                        Logger.d("Loaded ${notes.size} notes")
                    }
            } catch (e: Exception) {
                Logger.e("Error in loadNotes", e)
                _notesState.value = UiState(error = "Failed to load notes")
                _isLoading.value = false
            }
        }
    }

    fun addNoteFromUrl(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _addNoteResult.value = null
            
            try {
                val result = repository.addNoteFromUrl(url)
                _addNoteResult.value = result
                
                when (result) {
                    is AddNoteResult.Success -> {
                        Logger.d("Successfully added note: ${result.note.title}")
                        // Автоматически обновляем список
                        loadNotes()
                    }
                    is AddNoteResult.Error -> {
                        Logger.e("Failed to add note: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Logger.e("Error adding note from URL", e)
                _addNoteResult.value = AddNoteResult.Error(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                repository.deleteNote(note)
                Logger.d("Deleted note: ${note.title}")
            } catch (e: Exception) {
                Logger.e("Error deleting note", e)
            }
        }
    }

    fun refreshNotes() {
        loadNotes()
    }

    fun clearAddNoteResult() {
        _addNoteResult.value = null
    }
}
