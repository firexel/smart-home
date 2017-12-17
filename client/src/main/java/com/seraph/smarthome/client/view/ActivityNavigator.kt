package com.seraph.smarthome.client.view

import android.app.Activity
import android.content.Intent
import com.seraph.smarthome.client.presentation.Navigator
import com.seraph.smarthome.client.presentation.Screen

class ActivityNavigator(private val activity: Activity) : Navigator {

    companion object {
        private val EXTRA_SCREEN = "screen"
    }

    override fun show(screen: Screen) {
        val intent = screen.acceptVisitor(IntentVisitor())
        intent.putExtra(EXTRA_SCREEN, screen)
        activity.startActivity(intent)
    }

    override fun <T> getCurrentScreen(): T {
        return activity.intent.getSerializableExtra(EXTRA_SCREEN) as T
    }

    override fun goBack() {
        activity.finish()
    }

    private inner class IntentVisitor : Screen.Visitor<Intent> {
        override fun sceneScreenVisited(): Intent
                = Intent(activity, SceneActivity::class.java)

        override fun newBrokerScreenVisited(): Intent
                = Intent(activity, NewBrokerActivity::class.java)

        override fun brokersListScreenVisited(): Intent
                = Intent(activity, BrokersActivity::class.java)
    }
}