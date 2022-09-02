package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.app.Services
import com.seraph.smarthome.client.model.Facility
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetListPresenterImpl(private val services: Services) : WidgetListPresenter {

    private val listInteractor = services.facilityListInteractor()

    override suspend fun run(): Flow<WidgetListPresenter.ViewModel> {
        listInteractor.refresh()
        return listInteractor.currentFacility
            .flatMapLatest { selectedId ->
                listInteractor.run()
                    .map { facilities ->
                        FacilitiesSnapshot(
                            current = facilities.find { it.id == selectedId },
                            otherFacilities = facilities.filter { it.id != selectedId }
                        )
                    }
            }
            .flatMapLatest { snapshot ->
                if (snapshot.current == null) {
                    flow {
                        emit(
                            WidgetListPresenter.ViewModel(
                                currentFacility = null,
                                otherFacilities = snapshot.otherFacilities.map { it.toViewModel() },
                                groups = emptyList()
                            )
                        )
                    }
                } else {
                    services.widgetListInteractor(snapshot.current).run()
                        .map { groups ->
                            WidgetListPresenter.ViewModel(
                                currentFacility = snapshot.current.toViewModel(),
                                otherFacilities = snapshot.otherFacilities.map { it.toViewModel() },
                                groups = groups
                            )
                        }
                }
            }
    }

    fun Facility.toViewModel(): WidgetListPresenter.FacilityViewModel {
        return object : WidgetListPresenter.FacilityViewModel {
            override val name: String
                get() = this@toViewModel.name
            override val cover: String
                get() = this@toViewModel.cover

            override fun select() {
                listInteractor.setCurrent(this@toViewModel)
            }
        }
    }

    private data class FacilitiesSnapshot(
        val current: Facility?,
        val otherFacilities: List<Facility>
    )
}