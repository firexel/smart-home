package com.seraph.smarthome.client.presentation

import io.reactivex.Observable

interface UseCase<in P, R> {
    fun execute(params:P): Observable<R>
}