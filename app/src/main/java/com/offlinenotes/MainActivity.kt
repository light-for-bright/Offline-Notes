package com.offlinenotes

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.offlinenotes.databinding.ActivityMainBinding
import com.offlinenotes.databinding.DialogAddUrlBinding
import com.offlinenotes.domain.model.Note
import com.offlinenotes.ui.main.MainViewModel
import com.offlinenotes.ui.main.NoteAdapter
import com.offlinenotes.ui.viewer.ViewerActivity
import com.offlinenotes.utils.Logger

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
        } catch (e: Exception) {
            Logger.e("Failed to initialize MainActivity", e)
            Toast.makeText(this, getString(R.string.error_initialization_app), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setupRecyclerView()
        setupViewModel()
        setupFab()
        setupGithubLink()
        observeViewModel()
        
        // Тестируем функциональность
        testAppFunctionality()
    }

    private fun setupToolbar() {
        try {
            // Не устанавливаем Toolbar как SupportActionBar, используем его напрямую
            Logger.d("Toolbar setup completed (using directly)")
        } catch (e: Exception) {
            Logger.e("Error setting up toolbar", e)
        }
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            onNoteClick = { note ->
                openNoteViewer(note)
            },
            onNoteLongClick = { note ->
                showDeleteDialog(note)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
        }
    }

    private fun setupViewModel() {
        try {
            viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        } catch (e: Exception) {
            Logger.e("Failed to initialize ViewModel", e)
            Toast.makeText(this, getString(R.string.error_initialization_data), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            showAddUrlDialog()
        }
    }

    private fun setupGithubLink() {
        binding.githubLink.setOnClickListener {
            openGithubRepository()
        }
    }

    private fun observeViewModel() {
        viewModel.notesState.observe(this) { state ->
            when {
                state.isLoading -> {
                    // Показываем индикатор загрузки если нужно
                }
                state.error != null -> {
                    Toast.makeText(this, state.error, Toast.LENGTH_LONG).show()
                }
                state.data != null -> {
                    noteAdapter.submitList(state.data)
                    updateEmptyView(state.data.isEmpty())
                }
            }
        }

        viewModel.addNoteResult.observe(this) { result ->
            result?.let {
                when (it) {
                    is com.offlinenotes.data.repository.AddNoteResult.Success -> {
                        showToast(getString(R.string.note_added, it.note.title))
                    }
                    is com.offlinenotes.data.repository.AddNoteResult.Error -> {
                        showToast(getString(R.string.error_adding_note, it.message))
                    }
                }
                viewModel.clearAddNoteResult()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.fab.isEnabled = !isLoading
            if (isLoading) {
                binding.fab.setImageResource(android.R.drawable.ic_popup_sync)
            } else {
                binding.fab.setImageResource(android.R.drawable.ic_input_add)
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        // Показываем GitHub ссылку всегда
        binding.githubLink.visibility = View.VISIBLE
    }

    private fun showAddUrlDialog() {
        val dialogBinding = DialogAddUrlBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.add_note)) { _, _ ->
                val url = dialogBinding.urlInput.text.toString().trim()
                if (url.isNotEmpty()) {
                    // Простая валидация URL
                    val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                        url
                    } else {
                        // Автоматически добавляем https:// если не указан протокол
                        "https://$url"
                    }
                    addNoteFromUrl(finalUrl)
                } else {
                    showToast(getString(R.string.please_enter_url))
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        
        dialog.show()
        
        // Фокус на поле ввода
        dialogBinding.urlInput.requestFocus()
    }

    private fun showDeleteDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_note))
            .setMessage(getString(R.string.delete_confirm, note.title))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteNote(note)
                showToast(getString(R.string.note_deleted))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun addNoteFromUrl(url: String) {
        Logger.d("User wants to add note from URL: $url")
        viewModel.addNoteFromUrl(url)
    }

    private fun openNoteViewer(note: Note) {
        try {
            Logger.d("Opening note viewer for: ${note.title}")
            Logger.d("Note ID: ${note.id}")
            Logger.d("Note fileName: ${note.fileName}")
            
            val intent = Intent(this, ViewerActivity::class.java).apply {
                putExtra(ViewerActivity.EXTRA_NOTE_ID, note.id)
            }
            startActivity(intent)
            Logger.d("Successfully started ViewerActivity")
        } catch (e: Exception) {
            Logger.e("Error opening note viewer for: ${note.title}", e)
            showToast(getString(R.string.error_opening_note, e.message ?: "Unknown error"))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun openGithubRepository() {
        try {
            val githubUrl = "https://github.com/light-for-bright/Offline-Notes"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
            startActivity(intent)
            Logger.d("Opening GitHub repository: $githubUrl")
        } catch (e: Exception) {
            Logger.e("Failed to open GitHub repository", e)
            showToast(getString(R.string.failed_open_github))
        }
    }

    private fun testAppFunctionality() {
        // Простой тест без сетевых запросов
        Logger.d("Testing app functionality - simple version")
        
        // Тестируем только UI без загрузки веб-страниц
        binding.root.postDelayed({
            showToast(getString(R.string.app_working))
        }, 2000L)
    }
}
