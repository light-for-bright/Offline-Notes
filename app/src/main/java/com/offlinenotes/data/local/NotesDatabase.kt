package com.offlinenotes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.offlinenotes.domain.model.Note
import com.offlinenotes.utils.AppConfig
import com.offlinenotes.utils.Logger

@Database(
    entities = [Note::class],
    version = 1,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {
    
    abstract fun noteDao(): NoteDao
    
    companion object {
        @Volatile
        private var INSTANCE: NotesDatabase? = null
        
        fun getDatabase(context: Context): NotesDatabase {
            return INSTANCE ?: synchronized(this) {
                Logger.d("Creating new database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    AppConfig.DATABASE_NAME
                ).build()
                INSTANCE = instance
                Logger.d("Database instance created successfully")
                instance
            }
        }
    }
}
