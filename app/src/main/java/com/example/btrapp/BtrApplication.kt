package com.example.btrapp

import android.app.Application
import android.content.Context

class BtrApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as BtrApplication).container
