package com.ohuang.filemanager.server.util

import android.content.Context

object AppContext {
    lateinit var instance: Context
        private set

    fun init(context: Context) {
        instance = context.applicationContext
    }
}