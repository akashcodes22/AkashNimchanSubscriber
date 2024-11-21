package com.example.akashnimchansubscriber.adapters
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.akashnimchansubscriber.R
import com.example.akashnimchansubscriber.models.PubModel

class PubAdapter(private val publisherList: MutableList<PubModel> ) : RecyclerView.Adapter<PubAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentIDView: TextView = itemView.findViewById(R.id.studentIDField)
        val minSpeedView: TextView = itemView.findViewById(R.id.minSpeedText)
        val maxSpeedView: TextView = itemView.findViewById(R.id.maxSpeedText)
        val buttonView: Button = itemView.findViewById(R.id.viewMoreButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_publisher, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val publisher = publisherList[position]
        val textMin = "min speed: " + String.format("%.1f", publisher.minSpeed) + " km/h"
        val textMax = "max speed: " + String.format("%.1f", publisher.maxSpeed) + " km/h"

        holder.studentIDView.text = publisher.studentID
        holder.minSpeedView.text = textMin
        holder.maxSpeedView.text = textMax
    }

    override fun getItemCount(): Int {
        return publisherList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updatePublisherList(newPublisherList: MutableList<PubModel>) {
        publisherList.clear()
        publisherList.addAll(newPublisherList)
        notifyDataSetChanged()
    }
}