package com.tora_tech.smssync

import android.app.Application
import com.tora_tech.smssync.data.ConfigStore

/** Holds process-wide singletons (just the config store for now). */
class SmsSyncApplication : Application() {

    val configStore: ConfigStore by lazy { ConfigStore(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SmsSyncApplication
            private set
    }
}
