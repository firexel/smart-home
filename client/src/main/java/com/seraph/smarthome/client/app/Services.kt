package com.seraph.smarthome.client.app

import com.seraph.smarthome.client.interactors.FacilityListInteractor
import com.seraph.smarthome.client.interactors.WidgetListInteractor
import com.seraph.smarthome.client.model.Facility
import com.seraph.smarthome.client.repositories.NetworkRepository
import com.seraph.smarthome.util.Log

interface Services {

    fun facilityListInteractor(): FacilityListInteractor
    fun widgetListInteractor(facility: Facility): WidgetListInteractor

    val log: Log
}