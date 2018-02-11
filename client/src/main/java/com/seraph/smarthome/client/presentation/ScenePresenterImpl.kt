package com.seraph.smarthome.client.presentation

import android.support.v7.util.DiffUtil
import com.seraph.smarthome.client.cases.BrokerConnection
import com.seraph.smarthome.domain.Control
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class ScenePresenterImpl(
        private val view: ScenePresenter.View,
        private val useCaseFactory: UseCaseFactory,
        private val navigator: Navigator)
    : ScenePresenter {

    private val lock = Any()
    private val credentials = navigator.getCurrentScreen<ScenePresenter.SceneScreen>().credentials
    private var devices: List<Device> = emptyList()
    private val states = mutableMapOf<GlobalEndpointId, Any?>()

    init {
        useCaseFactory.observeDevices().execute(credentials)
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.filter { it.controls.isNotEmpty() } }
                .doOnNext { synchronized(lock) { devices = it } }
                .map { it.map { extractViewModelFrom(it) } }
                .scan(pairOfEmpty()) { old, new -> Pair(old.second, new) }
                .map { DiffedChange(it.second, calculateDiff(it)) }
                .subscribe { view.showDevices(it.list, it.diff) }

        useCaseFactory.observeConnectionState().execute(credentials)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { it.accept(UpdateVisitor(it)) }
                .subscribe {
                    view.showConnectionStatus(it.accept(ConnectionStatusNameVisitor()))
                }

        useCaseFactory.observeBrokerMetainfo().execute(credentials)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.showBrokerName(it.brokerName) }
    }

    private fun pairOfEmpty() =
            Pair(emptyList<ScenePresenter.DeviceViewModel>(), emptyList<ScenePresenter.DeviceViewModel>())

    override fun onGoingBack() {
        navigator.goBack()
    }

    private fun calculateDiff(change: Pair<List<ScenePresenter.DeviceViewModel>, List<ScenePresenter.DeviceViewModel>>) =
            DiffUtil.calculateDiff(CollectionComparator(change.first, change.second), true)

    private fun extractViewModelFrom(device: Device): ScenePresenter.DeviceViewModel {
        val mainControls = device.controls.filter { it.priority == Control.Priority.MAIN }
        return ScenePresenter.DeviceViewModel(
                device.id.value,
                device.id.value,
                mainControls.map { it.usage.accept(ActionVisitor(device.id)) }
                        .firstOrNull { it != null },
                mainControls.map { it.usage.accept(IndicatorStateVisitor(device.id)) }
                        .firstOrNull { it != null }
        )
    }

    private fun getEndpointState(device: Device.Id, endpoint: Endpoint<*>): Any? {
        val key = GlobalEndpointId(device, endpoint.id)
        synchronized(lock) {
            if (!states.containsKey(key)) {
                states[key] = null
                useCaseFactory.observeEndpoint(device, endpoint)
                        .execute(credentials)
                        .doOnNext { synchronized(lock) { states[key] = it } }
                        .map { synchronized(lock) { devices.map { extractViewModelFrom(it) } } }
                        .map { DiffedChange(it, calculateDiff(Pair(it, it))) }
                        .subscribe { view.showDevices(it.list, it.diff) }
            }
            return states[key]
        }
    }

    class CollectionComparator(
            private val oldOne: List<ScenePresenter.DeviceViewModel>,
            private val newOne: List<ScenePresenter.DeviceViewModel>)
        : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldOne.size

        override fun getNewListSize(): Int = newOne.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean
                = oldOne[oldItemPosition].sameAs(newOne[newItemPosition])

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean
                = oldOne[oldItemPosition] == newOne[newItemPosition]
    }

    data class DiffedChange(val list: List<ScenePresenter.DeviceViewModel>, val diff: DiffUtil.DiffResult)

    inner class ActionVisitor(private val deviceId: Device.Id) : Control.Usage.Visitor<(() -> Any)?> {
        override fun onButton(trigger: Endpoint<Unit>, alert: String) = {
            useCaseFactory.publishEndpoint(deviceId, trigger, Unit)
                    .execute(credentials)
                    .subscribe()
        }

        override fun onIndicator(source: Endpoint<Boolean>) = null
    }

    inner class IndicatorStateVisitor(private val device: Device.Id) : Control.Usage.Visitor<String?> {
        override fun onButton(trigger: Endpoint<Unit>, alert: String): String? = null
        override fun onIndicator(source: Endpoint<Boolean>): String? {
            return getEndpointState(device, source)?.let { toOnOff(it as Boolean) } ?: "---"
        }
    }

    class ConnectionStatusNameVisitor : BrokerConnection.State.Visitor<String> {
        override fun onConnectedState(): String = "Connected"
        override fun onDisconnectedState(): String = "Disconnected"
        override fun onDisconnectingState(): String = "Disconnecting..."
        override fun onConnectingState(): String = "Connecting..."
        override fun onWaitingState(msToReconnect: Long): String
                = "Reconnecting in ${Math.round(msToReconnect / 1000.0)}..."
    }

    class UpdateVisitor(private val state: BrokerConnection.State)
        : BrokerConnection.State.Visitor<Observable<BrokerConnection.State>> {
        override fun onConnectedState(): Observable<BrokerConnection.State> = Observable.just(state)
        override fun onDisconnectedState(): Observable<BrokerConnection.State> = Observable.just(state)
        override fun onDisconnectingState(): Observable<BrokerConnection.State> = Observable.just(state)
        override fun onConnectingState(): Observable<BrokerConnection.State> = Observable.just(state)
        override fun onWaitingState(msToReconnect: Long): Observable<BrokerConnection.State>
                = Observable.intervalRange(
                0,
                Math.round(msToReconnect / 1000.0),
                0,
                1, TimeUnit.SECONDS
        ).map { state }
    }

    private fun toOnOff(boolean: Boolean) = when (boolean) {
        true -> "On"
        else -> "Off"
    }

    private data class GlobalEndpointId(
            val device: Device.Id,
            val endpoint: Endpoint.Id
    )
}

