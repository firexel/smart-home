package com.seraph.smarthome.client.app

import android.app.Application

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
class ClientApp : Application() {
    lateinit var presenterFactory:PresenterFactory

    override fun onCreate() {
        super.onCreate()
        presenterFactory = PresenterFactory(this)
    }
}