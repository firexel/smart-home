package com.seraph.smarthome.client.app

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Created by aleksandr.naumov on 13.01.18.
 */
class ActivityDestroyListener : Application.ActivityLifecycleCallbacks {

    private val callbacks = mutableMapOf<Activity, MutableList<(Activity) -> Unit>>()

    override fun onActivityPaused(activity: Activity?) = Unit
    override fun onActivityResumed(activity: Activity?) = Unit
    override fun onActivityStarted(activity: Activity?) = Unit
    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) = Unit
    override fun onActivityStopped(activity: Activity?) = Unit
    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        callbacks.getOrDefault(activity, mutableListOf()).forEach { it(activity) }
        callbacks.remove(activity)
    }

    fun doWhenDestroyed(activity: Activity, action: (Activity) -> Unit) {
        callbacks.getOrPut(activity) { mutableListOf() }.add(action)
    }
}