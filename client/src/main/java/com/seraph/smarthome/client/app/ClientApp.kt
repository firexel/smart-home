package com.seraph.smarthome.client.app

import android.app.Activity
import android.app.Application
import android.content.Context
import com.seraph.smarthome.client.cases.BrokerRepo
import com.seraph.smarthome.client.cases.DisposableUseCaseFactory
import com.seraph.smarthome.client.cases.ProductionUseCaseFactory
import com.seraph.smarthome.client.model.BrokersInfoRepo
import com.seraph.smarthome.client.model.DatabaseBrokersRepo
import com.seraph.smarthome.client.model.MqttBrokerRepo
import com.seraph.smarthome.client.presentation.UseCaseFactory
import com.seraph.smarthome.client.view.ActivityNavigator
import com.seraph.smarthome.client.view.PresenterFactory

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
class ClientApp : Application() {

    companion object {
        val Context.app: ClientApp
            get() = applicationContext as ClientApp


        val Activity.presenters: PresenterFactory
            get() = app.obtainPresentersFactory(this)
    }

    private val log = AdbLog()
    private val lifecycle = ActivityDestroyListener()

    private lateinit var useCaseFactory: UseCaseFactory
    private lateinit var brokerRepo: BrokerRepo
    private lateinit var infoRepo: BrokersInfoRepo

    override fun onCreate() {
        super.onCreate()
        brokerRepo = MqttBrokerRepo(log.copy("BrokerRepo"))
        infoRepo = DatabaseBrokersRepo(this)
        useCaseFactory = ProductionUseCaseFactory(infoRepo, brokerRepo)
        registerActivityLifecycleCallbacks(lifecycle)
    }

    private fun newUseCaseFactory(activity: Activity): UseCaseFactory {
        val factory = DisposableUseCaseFactory(useCaseFactory)
        lifecycle.doWhenDestroyed(activity) { factory.dispose() }
        return factory
    }

    private fun obtainPresentersFactory(activity: Activity): PresenterFactory
            = PresenterFactoryImpl(newUseCaseFactory(activity), ActivityNavigator(activity))
}