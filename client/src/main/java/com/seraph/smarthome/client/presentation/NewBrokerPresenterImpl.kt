package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.model.BrokerCredentials
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class NewBrokerPresenterImpl(
        private val view: NewBrokerPresenter.View,
        private val useCaseFactory: UseCaseFactory,
        private val navigator: Navigator
) : NewBrokerPresenter {

    override fun onAddBroker(hostname: String, port: Int) {
        useCaseFactory.addBroker().execute(BrokerCredentials(hostname, port))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { }
                .subscribe(object : Observer<Unit> {
                    override fun onNext(t: Unit) = Unit
                    override fun onSubscribe(d: Disposable) = Unit

                    override fun onComplete() {
                        navigator.showPreviousScreen()
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        view.showAddError(e.localizedMessage)
                    }
                })
    }
}