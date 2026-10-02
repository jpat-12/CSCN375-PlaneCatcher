package com.planecatcher

import android.app.Application
import com.planecatcher.notify.AlertCoordinator
import com.planecatcher.notify.Notifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PlaneCatcherApp : Application() {
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var alerts: AlertCoordinator

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        alerts.start()
    }
}
