package com.veil

import android.app.Application
import com.veil.di.AppContainer

/**
 * VeilApp — Application class.
 *
 * Creates AppContainer once for the whole app lifetime.
 * Access anywhere via: (context.applicationContext as VeilApp).container
 */
class VeilApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
