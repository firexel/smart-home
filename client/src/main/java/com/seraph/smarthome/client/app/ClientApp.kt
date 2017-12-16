package com.seraph.smarthome.client.app

import android.app.Application
import com.seraph.smarthome.client.cases.BrokerRepo
import com.seraph.smarthome.client.cases.ProductionUseCaseFactory
import com.seraph.smarthome.client.model.BrokersSettingsRepo
import com.seraph.smarthome.client.model.DatabaseBrokersRepo
import com.seraph.smarthome.client.model.MqttBrokerRepo
import com.seraph.smarthome.client.presentation.UseCaseFactory
import com.seraph.smarthome.client.view.PresenterFactory
import com.seraph.smarthome.model.Log

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
class ClientApp : Application() {
    lateinit var presenterFactory: PresenterFactory
        private set

    lateinit var useCaseFactory: UseCaseFactory
        private set

    private lateinit var brokerRepo: BrokerRepo
    private lateinit var settingsRepo: BrokersSettingsRepo

    private val log = AdbLog()

    override fun onCreate() {
        super.onCreate()
        brokerRepo = MqttBrokerRepo(log)
        settingsRepo = DatabaseBrokersRepo(this)

        useCaseFactory = ProductionUseCaseFactory(settingsRepo, brokerRepo)
        presenterFactory = PresenterFactoryImpl(useCaseFactory)
    }

    class AdbLog : Log {
        override fun i(message: String) {
            android.util.Log.i("ClientApp", message)
        }

        override fun w(message: String) {
            android.util.Log.w("ClientApp", message)
        }
    }
}