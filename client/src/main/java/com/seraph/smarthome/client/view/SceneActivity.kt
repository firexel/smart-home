package com.seraph.smarthome.client.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.seraph.smarthome.broker.R
import com.seraph.smarthome.client.app.ClientApp.Companion.presenters
import com.seraph.smarthome.client.presentation.ScenePresenter

class SceneActivity : AppCompatActivity(), ScenePresenter.View {

    private val devicesAdapter = DevicesAdapter()
    private var presenter: ScenePresenter? = null
    private var brokerNameText: TextView? = null
    private var connectionStateName: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scene)
        with(findViewById<Toolbar>(R.id.toolbar)) {
            setSupportActionBar(this)
            brokerNameText = findViewById(R.id.text_broker_name)
            connectionStateName = findViewById(R.id.text_connection_state)
            setNavigationOnClickListener {
                presenter?.onGoingBack()
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        with(findViewById<RecyclerView>(R.id.list_devices)) {
            layoutManager = LinearLayoutManager(
                    this@SceneActivity,
                    LinearLayoutManager.VERTICAL,
                    false
            )
            adapter = devicesAdapter
        }
        presenter = presenters.createScenePresenter(this)
    }

    override fun showDevices(devices: List<ScenePresenter.DeviceViewModel>, diff: DiffUtil.DiffResult) {
        devicesAdapter.update(devices, diff)
    }

    override fun showConnectionStatus(status: String) {
        connectionStateName?.text = status
    }

    override fun showBrokerName(name: String) {
        brokerNameText?.text = name
    }

    inner class DevicesAdapter : RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

        private var devicesList: List<ScenePresenter.DeviceViewModel> = emptyList()

        fun update(devices: List<ScenePresenter.DeviceViewModel>, diff: DiffUtil.DiffResult) {
            devicesList = devices
            diff.dispatchUpdatesTo(this)
        }

        override fun getItemCount(): Int = devicesList.size

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent!!.context).inflate(
                    R.layout.item_device, parent, false
            ))
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            holder!!.bind(devicesList[position])
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val nameText = view.findViewById<TextView>(R.id.text_name)
            private val indicatorText = view.findViewById<TextView>(R.id.text_main_indicator)

            fun bind(device: ScenePresenter.DeviceViewModel) {
                bindName(device.name)
                bindMainIndicator(device.mainIndicator)
                bindMainAction(device.mainAction)
            }

            private fun bindName(name: String) {
                nameText.text = name
            }

            private fun bindMainAction(mainAction: (() -> Any)?) {
                if (mainAction != null) {
                    itemView.setOnClickListener {
                        mainAction()
                    }
                } else {
                    itemView.setOnClickListener(null)
                }
            }

            private fun bindMainIndicator(mainIndicatorValue: String?) {
                if (mainIndicatorValue != null) {
                    indicatorText.visibility = View.VISIBLE
                    indicatorText.text = mainIndicatorValue
                } else {
                    indicatorText.visibility = View.GONE
                }
            }
        }
    }
}

