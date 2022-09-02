package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.model.WidgetGroupModel
import kotlinx.coroutines.flow.Flow

interface WidgetListPresenter {

    suspend fun run(): Flow<ViewModel>

    data class ViewModel(
        val currentFacility: FacilityViewModel?,
        val otherFacilities: List<FacilityViewModel>,
        val groups: List<WidgetGroupModel>
    )

    interface FacilityViewModel {
        val name: String
        val cover: String
        fun select()
    }
}