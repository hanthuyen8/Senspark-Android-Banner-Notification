package com.senspark.android.notification

import android.util.Log

class Logger(private val enableLog: Boolean) {
    init {
        if (enableLog) {
            instance = this
        }
    }

    companion object {
        @Volatile
        private var instance: Logger? = null
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: Logger(false).also { instance = it }
            }
    }

    fun log(message: String) {
        if (enableLog) {
            Log.d("Unity", "[Senspark-Android] $message")
        }
    }

    fun error(message: String) {
        if (enableLog) {
            Log.e("Unity", "[Senspark-Android] $message")
        }
    }
}