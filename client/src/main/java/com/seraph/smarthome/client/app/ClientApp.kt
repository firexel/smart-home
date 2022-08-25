package com.seraph.smarthome.client.app

import android.app.Application
import android.content.Context
import com.seraph.smarthome.client.repositories.DiscoveryReceiver
import com.seraph.smarthome.client.repositories.MqttNetworkRepository
import com.seraph.smarthome.client.repositories.NetworkRepository
import com.seraph.smarthome.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class ClientApp : Application(), Services {

    override lateinit var networkRepository: NetworkRepository
    override val log: Log = AdbLog()

    override fun onCreate() {
        super.onCreate()
        val options = MqttNetworkRepository.ConnectionOptions( // set to copernicus
            "192.168.0.64", 1883,
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