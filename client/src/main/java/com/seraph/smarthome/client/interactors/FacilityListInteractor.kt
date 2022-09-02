package com.seraph.smarthome.client.interactors

import com.seraph.smarthome.client.model.Facility
import com.seraph.smarthome.client.repositories.DiscoveryReceiver
import com.seraph.smarthome.client.repositories.FacilityStorage
import com.seraph.smarthome.client.repositories.StoredFacility
import com.seraph.smarthome.device.validate
import com.seraph.smarthome.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FacilityListInteractor(
    private val storage: FacilityStorage,
    private val log: Log
) {

    private val _currentFacility = MutableStateFlow<String?>(storage.currentFacilityId )
    val currentFacility: Flow<String?> = _currentFacility.asStateFlow()

    suspend fun run(): Flow<List<Facility>> {
        return storage.database.facilityDao().getAll().map {
            it.map {
                Facility(
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

    fun setCurrent(facility: Facility) {
        storage.currentFacilityId = facility.id
        _currentFacility.value = facility.id
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
                    storage.database.facilityDao().insertFacility(facility)
                }
        }
    }
}