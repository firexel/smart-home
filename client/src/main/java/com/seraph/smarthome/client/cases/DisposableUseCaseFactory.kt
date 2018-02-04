package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.model.BrokerInfo
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.client.presentation.UseCaseFactory
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Metainfo
import io.reactivex.disposables.Disposable

/**
 * Created by aleksandr.naumov on 10.01.18.
 */
class DisposableUseCaseFactory(private val wrapped: UseCaseFactory) : UseCaseFactory {

    private val disposables = mutableListOf<Disposable>()

    override fun addBroker(): UseCase<BrokerCredentials, Unit>
            = wrapped.addBroker().disposable()

    override fun listBrokersSettings(): UseCase<Unit, List<BrokerInfo>>
            = wrapped.listBrokersSettings().disposable()

    override fun observeDevices(): UseCase<BrokerCredentials, List<Device>>
            = wrapped.observeDevices().disposable()

    override fun observeConnectionState(): UseCase<BrokerCredentials, BrokerConnection.State>
            = wrapped.observeConnectionState().disposable()

    override fun observeBrokerMetainfo(): UseCase<BrokerCredentials, Metainfo>
            = wrapped.observeBrokerMetainfo().disposable()

    override fun <T> publishEndpoint(deviceId: Device.Id, endpoint: Endpoint<T>, value: T): UseCase<BrokerCredentials, Unit>
            = wrapped.publishEndpoint(deviceId, endpoint, value).disposable()

    override fun <T> subscribeEndpoint(deviceId: Device.Id, endpoint: Endpoint<T>): UseCase<BrokerCredentials, T>
            = wrapped.subscribeEndpoint(deviceId, endpoint).disposable()

    private fun <P, R> UseCase<P, R>.disposable(): UseCase<P, R> {
        val wrapper = DisposableUseCase(this)
        disposables.add(wrapper)
        return wrapper
    }

    fun dispose() {
        disposables.forEach { it.dispose() }
        disposables.clear()
    }
}