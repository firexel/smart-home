package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.presentation.UseCase
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.*

/**
 * Created by aleksandr.naumov on 08.01.18.
 */
class DisposableUseCase<in P, R>(private val wrapped: UseCase<P, R>) : UseCase<P, R>, Disposable {

    private val disposables = Collections.synchronizedList(mutableListOf<Disposable>())

    override fun execute(params: P): Observable<R> {
        val disposable = DisposableObservable(wrapped.execute(params))
        disposables.add(disposable)
        return disposable
    }

    override fun isDisposed(): Boolean = disposables.all { it.isDisposed }

    override fun dispose() = disposables.forEach { it.dispose() }

    private class DisposableObservable<T>(private val wrapped: Observable<T>) : Observable<T>(), Disposable {

        private val disposables = Collections.synchronizedList(mutableListOf<Disposable>())

        override fun subscribeActual(observer: Observer<in T>) {
            wrapped.subscribe(object : Observer<T> {
                override fun onSubscribe(d: Disposable) {
                    disposables.add(d)
                    observer.onSubscribe(d)
                }

                override fun onNext(t: T) {
                    observer.onNext(t)
                }

                override fun onComplete() {
                    observer.onComplete()
                }

                override fun onError(e: Throwable) {
                    observer.onError(e)
                }
            })
        }

        override fun isDisposed(): Boolean = disposables.all { it.isDisposed }

        override fun dispose() = disposables.forEach { it.dispose() }
    }
}