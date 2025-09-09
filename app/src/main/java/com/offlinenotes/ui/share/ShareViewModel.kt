package com.offlinenotes.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.offlinenotes.data.local.LocalNotesDataSource
import com.offlinenotes.data.remote.WebPageDataSource
import com.offlinenotes.data.repository.AddNoteResult
import com.offlinenotes.data.repository.NotesRepository
import com.offlinenotes.utils.Logger
import kotlinx.coroutines.launch

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val localDataSource = LocalNotesDataSource(application)
    private val webPageDataSource = WebPageDataSource()
    private val repository = NotesRepository(localDataSource, webPageDataSource)

    private val _addNoteResult = MutableLiveData<AddNoteResult?>()
    val addNoteResult: LiveData<AddNoteResult?> = _addNoteResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun addNoteFromUrl(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _addNoteResult.value = null
            
            try {
                Logger.d("ShareViewModel: Adding note from URL: $url")
                val result = repository.addNoteFromUrl(url)
                _addNoteResult.value = result
                
                when (result) {
                    is AddNoteResult.Success -> {
                        Logger.d("ShareViewModel: Successfully added note: ${result.note.title}")
                    }
                    is AddNoteResult.Error -> {
                        Logger.e("ShareViewModel: Failed to add note: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Logger.e("ShareViewModel: Error adding note from URL", e)
                _addNoteResult.value = AddNoteResult.Error(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
