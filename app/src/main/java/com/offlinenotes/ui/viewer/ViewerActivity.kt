package com.offlinenotes.ui.viewer

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.offlinenotes.databinding.ActivityViewerBinding
import com.offlinenotes.domain.model.Note
import com.offlinenotes.utils.Logger

class ViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewerBinding
    private lateinit var viewModel: ViewerViewModel
    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Logger.d("ViewerActivity onCreate started")
            binding = ActivityViewerBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Logger.d("ViewerActivity layout set")

            setupToolbar()
            setupWebView()
            setupViewModel()
            getNoteFromIntent()
            observeViewModel()
            Logger.d("ViewerActivity onCreate completed successfully")
        } catch (e: Exception) {
            Logger.e("Error in ViewerActivity onCreate", e)
            showError("Failed to initialize viewer: ${e.message}")
        }
    }

    private fun setupToolbar() {
        try {
            Logger.d("Setting up toolbar")
            // Не устанавливаем Toolbar как SupportActionBar, используем его напрямую
            binding.toolbar.setNavigationOnClickListener {
                Logger.d("Toolbar navigation clicked")
                finish()
            }
            Logger.d("Toolbar setup completed")
        } catch (e: Exception) {
            Logger.e("Error setting up toolbar", e)
            throw e
        }
    }

    private fun setupWebView() {
        try {
            Logger.d("Setting up WebView")
            binding.webView.apply {
                settings.apply {
                    javaScriptEnabled = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        binding.progressBar.visibility = View.GONE
                        Logger.d("WebView page finished loading")
                    }
                }
            }
            Logger.d("WebView setup completed")
        } catch (e: Exception) {
            Logger.e("Error setting up WebView", e)
            throw e
        }
    }

    private fun setupViewModel() {
        try {
            Logger.d("Setting up ViewerViewModel")
            viewModel = ViewModelProvider(this)[ViewerViewModel::class.java]
            Logger.d("ViewerViewModel created successfully")
        } catch (e: Exception) {
            Logger.e("Failed to create ViewerViewModel", e)
            showError("Failed to initialize viewer")
        }
    }

    private fun getNoteFromIntent() {
        try {
            Logger.d("Getting note from intent")
            noteId = intent.getStringExtra(EXTRA_NOTE_ID)
            Logger.d("Note ID from intent: $noteId")
            
            if (noteId == null) {
                Logger.e("No note ID provided in intent")
                showError("Invalid note")
                return
            }
            
            Logger.d("Loading note with ID: $noteId")
            viewModel.loadNote(noteId!!)
        } catch (e: Exception) {
            Logger.e("Error getting note from intent", e)
            showError("Error loading note: ${e.message}")
        }
    }

    private fun observeViewModel() {
        try {
            Logger.d("Setting up ViewModel observer")
            viewModel.noteState.observe(this) { state ->
                Logger.d("Note state changed: isLoading=${state.isLoading}, error=${state.error}, data=${state.data?.title}")
                when {
                    state.isLoading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.webView.visibility = View.GONE
                        binding.errorView.visibility = View.GONE
                    }
                    state.error != null -> {
                        binding.progressBar.visibility = View.GONE
                        binding.webView.visibility = View.GONE
                        binding.errorView.visibility = View.VISIBLE
                        binding.errorView.text = state.error
                    }
                    state.data != null -> {
                        binding.progressBar.visibility = View.GONE
                        binding.webView.visibility = View.VISIBLE
                        binding.errorView.visibility = View.GONE
                        displayNote(state.data)
                    }
                }
            }
            
            viewModel.markdownContent.observe(this) { content ->
                Logger.d("Markdown content changed: ${content?.length ?: 0} characters")
                if (content != null) {
                    try {
                        val htmlContent = convertMarkdownToHtml(content)
                        binding.webView.loadDataWithBaseURL(
                            null,
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                        Logger.d("Successfully loaded markdown content into WebView")
                    } catch (e: Exception) {
                        Logger.e("Error loading markdown content into WebView", e)
                        showError("Error displaying content: ${e.message}")
                    }
                } else {
                    Logger.e("Markdown content is null")
                    showError("Failed to load note content")
                }
            }
            
            Logger.d("ViewModel observer setup completed")
        } catch (e: Exception) {
            Logger.e("Error setting up ViewModel observer", e)
            showError("Failed to setup observer: ${e.message}")
        }
    }

    private fun displayNote(note: Note) {
        binding.toolbar.title = note.title
        
        try {
            Logger.d("Loading markdown content for file: ${note.fileName}")
            viewModel.getMarkdownContent(note.fileName)
        } catch (e: Exception) {
            Logger.e("Error displaying note: ${note.title}", e)
            showError("Error loading note: ${e.message}")
        }
    }

    private fun convertMarkdownToHtml(markdown: String): String {
        return try {
            Logger.d("Converting markdown to HTML, length: ${markdown.length}")
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        color: #2c3e50;
                        margin-top: 24px;
                        margin-bottom: 16px;
                    }
                    h1 { font-size: 2em; border-bottom: 1px solid #eaecef; padding-bottom: 10px; }
                    h2 { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 8px; }
                    h3 { font-size: 1.25em; }
                    p { margin-bottom: 16px; }
                    code {
                        background-color: #f6f8fa;
                        padding: 2px 4px;
                        border-radius: 3px;
                        font-family: 'SFMono-Regular', Consolas, monospace;
                    }
                    pre {
                        background-color: #f6f8fa;
                        padding: 16px;
                        border-radius: 6px;
                        overflow-x: auto;
                    }
                    pre code {
                        background-color: transparent;
                        padding: 0;
                    }
                    blockquote {
                        border-left: 4px solid #dfe2e5;
                        padding-left: 16px;
                        color: #6a737d;
                        margin: 16px 0;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin: 16px 0;
                    }
                    th, td {
                        border: 1px solid #dfe2e5;
                        padding: 8px 12px;
                        text-align: left;
                    }
                    th {
                        background-color: #f6f8fa;
                        font-weight: 600;
                    }
                    a {
                        color: #0366d6;
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    img {
                        max-width: 100%;
                        height: auto;
                    }
                    ul, ol {
                        padding-left: 24px;
                        margin-bottom: 16px;
                    }
                    li {
                        margin-bottom: 4px;
                    }
                </style>
            </head>
            <body>
                ${markdownToHtml(markdown)}
            </body>
            </html>
        """.trimIndent()
        } catch (e: Exception) {
            Logger.e("Error converting markdown to HTML", e)
            "<html><body><p>Error converting content: ${e.message}</p></body></html>"
        }
    }

    private fun markdownToHtml(markdown: String): String {
        return try {
            Logger.d("Converting markdown to HTML, input length: ${markdown.length}")
            // Простая конвертация Markdown в HTML
            val result = markdown
            .replace(Regex("^# (.+)$", RegexOption.MULTILINE), "<h1>$1</h1>")
            .replace(Regex("^## (.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
            .replace(Regex("^### (.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
            .replace(Regex("^#### (.+)$", RegexOption.MULTILINE), "<h4>$1</h4>")
            .replace(Regex("^##### (.+)$", RegexOption.MULTILINE), "<h5>$1</h5>")
            .replace(Regex("^###### (.+)$", RegexOption.MULTILINE), "<h6>$1</h6>")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
            .replace(Regex("\\*(.+?)\\*"), "<em>$1</em>")
            .replace(Regex("`(.+?)`"), "<code>$1</code>")
            .replace(Regex("^> (.+)$", RegexOption.MULTILINE), "<blockquote>$1</blockquote>")
            .replace(Regex("^\\- (.+)$", RegexOption.MULTILINE), "<li>$1</li>")
            .replace(Regex("^\\d+\\. (.+)$", RegexOption.MULTILINE), "<li>$1</li>")
            .replace(Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)"), "<a href=\"$2\">$1</a>")
            .replace(Regex("!\\[([^\\]]*)\\]\\(([^)]+)\\)"), "<img src=\"$2\" alt=\"$1\">")
            .replace(Regex("^---$", RegexOption.MULTILINE), "<hr>")
            .replace(Regex("\n\n"), "</p><p>")
            .let { "<p>$it</p>" }
            .replace("<p></p>", "")
            Logger.d("Markdown conversion completed, result length: ${result.length}")
            result
        } catch (e: Exception) {
            Logger.e("Error in markdownToHtml", e)
            "<p>Error converting markdown: ${e.message}</p>"
        }
    }

    private fun showError(message: String) {
        try {
            Logger.e("Showing error: $message")
            binding.errorView.text = message
            binding.errorView.visibility = View.VISIBLE
            binding.webView.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
        } catch (e: Exception) {
            Logger.e("Error showing error message", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        try {
            Logger.d("Navigating up, finishing activity")
            finish()
            return true
        } catch (e: Exception) {
            Logger.e("Error navigating up", e)
            return false
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }
}
