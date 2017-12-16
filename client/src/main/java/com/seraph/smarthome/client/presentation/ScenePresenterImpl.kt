package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.model.Device
import com.seraph.smarthome.model.Endpoint
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class ScenePresenterImpl(
        view: ScenePresenter.View,
        useCaseFactory: UseCaseFactory,
        settings: BrokerSettings) : ScenePresenter {

    init {
        useCaseFactory.listDevices().execute(settings)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Collection<Device>> {
                    override fun onNext(devices: Collection<Device>) {
                        view.onShowActions(mapFromDevices(devices))
                    }

                    override fun onComplete() = Unit
                    override fun onSubscribe(d: Disposable) = Unit
                    override fun onError(e: Throwable) = Unit
                })
    }

    private fun mapFromDevices(devices: Collection<Device>): Collection<ScenePresenter.ActionViewModel> {
        val models = mutableListOf<ScenePresenter.ActionViewModel>()
        devices.forEach {
            models += extractActionsFrom(it)
        }
        return models
    }

    private fun extractActionsFrom(device: Device): List<ScenePresenter.ActionViewModel> {
        return device.properties
                .filter { it.type == Endpoint.Type.BOOLEAN }
                .map {
                    ScenePresenter.ActionViewModel(
                            "${device.id}:${it.id}",
                            "${device.name} > ${it.name}",
                            "OFF"
                    )
                }
    }

    override fun onActionPerformed(actionId: String) {

    }
}