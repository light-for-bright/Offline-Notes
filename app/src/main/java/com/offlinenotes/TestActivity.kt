package com.offlinenotes

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.offlinenotes.data.local.LocalNotesDataSource
import com.offlinenotes.utils.Logger
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {
    
    private lateinit var localDataSource: LocalNotesDataSource
    private lateinit var resultText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Создаем простой layout программно
        resultText = TextView(this).apply {
            text = "Testing database...\n"
            textSize = 14f
            setPadding(32, 32, 32, 32)
        }
        setContentView(resultText)
        
        localDataSource = LocalNotesDataSource(this)
        
        // Запускаем тесты
        runTests()
    }
    
    private fun runTests() {
        lifecycleScope.launch {
            try {
                appendText("=== Database Test Results ===\n\n")
                
                // Тест 1: Количество заметок
                val count = localDataSource.getNotesCount()
                appendText("1. Notes count: $count\n")
                
                // Тест 2: Получение всех заметок
                localDataSource.getAllNotes().collect { notes ->
                    appendText("2. Loaded ${notes.size} notes:\n")
                    notes.forEach { note ->
                        appendText("   - ${note.title} (${note.fileName})\n")
                    }
                }
                
                // Тест 3: Проверка файлов
                val filesDir = filesDir
                val pagesDir = java.io.File(filesDir, "pages")
                if (pagesDir.exists()) {
                    val files = pagesDir.listFiles()
                    appendText("\n3. Markdown files (${files?.size ?: 0}):\n")
                    files?.forEach { file ->
                        val content = file.readText()
                        appendText("   - ${file.name}: ${content.length} chars\n")
                    }
                } else {
                    appendText("\n3. Pages directory not found!\n")
                }
                
                appendText("\n=== Test completed ===\n")
                
            } catch (e: Exception) {
                appendText("ERROR: ${e.message}\n")
                Logger.e("Test failed", e)
            }
        }
    }
    
    private fun appendText(text: String) {
        runOnUiThread {
            resultText.text = resultText.text.toString() + text
        }
    }
}
