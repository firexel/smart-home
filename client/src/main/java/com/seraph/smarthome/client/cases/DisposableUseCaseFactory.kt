package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.*
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.client.presentation.UseCaseFactory
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

    override fun observeBrokerMetadata(): UseCase<BrokerCredentials, Metadata>
            = wrapped.observeBrokerMetadata().disposable()

    override fun observeDevices(): UseCase<BrokerCredentials, List<Device>>
            = wrapped.observeDevices().disposable()

    override fun observeConnectionState(): UseCase<BrokerCredentials, BrokerConnection.State>
            = wrapped.observeConnectionState().disposable()

    override fun <T> changePropertyValue(deviceId: Device.Id, property: Property<T>, value: T): UseCase<BrokerCredentials, Unit>
            = wrapped.changePropertyValue(deviceId, property, value).disposable()

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