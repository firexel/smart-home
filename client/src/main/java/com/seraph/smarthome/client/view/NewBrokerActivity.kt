package com.seraph.smarthome.client.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import com.seraph.smarthome.broker.R
import com.seraph.smarthome.client.app.ActivityNavigator
import com.seraph.smarthome.client.app.PresenterFactory
import com.seraph.smarthome.client.presentation.NewBrokerPresenter

/**
 * A login screen that offers login via email/password.
 */
class NewBrokerActivity : AppCompatActivity(), NewBrokerPresenter.View {

    private var presenter:NewBrokerPresenter? = null
    private lateinit var textHostname:EditText
    private lateinit var textPort:EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_broker)
        textHostname = findViewById(R.id.edit_hostname)
        textPort = findViewById(R.id.edit_port)
        presenter = PresenterFactory.from(this)
                .createNewBrokerPresenter(this, ActivityNavigator(this))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_new_broker, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.button_check_and_add -> { addBroker(); return true }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun addBroker() {
        val hostname = textHostname.text.toString()
        val port = textPort.text.toString().toInt()
        presenter?.onAddBroker(hostname, port)
    }

    override fun showAddError() {
        Toast.makeText(this, "Cannot add broker", Toast.LENGTH_SHORT).show()
    }
}
