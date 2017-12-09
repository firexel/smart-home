package com.seraph.smarthome.client.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.seraph.smarthome.broker.R
import com.seraph.smarthome.client.app.PresenterFactory
import com.seraph.smarthome.client.presentation.BrokersPresenter

class BrokersActivity : AppCompatActivity(), BrokersPresenter.View {

    private val brokersAdapter = BrokersAdapter()
    private var presenter: BrokersPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brokers)
        with(findViewById<RecyclerView>(R.id.list_brokers)) {
            layoutManager = LinearLayoutManager(
                    this@BrokersActivity,
                    LinearLayoutManager.VERTICAL,
                    false
            )
            adapter = brokersAdapter
        }
        presenter = PresenterFactory(this).createBrokersPresenter(this)
    }

    override fun onResume() {
        super.onResume()
        presenter?.onRefresh()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_brokers, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.button_add_broker -> {
                presenter?.onAddNewBroker()
                true // return
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showBrokers(brokers: List<BrokersPresenter.BrokerViewModel>) {
        brokersAdapter.brokersList = brokers
    }

    override fun showError(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    inner class BrokersAdapter : RecyclerView.Adapter<BrokersAdapter.ViewHolder>() {

        var brokersList: List<BrokersPresenter.BrokerViewModel> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount(): Int = brokersList.size

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent!!.context).inflate(
                    R.layout.item_broker, parent, false
            ))
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            holder!!.bind(brokersList[position])
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val nameText = view.findViewById<TextView>(R.id.text_name)

            fun bind(broker: BrokersPresenter.BrokerViewModel) {
                nameText.text = broker.name
                itemView.setOnClickListener {
                    presenter?.onBrokerSelected(broker)
                }
            }
        }
    }
}