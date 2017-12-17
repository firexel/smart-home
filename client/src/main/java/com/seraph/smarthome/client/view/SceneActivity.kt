package com.seraph.smarthome.client.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.seraph.smarthome.broker.R
import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.presentation.ScenePresenter

class SceneActivity : AppCompatActivity(), ScenePresenter.View {

    private val actionsAdapter = ActionsAdapter()
    private var presenter: ScenePresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scene)
        with(findViewById<RecyclerView>(R.id.list_actions)){
            layoutManager = LinearLayoutManager(
                    this@SceneActivity,
                    LinearLayoutManager.VERTICAL,
                    false)
            adapter = actionsAdapter
        }
        presenter = PresenterFactory.from(this).createScenePresenter(this, getParams())
    }

    private fun getParams(): BrokerCredentials {
        return intent.getSerializableExtra("param") as BrokerCredentials
    }

    override fun onShowActions(actions: Collection<ScenePresenter.ActionViewModel>) {
        actionsAdapter.actionsList = actions.toList()
    }

    inner class ActionsAdapter : RecyclerView.Adapter<ActionsAdapter.ViewHolder>() {

        var actionsList: List<ScenePresenter.ActionViewModel> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount(): Int = actionsList.size

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent!!.context).inflate(
                    R.layout.item_action, parent, false
            ))
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            holder!!.bind(actionsList[position])
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val nameText = view.findViewById<TextView>(R.id.text_name)
            private val valueText = view.findViewById<TextView>(R.id.text_value)

            fun bind(action: ScenePresenter.ActionViewModel) {
                nameText.text = action.name
                valueText.text = action.value
                itemView.setOnClickListener {
                    presenter?.onActionPerformed(action.id)
                }
            }
        }
    }
}

