package com.offlinenotes.utils

import android.util.Log
import com.offlinenotes.BuildConfig

object Logger {
    private val TAG = BuildConfig.LOG_TAG
    
    fun d(message: String) {
        if (BuildConfig.DEBUG_MODE) {
            Log.d(TAG, message)
        }
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
    
    fun i(message: String) {
        if (BuildConfig.DEBUG_MODE) {
            Log.i(TAG, message)
        }
    }
}
