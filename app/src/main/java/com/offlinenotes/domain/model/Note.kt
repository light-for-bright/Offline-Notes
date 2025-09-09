package com.offlinenotes.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String,           // UUID
    val title: String,                    // Заголовок страницы
    val url: String,                      // Исходный URL
    val fileName: String,                 // Имя markdown файла
    val dateCreated: Long,                // Timestamp создания
    val dateModified: Long,               // Timestamp изменения
    val size: Long                        // Размер файла в байтах
)
