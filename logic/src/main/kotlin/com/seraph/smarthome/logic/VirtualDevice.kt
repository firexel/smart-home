package com.seraph.smarthome.logic

/**
 * Created by aleksandr.naumov on 28.12.17.
 */
interface VirtualDevice {

    fun configure(visitor: Visitor)

    interface Visitor {
        fun declareBoolInput(id: String, name: String): Observable<Boolean>

        fun declareBoolOutput(id: String, name: String): Updatable<Boolean>

        fun declareIndicator(id: String, purpose: Purpose): Updatable<Boolean>

        fun declareAction(id: String, purpose: Purpose): Observable<Unit>
    }

    enum class Purpose {
        MAIN, PRIMARY, SECONDARY
    }

    interface Updatable<in T> {
        fun use(source: () -> T)
        fun invalidate()
    }

    interface Observable<out T> {
        fun observe(observer: (T) -> Unit)
    }
}