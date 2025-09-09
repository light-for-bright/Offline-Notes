package com.offlinenotes.ui.share

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.offlinenotes.databinding.ActivityShareBinding
import com.offlinenotes.data.repository.AddNoteResult
import com.offlinenotes.utils.Logger

class ShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareBinding
    private lateinit var viewModel: ShareViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        observeViewModel()
        handleShareIntent()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ShareViewModel::class.java]
    }

    private fun observeViewModel() {
        viewModel.addNoteResult.observe(this) { result ->
            result?.let {
                when (it) {
                    is AddNoteResult.Success -> {
                        showSuccessDialog(it.note.title)
                    }
                    is AddNoteResult.Error -> {
                        showErrorDialog(it.message)
                    }
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.statusText.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun handleShareIntent() {
        val intent = intent
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                handleTextShare(intent)
            } else {
                showErrorDialog("Unsupported content type: $type")
            }
        } else {
            showErrorDialog("Invalid share intent")
        }
    }

    private fun handleTextShare(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText.isNullOrBlank()) {
            showErrorDialog("No URL provided")
            return
        }

        val url = extractUrl(sharedText)
        if (url == null) {
            showErrorDialog("No valid URL found in shared text")
            return
        }

        Logger.d("Processing shared URL: $url")
        viewModel.addNoteFromUrl(url)
    }

    private fun extractUrl(text: String): String? {
        // Ищем URL в тексте
        val urlPattern = Regex("https?://[^\\s]+")
        val match = urlPattern.find(text)
        return match?.value
    }

    private fun showSuccessDialog(title: String) {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("Note '$title' has been saved successfully!")
            .setPositiveButton("View Notes") { _, _ ->
                openMainActivity()
            }
            .setNegativeButton("Close") { _, _ ->
                finish()
            }
            .setOnDismissListener {
                finish()
            }
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Try Again") { _, _ ->
                // Показываем диалог для ручного ввода URL
                showManualUrlDialog()
            }
            .setNegativeButton("Close") { _, _ ->
                finish()
            }
            .setOnDismissListener {
                finish()
            }
            .show()
    }

    private fun showManualUrlDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter URL"
            setText("https://")
        }

        AlertDialog.Builder(this)
            .setTitle("Add Web Page")
            .setMessage("Enter the URL of the web page you want to save:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    viewModel.addNoteFromUrl(url)
                } else {
                    showErrorDialog("Please enter a valid URL")
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }

    private fun openMainActivity() {
        val intent = Intent(this, com.offlinenotes.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
