package com.seraph.smarthome.client.app

import android.app.Application
import android.content.Context
import com.seraph.smarthome.client.interactors.FacilityListInteractor
import com.seraph.smarthome.client.interactors.WidgetListInteractor
import com.seraph.smarthome.client.model.Facility
import com.seraph.smarthome.client.repositories.FacilityStorage
import com.seraph.smarthome.client.repositories.MqttNetworkRepository
import com.seraph.smarthome.util.Log

class ClientApp : Application(), Services {

    override fun facilityListInteractor(): FacilityListInteractor {
        return FacilityListInteractor(
            FacilityStorage.getInstance(this),
            log.copy("FacilityListInteractor")
        )
    }

    private val widgetListInteractorCache = mutableMapOf<String, WidgetListInteractor>()
    override fun widgetListInteractor(facility: Facility): WidgetListInteractor {
        return widgetListInteractorCache.getOrPut(facility.id) {
            val creds = if (facility.brokerLogin != null && facility.brokerPassword != null) {
                MqttNetworkRepository.ConnectionOptions.Credentials(
                    facility.brokerLogin, facility.brokerPassword
                )
            } else {
                null
            }
            val options = MqttNetworkRepository.ConnectionOptions(
                facility.brokerHost, facility.brokerPort, creds
            )
            val repo = MqttNetworkRepository(
                options,
                log.copy("MqttNetworkRepository[${facility.id}]")
            )
            return WidgetListInteractor(
                repo,
                log.copy("WidgetListInteractor[${facility.id}]")
            )
        }
    }

    override val log: Log = AdbLog()

    override fun onCreate() {
        super.onCreate()
        val options = MqttNetworkRepository.ConnectionOptions( // set to copernicus
            "192.168.0.64", 1883,
            MqttNetworkRepository.ConnectionOptions.Credentials(
                "client", "2BpS3tMm5Q3ZXdv90Hxr"
            )
        )
        MqttNetworkRepository(options, log.copy("MqttNetworkRepository"))
    }
}

val Context.services: Services
    get() {
        return applicationContext as Services
    }