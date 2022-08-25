package com.seraph.smarthome.client.interactors

import com.seraph.smarthome.client.model.FacilityModel
import com.seraph.smarthome.client.repositories.DiscoveryReceiver
import com.seraph.smarthome.client.repositories.FacilityDatabase
import com.seraph.smarthome.client.repositories.StoredFacility
import com.seraph.smarthome.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class FacilityListInteractor(
    private val database: FacilityDatabase,
    private val log: Log
) {

    suspend fun run(): Flow<List<FacilityModel>> {
        return database.facilityDao().getAll().map {
            it.map {
                FacilityModel(
                    it.id,
                    it.name,
                    it.cover,
                    it.brokerHost,
                    it.brokerPort,
                    it.brokerLogin,
                    it.brokerPassword
                )
            }
        }
    }

    suspend fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            DiscoveryReceiver().collectDiscoveryAdvertises(3000)
                .onCompletion { log.i("Finished collecting facilities") }
                .collect {
                    log.v("Got info about ${it.name}")
                    val facility = StoredFacility(
                        it.id,
                        it.name,
                        it.imageUrl,
                        it.brokerHost,
                        it.brokerPort,
                        it.brokerLogin,
                        it.brokerPassword
                    )
                    database.facilityDao().insertFacility(facility)
                }
        }
    }
}