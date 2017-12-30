package com.seraph.smarthome.client.presentation

import android.support.v7.util.DiffUtil
import com.seraph.smarthome.client.model.ActionProperty
import com.seraph.smarthome.client.model.Device
import com.seraph.smarthome.client.model.IndicatorProperty
import com.seraph.smarthome.client.model.Property
import com.seraph.smarthome.client.util.onNextObserver
import io.reactivex.android.schedulers.AndroidSchedulers

class ScenePresenterImpl(
        private val view: ScenePresenter.View,
        private val useCaseFactory: UseCaseFactory,
        navigator: Navigator)
    : ScenePresenter {

    private val credentials = navigator.getCurrentScreen<ScenePresenter.SceneScreen>().credentials
    private var devices: List<Device> = emptyList()

    init {
        useCaseFactory.listDevices().execute(credentials)
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    it.filter { it.properties.isNotEmpty() }
                }
                .scan(Pair(emptyList<Device>(), emptyList<Device>())) { old, new ->
                    Pair(old.second, new)
                }
                .map {
                    DiffedChange(it.second, calculateDiff(it))
                }
                .subscribe(onNextObserver {
                    devices = it.list
                    view.onShowDevices(it.list.map { extractViewModelFrom(it) }, it.diff)
                })
    }

    private fun calculateDiff(change: Pair<List<Device>, List<Device>>) =
            DiffUtil.calculateDiff(CollectionComparator(change.first, change.second), true)

    private fun extractViewModelFrom(device: Device) = ScenePresenter.DeviceViewModel(
            device.id.hash,
            device.name,
            device.properties
                    .filter { it.priority == Property.Priority.MAIN }
                    .map { it.accept(ActionIdVisitor()) }
                    .firstOrNull { it != null },
            device.properties
                    .filter { it.priority == Property.Priority.MAIN }
                    .map { it.accept(IndicatorStateVisitor()) }
                    .firstOrNull { it != null }
    )

    override fun onDeviceActionPerformed(deviceId: String, actionId: String) {
        val device = devices.find { it.id == Device.Id(deviceId) }
        device?.properties
                ?.find { it.id == Property.Id(actionId) }
                ?.accept(FireActionVisitor(device.id))
    }

    class CollectionComparator(
            private val oldOne: List<Device>,
            private val newOne: List<Device>)
        : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldOne.size

        override fun getNewListSize(): Int = newOne.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean
                = oldOne[oldItemPosition].id == newOne[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean
                = oldOne[oldItemPosition] == newOne[newItemPosition]
    }

    data class DiffedChange(val list: List<Device>, val diff: DiffUtil.DiffResult)

    class ActionIdVisitor : Property.Visitor<String?> {
        override fun onIndicatorVisited(property: IndicatorProperty): String? = null
        override fun onActionVisited(property: ActionProperty): String? = property.id.hash
    }

    class IndicatorStateVisitor : Property.Visitor<Boolean?> {
        override fun onIndicatorVisited(property: IndicatorProperty): Boolean? = property.value
        override fun onActionVisited(property: ActionProperty): Boolean? = null
    }

    inner class FireActionVisitor(private val deviceId: Device.Id) : Property.Visitor<Unit> {
        override fun onIndicatorVisited(property: IndicatorProperty) = Unit
        override fun onActionVisited(property: ActionProperty) {
            useCaseFactory
                    .changePropertyValue(deviceId, property, Unit)
                    .execute(credentials)
                    .subscribe()
        }
    }
}