package com.seraph.smarthome.client.app

import android.app.Application
import android.content.Context
import com.seraph.smarthome.client.repositories.MqttNetworkRepository
import com.seraph.smarthome.client.repositories.NetworkRepository
import com.seraph.smarthome.util.Log

class ClientApp : Application(), Services {

    override lateinit var networkRepository: NetworkRepository
    override val log: Log = AdbLog()

    override fun onCreate() {
        super.onCreate()
        val options = MqttNetworkRepository.ConnectionOptions( // set to copernicus
                "192.168.1.202", 1883,
                MqttNetworkRepository.ConnectionOptions.Credentials(
                        "client", "2BpS3tMm5Q3ZXdv90Hxr"
                )
        )
        networkRepository = MqttNetworkRepository(options, log.copy("MqttNetworkRepository"))
    }
}

val Context.services: Services
    get() {
        return applicationContext as Services
    }