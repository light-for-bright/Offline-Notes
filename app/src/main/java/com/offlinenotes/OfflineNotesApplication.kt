package com.offlinenotes

import android.app.Application
import com.offlinenotes.utils.Logger

class OfflineNotesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            Logger.d("Application started")
            // Здесь можно добавить инициализацию библиотек
        } catch (e: Exception) {
            Logger.e("Failed to initialize application", e)
        }
    }
}
