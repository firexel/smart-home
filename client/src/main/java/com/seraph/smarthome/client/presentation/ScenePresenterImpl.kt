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

    private val credentials = navigator.getCurrentScreen<ScenePresenter.SceneScreen>().credentials
    private var devices: List<Device> = emptyList()

    init {
        useCaseFactory.observeDevices().execute(credentials)
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    it.filter { it.controls.isNotEmpty() }
                }
                .scan(Pair(emptyList<Device>(), emptyList<Device>())) { old, new ->
                    Pair(old.second, new)
                }
                .map {
                    DiffedChange(it.second, calculateDiff(it))
                }
                .subscribe {
                    devices = it.list
                    view.showDevices(it.list.map { extractViewModelFrom(it) }, it.diff)
                }

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

    override fun onGoingBack() {
        navigator.goBack()
    }

    private fun calculateDiff(change: Pair<List<Device>, List<Device>>) =
            DiffUtil.calculateDiff(CollectionComparator(change.first, change.second), true)

    private fun extractViewModelFrom(device: Device): ScenePresenter.DeviceViewModel {
        val mainControls = device.controls.filter { it.priority == Control.Priority.MAIN }
        return ScenePresenter.DeviceViewModel(
                device.id.value,
                mainControls.map { it.usage.accept(ActionVisitor(device.id)) }
                        .firstOrNull { it != null },
                mainControls.map { it.usage.accept(IndicatorStateVisitor()) }
                        .firstOrNull { it != null }
        )
    }

    private fun <T> getEndpointState(endpoint: Endpoint<T>): T? {
        return null
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

    inner class ActionVisitor(private val deviceId: Device.Id) : Control.Usage.Visitor<(() -> Any)?> {
        override fun onButton(trigger: Endpoint<Unit>, alert: String) = {
            useCaseFactory.publishEndpoint(deviceId, trigger, Unit)
                    .execute(credentials)
                    .subscribe()
        }

        override fun onIndicator(source: Endpoint<Boolean>) = null
    }

    inner class IndicatorStateVisitor : Control.Usage.Visitor<String?> {
        override fun onButton(trigger: Endpoint<Unit>, alert: String): String? = null
        override fun onIndicator(source: Endpoint<Boolean>): String? {
            return getEndpointState(source)?.toOnOff() ?: "---"
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
}

private fun Boolean.toOnOff() = when (this) {
    true -> "On"
    else -> "Off"
}
