package com.seraph.smarthome.client.util

import io.reactivex.Observer
import io.reactivex.disposables.Disposable

/**
 * Created by aleksandr.naumov on 29.12.17.
 */
abstract class BaseObserver<T> : Observer<T> {
    override fun onSubscribe(d: Disposable) {
    }

    override fun onError(e: Throwable) {
    }

    override fun onComplete() {
    }

    override fun onNext(t: T) {
    }
}
