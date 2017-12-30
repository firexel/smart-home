package com.seraph.smarthome.client.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.seraph.smarthome.broker.R
import com.seraph.smarthome.client.presentation.ScenePresenter

class SceneActivity : AppCompatActivity(), ScenePresenter.View {

    private val devicesAdapter = DevicesAdapter()
    private var presenter: ScenePresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scene)
        with(findViewById<RecyclerView>(R.id.list_devices)) {
            layoutManager = LinearLayoutManager(
                    this@SceneActivity,
                    LinearLayoutManager.VERTICAL,
                    false
            )
            adapter = devicesAdapter
        }
        presenter = PresenterFactory.from(this)
                .createScenePresenter(this, ActivityNavigator(this))
    }

    override fun onShowDevices(devices: List<ScenePresenter.DeviceViewModel>, diff: DiffUtil.DiffResult) {
        devicesAdapter.update(devices, diff)
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
                bindMainIndicator(device.mainIndicatorValue)
                bindMainAction(device.mainActionId, device.id)
            }

            private fun bindName(name: String) {
                nameText.text = name
            }

            private fun bindMainAction(mainActionId: String?, deviceId: String) {
                if (mainActionId != null) {
                    itemView.setOnClickListener {
                        presenter?.onDeviceActionPerformed(deviceId, mainActionId)
                    }
                } else {
                    itemView.setOnClickListener(null)
                }
            }

            private fun bindMainIndicator(mainIndicatorValue: Boolean?) {
                if (mainIndicatorValue != null) {
                    indicatorText.visibility = View.VISIBLE
                    indicatorText.text = if (mainIndicatorValue) "On" else "Off"
                } else {
                    indicatorText.visibility = View.GONE
                }
            }
        }

    }
}

