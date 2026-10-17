package com.broll.poc

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BrollApplication : Application() {
    val applicationScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    var startupFailure: Throwable? = null
        private set

    override fun attachBaseContext(base: Context) {
        CrashGuard.install(base)
        val localized = try { LocaleStore.wrap(base) } catch (error: Throwable) {
            startupFailure = error
            base
        }
        super.attachBaseContext(localized)
    }

    override fun onCreate() {
        super.onCreate()
        try { EngineHolder.init(this) } catch (error: Throwable) { startupFailure = error }
    }
}
