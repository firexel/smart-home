package com.seraph.smarthome.client.app

import android.app.Application
import android.content.Context
import com.seraph.smarthome.client.interactors.FacilityListInteractor
import com.seraph.smarthome.client.interactors.WidgetListInteractor
import com.seraph.smarthome.client.model.Facility
import com.seraph.smarthome.client.repositories.FacilityStorage
import com.seraph.smarthome.client.repositories.MqttNetworkRepository
import com.seraph.smarthome.util.Log

private val widgetListInteractorCache = mutableMapOf<String, WidgetListInteractor>()

class ClientApp : Application(), Services {

    override fun facilityListInteractor(): FacilityListInteractor {
        return FacilityListInteractor(
            FacilityStorage.getInstance(this),
            log.copy("FacilityListInteractor")
        )
    }

    override fun widgetListInteractor(facility: Facility): WidgetListInteractor {
        return synchronized(widgetListInteractorCache) {
            widgetListInteractorCache.getOrPut(facility.id) {
                createWidgetListInteractor(facility)
            }
        }
    }

    private fun createWidgetListInteractor(facility: Facility): WidgetListInteractor {
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

    override val log: Log = AdbLog()
}

val Context.services: Services
    get() {
        return applicationContext as Services
    }